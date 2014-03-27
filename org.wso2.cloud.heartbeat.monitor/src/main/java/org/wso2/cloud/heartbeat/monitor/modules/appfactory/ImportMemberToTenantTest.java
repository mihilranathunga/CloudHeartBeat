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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.JaggeryAppAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.https.HttpsJaggeryClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.JagApiProperties;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class ImportMemberToTenantTest implements Job {

    private static final Log log = LogFactory.getLog(ImportMemberToTenantTest.class);

    private final String TEST_NAME = "ImportMemberToTenantTest";

    private String hostName;
    private String tenantUser;
    private String tenantUserPwd;
    private int deploymentWaitTime;
    private String serviceName;

    private String completeTestName;

    private boolean errorsReported;
    private int requestCount = 0;

    private JaggeryAppAuthenticatorClient authenticatorClient;
    private boolean isTenantAdmin = false;
    private boolean loginStatus = false;

    private String memberName = "heartbeat-member";
    private String memberUserName;
    private String memberDefaultPassword = "admin";


    /**
     * @param jobExecutionContext
     * "managementHostName", "hostName" ,"tenantUser", "tenantUserPwd" "httpPort"
     * "deploymentWaitTime" params passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        setCompleteTestName(CaseConverter.splitCamelCase(serviceName) + " - Import Member Test : ");
        initWebAppTest();
        if(!errorsReported){
            addMember();
        }
        if(!errorsReported){
            updateMember();
        }
        if(!errorsReported){
            loginWithMember();
        }
        if(!errorsReported){
            deleteMember();
        }
    }

    /**
     * Initializes Web application service test
     */
    private void initWebAppTest() {
        errorsReported = false;
        hostName = "https://" + hostName;

        memberUserName = memberName +"@"+ ModuleUtils.getDomainName(tenantUser);

        authenticatorClient = new JaggeryAppAuthenticatorClient(hostName);
        loginStatus = authenticatorClient.login(tenantUser,tenantUserPwd);
        isTenantAdmin = true;
    }

    /**
     * Add Member to tenant
     */
    private void addMember() {
        if(loginStatus){
            String url = hostName + JagApiProperties.ADD_USER_TO_TENANT_URL_SFX;
            Map<String, String> params = new HashMap<String, String>();
            params.put("action", "bulkImportUsers");
            params.put("users", memberName);
            params.put("defaultPassword", memberDefaultPassword);
            String result = HttpsJaggeryClient.httpPost(url, params);
            if(result.equals("false")){
                countNoOfRequests("FailedMemberAddition","addMember");
            }
        }else {
            countNoOfRequests("LoginError","addMember");
        }
    }

    /**
     * Updates the member with 'developer' role
     */
    private void updateMember() {
        if(loginStatus){
            String url = hostName + JagApiProperties.ADD_USER_TO_TENANT_URL_SFX;
            Map<String, String> params = new HashMap<String, String>();
            params.put("action", "updateUserRoles");
            params.put("rolesToAdd", "developer");
            params.put("rolesToDelete", "");
            params.put("userName", memberName);
            String result = HttpsJaggeryClient.httpPost(url, params);
            if(result.equals("false")){
                countNoOfRequests("FailedMemberUpdate","updateMember");
            }
        }else {
            countNoOfRequests("LoginError","updateMember");
        }
    }

    /**
     * Log in with the created member
     */
    private void loginWithMember() {
        authenticatorClient.logout();
        loginStatus = false;
        isTenantAdmin = false;
        loginStatus = authenticatorClient.login(memberUserName ,memberDefaultPassword);
        if(!loginStatus){
            countNoOfRequests("LoginError","loginWithMember");
        }
        authenticatorClient.logout();
    }

    private void deleteMember() {
        loginStatus = authenticatorClient.login(tenantUser,tenantUserPwd);
        isTenantAdmin = true;
        if(loginStatus){
            String url = hostName + JagApiProperties.ADD_USER_TO_TENANT_URL_SFX;
            Map<String, String> params = new HashMap<String, String>();
            params.put("action", "deleteUserFromTenant");
            params.put("userName", memberName);
            String result = HttpsJaggeryClient.httpPost(url, params);
            if(result.equals("true")){
                onSuccess();
            }else {
                countNoOfRequests("FailedMemberDeletion","deleteMember");
            }
        }else {
            countNoOfRequests("LoginError","deleteMember");
        }
    }

    private void countNoOfRequests(String type,String method) {
        requestCount++;
        if(requestCount == 3){
            handleError(type,method);
            requestCount = 0;
        }
        else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                //Exception ignored
            }

            //Logs only if this login needs to be a tenant admin login, therefore ignores 'loginWithMember'
            if(type.equals("LoginError") && !method.equals("loginWithMember")){
                loginStatus = authenticatorClient.login(tenantUser,tenantUserPwd);
            }

            if(method.equals("addMember")){
                addMember();
            }else if (method.equals("updateMember")){
                updateMember();
            }else if (method.equals("loginWithMember")){
                loginWithMember();
            }else if (method.equals("deleteMember")){
                deleteMember();
            }
        }
    }

    private void handleError(String type,String method) {
        if(type.equals("LoginError")) {
            String msg = null;
            //Which method gave the error
            if(method.equals("addMember")){
                msg = "Adding Member";
            }else if (method.equals("updateMember")){
                msg = "Updating Member";
            }else if (method.equals("loginWithMember")){
                msg = "Login with Member";
            }else if (method.equals("deleteMember")){
                msg = "Deleting Member";
            }
            if(isTenantAdmin){
                msg = "Tenant Admin login failure to appmgt jaggery app while " + msg + ". Returned false as a login status.";
            } else {
                msg = msg + "Member login failure to appmgt jaggery app while " + msg + ". Returned false as a login status.";
            }
            log.error(completeTestName + msg);
            onFailure(msg);
        } else if(type.equals("FailedMemberAddition")) {
            log.error(completeTestName + "Failed to add member into tenant.");
            onFailure("Failed to add member into tenant");
        } else if(type.equals("FailedMemberUpdate")) {
            log.error(completeTestName + "Failed to update the newly added member in tenant.");
            onFailure("Failed to update the newly added member in tenant");
        }else if(type.equals("FailedMemberDeletion")) {
            log.error(completeTestName + "Failed to delete the newly added member in tenant.");
            onFailure("Failed to delete the newly added member in tenant");
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
     * Sets Display Service name
     * @param completeTestName Service name
     */
    public void setCompleteTestName(String completeTestName) {
        this.completeTestName = completeTestName;
    }

}
