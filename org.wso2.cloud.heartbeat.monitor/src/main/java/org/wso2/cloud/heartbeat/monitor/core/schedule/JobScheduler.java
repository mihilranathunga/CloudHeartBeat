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
import org.quartz.*;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;
import org.wso2.cloud.heartbeat.monitor.core.schedule.utils.LoginJobScheduler;
import org.wso2.cloud.heartbeat.monitor.core.schedule.utils.TriggerUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.futureDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Services' Quartz job scheduling is implemented in this class
 */
public class JobScheduler {

    private static final Log log = LogFactory.getLog(JobScheduler.class);

    private Scheduler scheduler;
    private Node moduleNode;
    private Node tenantNode;
    private List <Class> classes;
    private Random generator;

    /**
     * Initializes JobScheduler object
     * @param scheduler Quartz scheduler uses to schedule
     * @param moduleNode Service module node
     * @param tenantNode Tenant credentials node
     * @param moduleClasses Service module's java classes
     */
    public JobScheduler (Scheduler scheduler, Node moduleNode, Node tenantNode, List <Class> moduleClasses){
        this.scheduler = scheduler;
        this.moduleNode = moduleNode;
        this.tenantNode = tenantNode;
        this.classes = moduleClasses;
        this.generator = new Random();
    }

    /**
     * Schedule Service module tests
     * @throws org.quartz.SchedulerException
     */
    public void schedule() throws SchedulerException {

        moduleNode.addProperty("tenant_user",tenantNode.getProperty("tenant_user"));
        moduleNode.addProperty("tenant_user_pwd",tenantNode.getProperty("tenant_user_pwd"));

        LoginJobScheduler loginJobScheduler = new LoginJobScheduler(scheduler, moduleNode, tenantNode);
        loginJobScheduler.schedule();

        if(moduleNode.getChildNodes()!=null){
            List<Node> jobs = moduleNode.getChildNodes();
            for (Node node : jobs) {
                try {
                    JobDetail jobDetail = buildJob(node);
                    Trigger trigger = buildTrigger(node);
                    scheduler.scheduleJob(jobDetail, trigger);
                } catch (ClassNotFoundException e) {
                    log.warn("JobScheduler: Scheduler shutting down");
                    scheduler.shutdown();
                    break;
                } catch (SchedulerException e) {
                    log.warn("JobScheduler: Scheduler shutting down");
                    scheduler.shutdown();
                    log.error("JobScheduler: scheduling job failure detected: in scheduling: " + node.getName());
                    throw new SchedulerException("JobScheduler: scheduling job failure detected:" +
                                                 " in scheduling: " + node.getName(), e);
                } catch (Exception e) {
                    log.warn("JobScheduler: Scheduler shutting down");
                    scheduler.shutdown();
                    break;
                }
            }
        }
    }

    /**
     * Schedule Service module tests with a custom login test
     * @throws org.quartz.SchedulerException
     */
    public void schedule(Class aClass) throws SchedulerException {

        moduleNode.addProperty("tenant_user",tenantNode.getProperty("tenant_user"));
        moduleNode.addProperty("tenant_user_pwd",tenantNode.getProperty("tenant_user_pwd"));

        LoginJobScheduler loginJobScheduler = new LoginJobScheduler(scheduler, moduleNode, tenantNode);
        loginJobScheduler.schedule(aClass);

        if(moduleNode.getChildNodes()!=null){
            List<Node> jobs = moduleNode.getChildNodes();
            for (Node node : jobs) {
                try {
                    JobDetail jobDetail = buildJob(node);
                    Trigger trigger = buildTrigger(node);
                    scheduler.scheduleJob(jobDetail, trigger);
                } catch (ClassNotFoundException e) {
                    log.warn("JobScheduler: Scheduler shutting down");
                    scheduler.shutdown();
                    break;
                } catch (SchedulerException e) {
                    log.warn("JobScheduler: Scheduler shutting down");
                    scheduler.shutdown();
                    log.error("JobScheduler: scheduling job failure detected: in scheduling: " + node.getName());
                    throw new SchedulerException("JobScheduler: scheduling job failure detected:" +
                                                 " in scheduling: " + node.getName(), e);
                } catch (Exception e) {
                    log.warn("JobScheduler: Scheduler shutting down");
                    scheduler.shutdown();
                    break;
                }
            }
        }
    }

    /**
     * Returns Quartz trigger for service jobs
     * @param node Service test node
     * @return Quartz Trigger
     * @throws Exception
     */
    private Trigger buildTrigger(Node node) throws Exception {
        String expression;
          if(TriggerUtils.getTriggerType(node)==TriggerUtils.TriggerType.CRON){
              expression = node.getProperty(TriggerUtils.CRON_TRIGGER);
              if(TriggerUtils.isValidCronExpression(expression)){

                  /*
                   * Triggers will have tests' name as the name and service name as the group name
                   * in underscore case format
                   */
                  String triggerName = node.getName();
                  String triggerGroup = moduleNode.getName();

                  CronTrigger trigger;
                  trigger = newTrigger()
                          .withIdentity(triggerName, triggerGroup)
                          .withSchedule(cronSchedule(expression))
                          .build();
                  return trigger;
              } else {
                  log.error("JobScheduler: In" +moduleNode.getName()
                            + ": " +node.getName() +": invalid cron expression detected: " + expression);
                  throw new Exception(node.getName() + "cron expression " +expression + " not valid");
              }
          } else {
              expression = node.getProperty(TriggerUtils.SIMPLE_TRIGGER);
              if (TriggerUtils.isValidSimpleTriggerExpression(expression)){
                  if(expression.endsWith(TriggerUtils.MINUTES)){
                       return simpleTriggerIntervalsInMinutes(node);
                  } else return simpleTriggerIntervalsInHours(node);
              } else {
                  log.error("JobScheduler: In" +moduleNode.getName()
                            + ": " +node.getName() +": invalid expression detected: " + expression);
                  throw new Exception(node.getName() + "trigger expression " +expression + " not valid");
              }
          }
    }

    /**
     * Returns Simple trigger with interval in hours
     * @param node Service test node
     * @return Quartz Simple Trigger
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
                .startAt(futureDate(generator.nextInt(1), DateBuilder.IntervalUnit.MINUTE))
                .build();

        return trigger;
    }

    /**
     * Returns Simple trigger with interval in minutes
     * @param node Service test node
     * @return Quartz Simple Trigger
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
                .startAt(futureDate(generator.nextInt(1), DateBuilder.IntervalUnit.MINUTE))
                .build();

        return trigger;
    }

    /**
     * Builds Quartz Job for service test
     * @param node Service test node
     * @return Quartz JobDetail
     * @throws ClassNotFoundException
     */
    private JobDetail buildJob(Node node) throws ClassNotFoundException {

        /*
        * Jobs will have tests' name as the name and service name as the group name
        * in camel case format
        */
        String jobName = CaseConverter.underscoreToTitleCase(node.getName());
        String jobGroup = CaseConverter.underscoreToTitleCase(moduleNode.getName());

        try {
            Class<? extends Job> testClass = getJobClass(jobName);
            return newJob(testClass).withIdentity(jobName, jobGroup)
                    .usingJobData(buildJobDataMap(node))
                    .build();
        } catch (ClassNotFoundException e) {
            log.error("JobScheduler: In" + moduleNode.getName() + ":  invalid tag detected: " + node.getName());
            throw new ClassNotFoundException(jobName + ".class not found");
        }
    }

    /**
     * Returns Job Data Map for a Job
     * @param testNode Service test node
     * @return JobDataMap
     */
    private JobDataMap buildJobDataMap(Node testNode){
        JobDataMap dataMap=new JobDataMap();

        dataMap.put("serviceName",CaseConverter.underscoreToTitleCase(moduleNode.getName()));

        //Setting common properties of a module
        setProperties(moduleNode, dataMap);

        //setting specific properties of a job
        setProperties(testNode,dataMap);

        return dataMap;
    }

    /**
     * Sets properties of the job data map
     * @param node service test node
     * @param jobDataMap JobDataMap for the Job
     */
    private void setProperties(Node node, JobDataMap jobDataMap){
        for (Map.Entry<String, String> properties : node.getProperties().entrySet()) {

            if(!(properties.getKey().equals
                    (TriggerUtils.SIMPLE_TRIGGER) || properties.getKey().equals(TriggerUtils.CRON_TRIGGER))){
                String key = CaseConverter.underscoreToCamelCase(properties.getKey());
                String value = properties.getValue();

                jobDataMap.put(key,value);
            }
        }
    }

    /**
     * Returns Java class of the test which implements the Job
     * @param jobName Service test name
     * @return Java class
     * @throws ClassNotFoundException
     */
    private Class<? extends Job> getJobClass(String jobName) throws ClassNotFoundException {
        for (Class aClass : classes) {
            Class c = (aClass);
            if (c.getName().contains(jobName)) {
                return (Class<? extends Job>)c;
            }
        }
        throw new ClassNotFoundException(jobName + ".class not found");
    }
}
