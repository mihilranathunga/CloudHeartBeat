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
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappMetadata;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

import javax.activation.DataHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * Web application admin services under WebappAdminStub implemented in this class
 */
public class WebAppAdminClient {

    private static final Log log = LogFactory.getLog(WebAppAdminClient.class);

    private WebappAdminStub webappAdminStub;

    /**
     * Initializes and authenticates Web application admin service client
     * @param hostName Service host
     * @param sessionCookie Authorized session cookie
     * @throws org.apache.axis2.AxisFault
     */
    public WebAppAdminClient(String hostName, String sessionCookie) throws AxisFault {

        String backendUrl= "https://" + hostName + "/services/";
        String serviceName = "WebappAdmin";
        String endPoint = backendUrl + serviceName;
        webappAdminStub = new WebappAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, webappAdminStub);
    }

    /**
     * Uploads Web app war file
     * @param fileName File name
     * @param filePath Relative file path
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     * @throws InterruptedException
     */
    public void warFileUploader(String fileName, String filePath)
            throws RemoteException, MalformedURLException, InterruptedException {
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
            webappAdminStub.uploadWebapp(new WebappUploadData[]{webApp});
        } catch (RemoteException e) {
            log.error("Fail to upload webapp file :" + e);
            throw new RemoteException("Fail to upload webapp file :" + e);
        }
    }

    /**
     * Deletes Web app war files
     * @param fileName File name
     * @throws java.rmi.RemoteException
     */
    public void deleteWebAppFile(String fileName) throws RemoteException {
        webappAdminStub.deleteWebapp(fileName);
    }

    /**
     * Stops web application
     * @param fileName File name
     * @return True if the stopping was success
     * @throws java.rmi.RemoteException
     */
    public boolean stopWebApp(String fileName) throws RemoteException {
        webappAdminStub.stopWebapps(new String[]{fileName});
        WebappMetadata webappMetadata = webappAdminStub.getStoppedWebapp(fileName);
        if (webappMetadata.getWebappFile().equals(fileName)) {
            return true;
        }
        return false;
    }


}
