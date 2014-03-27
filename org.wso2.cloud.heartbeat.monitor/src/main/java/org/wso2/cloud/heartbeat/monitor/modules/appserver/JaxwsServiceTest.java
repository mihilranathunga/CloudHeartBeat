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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.application.JAXWSWebAppAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.application.WebAppAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.sql.Connection;

/**
 * JAXWS service test for Application Server implemented in this class
 * Test run with the JAXWS sample "async_jaxws"
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class JaxwsServiceTest implements Job {

    private static final Log log = LogFactory.getLog(JaxwsServiceTest.class);

    private final String TEST_NAME = "JAXWSServiceTest";

    private final String JAXWS_WAR_FILE_PATH= "resources" + File.separator + "artifacts"+ File.separator +
                                              "appserver" + File.separator + "jaxws"+ File.separator +
                                              "war"+ File.separator + "async_jaxws.war";

    private final String ARTIFACT_NAME="async_jaxws.war";

    /*
     * Heartbeat tenant credentials
     */
    private String managementHostName;
    private String hostName;
    private String tenantUser;
    private String tenantUserPwd;
    private String httpPort;
    private int deploymentWaitTime;

    private JAXWSWebAppAdminClient jaxwsWebAppAdminClient;
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
        initJAXWSServiceTest();

        deployJAXWSService();
        sendGreetingsRequest();
        unDeployJAXWSService();
    }

    /**
     * Initializes the JAXWS service test
     */
    private void initJAXWSServiceTest() {
        errorsReported = false;
        responseCorrect = false;
        String sessionCookie;

        try{
            CarbonAuthenticatorClient carbonAuthenticatorClient = new CarbonAuthenticatorClient(managementHostName);
            sessionCookie = carbonAuthenticatorClient.login(tenantUser, tenantUserPwd, managementHostName);
            jaxwsWebAppAdminClient = new JAXWSWebAppAdminClient(managementHostName, sessionCookie);
            webAppAdminClient = new WebAppAdminClient(managementHostName, sessionCookie);
        } catch (AxisFault axisFault) {
            log.error("Application Server - JAXWS Service: AxisFault thrown while initializing the test: "
                    , axisFault);
            onFailure(axisFault.getMessage());
        } catch (RemoteException e) {
            log.error("Application Server - JAXWS Service: RemoteException thrown while initializing " +
                      "the test: ", e);
            onFailure(e.getMessage());
        } catch (LoginAuthenticationExceptionException e) {
            log.error("Application Server - JAXWS Service: LoginAuthenticationExceptionException " +
                      "thrown while initializing the test: ", e);
            onFailure(e.getMessage());
        } catch (Exception e) {
            log.error("Application Server - JAXWS Service: Exception thrown while initializing the test: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Deploys JAXWS service
     */
    private void deployJAXWSService() {

        //if service exists remove jar and deploy again
        try{
            deleteWarFile();
            uploadWarFile();
        } catch (RemoteException e) {
            log.error("Application Server - JAXWS Service: RemoteException thrown while deploying JAXWS" +
                      " service: ", e);
            onFailure(e.getMessage());
        }   catch (MalformedURLException e) {
            log.error("Application Server - JAXWS Service: MalformedURLException thrown while deploying" +
                      " JAXWS service: ", e);
            onFailure(e.getMessage());
        } catch (Exception e) {
            log.error("Application Server - JAXWS Service: Exception thrown while deploying JAXWS service: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Sends SOAP request ans assert the response
     */
    private void sendGreetingsRequest() {
        Options options = setOptions();
        ServiceClient serviceClient;

        try{
            serviceClient = new ServiceClient();
            serviceClient.setOptions(options);
            OMElement response = serviceClient.sendReceive(createStandardRequest("WSO2"));
            if(response.toString().contains("How are you WSO2")){
                responseCorrect = true;
                requestCount = 0;
            } else {
                countNoOfRequests("ResponseError",null);
            }
        } catch (AxisFault axisFault) {
            countNoOfRequests("AxisFault",axisFault);
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
            sendGreetingsRequest();
        }
    }

    private void handleError(String type, Object obj) {
        if(type.equals("ResponseError")) {
            log.error("Application Server - JAXWS Service: Response doesn't contain required values.");
            onFailure("Response doesn't contain required values");
        }else if(type.equals("AxisFault")) {
            AxisFault axisFault = (AxisFault) obj;
            log.error("Application Server - JAXWS Service: AxisFault thrown while sending calculate request: "
                    , axisFault);
            onFailure(axisFault.getMessage());
        }else if(type.equals("Exception")) {
            Exception e = (Exception) obj;
            log.error("Application Server - JAXWS Service: Exception thrown while sending calculate request: "
                    , e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Undeploys JAXWS service after test
     */
    private void unDeployJAXWSService() {
        try {
            deleteWarFile();
            //if no errors reported and the response is correct service is healthy
            if(!errorsReported && responseCorrect){
                onSuccess();
            }
        } catch (RemoteException e) {
            log.error("Application Server - JAXWS Service: RemoteException thrown while undeploying " +
                      "JAXWS service: ", e);
            onFailure(e.getMessage());
        } catch (Exception e) {
            log.error("Application Server - JAXWS Service: Exception thrown while undeploying JAXWS " +
                      "service: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Uploads jar to deploy JAXWS service
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     */
    private void uploadWarFile()
            throws MalformedURLException, RemoteException {

        try {
            jaxwsWebAppAdminClient.uploadWarFile(ARTIFACT_NAME, JAXWS_WAR_FILE_PATH);
            Thread.sleep(deploymentWaitTime);
        } catch (InterruptedException ignored) {
            //Exception ignored
        } catch (NullPointerException e){
            log.error("Application Server - JAXWS Service: NullPointerException thrown while " +
                      "deploying JAXWS service: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Deletes JAXWS service jar
     * @throws java.rmi.RemoteException
     */
    private void deleteWarFile() throws RemoteException {
        try{
            webAppAdminClient.deleteWebAppFile(ARTIFACT_NAME);
            Thread.sleep(deploymentWaitTime);
        } catch (InterruptedException ignored) {
            //Exception ignored
        } catch (NullPointerException e){
            log.error("Application Server - JAXWS Service: NullPointerException thrown while deleting war " +
                      "file: ", e);
            onFailure(e.getMessage());
        }
    }

    /**
     * Creates OMElement as required for calculator request (adding)
     * @param symbol  value
     * @return OMElement
     */
    private OMElement createStandardRequest(String symbol) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://apache.org/hello_world_async_soap_http/types", "ns");
        OMElement method = fac.createOMElement("greetMeSometime", omNs);
        OMElement value = fac.createOMElement("requestType", omNs);

        value.addChild(fac.createOMText(method, symbol));
        method.addChild(value);
        return method;
    }

    /**
     * Sets options of the Axis2ServiceClient
     * @return Options
     */
    private Options setOptions() {
        Options options;
        options = new Options();
        String trpUrl = "http://" + ModuleUtils.getHostWithHttpPort(hostName, httpPort) + "/t/"
                        + ModuleUtils.getDomainName(tenantUser) + "/jaxwebapps/async_jaxws/services/AsyncServicePort";
        EndpointReference endpointReference= new EndpointReference(trpUrl);
        options.setTo(endpointReference);
//        options.setAction("urn:greetMeSometime");
        return options;
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

        log.info("Application Server - JAXWS Service: SUCCESS");
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
            log.error("Application Server - JAXWS Service test using first tenant was failed. Trying with the second tenant.");
            initJAXWSServiceTest();
            deployJAXWSService();
            sendGreetingsRequest();
            unDeployJAXWSService();
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
    public void setManagementHostName(String managementHostName) {
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
