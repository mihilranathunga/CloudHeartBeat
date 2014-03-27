/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.cloud.heartbeat.monitor.modules.appfactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.JaggeryAppAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.https.HttpsJaggeryClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.JagApiProperties;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.modules.appfactory.entities.BuildInfo;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Application Build test for Appfactory implemented in this class
 * Test runs with a Web application sample "SimpleServlet"
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class ApplicationBuildTest implements Job {

    private static final Log log = LogFactory.getLog(ApplicationBuildTest.class);

    private final String TEST_NAME = "ApplicationBuildTest";

    private String hostName;
    private String tenantUser;
    private String tenantUserPwd;
    private int deploymentWaitTime;
    private String serviceName;
    private String applicationKey;

    private String completeTestName;

    private boolean errorsReported;
    private int requestCount = 0;

    private JaggeryAppAuthenticatorClient authenticatorClient;
    private BuildInfo lastBuildInfo;
    private boolean loginStatus = false;


    /**
     * @param jobExecutionContext
     * "managementHostName", "hostName" ,"tenantUser", "tenantUserPwd" "httpPort"
     * "deploymentWaitTime" params passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        setCompleteTestName(CaseConverter.splitCamelCase(serviceName) + " - Application Build Test : ");
        initWebAppTest();
        if(!errorsReported){
            triggerBuild();
        }
        if(!errorsReported){
            testBuildStatus();
        }
    }

    /**
     * Initializes Web application service test
     */
    private void initWebAppTest() {
        errorsReported = false;
        hostName = "https://" + hostName;
        authenticatorClient = new JaggeryAppAuthenticatorClient(hostName);
        loginStatus = authenticatorClient.login(tenantUser,tenantUserPwd);
        try {
            lastBuildInfo = getBuildInfo();
        } catch (Exception e) {
            log.error(completeTestName + "Login failure to appmgt jaggery app. Returned false as a login status.");
            onFailure("Tenant login failure to appmgt jaggery app.");
        }
    }

    /**
     * Triggers a build
     */
    private void triggerBuild() {
        if(loginStatus){
            Map<String, String> params = new HashMap<String, String>();
            params.put("action", "createArtifact");
            params.put("applicationKey", applicationKey);
            params.put("doDeploy", "true");
            params.put("revision", "");
            params.put("stage", "Development");
            params.put("tagName", "");
            params.put("version", "trunk");
            String url =   hostName + JagApiProperties.BUILD_APPLICATION_URL_SFX ;
            HttpsJaggeryClient.httpPost(url, params);
        }else{
            countNoOfRequests("LoginError");
        }
    }

    /**
     * Test the build by comparing the last build and current build
     * Checks whether build no has incremented by 1 and build status is successful
     */
    private void testBuildStatus() {
        try {
            Thread.sleep(deploymentWaitTime);
            loginStatus = authenticatorClient.login(tenantUser,tenantUserPwd);
            BuildInfo currentBuildInfo = getBuildInfo();
            if(currentBuildInfo.getBuildNo() == lastBuildInfo.getBuildNo()+1 && currentBuildInfo.getBuildStatus().equals("successful")){
                authenticatorClient.logout();
                onSuccess();
            }else{
                String msg;
                if(currentBuildInfo.getBuildNo()== lastBuildInfo.getBuildNo()){
                    msg = " Build was not triggered.";
                }else {
                    msg = " Build was not successful.";
                }
                onFailure(msg);
            }
        } catch (Exception e) {
            log.error(completeTestName + "Login failure to appmgt jaggery app. Returned false as a login status.");
            onFailure("Tenant login failure to appmgt jaggery app.");
        }
    }

    private void countNoOfRequests(String type) {
        requestCount++;
        if(requestCount == 3){
            handleError(type);
            requestCount = 0;
        }
        else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                //Exception ignored
            }
            loginStatus = authenticatorClient.login(tenantUser,tenantUserPwd);
            triggerBuild();
        }
    }

    private void handleError(String type) {
        if(type.equals("LoginError")) {
            log.error(completeTestName + "Login failure to appmgt jaggery app. Returned false as a login status.");
            onFailure("Tenant login failure to appmgt jaggery app.");
        } else if(type.equals("ResponseError")) {
            log.error(completeTestName + "Response doesn't contain required values.");
            onFailure("Response doesn't contain required values");
        }
    }

    /**
     * Gets last build information of a application
     * @return BuildInfo Last Build Information
     */
    private BuildInfo getBuildInfo() throws Exception {

        if(loginStatus){
            Map<String, String> params = new HashMap<String, String>();
            params.put("action", "getbuildandrepodata");
            params.put("applicationKey", applicationKey);
            params.put("buildable", "true");
            params.put("isRoleBasedPermissionAllowed", "false");
            params.put("metaDataNeed", "false");
            params.put("userName", tenantUser);
            String buildInfoUrl =   hostName + JagApiProperties.BUILD_INFO_URL_SFX;
            String result = HttpsJaggeryClient.httpPost(buildInfoUrl,params);

            JsonParser parser = new JsonParser();
            JsonArray resultAsJsonArray = parser.parse(result).getAsJsonArray();
            JsonObject resultAsJsonObject = resultAsJsonArray.get(0).getAsJsonObject();
            JsonObject buildJsonObject = resultAsJsonObject.get("build").getAsJsonObject();

            int lastBuildNo = buildJsonObject.get("lastBuildId").getAsInt();
            String lastBuildStatus = buildJsonObject.get("status").getAsString();
            return new BuildInfo(lastBuildNo,lastBuildStatus);
        } else {
            throw new Exception("Login failure to appmgt jaggery app. Returned false as a login status.");
        }
    }

    /**
     * On success
     */
    private void onSuccess() {
        boolean success = true;
        DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
        Connection connection = dbConnectionManager.getConnection();

        long timestamp = System.currentTimeMillis();
        DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);

        log.info(completeTestName + "SUCCESS");
    }

    /**
     * On failure
     * @param msg fault message
     */
    private void onFailure(String msg) {
        if(!errorsReported){
            boolean success = false;
            DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
            Connection connection = dbConnectionManager.getConnection();

            long timestamp  = System.currentTimeMillis();
            DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);
            DbConnectionManager.insertFailureDetail(connection, timestamp, serviceName, TEST_NAME, msg);

            Mailer mailer = Mailer.getInstance();
            mailer.send(CaseConverter.splitCamelCase(serviceName) + ": FAILURE", CaseConverter.splitCamelCase(TEST_NAME)+": " + msg, "");

            SMSSender smsSender = SMSSender.getInstance();
            smsSender.send(CaseConverter.splitCamelCase(serviceName) +": "+ CaseConverter.splitCamelCase(TEST_NAME) +": Failure");
            errorsReported = true;
        }
    }

    /**
     * Sets service host
     * @param hostName Service host
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Sets Tenant user name
     * @param tenantUser Tenant user name
     */
    public void setTenantUser(String tenantUser) {
        this.tenantUser = tenantUser;
    }

    /**
     * Sets Tenant user password
     * @param tenantUserPwd Tenant user password
     */
    public void setTenantUserPwd(String tenantUserPwd) {
        this.tenantUserPwd = tenantUserPwd;
    }

    /**
     * Sets deployment waiting time
     * @param deploymentWaitTime Deployment wait time
     */
    public void setDeploymentWaitTime(String deploymentWaitTime) {
        this.deploymentWaitTime = Integer.parseInt(deploymentWaitTime.split("s")[0].replace(" ", ""))*1000;
    }

    /**
     * Sets Service name
     * @param serviceName Service name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Sets Application key
     * @param applicationKey Application key
     */
    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    /**
     * Sets Display Service name
     * @param completeTestName Service name
     */
    public void setCompleteTestName(String completeTestName) {
        this.completeTestName = completeTestName;
    }

}