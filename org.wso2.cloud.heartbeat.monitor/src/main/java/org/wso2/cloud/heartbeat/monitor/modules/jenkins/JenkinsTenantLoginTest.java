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

package org.wso2.cloud.heartbeat.monitor.modules.jenkins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.quartz.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;

/**
 * Tenant login test scenario for a Jenkins Cloud setup, implemented in this class
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class JenkinsTenantLoginTest implements Job{

    private static final Log log = LogFactory.getLog(JenkinsTenantLoginTest.class);
    private final String TEST_NAME = "TenantLoginTest";

    private String tenantUser;
    private String tenantUserPwd;
    private String hostName;
    private String serviceName;
    private int requestCount = 0;
    private String jenkinsTenantUrl;

    private DefaultHttpClient httpClient;
    private BasicHttpContext context;

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
        jenkinsTenantUrl = "https://" + hostName +  "/t/" + ModuleUtils.getDomainName(tenantUser)
                + "/webapps/jenkins";

        httpClient = new DefaultHttpClient();
        //provide the credentials
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(tenantUser, tenantUserPwd));

        // Generate BASIC scheme object and stick it to the execution context
        BasicScheme basicAuth = new BasicScheme();
        context = new BasicHttpContext();
        context.setAttribute("preemptive-auth", basicAuth);

        // Add as the first (because of the zero) request interceptor
        // It will first intercept the request and preemptively initialize the authentication scheme if there is not
        httpClient.addRequestInterceptor(new PreemptiveAuth(), 0);
    }

    /**
     * checks login for a service
     */
    private void login(){
        String getUrl = jenkinsTenantUrl + "/api/xml";
        HttpGet httpGet = new HttpGet(getUrl);
        int HTTP_SUCCESS = 200;
        try {
            // Execute your request with the given context
            HttpResponse response = httpClient.execute(httpGet, context);
            int code = response.getStatusLine().getStatusCode();
            ResponseHandler<String> handler = new BasicResponseHandler();
            String body = handler.handleResponse(response);
            if(code == HTTP_SUCCESS && body!= null){
                if(checkValidity(body)){
                    onSuccess();
                }else {
                    countNoOfLoginRequests("LoginError",null);
                }
            } else {
                countNoOfLoginRequests("LoginError",null);
            }
        }
        catch (IOException e) {
            countNoOfLoginRequests("IOException",e);
        }
    }

    private boolean checkValidity(String body) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        InputSource is;
        try {
            builder = factory.newDocumentBuilder();
            is = new InputSource(new StringReader(body));
            Document doc = builder.parse(is);
            NodeList list = doc.getElementsByTagName("job");
            if(list.getLength() > 0){
                return true;
            }
        } catch (ParserConfigurationException e) {
            log.warn(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " +
                    "ParserConfigurationException while parsing response");
        } catch (SAXException e) {
            log.warn(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " +
                    "SAXException parsing response");
        } catch (IOException e) {
            log.warn(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " +
                    "IOException parsing response");
        }
        return false;
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
        }else if(type.equals("IOException")) {
            IOException ioException = (IOException) obj;
            log.error(CaseConverter.splitCamelCase(serviceName) +" - Tenant Login: " + hostName +
                    ": IOException thrown while login from Heartbeat tenant : ", ioException);
            onFailure(ioException.getMessage());
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
     * Preemptive authentication interceptor
     */
    static class PreemptiveAuth implements HttpRequestInterceptor {

        /*
         * (non-Javadoc)
         *
         * @see org.apache.http.HttpRequestInterceptor#process(org.apache.http.HttpRequest,
         * org.apache.http.protocol.HttpContext)
         */
        public void process(HttpRequest request, HttpContext context) throws org.apache.http.HttpException, IOException {
            // Get the AuthState
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context
                        .getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost
                            .getPort()));
                    if (creds == null) {
                        throw new org.apache.http.HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme,creds);
                }
            }
        }

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
