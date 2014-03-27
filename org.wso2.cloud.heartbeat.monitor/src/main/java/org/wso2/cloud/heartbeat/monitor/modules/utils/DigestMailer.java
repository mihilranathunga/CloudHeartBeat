/*
 * Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
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

package org.wso2.cloud.heartbeat.monitor.modules.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Digest mail notification utility implemented in this class
 */
public class DigestMailer implements Job{
    private static final Log log = LogFactory.getLog(DigestMailer.class);

    private String digestMailInterval;

    /**
     * @param jobExecutionContext "digestMailInterval" param passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        sendMail();
    }

    /**
     * Sends digest mails
     */
    private void sendMail(){
        DbConnectionManager dbConnectionManager= DbConnectionManager.getInstance();
        Connection connection = dbConnectionManager.getConnection();

        int passedTests;
        int failedTests;
        try {
            passedTests = DbConnectionManager.getTestCount(connection, digestMailInterval, true);
            failedTests = DbConnectionManager.getTestCount(connection, digestMailInterval, false);

            int totalTestsRun = passedTests + failedTests;

            DateFormat dateFormat = new SimpleDateFormat("Z");
            Date date = new Date();
            String timeZone = dateFormat.format(date);

            String subject = "Past " + digestMailInterval + " hour(s) digest mail. (GMT " + timeZone +" Standard Time)";
            String status = "Tests: "+totalTestsRun +"    Success: "+passedTests+"    Failure: " +failedTests;
            Mailer mailer = Mailer.getInstance();
            if(failedTests>0){
                ResultSet resultSet = DbConnectionManager.getFailedTestData(connection, digestMailInterval);
                mailer.send(subject, status, convertToHTML(resultSet));
                if(resultSet!=null){
                    try {
                        resultSet.close();
                    }catch (SQLException e) {
                        log.error("Digest Mailing: SQLException thrown while querying the data source ", e);
                    }
                }
            } else {
                mailer.send(subject, status, "");
            }
        }catch (SQLException e){
            log.fatal("Digest Mailing: SQLException thrown while querying the data source", e);
        } catch (Exception e){
            log.error("Digest Mailing: Exception thrown while querying the data source: Possibly " +
                      "Database connection is not available ", e);
        }

    }

    /**
     * Sets digest mail interval
     * @param interval Digest mail interval
     */
    public void setDigestMailInterval(String interval) {
        try{
            Integer.parseInt(interval.split("h")[0].replace(" ", ""));
            this.digestMailInterval = interval.split("h")[0].replace(" ", "");
        }catch (Exception e){
            log.error("Digest Mailing: Invalid interval specified for digest mail");
            this.digestMailInterval = "24";       //Default value 24 hours
        }
    }

    /**
     * Converts resultSet into HTML
     * @param resultSet ResultSet
     * @return HTML table
     */
    private String convertToHTML(ResultSet resultSet) {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append( "<P ALIGN='center'><TABLE BORDER=1>");
        ResultSetMetaData resultSetMetaData;
        try {
            resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            // table header
            stringBuffer.append("<TR>");
            for (int i = 0; i < columnCount; i++) {
                stringBuffer.append("<TH>").append(resultSetMetaData.getColumnLabel(i + 1)).append("</TH>");
            }
            stringBuffer.append("</TR>");
            // the data
            while (resultSet.next()) {
                stringBuffer.append("<TR>");
                for (int i = 0; i < columnCount; i++) {
                    stringBuffer.append("<TD>").append(resultSet.getString(i + 1)).append("</TD>");
                }
                stringBuffer.append("</TR>");
            }
            stringBuffer.append("</TABLE></P>");
        } catch (SQLException e) {
            log.error("Digest Mailing: SQLException thrown while querying the data source ", e);
        } catch (Exception e) {
            log.error("Digest Mailing: Exception thrown while querying the data source: Possibly " +
                      "Database connection is not available", e);
        }
        return stringBuffer.toString();
    }
}
