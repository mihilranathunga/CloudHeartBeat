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

package org.wso2.cloud.heartbeat.monitoring.ui.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.cloud.heartbeat.monitoring.ui.configuration.Node;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class DataAccess {

    private static final Log log = LogFactory.getLog(DataAccess.class);
    private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";

    private Connection connection;

    public DataAccess(Node dataSource){

        String userName = dataSource.getProperty("user");
        String password = dataSource.getProperty("password");
        String dbUrl = "jdbc:mysql://" + dataSource.getProperty("host_name") + "/" +
                       dataSource.getProperty("database_name");

        try {
            Class.forName(DRIVER_CLASS_NAME).newInstance();
            connection = DriverManager.getConnection(dbUrl, userName, password);
        } catch (ClassNotFoundException e) {
            log.error("Heartbeat - Monitor - ClassNotFoundException thrown while initializing data access: ", e);
        } catch (SQLException e) {
            log.error("Heartbeat - Monitor - SQLException thrown while initializing data access: ", e);
        } catch (InstantiationException e) {
            log.error("Heartbeat - Monitor - InstantiationException thrown while initializing data access: ", e);
        } catch (IllegalAccessException e) {
            log.error("Heartbeat - Monitor - IllegalAccessException thrown while initializing data access: ", e);
        }
    }

    public ResultSet getServiceState(String serviceName, String testName) throws SQLException {
        String query;
        query = "SELECT STATUS, DATETIME FROM LIVE_STATUS WHERE (SERVICE, TEST) = (?,?) ORDER BY DATETIME DESC LIMIT 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setString(2, testName);
        return preparedStatement.executeQuery();
    }

    public ResultSet getServiceHistory (String serviceName) throws SQLException {
        String query;
        query = "SELECT DATE, STATUS  FROM HISTORY WHERE SERVICE = (?) ORDER BY DATE DESC LIMIT 35";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);

        return preparedStatement.executeQuery();
    }

    public ResultSet getTestsExecutedFromLiveStatus(String serviceName, Calendar date) throws SQLException {
        String query;
        query = "SELECT * FROM LIVE_STATUS WHERE SERVICE = (?) AND EXTRACT(DAY FROM DATETIME) = (?) " +
                "AND EXTRACT(MONTH FROM DATETIME) = (?) AND EXTRACT(YEAR FROM DATETIME) = (?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setString(2, String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        preparedStatement.setString(3, String.valueOf(date.get(Calendar.MONTH)+1));
        preparedStatement.setString(4, String.valueOf(date.get(Calendar.YEAR)));
        return preparedStatement.executeQuery();
    }

    public ResultSet getTestsFailedFromFailureDetail(String serviceName, Calendar date)
            throws SQLException {
        String query;
        query = "SELECT *  FROM FAILURE_DETAIL WHERE SERVICE = (?) AND EXTRACT(DAY FROM DATETIME) = (?) " +
                "AND EXTRACT(MONTH FROM DATETIME) = (?) AND EXTRACT(YEAR FROM DATETIME) = (?) ";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setString(2, String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        preparedStatement.setString(3, String.valueOf(date.get(Calendar.MONTH)+1));
        preparedStatement.setString(4, String.valueOf(date.get(Calendar.YEAR)));

        return preparedStatement.executeQuery();
    }

    public ResultSet getTestsFailedDetailsFromFailureDetail(String serviceName, String testName)
            throws SQLException {
        String query;
        query = "SELECT *  FROM FAILURE_DETAIL WHERE (SERVICE, TEST)= (?,?) ORDER BY TIMESTAMP DESC LIMIT 1";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setString(2, testName);

        return preparedStatement.executeQuery();
    }

    public void insertHistoryData(String serviceName, Date date, String state)
            throws SQLException {
        String query;
        query = "INSERT INTO HISTORY VALUES (?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setDate(2, date);
        preparedStatement.setString(3, state);

        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public ResultSet getHistoryNotes(String serviceName) throws SQLException {
        String query;
        query = "SELECT DATETIME, NOTE FROM HISTORY_NOTES WHERE SERVICE = (?) AND DATETIME > DATE_SUB(NOW() , INTERVAL 35 DAY)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);

        return preparedStatement.executeQuery();
    }

    public ResultSet getCurrentNotes(String serviceName, Calendar date) throws SQLException {
        String query;
        query = "SELECT DATETIME, NOTE FROM HISTORY_NOTES WHERE SERVICE = (?) AND EXTRACT(DAY FROM DATETIME) = (?) " +
                "AND EXTRACT(MONTH FROM DATETIME) = (?) AND EXTRACT(YEAR FROM DATETIME) = (?) ";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setString(2, String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
        preparedStatement.setString(3, String.valueOf(date.get(Calendar.MONTH)+1));
        preparedStatement.setString(4, String.valueOf(date.get(Calendar.YEAR)));

        return preparedStatement.executeQuery();
    }

    public void insertNotes(String serviceName, String text) throws SQLException {
        String query;
        query = "INSERT INTO HISTORY_NOTES (SERVICE,NOTE) VALUES (?,?)";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName);
        preparedStatement.setString(2, text);

        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public ResultSet getLastTestDate(String serviceName) throws SQLException {
        String query;
        query = "SELECT DATETIME FROM LIVE_STATUS WHERE SERVICE = (?) ORDER BY DATETIME DESC LIMIT 1";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setString(1, serviceName.replace(" ",""));                    //according to db containing values

        return preparedStatement.executeQuery();
    }

    public void clearObsoleteHistoryData(int interval) throws SQLException {
        String query;
        query = "DELETE FROM HISTORY  WHERE DATE < DATE_SUB(NOW() , INTERVAL (?) DAY)";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, interval);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public void clearObsoleteHistoryNotes(int interval) throws SQLException {
        String query;
        query = "DELETE FROM HISTORY_NOTES WHERE DATETIME < DATE_SUB(NOW() , INTERVAL (?) DAY)";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, interval);
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    public void closeConnection() throws SQLException {
        connection.close();
    }
}
