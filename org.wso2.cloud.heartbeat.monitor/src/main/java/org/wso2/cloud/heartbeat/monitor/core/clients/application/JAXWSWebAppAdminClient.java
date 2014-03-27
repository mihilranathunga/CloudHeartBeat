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

package org.wso2.cloud.heartbeat.monitor.core.clients.application;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.webapp.mgt.stub.WebappAdminStub;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

import javax.activation.DataHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * JAXWS web application admin services under WebappAdminStub implemented in this class
 */
public class JAXWSWebAppAdminClient {
    private final Log log = LogFactory.getLog(JAXWSWebAppAdminClient.class);
    public static final int MILLISECONDS_PER_MINUTE = 60 * 1000;
    private WebappAdminStub webappAdminStub;

    /**
     * Initializes and authenticates JAXWS web application admin client
     * @param hostName Service host
     * @param sessionCookie Authorized session cookie
     * @throws org.apache.axis2.AxisFault
     */
    public JAXWSWebAppAdminClient(String hostName, String sessionCookie) throws AxisFault {
        String backendUrl= "https://" + hostName + "/services/";
        String serviceName = "JaxwsWebappAdmin";
        String endPoint = backendUrl + serviceName;
        webappAdminStub = new WebappAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, webappAdminStub);
    }

    /**
     * Stops service of a web apps
     * @param webappFileNames Web app file names
     * @throws java.rmi.RemoteException
     */
    public void stopWebapps(String[] webappFileNames) throws RemoteException {
        try {
            webappAdminStub.stopWebapps(webappFileNames);
        } catch (RemoteException e) {
            log.error("Fail to stop JAXWS webapp :" + e);
            throw new RemoteException("Fail to stop JAXWS webapp :" + e);
        }
    }

    /**
     * Uploads JAXWS war file
     * @param artifactName File name
     * @param artifactLocation Relative file path
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     */
    public void uploadWarFile(String artifactName, String artifactLocation)
            throws RemoteException, MalformedURLException {
        File file = new File(artifactLocation);
        URL url;
        try {
            url = new URL("file://" + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            log.error("Malformed URL " + e);
            throw new MalformedURLException("File path URL is invalid" + e);
        }
        DataHandler dh = new DataHandler(url);
        WebappUploadData webApp;
        webApp = new WebappUploadData();
        webApp.setFileName(artifactName);

        webApp.setDataHandler(dh);

        try {
            webappAdminStub.uploadWebapp(new WebappUploadData[]{webApp});
        } catch (RemoteException e) {
            log.error("Fail to upload JAXWS war file :" + e);
            throw new RemoteException("Fail to upload JAXWS war file :" + e);
        }
    }
}
