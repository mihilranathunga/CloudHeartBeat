package org.wso2.cloud.heartbeat.monitor.core.clients.service;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.hive.stub.*;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResult;
import org.wso2.carbon.service.mgt.stub.ServiceAdminStub;

import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

import java.rmi.RemoteException;
import java.util.Calendar;

public class HiveExecutionServiceClient {

	private static final Log log = LogFactory.getLog(HiveExecutionServiceClient.class);

	private final String serviceName = "HiveExecutionService";
	private HiveExecutionServiceStub hiveExecutionServiceStub;
	String backendUrl;
	private String endPoint;

	
	
	/**
     * Authenticating the service admin stub
     * @param hostName host name
     * @param sessionCookie session cookie
     * @throws org.apache.axis2.AxisFault
     */
	
	public HiveExecutionServiceClient(String hostName, String sessionCookie) throws AxisFault {

        backendUrl= "https://" + hostName + "/services/";
        endPoint = backendUrl + serviceName;
        hiveExecutionServiceStub = new HiveExecutionServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, hiveExecutionServiceStub);
      
    }
	
	/**
     * Execute Hive Script
     * @param String HiveScriptName,HiveScript
	 * @return 
	 * @throws HiveExecutionServiceHiveExecutionException 
     * @throws java.rmi.RemoteException
     */
	
	public QueryResult[] executeHiveScript(String hiveScriptName, String hiveScript) throws RemoteException, HiveExecutionServiceHiveExecutionException{
		
		return hiveExecutionServiceStub.executeHiveScript( hiveScriptName, hiveScript);	
	}
	
	/**
     * Execute Hive Script with Callback Handler
     * @param String HiveScriptName,HiveScript HiveExecutionServiceCallbackHandler callBack
     * @throws java.rmi.RemoteException
     */
	
	public void startExecutingHiveScript(String hiveScriptName, String hiveScript, HiveExecutionServiceCallbackHandler callBack) throws RemoteException{
		
		hiveExecutionServiceStub.startexecuteHiveScript(hiveScriptName, hiveScript, callBack);
	}

}
