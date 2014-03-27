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

package org.wso2.cloud.heartbeat.monitor.core.clients.authentication;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;

import java.rmi.RemoteException;

/**
 * Authentication for carbon servers under authentication admin stub implemented in this class
 */
public class CarbonAuthenticatorClient {

    private static final Log log = LogFactory.getLog(CarbonAuthenticatorClient.class);
    private AuthenticationAdminStub authenticationAdminStub;

    /**
     * Initializes authenticator client
     * @param hostName Service host
     * @throws org.apache.axis2.AxisFault
     */
    public CarbonAuthenticatorClient(String hostName) throws AxisFault {

        String serviceName = "AuthenticationAdmin";
        String endPoint = "https://" + hostName + "/services/" + serviceName;
        try {
            authenticationAdminStub = new AuthenticationAdminStub(endPoint);
        } catch (AxisFault axisFault) {
            log.error("authenticationAdminStub initialization fails: "+ axisFault.getMessage());
            throw new AxisFault("authenticationAdminStub initialization fails: "+ axisFault.getMessage());
        }
    }

    /**
     * Authenticates an user into a service
     * @param userName User name
     * @param password Password
     * @param host Service host
     * @return Authorized session cookie
     * @throws org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException
     * @throws java.rmi.RemoteException
     */
    public String login(String userName, String password, String host)
            throws LoginAuthenticationExceptionException, RemoteException {
        Boolean loginStatus;
        ServiceContext serviceContext;
        String sessionCookie;
        long timeout = 2 * 60 * 1000; // Setting the time out to Two minutes
        authenticationAdminStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout);
        loginStatus = authenticationAdminStub.login(userName, password, hostWithoutHTTPSPort(host));

        if (!loginStatus) {
            log.error("Login failure. Returned false as a login status by Server");
            throw new LoginAuthenticationExceptionException("Login failure. Returned false as a " +
                                                            "login status by Server");
        }
        serviceContext = authenticationAdminStub._getServiceClient().getLastOperationContext()
                .getServiceContext();
        sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
        return sessionCookie;
    }

    /**
     * Checks availability of login on a service
     * @param userName User name
     * @param password Password
     * @param host Service host
     * @return Login success/failure
     * @throws org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException
     * @throws java.rmi.RemoteException
     */
    public Boolean checkLogin(String userName, String password, String host)
            throws LoginAuthenticationExceptionException, RemoteException {
        long timeout = 2 * 60 * 1000; // Setting the time out to Two minutes
        authenticationAdminStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(timeout);
        return authenticationAdminStub.login(userName, password, hostWithoutHTTPSPort(host));
    }

    /**
     * Logs out from a service
     * @throws org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException
     * @throws java.rmi.RemoteException
     */
    public void logOut() throws LogoutAuthenticationExceptionException, RemoteException {
        authenticationAdminStub.logout();
    }

    /**
     * Returns the Authentication Service stub
     * @return Authorized authenticationAdminStub
     */
    public Stub getAuthenticationAdminStub() {
        return authenticationAdminStub;
    }

    /**
     * Returns the host address after removing port.
     * @param hostName Service host with port
     * @return Service host without port
     */
    private String hostWithoutHTTPSPort(String hostName){
        return hostName.contains(":") ? hostName.split(":")[0] : hostName;
    }
}
