package org.wso2.cloud.heartbeat.monitor.core.clients.service;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cassandra.mgt.stub.ks.CassandraKeyspaceAdminCassandraServerManagementException;
import org.wso2.carbon.cassandra.mgt.stub.ks.CassandraKeyspaceAdminStub;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.ColumnFamilyInformation;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

public class CassandraKeyspaceAdminClient {

	/**
	 * @param args
	 */
	
	private static final Log log = LogFactory.getLog(CassandraKeyspaceAdminClient.class);

	private final String serviceName = "CassandraKeyspaceAdmin";
	private CassandraKeyspaceAdminStub cassandraKeyspaceAdminStub;
	String backendUrl;
	private String endPoint;
	
	
	/**
     * Authenticating the service admin stub
     * @param hostName host name
     * @param sessionCookie session cookie
     * @throws org.apache.axis2.AxisFault
     */
	
	public CassandraKeyspaceAdminClient(String hostName, String sessionCookie) throws AxisFault {

        backendUrl= "https://" + hostName + "/services/";
        endPoint = backendUrl + serviceName;
        cassandraKeyspaceAdminStub = new CassandraKeyspaceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, cassandraKeyspaceAdminStub);
      
    }
	
	/**
	 * Get Column Families
	 * 
	 * @param keySpace
	 * @return String[]
	 * @throws CassandraKeyspaceAdminCassandraServerManagementException 
	 * @throws RemoteException 
	 */
	
	public String[] ListColumnFamiliesOfCurrentUser(String keySpace) throws RemoteException, CassandraKeyspaceAdminCassandraServerManagementException{
		
		return cassandraKeyspaceAdminStub.listColumnFamiliesOfCurrentUser(keySpace);
	}
	
	/**
	 * Get Column Family
	 * @param keySpace
	 * @param cfName
	 * @return
	 * @throws RemoteException
	 * @throws CassandraKeyspaceAdminCassandraServerManagementException
	 */
	public ColumnFamilyInformation getColumnFamilyof(String keySpace, String cfName) throws RemoteException, CassandraKeyspaceAdminCassandraServerManagementException{
		return cassandraKeyspaceAdminStub.getColumnFamilyOfCurrentUser(keySpace, cfName);
	}

}
