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
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ServicesHistoryRetriever {

    public enum DailyState {NORMAL, DISRUPTION, DOWN, NA}
    private static final Log log = LogFactory.getLog(ServicesHistoryRetriever.class);

    private String configPath;
    private Node rootNode;
    private DataAccess dataAccess;
    private Map<String, Map<Date, DailyState>> servicesHistory;
    ArrayList<String> services;

    public ServicesHistoryRetriever (String configPath){
        this.configPath = configPath;
        rootNode = new Node();
        servicesHistory = new HashMap<String, Map<Date, DailyState>>();
        services = new ArrayList<String>();
        populate ();
    }

    public Map<String, Map<Date, DailyState>> getServicesHistory(){
        return servicesHistory;
    }


    public ArrayList <String> getServices(){
        return services;
    }

    private void populate() {
        try {
            NodeBuilder.buildNode(rootNode, FileManager.readFile(configPath));
            dataAccess = new DataAccess(rootNode.findChildNodeByName("data_source"));
            for (Node node : rootNode.findChildNodeByName("modules").getChildNodes()) {
                retrieve(node);
            }
            clearObsoleteHistory();
            dataAccess.closeConnection();
        } catch (IOException e) {
            log.error("Heartbeat - Monitor - IOException thrown while reading the configuration file:" +
                      " near 'data_source' tag. ", e);
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while data access: ", e);
        }
    }

    private void clearObsoleteHistory() throws SQLException {
        String interval = rootNode.findChildNodeByName("data_source").getProperty("clean_database");
        String flushInterval;
        try{
            if(Integer.parseInt(interval.split("d")[0].replace(" ", ""))>=35){
                flushInterval = interval.split("d")[0].replace(" ", "");
            }
            else {
                throw new Exception();
            }
        }catch (Exception e){
            log.error("Heartbeat - Monitor - Invalid interval specified for data flush, default 35 days");
            flushInterval = "35";       //Default value 35 days
        }
        dataAccess.clearObsoleteHistoryData(Integer.valueOf(flushInterval) + 1);              //to maintain 35 days
        dataAccess.clearObsoleteHistoryNotes(Integer.valueOf(flushInterval) + 1);
    }

    private void retrieve(Node node) {
        String serviceNameInTitleCase = CaseConverter.underscoreToTitleCase(node.getName());
        Map <Date, DailyState> serviceHistory = new TreeMap <Date, DailyState>();
        try {
            ResultSet resultSet = dataAccess.getServiceHistory(serviceNameInTitleCase);
            Date date = null;
            while (resultSet.next()) {
                date = resultSet.getDate("DATE");
                DailyState status = DailyState.valueOf(resultSet.getString("STATUS"));
                serviceHistory.put(date, status);
            }

            Calendar start = Calendar.getInstance();
            start.add(Calendar.DATE, -1);
            Calendar end = Calendar.getInstance();
            if(date!=null){
                resultSet.first();
                end.setTime(resultSet.getDate("DATE"));
            } else {
                end.add(Calendar.DATE, -36);
            }
            resultSet.close();

            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);

            for (; !start.before(end) && !start.getTime().equals(end.getTime()) ; start.add(Calendar.DATE, -1)) {
                Date current = new Date((start.getTime()).getTime());
                DailyState dailyState = getState(serviceNameInTitleCase, start);
                serviceHistory.put(current, dailyState);
                addToHistory(serviceNameInTitleCase, current, dailyState);
            }


            servicesHistory.put(CaseConverter.splitCamelCase(serviceNameInTitleCase), serviceHistory);
            services.add(CaseConverter.splitCamelCase(serviceNameInTitleCase));
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while data access: retrieving history data", e);
        }
    }

    private void addToHistory(String serviceNameInTitleCase, Date date,
                              DailyState dailyState) {
        try {
            dataAccess.insertHistoryData(serviceNameInTitleCase, date, dailyState.name());
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while data access: adding summarized history data ", e);
        }
    }

    private DailyState getState(String serviceNameInTitleCase, Calendar current) {
        try {
            ResultSet resultSet = dataAccess.getTestsExecutedFromLiveStatus(serviceNameInTitleCase, current);
            if(resultSet.next()){
                resultSet.close();
                resultSet = dataAccess.getTestsFailedFromFailureDetail(serviceNameInTitleCase, current);
                if(resultSet.next()) {
                    resultSet.close();
                    resultSet = dataAccess.getTestsExecutedFromLiveStatus(serviceNameInTitleCase, current);
                    while (resultSet.next()){
                        if(resultSet.getBoolean("STATUS")) {
                            resultSet.close();
                            return DailyState.DISRUPTION;
                        }
                    }
                    resultSet.close();
                    return DailyState.DOWN;
                } else {
                    resultSet.close();
                    return DailyState.NORMAL;
                }

            } else {
                resultSet.close();
                return DailyState.NA;
            }
        } catch (SQLException e) {
            return DailyState.NA;
        }
    }
}
