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
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.HiveExecutionServiceClient;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;
import org.wso2.cloud.heartbeat.monitor.core.configuration.parser.nginx.NodeBuilder;
import org.wso2.cloud.heartbeat.monitor.core.schedule.utils.UtilJobScheduler;
import org.wso2.cloud.heartbeat.monitor.modules.appfactory.ApplicationBuildTest;
import org.wso2.cloud.heartbeat.monitor.modules.appfactory.ImportMemberToTenantTest;
import org.wso2.cloud.heartbeat.monitor.modules.bam.CassandraLogColumnFamilyExistanceTest;
import org.wso2.cloud.heartbeat.monitor.modules.bam.HiveScriptExecutionTest;
import org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.ImportUserMembersToTenantTest;
import org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.UserFunctionsTest;
import org.wso2.cloud.heartbeat.monitor.modules.gitblit.GitTenantLoginTest;
import org.wso2.cloud.heartbeat.monitor.modules.jenkins.JenkinsTenantLoginTest;
import org.wso2.cloud.heartbeat.monitor.modules.ues.UESTenantLoginTest;
import org.wso2.cloud.heartbeat.monitor.modules.utils.DbCleaner;
import org.wso2.cloud.heartbeat.monitor.modules.utils.DigestMailer;
import org.wso2.cloud.heartbeat.monitor.utils.Constants;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.FileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to schedule service tests using JobScheduler
 */
public class ScheduleManager {

    private static final Log log = LogFactory.getLog(ScheduleManager.class);

    private Node rootNode;
    private Node heartbeatTenant;
    private Scheduler scheduler;

    /**
     * Initializes ScheduleManager
     * @throws java.io.IOException
     */
    public ScheduleManager () throws IOException {
        rootNode = new Node();
        buildConfTree();
    }

    /**
     * Starts Scheduler
     * @throws org.quartz.SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        scheduler.start();
    }

    /**
     * Shutdown Scheduler
     * @throws org.quartz.SchedulerException
     */
    public void shutDownScheduler () throws SchedulerException {
        scheduler.shutdown();
    }

    /**
     * Schedules Jobs
     * @throws org.quartz.SchedulerException
     */
    public void schedule() throws SchedulerException {
        SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
        scheduler=schedulerFactory.getScheduler();
        scheduleTests();
        scheduleUtilJobs();

        SchedulerListener heartbeatSchedListener = new SchedulerListener();
        scheduler.getListenerManager().addSchedulerListener(heartbeatSchedListener);
    }

    /**
     * Schedule utility Jobs
     * @throws org.quartz.SchedulerException
     */
    private void scheduleUtilJobs() throws SchedulerException {
        //Digest mail scheduler
        Node digestMailNode = rootNode.findChildNodeByName(Constants.NOTIFICATION).findChildNodeByName("email");
        ArrayList <String> digestMailTags = new ArrayList<String>();
        digestMailTags.add("digest_mail_interval");

        String digestMailTriggerTime = digestMailNode.getProperty("digest_mail_interval");
        int triggerTimeDigestMail;
        try{
            triggerTimeDigestMail = Integer.parseInt(((digestMailTriggerTime.split("h"))[0]).replace(" ",""));
        }catch (Exception e){
            log.error("ScheduleManager: Invalid digest mail interval: Value changed to 24 hours." +
                      " value should be in hours (h)");
            triggerTimeDigestMail = 24;
        }

        UtilJobScheduler digestMailJobScheduler = new UtilJobScheduler(scheduler, digestMailNode,
                                                                       digestMailTags, triggerTimeDigestMail,
                                                                       DigestMailer.class, false);
        digestMailJobScheduler.schedule();

        //database flushing scheduler
        Node dbCleanerNode = rootNode.findChildNodeByName(Constants.DATA_SOURCE);
        ArrayList <String> dbCleanerTags = new ArrayList<String>();
        dbCleanerTags.add("flush_before");

        String dbCleanerTriggerTime = dbCleanerNode.getProperty("flush_before");
        int triggerTimeDbCleaner;
        try{
            int days = Integer.parseInt(((dbCleanerTriggerTime.split("d"))[0]).replace(" ",""));
            if(days>=30){
                triggerTimeDbCleaner = 24*days;
            } else {
                throw new Exception();
            }
        }catch (Exception e){
            log.error("ScheduleManager: Invalid database flush interval: Value changed to 30 days." +
                      " value should be in days (d) and minimum is 30 days");
            triggerTimeDbCleaner = 24*30;
        }
        UtilJobScheduler dbCleanerJobScheduler = new UtilJobScheduler(scheduler, dbCleanerNode,
                                                                      dbCleanerTags, triggerTimeDbCleaner,
                                                                      DbCleaner.class, false);
        dbCleanerJobScheduler.schedule();
    }

    /**
     * Schedule service tests
     * @throws org.quartz.SchedulerException
     */
    private void scheduleTests() throws SchedulerException {
        Node modules = rootNode.findChildNodeByName(Constants.MODULES);
        heartbeatTenant = rootNode.findChildNodeByName(Constants.HEARTBEAT_TENANT);

        //AppFactory
        if(modules.findChildNodeByName(Constants.APPFACTORY)!=null){
            List<Class> appfactoryClasses= new ArrayList<Class>();
            appfactoryClasses.add(ApplicationBuildTest.class);
            appfactoryClasses.add(ImportMemberToTenantTest.class);
            scheduleJobs(modules.findChildNodeByName(Constants.APPFACTORY), appfactoryClasses);
        }

        //CloudMgt - contains both cloudmgt and issue tracker
        if(modules.findChildNodeByName(Constants.CLOUD_MGT)!=null){
            List<Class> cloudMgtClasses= new ArrayList<Class>();
            cloudMgtClasses.add(ImportUserMembersToTenantTest.class);
            cloudMgtClasses.add(UserFunctionsTest.class);
            scheduleJobs(modules.findChildNodeByName(Constants.CLOUD_MGT), cloudMgtClasses);
        }

        //Identity Server
        if(modules.findChildNodeByName(Constants.IDENTITY_SERVER)!=null){
            List<Class> isClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.IDENTITY_SERVER),isClasses);
        }

        //Business Process Server
        if(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER)!=null){
            List<Class> bpsClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER),bpsClasses);
        }

        //Storage Server
        if(modules.findChildNodeByName(Constants.STORAGE_SERVER)!=null){
            List<Class> storageServerClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.STORAGE_SERVER),storageServerClasses);
        }

        //UES server
        if(modules.findChildNodeByName(Constants.UES)!=null){
            List<Class> uesClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.UES),uesClasses,UESTenantLoginTest.class);
        }

        //Gitblit server
        if(modules.findChildNodeByName(Constants.GITBLIT)!=null){
            List<Class> gitblitClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.GITBLIT),gitblitClasses,GitTenantLoginTest.class);
        }

        //Gitblit server
        if(modules.findChildNodeByName(Constants.JENKINS)!=null){
            List<Class> jenkinsClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.JENKINS),jenkinsClasses,JenkinsTenantLoginTest.class);
        }

        //S2 Gitblit server
        if(modules.findChildNodeByName(Constants.S2_GITBLIT)!=null){
            List<Class> s2gitblitClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.S2_GITBLIT),s2gitblitClasses,GitTenantLoginTest.class);
        }

        //Cloud Controller
        if(modules.findChildNodeByName(Constants.CLOUD_CONTROLLER)!=null){
            List<Class> cloudControllerClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.CLOUD_CONTROLLER),cloudControllerClasses);
        }

        //Business Activity Monitor
        if(modules.findChildNodeByName(Constants.BUSINESS_ACTIVITY_MONITOR)!=null){
            List<Class> bamClasses = new ArrayList<Class>();
            bamClasses.add(HiveScriptExecutionTest.class);
            bamClasses.add(CassandraLogColumnFamilyExistanceTest.class);
            scheduleJobs(modules.findChildNodeByName(Constants.BUSINESS_ACTIVITY_MONITOR),bamClasses);
        }

        //Task Server
        if(modules.findChildNodeByName(Constants.TASK_SERVER)!=null){
            List<Class> taskServerClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.TASK_SERVER),taskServerClasses);
        }

        //Stratos Controllers (Development, Test, Production)
        if(modules.findChildNodeByName(Constants.STRATOS_CONTROLLER_DEV)!=null){
            List<Class> scDevClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.STRATOS_CONTROLLER_DEV),scDevClasses);
        }
        if(modules.findChildNodeByName(Constants.STRATOS_CONTROLLER_TEST)!=null){
            List<Class> scTestClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.STRATOS_CONTROLLER_TEST), scTestClasses);
        }
        if(modules.findChildNodeByName(Constants.STRATOS_CONTROLLER_PROD)!=null){
            List<Class> scProdClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.STRATOS_CONTROLLER_PROD), scProdClasses);
        }
      // local Application Servers
        if(modules.findChildNodeByName(Constants.APPLICATION_SERVER)!=null){
            List<Class> asClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.APPLICATION_SERVER),asClasses);
        }

        //Application Servers (Development, Test, Production)
        if(modules.findChildNodeByName(Constants.APPLICATION_SERVER_DEV)!=null){
            List<Class> asDevClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.APPLICATION_SERVER_DEV),asDevClasses);
        }
        if(modules.findChildNodeByName(Constants.APPLICATION_SERVER_TEST)!=null){
            List<Class> asTestClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.APPLICATION_SERVER_TEST),asTestClasses);
        }
        if(modules.findChildNodeByName(Constants.APPLICATION_SERVER_PROD)!=null){
            List<Class> asProdClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.APPLICATION_SERVER_PROD),asProdClasses);
        }

        //Enterprise Service Bus (Development, Test, Production)
        if(modules.findChildNodeByName(Constants.ESB_DEV)!=null){
            List<Class> esbDevClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.ESB_DEV),esbDevClasses);
        }
        if(modules.findChildNodeByName(Constants.ESB_TEST)!=null){
            List<Class> esbTestClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.ESB_TEST),esbTestClasses);
        }
        if(modules.findChildNodeByName(Constants.ESB_PROD)!=null){
            List<Class> esbProdClasses= new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.ESB_PROD),esbProdClasses);
        }

        //Business Process Server (Development, Test, Production)
        if(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER_DEV)!=null){
            List<Class> bpsDevClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER_DEV),bpsDevClasses);
        }
        if(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER_TEST)!=null){
            List<Class> bpsTestClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER_TEST),bpsTestClasses);
        }
        if(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER_PROD)!=null){
            List<Class> bpsProdClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.BUSINESS_PROCESS_SERVER_PROD),bpsProdClasses);
        }

        //API Manager
        if(modules.findChildNodeByName(Constants.API_MANAGER)!=null){
            List<Class> apiManagerClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.API_MANAGER),apiManagerClasses);
        }

        /*
         * API Cloud
         */
        //API  - Gateway
        if(modules.findChildNodeByName(Constants.API_GATEWAY)!=null){
            List<Class> gatewayClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.API_GATEWAY),gatewayClasses);
        }

        //API - Store
        if(modules.findChildNodeByName(Constants.API_STORE)!=null){
            List<Class> storeClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.API_STORE),storeClasses);
        }

        //API - Publisher
        if(modules.findChildNodeByName(Constants.API_PUBLISHER)!=null){
            List<Class> publisherClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.API_PUBLISHER),publisherClasses);
        }

        //API - KeyManager
        if(modules.findChildNodeByName(Constants.API_KEY_MANAGER)!=null){
            List<Class> keyManagerClasses = new ArrayList<Class>();
            scheduleJobs(modules.findChildNodeByName(Constants.API_KEY_MANAGER),keyManagerClasses);
        }
    }

    private void scheduleJobs(Node moduleNode, List<Class> classes) throws SchedulerException {
        JobScheduler jobScheduler;
        jobScheduler = new JobScheduler(scheduler, moduleNode, heartbeatTenant, classes);
        jobScheduler.schedule();
    }

    private void scheduleJobs(Node moduleNode, List<Class> classes, Class loginTestClass) throws SchedulerException {
        JobScheduler jobScheduler;
        jobScheduler = new JobScheduler(scheduler, moduleNode, heartbeatTenant, classes);
        jobScheduler.schedule(loginTestClass);
    }

    /**
     * Builds configuration nodes tree
     * @throws java.io.IOException
     */
    private void buildConfTree() throws IOException {
        rootNode.setName("root");
        NodeBuilder.buildNode(rootNode, FileManager.readFile(Constants.HEARTBEAT_CONF_PATH));
    }
}
