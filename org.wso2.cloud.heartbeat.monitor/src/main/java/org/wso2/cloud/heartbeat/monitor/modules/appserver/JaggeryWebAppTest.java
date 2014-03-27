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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.application.JaggeryAppAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.application.WebAppAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.Connection;

/**
 * Jaggery web application test for Application Server implemented in this class
 * Test run with a Jaggery sample name "jaggery_sample"
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class JaggeryWebAppTest implements Job {
    private static final Log log = LogFactory.getLog(JaggeryWebAppTest.class);

    private final String TEST_NAME = "JaggeryWebAppTest";

    private final String ARTIFACT_PATH= "resources" + File.separator + "artifacts"+ File.separator +
                                        "appserver" + File.separator + "jaggery"+ File.separator +
                                        "jaggery_sample.zip";

    private final String ARTIFACT_NAME="jaggery_sample";

    /*
     * Heartbeat tenant credentials
     */
    private String managementHostName;
    private String hostName;
    private String tenantUser;
    private String tenantUserPwd;
    private String httpPort;
    private int deploymentWaitTime;

    private JaggeryAppAdminClient jaggeryAppAdminClient;
    private WebAppAdminClient webAppAdminClient;

    private boolean errorsReported;
    private boolean responseCorrect;
    private int requestCount = 0;

    private String tenantUserSecondary;
    private boolean isFirstTenant;

    /**
     * @param jobExecutionContext
     * "managementHostName", "hostName" ,"tenantUser", "tenantUserPwd" "httpPort"
     * "deploymentWaitTime" params passed via JobDataMap.
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        isFirstTenant =true;
        initJaggeryAppTest();

        deployJaggeryApp();
        sendHTTPGET();
        unDeployJaggeryApp();
    }

    /**
     * Initializes Jaggery web application test
     */
    private void initJaggeryAppTest() {
        errorsReported = false;
        responseCorrect = false;
        String sessionCookie;

        try{
            CarbonAuthenticatorClient carbonAuthenticatorClient = new CarbonAuthenticatorClient(managementHostName);
            sessionCookie = carbonAuthenticatorClient.login(tenantUser, tenantUserPwd, managementHostName);
            jaggeryAppAdminClient = new JaggeryAppAdminClient(managementHostName, sessionCookie);
            webAppAdminClient = new WebAppAdminClient(managementHostName, sessionCookie);
        } catch (AxisFault axisFault) {
            log.error("Application Server - Jaggery Webapp: AxisFault thrown while initializing the test: "
                    , axisFault);
            onFailure(axisFault.getMessage());
        } catch (RemoteException e) {
            log.error("Application Server - Jaggery Webapp: RemoteException thrown while initializing " +
                      "the test: ", e);
            onFailure(e.getMessage());
        } catch (LoginAuthenticationExceptionException e) {
            log.error("Application Server - Jaggery Webapp: LoginAuthenticationExceptionException " +
                      "thrown while initializing the test: ", e);
            onFailure(e.getMessage());
        } catch (Exception e) {
            log.error("Application Server - Jaggery Webapp: Exception thrown while initializing the test: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Deploys Jaggery webapp
     */
    private void deployJaggeryApp() {

        //if service exists remove jar and deploy again
        try{
            deleteWarFile();
            uploadWarFile();
        } catch (RemoteException e) {
            log.error("Application Server - Jaggery Webapp: RemoteException thrown while deploying JAXRS" +
                      " service: ", e);
            onFailure(e.getMessage());
        }   catch (MalformedURLException e) {
            log.error("Application Server - Jaggery Webapp: MalformedURLException thrown while deploying" +
                      " Jaggery webapp: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Sends a RESTful GET and assert the response
     */
    private void sendHTTPGET(){
        String serviceUrl = "http://" + ModuleUtils.getHostWithHttpPort(hostName, httpPort) + "/t/"
                            + ModuleUtils.getDomainName(tenantUser) + "/jaggeryapps/jaggery_sample/";

        try {
            String response = get(serviceUrl);
            if(response.contains("This is a sample page")){
                responseCorrect = true;
                requestCount = 0;
            } else {
                countNoOfRequests("ResponseError",null);
            }
        } catch (Exception e) {
            countNoOfRequests("Exception",e);
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
            sendHTTPGET();
        }
    }

    private void handleError(String type, Object obj) {
        if(type.equals("ResponseError")) {
            log.error("Application Server - Jaggery Webapp: Response does not contain required values ");
            onFailure("Response does not contain required values");
        }else if(type.equals("Exception")) {
            Exception e = (Exception) obj;
            log.error("Application Server - Jaggery Webapp: Exception thrown while getting HTTP " +
                    "GET response: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Undeploys Jaggery web application after test
     */
    private void unDeployJaggeryApp() {
        try {
            deleteWarFile();
            //if no errors reported and the response is correct service is healthy
            if(!errorsReported && responseCorrect){
                onSuccess();
            }
        } catch (RemoteException e) {
            log.error("Application Server - Jaggery Webapp: RemoteException thrown while undeploying " +
                      "Jaggery webapp: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Sends a RESTful GET and returns the response
     * @param endpoint End point/Service URL
     * @return Response string
     * @throws Exception
     */
    public String get(String endpoint) throws Exception {
        HttpURLConnection httpCon = null;
        String xmlContent = null;
        int responseCode;
        int connectionTimeOut = 30000;
        try {
            URL url = new URL(endpoint);
            httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setConnectTimeout(connectionTimeOut);
            InputStream in = httpCon.getInputStream();
            responseCode = httpCon.getResponseCode();
            xmlContent = getStringFromInputStream(in);
            in.close();
        } catch (Exception e) {
            throw new Exception("Failed to get the response :" + e);
        } finally {
            if (httpCon != null) {
                httpCon.disconnect();
            }
        }
        if(responseCode == 200){
            if (xmlContent != null){
                return xmlContent;
            } else throw new Exception("Response is null");
        } else throw new Exception("Response code: " +responseCode);
    }

    /**
     * Returns String from the input stream
     * @param in Input stream
     * @return String from the input stream
     * @throws Exception
     */
    private static String getStringFromInputStream(InputStream in) throws Exception {
        InputStreamReader reader = new InputStreamReader(in);
        char[] buff = new char[1024];
        int i;
        StringBuilder retValue = new StringBuilder();
        try {
            while ((i = reader.read(buff)) > 0) {
                retValue.append(new String(buff, 0, i));
            }
        } catch (Exception e) {
            log.error("Failed to get the response " + e);
            throw new Exception("Failed to get the response :" + e);
        }
        return retValue.toString();
    }

    /**
     * Uploads jar to deploy Jaggery webapp
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     */
    private void uploadWarFile()
            throws MalformedURLException, RemoteException {

        try {
            jaggeryAppAdminClient.uploadWarFile(ARTIFACT_NAME, ARTIFACT_PATH);
            Thread.sleep(deploymentWaitTime);
        } catch (InterruptedException ignored) {
            //Exception ignored
        } catch (Exception e){
            log.error("Application Server - Jaggery Webapp: Exception thrown while deploying Jaggery webapp: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Deletes Jaggery webapp jar
     * @throws java.rmi.RemoteException
     */
    private void deleteWarFile() throws RemoteException {
        try{
            webAppAdminClient.deleteWebAppFile(ARTIFACT_NAME);
            Thread.sleep(deploymentWaitTime);
        } catch (InterruptedException ignored) {
            //Exception ignored
        } catch (Exception e){
            log.error("Application Server - Jaggery Webapp: Exception thrown while deleting war file: ", e);
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
        DbConnectionManager.insertLiveStatus(connection, timestamp, "ApplicationServer", TEST_NAME, success);

        log.info("Application Server - Jaggery Webapp: SUCCESS");
    }

    /**
     * On test failure
     * @param msg error message
     */
    private void onFailure(String msg) {
        if(!errorsReported & isFirstTenant){
            isFirstTenant = false;
            setTenantUser(tenantUserSecondary);
            requestCount = 0;
            log.error("Application Server - Jaggery Web app test using first tenant was failed. Trying with the second tenant.");
            initJaggeryAppTest();
            deployJaggeryApp();
            sendHTTPGET();
            unDeployJaggeryApp();
        }

        else if(!errorsReported){
            boolean success = false;
            DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
            Connection connection = dbConnectionManager.getConnection();

            long timestamp  = System.currentTimeMillis();
            DbConnectionManager.insertLiveStatus(connection, timestamp, "ApplicationServer", TEST_NAME, success);
            DbConnectionManager.insertFailureDetail(connection, timestamp, "ApplicationServer", TEST_NAME, msg);

            Mailer mailer = Mailer.getInstance();
            mailer.send("Application Server: FAILURE", CaseConverter.splitCamelCase(TEST_NAME)+": " + msg, "");

            SMSSender smsSender = SMSSender.getInstance();
            smsSender.send("Application Server: "+ CaseConverter.splitCamelCase(TEST_NAME) +": Failure");
            errorsReported = true;
        }
    }

    /**
     * Sets Management service host
     * @param managementHostName Management service host
     */
    public void setManagementHostName(String managementHostName){
        this.managementHostName = managementHostName;
    }

    /**
     * Sets service host
     * @param hostName Service host
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Sets Secondary Tenant user name
     * @param tenantUser Tenant user name
     */
    public void setTenantUser(String tenantUser) {
        this.tenantUser = tenantUser;
    }

    /**
     * Sets Tenant user name
     * @param tenantUserSecondary Tenant user name
     */
    public void setTenantUserSecondary(String tenantUserSecondary) {
        this.tenantUserSecondary = tenantUserSecondary;
    }

    /**
     * Sets Tenant user password
     * @param tenantUserPwd Tenant user password
     */
    public void setTenantUserPwd(String tenantUserPwd) {
        this.tenantUserPwd = tenantUserPwd;
    }

    /**
     * Sets http port
     * @param httpPort Http port
     */
    public void setHttpPort(String httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * Sets deployment waiting time
     * @param deploymentWaitTime Deployment wait time
     */
    public void setDeploymentWaitTime(String deploymentWaitTime) {
        this.deploymentWaitTime = Integer.parseInt(deploymentWaitTime.split("s")[0].replace(" ", ""))*1000;
    }


}
