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

package org.wso2.cloud.heartbeat.monitor.core.schedule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;
import org.quartz.listeners.SchedulerListenerSupport;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Scheduler listener extending SchedulerListenerSupport implemented in this class
 */
public class SchedulerListener extends SchedulerListenerSupport{

    private static final Log log = LogFactory.getLog(SchedulerListener.class);

    @Override
    public void schedulerStarted(){
        Mailer mailer = Mailer.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a (Z z)");
        Date date = new Date();
        mailer.send(" INFO: Heartbeat scheduler: Started","Scheduler started at " +
                                                          (simpleDateFormat.format(date)),"" );
    }

    @Override
    public void schedulerShutdown(){
        Mailer mailer = Mailer.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a (Z z)");
        Date date = new Date();
        mailer.send(" WARNING: Heartbeat scheduler: Shutdown","Scheduler stopped at " +
                                                              (simpleDateFormat.format(date)),"" );
        log.warn("Scheduler Health: Heartbeat scheduler: Shutdown");
    }

    @Override
    public void schedulingDataCleared(){
        Mailer mailer = Mailer.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a (Z z)");
        Date date = new Date();
        mailer.send(" WARNING: Heartbeat scheduler: Data Cleared","Scheduling data cleared at " +
                                                                  (simpleDateFormat.format(date)),"" );
        log.warn("Scheduler Health: Heartbeat scheduler: Data Cleared");
    }

    @Override
    public void schedulerError(String msg, SchedulerException cause){
        Mailer mailer = Mailer.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a (Z z)");
        Date date = new Date();
        mailer.send(" WARNING: Heartbeat scheduler: Error","Scheduler error occurred at " +
                                                           (simpleDateFormat.format(date)),"" );
        log.warn("Scheduler Health: Heartbeat scheduler: Scheduler Error");
    }

}
