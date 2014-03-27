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
import org.wso2.cloud.heartbeat.monitor.modules.common.tests.TenantLoginTest;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.util.Random;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Scheduling Login jobs for services implemented in this class. this is implemented as
 * an utility class for JobScheduler class
 */
public class LoginJobScheduler {

    private static final Log log = LogFactory.getLog(LoginJobScheduler.class);

    private Node moduleNode;
    private Node tenantNode;
    private Scheduler scheduler;
    private Random generator;

    /**
     * Initializes Login job scheduler object
     * @param scheduler Scheduler use to scheduler jobs
     * @param moduleNode Service module node
     * @param tenantNode  Tenant credentials node
     */
    public LoginJobScheduler (Scheduler scheduler, Node moduleNode, Node tenantNode){
        this.moduleNode = moduleNode;
        this.tenantNode = tenantNode;
        this.scheduler = scheduler;
        this.generator = new Random();
    }

    /**
     * Schedules Login job
     * @throws org.quartz.SchedulerException
     */
    public void schedule() throws SchedulerException {
        try {
            JobDetail jobDetail = buildJob();
            Trigger trigger = buildTrigger();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            log.warn("JobScheduler: Scheduler shutting down");
            scheduler.shutdown();
        }
    }

    /**
     * Schedules Login job with a custom login test
     * @throws org.quartz.SchedulerException
     */
    public void schedule(Class aClass) throws SchedulerException {
        try {
            JobDetail jobDetail = buildJob(aClass);
            Trigger trigger = buildTrigger();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            log.warn("JobScheduler: Scheduler shutting down");
            scheduler.shutdown();
        }
    }

    /**
     * Returns trigger for the job
     * @return Trigger for the job
     * @throws Exception
     */
    private Trigger buildTrigger() throws Exception {
        String expression;
        Node loginTestNode = tenantNode.findChildNodeByName("tenant_login_test");
        if(TriggerUtils.getTriggerType(loginTestNode)==TriggerUtils.TriggerType.CRON){
            expression = loginTestNode.getProperty(TriggerUtils.CRON_TRIGGER);
            if(TriggerUtils.isValidCronExpression(expression)){

                /*
                * Triggers will have tests' name as the name and service name as the group name
                * in underscore case format
                */
                String triggerName = loginTestNode.getName();
                String triggerGroup = moduleNode.getName();

                CronTrigger trigger;
                trigger = newTrigger()
                        .withIdentity(triggerName, triggerGroup)
                        .withSchedule(cronSchedule(expression))
                        .build();
                return trigger;
            } else {
                log.error("LoginJobScheduler: In" +moduleNode.getName()
                          + ": " +loginTestNode.getName() +": invalid cron expression detected: " + expression);
                throw new Exception(loginTestNode.getName() + "cron expression " +expression + " not valid");
            }
        } else {
            expression = loginTestNode.getProperty(TriggerUtils.SIMPLE_TRIGGER);
            if (TriggerUtils.isValidSimpleTriggerExpression(expression)){
                if(expression.endsWith(TriggerUtils.MINUTES)){
                    return simpleTriggerIntervalsInMinutes(loginTestNode);
                } else return simpleTriggerIntervalsInHours(loginTestNode);
            } else {
                log.error("LoginJobScheduler: In" +moduleNode.getName()
                          + ": " +loginTestNode.getName() +": invalid expression detected: " + expression);
                throw new Exception(loginTestNode.getName() + "trigger expression " +expression + " not valid");
            }
        }
    }

    /**
     * Returns simple trigger created with Interval in hours
     * @param node Service node
     * @return Simple trigger with interval in hours
     */
    private Trigger simpleTriggerIntervalsInHours(Node node) {
        String expression = node.getProperty(TriggerUtils.SIMPLE_TRIGGER);

        /*
        * Triggers will have tests' name as the name and service name as the group name
        * in underscore case format
        */
        String triggerName = node.getName();
        String triggerGroup = moduleNode.getName();

        int interval = Integer.parseInt(expression.split(TriggerUtils.HOURS)[0].replace(" ", ""));

        SimpleTrigger trigger;
        trigger = newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .withSchedule(simpleSchedule()
                                      .withIntervalInHours(interval)
                                      .repeatForever())
                .startAt(futureDate(generator.nextInt(60), DateBuilder.IntervalUnit.SECOND))
                .build();

        return trigger;
    }

    /**
     * Returns simple trigger created with Interval in minutes
     * @param node Service node
     * @return Simple trigger with interval in minutes
     */
    private Trigger simpleTriggerIntervalsInMinutes(Node node) {
        String expression = node.getProperty(TriggerUtils.SIMPLE_TRIGGER);

        /*
        * Triggers will have tests' name as the name and service name as the group name
        * in underscore case format
        */
        String triggerName = node.getName();
        String triggerGroup = moduleNode.getName();

        int interval = Integer.parseInt(expression.split(TriggerUtils.MINUTES)[0].replace(" ", ""));

        SimpleTrigger trigger;
        trigger = newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .withSchedule(simpleSchedule()
                                      .withIntervalInMinutes(interval)
                                      .repeatForever())
                .startAt(futureDate(generator.nextInt(60), DateBuilder.IntervalUnit.SECOND))
                .build();

        return trigger;
    }

    /**
     * Returns Quartz job for login
     * @return Login Quartz job
     */
    private JobDetail buildJob() {
        String jobName = CaseConverter.underscoreToTitleCase(moduleNode.getName()) + "LoginTest";
        String jobGroup = CaseConverter.underscoreToTitleCase(moduleNode.getName());

        return newJob(TenantLoginTest.class).withIdentity(jobName, jobGroup)
                .usingJobData(buildJobDataMap())
                .build();
    }

    /**
     * Returns Quartz job for a custom login test
     * @return Custom Login Quartz job
     */
    private JobDetail buildJob(Class aClass) {
        String jobName = CaseConverter.underscoreToTitleCase(moduleNode.getName()) + aClass.getName();
        String jobGroup = CaseConverter.underscoreToTitleCase(moduleNode.getName());

        return newJob(aClass).withIdentity(jobName, jobGroup)
                .usingJobData(buildJobDataMap())
                .build();
    }

    /**
     * Creates Quartz job data map
     * @return Quartz JobDataMap
     */
    private JobDataMap buildJobDataMap() {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("serviceName",CaseConverter.underscoreToTitleCase(moduleNode.getName()));

        //login test will run against manager node if worker and manager, 2 nodes exists.
        if(moduleNode.getProperty("management_host_name") != null){
            jobDataMap.put("hostName", moduleNode.getProperty("management_host_name"));
        } else {
            jobDataMap.put("hostName", moduleNode.getProperty("host_name"));
        }
        if(moduleNode.getProperty("admin_username") != null){
            jobDataMap.put("tenantUser", moduleNode.getProperty("admin_username"));
            jobDataMap.put("tenantUserPwd", moduleNode.getProperty("admin_password"));
        }else{
            jobDataMap.put("tenantUser", tenantNode.getProperty("tenant_user"));
            jobDataMap.put("tenantUserPwd", tenantNode.getProperty("tenant_user_pwd"));
        }

        return jobDataMap;
    }
}
