/*
 * Copyright 2005-2012 WSO2, Inc. http://www.wso2.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.cloud.heartbeat.monitoring.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.cloud.heartbeat.monitoring.ui.configuration.Node;
import org.wso2.cloud.heartbeat.monitoring.ui.configuration.parser.nginx.NodeBuilder;
import org.wso2.cloud.heartbeat.monitoring.ui.utils.CaseConverter;
import org.wso2.cloud.heartbeat.monitoring.ui.utils.DataAccess;
import org.wso2.cloud.heartbeat.monitoring.ui.utils.FileManager;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class ServicesStatusRetriever {

    private static final Log log = LogFactory.getLog(ServicesStatusRetriever.class);

    private ArrayList <ServiceHealth> serviceHealths;
    private Node rootNode;
    private String configPath;
    private DataAccess dataAccess;

    public ServicesStatusRetriever(String path){
        this.rootNode = new Node();
        this.configPath = path;
        this.serviceHealths = new ArrayList<ServiceHealth>();
    }

    public ArrayList<ServiceHealth> getServiceHealths() {
        populate();
        return serviceHealths;
    }

    private void populate(){
        try {
            NodeBuilder.buildNode(rootNode, FileManager.readFile(configPath));
            dataAccess = new DataAccess(rootNode.findChildNodeByName("data_source"));
            for (Node node : rootNode.findChildNodeByName("modules").getChildNodes()) {
               retrieve(node);
            }
            dataAccess.closeConnection();
        } catch (IOException e) {
            log.error("Heartbeat - Monitor - IOException thrown while reading the configuration file:" +
                      " near 'data_source' tag. ", e);
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while data access: ", e);
        }

    }

    public void addNotes(String serviceName, String note){
        try {
            NodeBuilder.buildNode(rootNode, FileManager.readFile(configPath));
            dataAccess = new DataAccess(rootNode.findChildNodeByName("data_source"));

            dataAccess.insertNotes(serviceName, note);
            dataAccess.closeConnection();
        } catch (IOException e) {
            log.error("Heartbeat - Monitor - IOException thrown while reading the configuration file:" +
                      " near 'data_source' tag. ", e);
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while data access: adding notes", e);
        }

    }

    public Map<Timestamp, String> getNotes(String serviceName) {
        Map<Timestamp, String> note = new HashMap<Timestamp, String>();
        try {
            NodeBuilder.buildNode(rootNode, FileManager.readFile(configPath));
            dataAccess = new DataAccess(rootNode.findChildNodeByName("data_source"));

            ResultSet resultSet = dataAccess.getLastTestDate(serviceName);

            Calendar date = null;
            if (resultSet.next()){
                date = Calendar.getInstance();
                date.setTime(resultSet.getTimestamp("DATETIME"));
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
            }
            resultSet.close();
            if(date!=null){
                resultSet = dataAccess.getCurrentNotes(serviceName, date);
                while (resultSet.next()){
                    note.put(resultSet.getTimestamp("DATETIME"), resultSet.getString("NOTE"));
                }
                resultSet.close();
            } else {
                throw new SQLException("Test last date cannot be null");
            }

            dataAccess.closeConnection();
            return note;
        } catch (IOException e) {
            log.error("Heartbeat - Monitor - IOException thrown while reading the configuration file:" +
                      " near 'data_source' tag. ", e);
            return note;
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while data access: ", e);
            return note;
        }
    }

    private void retrieve(Node node) throws SQLException {
        String serviceNameInTitleCase = CaseConverter.underscoreToTitleCase(node.getName());
        ServiceHealth serviceHealth = new ServiceHealth(CaseConverter.splitCamelCase(serviceNameInTitleCase));
        log.info(CaseConverter.splitCamelCase(serviceNameInTitleCase));
        String[] tests = (node.getProperty("tests")).split(",");
        for (String test : tests) {
            String testNameInTitleCase = CaseConverter.underscoreToTitleCase(test.trim());
            ResultSet resultSet = dataAccess.getServiceState(serviceNameInTitleCase, testNameInTitleCase);
            if(resultSet.next()){
                if(resultSet.getBoolean("STATUS")) {
                    serviceHealth.addSuccessTest(CaseConverter.splitCamelCase(testNameInTitleCase),
                            resultSet.getTimestamp("DATETIME"));
                } else {
                    serviceHealth.addFailureTest(CaseConverter.splitCamelCase(testNameInTitleCase),
                            resultSet.getTimestamp("DATETIME"));
                    ResultSet failureSet = dataAccess.getTestsFailedDetailsFromFailureDetail(serviceNameInTitleCase, testNameInTitleCase);

                    if(failureSet.next()){
                        serviceHealth.addFailureTestDetails(CaseConverter.splitCamelCase(testNameInTitleCase),
                                failureSet.getString("DETAIL"),resultSet.getTimestamp("DATETIME"));
                    }

                }
            }
            resultSet.close();
        }
        serviceHealths.add(serviceHealth);
    }
}
