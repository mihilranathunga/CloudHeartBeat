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

package org.wso2.cloud.heartbeat.monitor.modules.appserver;

import org.apache.axis2.AxisFault;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.application.WebAppAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.Connection;

/**
 * Web application test for Application Server implemented in this class
 * Test runs with a Web application sample "SimpleServlet"
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class WebAppTest implements Job {

    private static final Log log = LogFactory.getLog(WebAppTest.class);

    private final String TEST_NAME = "WebAppTest";

    private final String WAR_FILE_DIRECTORY = "resources" + File.separator + "artifacts"+ File.separator +
                                              "appserver" + File.separator + "webapp"+ File.separator +
                                              "war"+ File.separator;
    /*
     * Heartbeat tenant credentials
     */
    private String hostName;
    private String tenantUser;
    private String tenantUserPwd;
    private int deploymentWaitTime;
    private String serviceName;

    private String completeTestName;
    private WebAppAdminClient webAppAdminClient;

    private boolean errorsReported;
    private boolean responseCorrect;
    private int requestCount = 0;

    /**
     * @param jobExecutionContext
     * "managementHostName", "hostName" ,"tenantUser", "tenantUserPwd" "httpPort"
     * "deploymentWaitTime" params passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        setCompleteTestName(CaseConverter.splitCamelCase(serviceName) + " - Web App: ");
        initWebAppTest();
        if (!errorsReported) {
            deployWebApp();
        }
        if (!errorsReported) {
            sendGETRequest();
        }
        if (!errorsReported) {
            unDeployWebApp();
        }
    }

    /**
     * Initializes Web application service test
     */
    private void initWebAppTest() {
        errorsReported = false;
        responseCorrect = false;

        try{
            CarbonAuthenticatorClient carbonAuthenticatorClient = new CarbonAuthenticatorClient(hostName);
            String sessionCookie = carbonAuthenticatorClient.login(tenantUser, tenantUserPwd, hostName);
            webAppAdminClient=new WebAppAdminClient(hostName, sessionCookie);
        } catch (AxisFault axisFault) {
            log.error(completeTestName + "AxisFault thrown while initializing the test: ",
                      axisFault);
            onFailure(axisFault.getMessage());
        } catch (RemoteException e) {
            log.error(completeTestName + "RemoteException thrown while initializing the " +
                      "test: ", e);
            onFailure(e.getMessage());
        } catch (LoginAuthenticationExceptionException e) {
            log.error(completeTestName + "LoginAuthenticationExceptionException thrown while" +
                      " initializing the test: ", e);
            onFailure(e.getMessage());
        } catch (Exception e) {
            log.error(completeTestName + "Exception thrown while initializing the test: ", e);
            onFailure(e.getMessage());
        }

    }

    /**
     * Deploys web app
     */
    private void deployWebApp()   {
        try {
            warFileUploader("SimpleServlet.war");
            Thread.sleep(deploymentWaitTime);
        } catch (InterruptedException ignored) {
            //Exception ignored
        } catch (RemoteException e) {
            log.error(completeTestName + "RemoteException thrown while deploying web app: ", e);
            onFailure(e.getMessage());
        } catch (MalformedURLException e) {
            log.error(completeTestName + "MalformedURLException thrown while deploying web app: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Sends get request to web app url
     */
    private void sendGETRequest() {
        String url;
        url = "https://" + hostName + "/t/" + ModuleUtils.getDomainName(tenantUser)
              + "/webapps/SimpleServlet/simple-servlet";
        HttpMethod method = new GetMethod(url);
        HttpClient httpClient = new HttpClient();

        try{
            int HTTP_OK = 200;
            if(httpClient.executeMethod(method)== HTTP_OK){
                String responseBody = method.getResponseBodyAsString();
                if(responseBody.contains("Hello, World")){
                    responseCorrect = true;
                    requestCount = 0;
                } else {
                    countNoOfRequests("ResponseError",null);
                }
            }
            else {
                countNoOfRequests("ResponseError",null);
            }
        } catch (HttpException e) {
            countNoOfRequests("HttpException",e);
        } catch (IOException e) {
            countNoOfRequests("IOException",e);
        }
    }

    private void countNoOfRequests(String type, Object obj) {
        requestCount++;
        if(requestCount == 3){
            handleError(type, obj);
            requestCount = 0;
        }
        else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                //Exception ignored
            }
            sendGETRequest();
        }
    }

    private void handleError(String type, Object obj) {
        if(type.equals("ResponseError")) {
            log.error(completeTestName + "Response doesn't contain required values.");
            onFailure("Response doesn't contain required values");
        }else if(type.equals("HttpException")) {
            HttpException httpException = (HttpException) obj;
            log.error(completeTestName + "HttpException thrown while sending GET request: ", httpException);
            onFailure(httpException.getMessage());
        }else if(type.equals("IOException")) {
            IOException ioException = (IOException) obj;
            log.error(completeTestName + "IOException thrown while sending GET request: ", ioException);
            onFailure(ioException.getMessage());
        }
    }

    /**
     * Undeploys web app after test
     */
    private void unDeployWebApp(){
        try {
            deleteWarFile("SimpleServlet.war");
            //if no errors reported and response is correct
            if(!errorsReported && responseCorrect){
                onSuccess();
            }
        } catch (RemoteException e) {
            log.error(completeTestName + "RemoteException thrown while undeploying web app: ", e);
            onFailure(e.getMessage());
        } catch (Exception e) {
            log.error(completeTestName + "Exception thrown while undeploying web app: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Deletes war file
     * @param artifactName war file name
     * @throws java.rmi.RemoteException
     */
    private void deleteWarFile(String artifactName) throws RemoteException {
        try{
            webAppAdminClient.deleteWebAppFile(artifactName);
        } catch (Exception e){
            log.error(completeTestName + "Exception thrown while deleting war file: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Uploads war file
     * @param artifactName war file name
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException     *
     */
    private void warFileUploader(String artifactName)
            throws RemoteException, MalformedURLException {
        String filePath=WAR_FILE_DIRECTORY + artifactName;

        try {
            webAppAdminClient.warFileUploader(artifactName, filePath);
            Thread.sleep(deploymentWaitTime);
        } catch (InterruptedException ignored) {
            //Exception ignored
        } catch (Exception e){
            log.error(completeTestName + "Exception thrown while deploying web app: ", e);
            onFailure(e.getMessage());
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
