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

package org.wso2.cloud.heartbeat.monitor.modules.ues;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.rmi.RemoteException;
import java.sql.Connection;

/**
 * Tenant login test scenario for a Cloud setup, implemented in this class
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class UESTenantLoginTest implements Job{

    private static final Log log = LogFactory.getLog(UESTenantLoginTest.class);
    private final String TEST_NAME = "TenantLoginTest";

    private String tenantUser;
    private String tenantUserPwd;
    private String hostName;
    private String serviceName;
    private int requestCount = 0;

    private CarbonAuthenticatorClient carbonAuthenticatorClient;

    /**
     * @param jobExecutionContext
     * "hostName" ,"tenantUser", "tenantUserPwd" "serviceName" params passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        initializeLoginTest();
        //test scenario
        login();
    }

    private void initializeLoginTest() {
        try {
            carbonAuthenticatorClient = new CarbonAuthenticatorClient(hostName + "/admin");
        } catch (AxisFault axisFault) {
            log.error(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " + hostName +
                    ": AxisFault thrown while initiating the test : ", axisFault);
        }
    }

    /**
     * checks login for a service
     */
    private void login(){
        try {
            boolean loginStatus=
                    carbonAuthenticatorClient.checkLogin(tenantUser, tenantUserPwd, hostName);
            if(loginStatus){
                onSuccess();
            } else {
                countNoOfLoginRequests("LoginError", null);
            }
        }  catch (RemoteException e) {
            countNoOfLoginRequests("RemoteException", e);
        } catch (LoginAuthenticationExceptionException e) {
            countNoOfLoginRequests("LoginAuthenticationExceptionException", e);
        } catch (Exception e) {
            countNoOfLoginRequests("Exception", e);
        }
    }

    private void countNoOfLoginRequests(String type, Object obj) {
        requestCount++;
        if(requestCount == 3){
            handleError(type, obj);
            requestCount = 0;
        }
        else {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                //Exception ignored
            }
            login();
        }
    }

    private void handleError(String type, Object obj) {
        if(type.equals("LoginError")) {
            log.error(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " + hostName +
                    ": Login failure. Returned false as a login status by Server");
            onFailure("Tenant login failure");
        }else if(type.equals("AxisFault")) {
            AxisFault axisFault = (AxisFault) obj;
            log.error(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " + hostName +
                    ": AxisFault thrown while authenticating the stub : ", axisFault);
            onFailure(axisFault.getMessage());
        }else if(type.equals("RemoteException")) {
            RemoteException remoteException = (RemoteException) obj;
            log.error(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " + hostName +
                    ": RemoteException thrown while login from Heartbeat tenant : ", remoteException);
            onFailure(remoteException.getMessage());
        }else if(type.equals("LoginAuthenticationExceptionException")) {
            LoginAuthenticationExceptionException e = (LoginAuthenticationExceptionException) obj;
            log.error(CaseConverter.splitCamelCase(serviceName)+" - Tenant Login: " + hostName +
                    ": LoginAuthenticationException thrown while login from Heartbeat tenant : ", e);
            onFailure(e.getMessage());
        }else if(type.equals("Exception")) {
            Exception e = (Exception) obj;
            log.error(CaseConverter.splitCamelCase(serviceName)+" - Tenant Login: " + hostName +
                    ": Exception thrown while login from Heartbeat tenant : ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * On test success
     */
    private void onSuccess() {
        boolean success = true;
        DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
        Connection connection = dbConnectionManager.getConnection();

        long timestamp = System.currentTimeMillis();
        DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);

        log.info(CaseConverter.splitCamelCase(serviceName)+ " - Tenant Login: SUCCESS");
    }

    /**
     * On test failure
     * @param msg error message
     */
    private void onFailure(String msg) {

        boolean success = false;
        DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
        Connection connection = dbConnectionManager.getConnection();

        long timestamp = System.currentTimeMillis();
        DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);
        DbConnectionManager.insertFailureDetail(connection, timestamp, serviceName, TEST_NAME, msg);

        Mailer mailer = Mailer.getInstance();
        mailer.send(CaseConverter.splitCamelCase(serviceName) + " :FAILURE",
                CaseConverter.splitCamelCase(TEST_NAME) + ": " + msg, "");
        SMSSender smsSender = SMSSender.getInstance();
        smsSender.send(CaseConverter.splitCamelCase(serviceName) + ": " +
                CaseConverter.splitCamelCase(TEST_NAME) + ": Failure");
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
     * Sets service host
     * @param hostName Service host
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Sets Service name
     * @param serviceName Service name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
