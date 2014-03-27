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
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Obsolete data removal from the database utility implemented in this class
 */
public class DbCleaner implements Job {

    private static final Log log = LogFactory.getLog(DigestMailer.class);

    private int flushInterval;

    /**
     * @param jobExecutionContext "flushBefore" param passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        flushObsolete();
    }

    /**
     * Removes obsolete data
     */
    private void flushObsolete (){
        DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
        Connection connection = dbConnectionManager.getConnection();

        try {
            DbConnectionManager.deleteObsoleteData(connection, flushInterval);
        } catch (SQLException e) {
            log.error("Obsolete Data Flush: SQLException thrown while flushing ", e);
        } catch (Exception e) {
            log.error("Obsolete Data Flush: SQLException thrown while flushing: Possibly Database " +
                      "connection is not available ", e);
        }
    }

    /**
     * Sets the flushing interval
     * @param interval Flush interval
     */
    public void setFlushBefore(String interval) {
        try{
            if(Integer.parseInt(interval.split("d")[0].replace(" ", ""))>=35){
                this.flushInterval = Integer.valueOf(interval.split("d")[0].replace(" ", "")) + 1;  //to maintain 35 days of period
            } else {
                throw new Exception();
            }
        }catch (Exception e){
            log.error("Obsolete Data Flush: Invalid interval specified for data flush, default 35 days");
            this.flushInterval = 36;       //Default value 35 days
        }
    }
}
