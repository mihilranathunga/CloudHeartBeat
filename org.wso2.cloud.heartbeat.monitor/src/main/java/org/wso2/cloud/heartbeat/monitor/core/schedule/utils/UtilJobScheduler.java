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

package org.wso2.cloud.heartbeat.monitor.core.schedule.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.util.ArrayList;

import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Scheduling utility for jobs other than service tests which has login jobs implemented in this class
 */
public class UtilJobScheduler {

    private static final Log log = LogFactory.getLog(UtilJobScheduler.class);

    private Node jobNode;
    private ArrayList<String> tags;
    private Scheduler scheduler;
    private Class jobClass;
    private int triggerTime;
    private boolean startNow;

    /**
     * Initializes UtilJobScheduler object
     * @param scheduler Scheduler use to schedule
     * @param jobNode Job node
     * @param tags specific tags added
     * @param triggerTime Trigger time
     * @param jobClass Job java class
     * @param startNow  boolean value of starting now
     */
    public UtilJobScheduler (Scheduler scheduler , Node jobNode,
                             ArrayList<String> tags, int triggerTime, Class jobClass, boolean startNow){
        this.jobNode = jobNode;
        this.tags = tags;
        this.jobClass = jobClass;
        this.scheduler = scheduler;
        this.triggerTime = triggerTime;
        this.startNow = startNow;
    }

    /**
     * Schedules Quartz Job
     * @throws org.quartz.SchedulerException
     */
    public void schedule() throws SchedulerException {
        try {
            JobDetail jobDetail = buildJob();
            Trigger trigger = buildTrigger();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            log.error("JobScheduler: " + jobNode.getName() + ": Exception thrown while scheduling," +
                     " Scheduler shutting down", e);
            scheduler.shutdown();
        }
    }

    /**
     * Returns trigger for the Job
     * @return Simple trigger created for the job
     */
    private Trigger buildTrigger() {
        String triggerName = jobNode.getName();
        String triggerGroup = "maintenance";

        SimpleTrigger trigger;
        if(!startNow){
            trigger = newTrigger()
                    .withIdentity(triggerName, triggerGroup)
                    .withSchedule(simpleSchedule()
                                            .withIntervalInHours(triggerTime)
                                            .repeatForever())
                    .startAt(futureDate(triggerTime, DateBuilder.IntervalUnit.HOUR))
                    .build();
        } else {
            trigger = newTrigger()
                    .withIdentity(triggerName, triggerGroup)
                    .withSchedule(simpleSchedule()
                                          .withIntervalInHours(triggerTime)
                                          .repeatForever())
                    .build();
        }
        return trigger;
    }

    /**
     * Creates and returns Quartz job for the utility
     * @return Quartz JobDetail of the job
     */
    private JobDetail buildJob() {
        String jobName = jobNode.getName();
        String jobGroup = "utilities";
        return newJob((Class<? extends Job>)jobClass).withIdentity(jobName, jobGroup)
                .usingJobData(buildJobDataMap())
                .build();
    }

    /**
     * Creates Job data map for the job
     * @return Quartz JobDataMap for the Job
     */
    private JobDataMap buildJobDataMap() {
        JobDataMap jobDataMap = new JobDataMap();
        for (String tagId : tags) {
            jobDataMap.put(CaseConverter.underscoreToCamelCase(tagId), jobNode.getProperty(tagId));
        }
        return jobDataMap;
    }
}
