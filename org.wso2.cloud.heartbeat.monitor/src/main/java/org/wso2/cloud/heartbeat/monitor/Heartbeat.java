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

package org.wso2.cloud.heartbeat.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.SchedulerException;
import org.wso2.cloud.heartbeat.monitor.core.schedule.ScheduleManager;
import org.wso2.cloud.heartbeat.monitor.utils.Constants;
import org.wso2.cloud.heartbeat.monitor.utils.PlatformUtils;

import java.io.IOException;

/**
 * Helper class that executes Heartbeat-Monitor from the command line
 */
public class Heartbeat {
    private static final Log log = LogFactory.getLog(Heartbeat.class);

    public static void main (String []args) throws IOException, SchedulerException {

        PlatformUtils.setKeyStoreProperties();
        PropertyConfigurator.configure(Constants.LOG4J_PROPERTY_PATH);
        try {

            ScheduleManager scheduleManager = new ScheduleManager();
            scheduleManager.schedule();
            scheduleManager.startScheduler();
        } catch (IOException e) {
            log.fatal("Heartbeat Initializer: IOException thrown while initializing");
            throw new IOException("Heartbeat Initializer: IOException thrown while initializing", e);
        } catch (SchedulerException e) {
            log.fatal("Heartbeat Initializer: SchedulerException thrown while initializing");
            throw new SchedulerException("Heartbeat Initializer: SchedulerException thrown while initializing", e);
        }
    }

}
