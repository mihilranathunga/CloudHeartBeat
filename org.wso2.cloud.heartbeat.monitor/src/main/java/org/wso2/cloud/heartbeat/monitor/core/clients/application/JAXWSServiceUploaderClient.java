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
import org.wso2.carbon.jaxwsservices.stub.JAXWSServiceUploaderStub;
import org.wso2.carbon.jaxwsservices.stub.types.JAXServiceData;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

import javax.activation.DataHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * JAXWS service uploader under JAXWSServiceUploaderStub implemented in this class
 */
public class JAXWSServiceUploaderClient {
    private static final Log log = LogFactory.getLog(JAXWSServiceUploaderClient.class);

    private final String serviceName = "JAXWSServiceUploader";
    private JAXWSServiceUploaderStub jAXWSServiceUploaderStub;

    /**
     * Initializes and authenticates JAXWS service uploader client
     * @param hostName Service host
     * @param sessionCookie Authorized session cookie
     * @throws org.apache.axis2.AxisFault
     */
    public JAXWSServiceUploaderClient(String hostName, String sessionCookie) throws AxisFault {
        String backendUrl = "https://" + hostName + "/services/";
        String endPoint = backendUrl + serviceName;
        jAXWSServiceUploaderStub = new JAXWSServiceUploaderStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, jAXWSServiceUploaderStub);
    }

    /**
     * Authenticate JAXWS service uploader client
     * @param hostName Service host
     * @param userName User name
     * @param password Password
     * @throws org.apache.axis2.AxisFault
     */
    public JAXWSServiceUploaderClient(String hostName, String userName, String password)
            throws AxisFault {
        String backendUrl = "https://" + hostName + "/services/";
        String endPoint = backendUrl + serviceName;
        jAXWSServiceUploaderStub = new JAXWSServiceUploaderStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, jAXWSServiceUploaderStub);
    }

    /**
     * Uploads JAXWS service file
     * @param fileName JAXWS file name
     * @param filePath Relative file path
     * @throws java.rmi.RemoteException
     * @throws java.net.MalformedURLException
     */
    public void uploadJAXWSFile(String fileName,String filePath)
            throws RemoteException, MalformedURLException {

        DataHandler dh = createDataHandler(filePath);
        JAXServiceData jAXServiceData;
        jAXServiceData = new JAXServiceData();
        jAXServiceData.setFileName(fileName);
        jAXServiceData.setDataHandler(dh);

        jAXWSServiceUploaderStub.uploadService(new JAXServiceData[]{jAXServiceData});
        log.debug("Artifact Uploaded");
    }

    /**
     * Returns Data handler from that file path
     * @param filePath Relative file path
     * @return DataHandler created from that file
     * @throws java.net.MalformedURLException
     */
    private DataHandler createDataHandler(String filePath) throws MalformedURLException {
        URL url;
        File file=new File(filePath);
        try {
            url = new URL("file://" + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            log.error("File path URL is invalid" + e);
            throw new MalformedURLException("File path URL is invalid" + e);
        }
        return new DataHandler(url);
    }
}
