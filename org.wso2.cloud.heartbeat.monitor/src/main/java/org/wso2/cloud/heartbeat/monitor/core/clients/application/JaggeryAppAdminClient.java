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
import org.wso2.carbon.jaggery.app.mgt.stub.JaggeryAppAdminStub;
import org.wso2.carbon.jaggery.app.mgt.stub.types.carbon.WebappUploadData;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

import javax.activation.DataHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * Jaggery app admin services under JaggeryAppAdminStub implemented in this class
 */
public class JaggeryAppAdminClient {
    private final Log log = LogFactory.getLog(JaggeryAppAdminClient.class);
    private JaggeryAppAdminStub jaggeryAppAdminStub;

    /**
     * Initializes and authenticates Jaggery app admin client
     * @param hostName Service host
     * @param sessionCookie Authorized session cookie
     * @throws org.apache.axis2.AxisFault
     */
    public JaggeryAppAdminClient(String hostName, String sessionCookie) throws AxisFault {
        String backendUrl= "https://" + hostName + "/services/";
        String serviceName = "JaggeryAppAdmin";
        String endPoint = backendUrl + serviceName;
        jaggeryAppAdminStub = new JaggeryAppAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, jaggeryAppAdminStub);
    }

    /**
     * Uploads Jaggery war file
     * @param fileName File name
     * @param filePath Relative file path
     * @throws java.net.MalformedURLException
     * @throws java.rmi.RemoteException
     */
    public void uploadWarFile(String fileName, String filePath)
            throws MalformedURLException, RemoteException {
        File file = new File(filePath);
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
        webApp.setFileName(fileName);

        webApp.setDataHandler(dh);

        try {
            jaggeryAppAdminStub.uploadWebapp(new WebappUploadData[]{webApp});
        } catch (RemoteException e) {
            log.error("Fail to upload webapp file :" + e);
            throw new RemoteException("Fail to upload webapp file :" + e);
        }

    }
}
