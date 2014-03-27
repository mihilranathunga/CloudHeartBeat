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

package org.wso2.cloud.heartbeat.monitor.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;
import org.wso2.cloud.heartbeat.monitor.core.configuration.parser.nginx.NodeBuilder;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.FileManager;

import java.io.IOException;
import java.sql.*;

/**
 * MySQL database connection implemented in this class
 */
public class DbConnectionManager {
    private static final Log log = LogFactory.getLog(DbConnectionManager.class);

    private static DbConnectionManager instance;
    private Connection connection;

    private static final String DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";

    /**
     * creates database connection
     */
    private DbConnectionManager() {
       this.connection = setDbConnection();
    }

    /**
     * Returns database connection instance, if database connection instance is null creates
     * a database connection
     * @return Database connection
     */
    public static DbConnectionManager getInstance() {
         if(instance == null){
            instance = new DbConnectionManager();
         }
        return instance;
    }

    /**
     * Initializes MySQL database connection
     * @return java.sql.Connection
     */
    private Connection setDbConnection() {
        Node rootNode = new Node();
        try {
        NodeBuilder.buildNode(rootNode, FileManager.readFile(Constants.HEARTBEAT_CONF_PATH));
        Node dataSource = rootNode.findChildNodeByName(Constants.DATA_SOURCE);
        String userName = dataSource.getProperty("user");
        String password = dataSource.getProperty("password");
        String dbUrl = "jdbc:mysql://" + dataSource.getProperty("host_name") + "/" +
                                                            dataSource.getProperty("database_name");

        Class.forName(DRIVER_CLASS_NAME).newInstance();

            connection = DriverManager.getConnection(dbUrl,userName,password);
        } catch (SQLException e) {
            log.fatal("SQLException thrown while getting the connection: ", e);
        } catch (ClassNotFoundException e) {
            log.fatal("ClassNotFoundException thrown while getting the connection: Please add the mysql" +
                      " connector to lib folder: ", e);
        } catch (InstantiationException e) {
            log.fatal("InstantiationException thrown while getting the connection: ", e);
        } catch (IllegalAccessException e) {
            log.fatal("IllegalAccessException thrown while getting the connection: ", e);
        } catch (IOException e) {
            log.fatal("IOException thrown while getting the connection: reading the conf", e);
        }
        return connection;
    }

    /**
     * Returns MySQL database connection
     * @return Database connection
     */
    public Connection getConnection(){
        return this.connection;
    }

    /**
     * Inserts status data into 'LIVE_STATUS' table
     * @param con Database connection
     * @param timeStamp Time stamp
     * @param service Service name
     * @param test Test name
     * @param status Status need to added
     */
    public static void insertLiveStatus(Connection con, long timeStamp, String service, String test, boolean status){
        String updateStatus = "INSERT INTO LIVE_STATUS (TIMESTAMP,SERVICE,TEST,STATUS) VALUES (?,?,?,?)";

        try {
            PreparedStatement preparedStatement = con.prepareStatement(updateStatus);
            preparedStatement.setLong(1, timeStamp);
            preparedStatement.setString(2, service);
            preparedStatement.setString(3, test);
            preparedStatement.setBoolean(4, status);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            log.fatal("SQLException thrown while inserting stats: ", e);
        } catch (Exception e) {
            log.error("Exception thrown while inserting stats: Possibly Database connection is not available ", e);
        }
    }

    /**
     * Inserts details of failed tests into 'FAILURE_DETAIL' table
     * @param con Database connection
     * @param timeStamp Time stamp
     * @param service Service name
     * @param test Test name
     * @param msg Detailed message
     */
    public static void insertFailureDetail(Connection con, long timeStamp, String service, String test, String msg){
        String updateFailureDetail = "INSERT INTO FAILURE_DETAIL (TIMESTAMP,SERVICE,TEST,DETAIL) VALUES (?,?,?,?)";
        try {
            PreparedStatement preparedStatement = con.prepareStatement(updateFailureDetail);
            preparedStatement.setLong(1, timeStamp);
            preparedStatement.setString(2, service);
            preparedStatement.setString(3, test);
            preparedStatement.setString(4, msg);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            log.fatal("SQLException thrown while inserting stats: ", e);
        } catch (Exception e) {
            log.error("Exception thrown while inserting stats: Possibly Database connection is not available ", e);
        }
    }

    /**
     * Returns failed test details within an interval
     * @param con Database connection
     * @param interval Time interval
     * @return Failed test details ResultSet
     * @throws java.sql.SQLException
     */
    public static ResultSet getFailedTestData(Connection con,String interval) throws SQLException {
        String periodicData = "SELECT SERVICE, TEST, DETAIL, DATETIME FROM  FAILURE_DETAIL " +
                                            "WHERE DATETIME > DATE_SUB(NOW() , INTERVAL (?) HOUR)";
        PreparedStatement preparedStatement = con.prepareStatement(periodicData);
        preparedStatement.setString(1,interval);
        return preparedStatement.executeQuery();

    }

    /**
     * Returns a count of a success/failure tests within a time interval
     * @param con Database connection
     * @param interval Time interval
     * @param status Status
     * @return Integer count
     * @throws java.sql.SQLException
     */
    public static int getTestCount(Connection con,String interval, boolean status)
            throws SQLException {
        String query = "SELECT COUNT(*) FROM  LIVE_STATUS WHERE (DATETIME > DATE_SUB(NOW() " +
                                                ", INTERVAL (?) HOUR)) AND STATUS = (?)" ;
        PreparedStatement preparedStatement = con.prepareStatement(query);
        preparedStatement.setString(1,interval);
        preparedStatement.setBoolean(2,status);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        resultSet.close();
        return count;
    }

    /**
     * Deletes obsolete data in the database a before specified interval
     * @param con Database connection
     * @param interval time interval
     * @throws java.sql.SQLException
     */
    public static void deleteObsoleteData(Connection con,int interval) throws SQLException {
        String liveStatusQuery = "DELETE FROM  LIVE_STATUS  WHERE DATETIME < DATE_SUB(NOW() , INTERVAL (?) DAY)";
        String failureDetailQuery = "DELETE FROM  FAILURE_DETAIL  WHERE DATETIME < DATE_SUB(NOW() , INTERVAL (?) DAY)";

        PreparedStatement preparedStatement = con.prepareStatement(liveStatusQuery);
        preparedStatement.setInt(1, interval);
        preparedStatement.execute();
        preparedStatement.close();

        preparedStatement = con.prepareStatement(failureDetailQuery);
        preparedStatement.setInt(1, interval);
        preparedStatement.execute();
        preparedStatement.close();
    }
}


