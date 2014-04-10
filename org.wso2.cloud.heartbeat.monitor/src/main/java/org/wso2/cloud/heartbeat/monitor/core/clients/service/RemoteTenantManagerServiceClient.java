package org.wso2.cloud.heartbeat.monitor.core.clients.service;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.um.ws.api.stub.RemoteTenantManagerServiceStub;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.AuthenticateStub;

public class RemoteTenantManagerServiceClient {

	private static final Log log = LogFactory.getLog(RemoteTenantManagerServiceClient.class);

	private final String serviceName = "RemoteUserStoreManagerService";
	private RemoteTenantManagerServiceStub remoteTMSStub;
	String backendUrl;
	private String endPoint;

	
	
	/**
     * Authenticating the service admin stub
     * @param hostName host name
     * @param sessionCookie session cookie
     * @throws org.apache.axis2.AxisFault
     */
	
	public RemoteTenantManagerServiceClient(String hostName, String sessionCookie) throws AxisFault {

        backendUrl= "https://" + hostName + "/services/";
        endPoint = backendUrl + serviceName;
        remoteTMSStub = new RemoteTenantManagerServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, remoteTMSStub);
      
    }
	
	/**
     * Get Tenant Id of user
	 *
     */
	
	public int getTenantIdofUser(String tenant){
		
		int tenantId=-1;
		
		try {
	        tenantId = remoteTMSStub.getTenantId(tenant);
        } catch (Exception e) {
	        log.error(e.getMessage());
	        e.printStackTrace();
        }

		return tenantId;
	}

	

}
