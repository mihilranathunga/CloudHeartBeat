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

package org.wso2.cloud.heartbeat.monitor.core.clients.service;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.service.mgt.stub.ServiceAdminException;
import org.wso2.carbon.service.mgt.stub.ServiceAdminStub;
import org.wso2.carbon.service.mgt.stub.types.carbon.ServiceMetaData;
import org.wso2.carbon.service.mgt.stub.types.carbon.ServiceMetaDataWrapper;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

import java.rmi.RemoteException;
import java.util.Calendar;

public class ServiceAdminClient {

    //waiting till services get deployed
    public static final int SERVICE_DEPLOYMENT_DELAY = 60000;

    private static final Log log = LogFactory.getLog(ServiceAdminClient.class);
    private ServiceAdminStub serviceAdminStub;

    /**
     * Authenticating the service admin stub
     * @param hostName host name
     * @param sessionCookie session cookie
     * @throws org.apache.axis2.AxisFault
     */
    public ServiceAdminClient(String hostName, String sessionCookie) throws AxisFault {

        String backendUrl= "https://" + hostName + "/services/";
        String serviceName = "ServiceAdmin";
        String endPoint = backendUrl + serviceName;
        serviceAdminStub = new ServiceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, serviceAdminStub);
    }

    /**
     * Delete service group
     * @param serviceGroup service group
     * @throws java.rmi.RemoteException
     */
    public void deleteService(String[] serviceGroup) throws RemoteException {
        serviceAdminStub.deleteServiceGroups(serviceGroup);
    }

    /**
     * Checking a service existence
     * @param serviceName Service name
     * @return
     * @throws java.rmi.RemoteException
     */
    public boolean isServiceExists(String serviceName)
            throws RemoteException {
        boolean serviceState = false;
        ServiceMetaDataWrapper serviceMetaDataWrapper;
        ServiceMetaData[] serviceMetaDataList;
        serviceMetaDataWrapper = listServices(serviceName);
        serviceMetaDataList = serviceMetaDataWrapper.getServices();
        if (serviceMetaDataList == null || serviceMetaDataList.length == 0) {
            serviceState = false;
        } else {
            for (ServiceMetaData serviceData : serviceMetaDataList) {
                if (serviceData != null && serviceData.getName().equalsIgnoreCase(serviceName)) {
                    return true;
                }
            }
        }
        return serviceState;
    }

    /**
     * Deleting a service by specifying its file name
     * @param serviceFileName service file name
     * @throws java.rmi.RemoteException
     */
    public void deleteMatchingServiceByGroup(String serviceFileName)
            throws RemoteException {
        String matchingServiceName = getMatchingServiceName(serviceFileName);
        if (matchingServiceName != null) {
            String serviceGroup[] = {getServiceGroup(matchingServiceName)};

            serviceAdminStub.deleteServiceGroups(serviceGroup);

        } else {
            log.error("Service group name cannot be null");
        }
    }

    /**
     * Get service group of a service
     * @param serviceName service name
     * @return
     * @throws java.rmi.RemoteException
     */
    public String getServiceGroup(String serviceName) throws RemoteException {
        ServiceMetaDataWrapper serviceMetaDataWrapper;
        ServiceMetaData[] serviceMetaDataList;
        serviceMetaDataWrapper = listServices(serviceName);
        serviceMetaDataList = serviceMetaDataWrapper.getServices();
        if (serviceMetaDataList != null && serviceMetaDataList.length > 0) {

            for (ServiceMetaData serviceData : serviceMetaDataList) {
                if (serviceData != null && serviceData.getName().equalsIgnoreCase(serviceName)) {
                    return serviceData.getServiceGroupName();
                }
            }
        }
        return null;
    }

    /**
     * Start a service by specifying its name
     * @param serviceName service name
     * @throws java.rmi.RemoteException
     * @throws ServiceAdminException
     */
    public void startService(String serviceName) throws RemoteException, ServiceAdminException {
        serviceAdminStub.startService(serviceName);
    }

    /**
     * Stopping a service by specifying its name
     * @param serviceName service name
     * @throws java.rmi.RemoteException
     * @throws ServiceAdminException
     */
    public void stopService(String serviceName) throws RemoteException, ServiceAdminException {
        serviceAdminStub.stopService(serviceName);
    }

    /**
     * Getting list of services
     * @param serviceName service name
     * @return List of services under service name
     * @throws java.rmi.RemoteException
     */
    public ServiceMetaDataWrapper listServices(String serviceName)
            throws RemoteException {
        ServiceMetaDataWrapper serviceMetaDataWrapper;
        serviceMetaDataWrapper = serviceAdminStub.listServices("ALL", serviceName, 0);
        return serviceMetaDataWrapper;
    }

    /**
     * Getting service name by specifying its file name
     * @param serviceFileName service file name
     * @return Service Name
     * @throws java.rmi.RemoteException
     */
    public String getMatchingServiceName(String serviceFileName)
            throws RemoteException {
        ServiceMetaDataWrapper serviceMetaDataWrapper;
        serviceMetaDataWrapper = serviceAdminStub.listServices("ALL", serviceFileName, 0);

        ServiceMetaData[] serviceMetaDataList;
        if (serviceMetaDataWrapper != null) {
            serviceMetaDataList = serviceMetaDataWrapper.getServices();
            if (serviceMetaDataList != null && serviceMetaDataList.length > 0) {

                for (ServiceMetaData serviceData : serviceMetaDataList) {
                    if (serviceData != null && serviceData.getName().contains(serviceFileName)) {
                        return serviceData.getName();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Wait till the service get deployed
     * @param serviceName service name
     * @return deploy status
     * @throws java.rmi.RemoteException
     */
    public boolean isServiceDeployed(String serviceName)
            throws RemoteException ,NullPointerException {

        boolean isServiceDeployed = false;
        Calendar startTime = Calendar.getInstance();
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())
               < SERVICE_DEPLOYMENT_DELAY) {
            if (isServiceExists(serviceName)) {
                isServiceDeployed = true;
                break;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                //Exception is ignored
            }
        }
        return isServiceDeployed;
    }

    /**
     * Wait till the service get deployed
     * @param serviceName service name
     * @return deploy status
     * @throws java.rmi.RemoteException
     */
    public boolean isServiceUnDeployed(String serviceName)
            throws RemoteException ,NullPointerException {

        boolean isServiceUnDeployed = false;
        Calendar startTime = Calendar.getInstance();
        while ((Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())
               < SERVICE_DEPLOYMENT_DELAY) {
            if (!isServiceExists(serviceName)) {
                isServiceUnDeployed = true;
                break;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                //Exception is ignored
            }
        }
        return isServiceUnDeployed;
    }
}
