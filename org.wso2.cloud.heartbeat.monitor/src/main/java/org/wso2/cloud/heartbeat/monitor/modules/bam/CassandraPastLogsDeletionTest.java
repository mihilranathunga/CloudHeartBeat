package org.wso2.cloud.heartbeat.monitor.modules.bam;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.cassandra.mgt.stub.ks.CassandraKeyspaceAdminCassandraServerManagementException;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.CassandraKeyspaceAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.RemoteTenantManagerServiceClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

public class CassandraPastLogsDeletionTest implements Job {

	private static final Log log = LogFactory.getLog(CassandraPastLogsDeletionTest.class);

	private final String TEST_NAME = "CassandraLogColumnFamilyExistanceTest";

	/**
	 * Parameters set by values in HeartBeat.conf
	 */

	private String tenantUser;
	private String tenantUserPwd;

	private String adminUsername;
	private String adminPassword;

	private String cassandraKsName;

	private String hostName;
	private String serviceName;

	/**
	 * Parameters used by this class
	 */

	private CarbonAuthenticatorClient carbonAuthenticatorClient;
	private CassandraKeyspaceAdminClient keyspaceClient;

	private String sessionCookie = null;

	private int requestCount = 0;
	private boolean errorsReported;
	
	String[] columnFamilies = null;

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {

		if (!errorsReported) {
			log.info("Tenant : " + tenantUser + " ,TenantPw : " + tenantUserPwd);
			log.info("BAM -Host: " + hostName + ",Service: " + serviceName + " ,adminUser: " +
			         adminUsername + " ,adminPw: " + adminPassword);

			connectToBAMServer();
		}
		
		if (!errorsReported) {

			log.info("Cassandra KS name: " + cassandraKsName);

			getColumnFamilies();
		}
		
		if (!errorsReported) {

			checkColumnfamilyExistence();
		}

	}

	/**
	 * Login to carbon server and authenticate stub
	 */
	private void connectToBAMServer() {

		try {
			carbonAuthenticatorClient = new CarbonAuthenticatorClient(hostName);
			sessionCookie = carbonAuthenticatorClient.login(adminUsername, adminPassword, hostName);
			if (sessionCookie.equals(null)) {
				throw new LoginAuthenticationExceptionException("Session Cookie not recieved");
			}
			keyspaceClient = new CassandraKeyspaceAdminClient(hostName, sessionCookie);

		} catch (AxisFault axisFault) {
			countNoOfRequests("AxisFault", axisFault, "connectToBAMServer");
		} catch (RemoteException re) {
			countNoOfRequests("RemoteException", re, "connectToBAMServer");
		} catch (LoginAuthenticationExceptionException lae) {
			countNoOfRequests("LoginAuthenticationExceptionException", lae, "connectToBAMServer");
		}
		requestCount = 0;
	}
	
	/**
	 * Gets cassandra column families of super tenant and provided keyspace
	 */
	
	private void getColumnFamilies(){
		
		// Get column families
		
		try {
			columnFamilies = keyspaceClient.ListColumnFamiliesOfCurrentUser(cassandraKsName);
		} catch (CassandraKeyspaceAdminCassandraServerManagementException e) {
			countNoOfRequests("CassandraServiceException", e, "getColumnFamilies");
		} catch (RemoteException e) {
			countNoOfRequests("RemoteException", e, "getColumnFamilies");
		}
		requestCount = 0;
	}
	
    /**
     * checks if past column families exists
     */

    @SuppressWarnings("static-access")
    private void checkColumnfamilyExistence() {

		ArrayList<String> logColumnFamilies = new ArrayList<String>();
		ArrayList<String> pastLogColumnFamilies = new ArrayList<String>();

		Calendar cal = new GregorianCalendar();

		int year = cal.get(Calendar.YEAR);
		int month = cal.get(cal.MONTH)+1;
        int day = cal.get(cal.DAY_OF_MONTH);
		
        //Regex used to detect log column families
        
		String regex = "^log_\\d_[A-Z]+_[0-9]+_[0-9]+_[0-9]+";
		String todayRegex = "^log_\\d_[A-Z]+_"+Integer.toString(year)+"_"+String.format("%02d", month)+"_"+String.format("%02d", day);
		String yesterdayRegex = "^log_\\d_[A-Z]+_"+Integer.toString(year)+"_"+String.format("%02d", month)+"_"+String.format("%02d", (day-1));
		
		StringBuffer pastlogs = new StringBuffer();

		try {
	        for (String s : columnFamilies) {

	        	if (s.matches(regex)) {
	        		logColumnFamilies.add(s);
	        	}
	        }
	        if (logColumnFamilies.isEmpty()) {
	        	onSuccess("No Column family matches regex");
	        } else {
	        	for (String s : logColumnFamilies) {
	        		if (s.matches(todayRegex)| s.matches(yesterdayRegex)) {
	        		}
	        		else{
	        			pastLogColumnFamilies.add(s);
	        		}
	        	}
	        	if(pastLogColumnFamilies.isEmpty()){
	        		onSuccess("No past column families Exist");
	        	}
	        	else{
	        		
	        		for (String s : pastLogColumnFamilies) {
		        		pastlogs.append(s);
		        		pastlogs.append(" , ");
		        	}
	        		pastlogs.delete(pastlogs.length()-3, pastlogs.length()-1);
	        		pastlogs.trimToSize();
	        		onFailure("Past logs "+pastlogs.toString());
	        	}
	        }
        } catch (Exception e) {
        	countNoOfRequests("CheckColumnFamilyExistenceException", e, "checkColumnfamilyExistence");
        }
		requestCount = 0;
	}

	private void countNoOfRequests(String type, Object obj, String method) {
		requestCount++;
		if (requestCount == 3) {
			handleError(type, obj, method);
			requestCount = 0;
		} else {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			if (type.equals("RemoteException") |
			    type.equals("LoginAuthenticationExceptionException") | type.equals("AxisFault")) {

				if (method.equals("connectToBAMServer")) {
					connectToBAMServer();
				} else if (method.equals("getColumnFamilies")) {
					getColumnFamilies();
				}
			} else if (type.equals("CassandraServiceException")) {
				getColumnFamilies();
			} else if (type.equals("CheckColumnFamilyExistenceException")) {
				checkColumnfamilyExistence();
			}
		}
	}

	private void handleError(String type, Object obj, String method) {
		if (type.equals("AxisFault")) {
			AxisFault axisFault = (AxisFault) obj;
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Authentication Stub: " +
				                  hostName +
				                  ": AxisFault thrown while authenticating the stub from BAM: ",
				          axisFault);
			
			onFailure(axisFault.getMessage());
		} else if (type.equals("RemoteException")) {
			RemoteException remoteException = (RemoteException) obj;
			if (method.equals("connectToBAMServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Authentication Stub: " +
				                  hostName +
				                  ": RemoteException thrown while authenticating the stub : ",
				          remoteException);
			} else if (method.equals("getColumnFamilies")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Getting Column Families: " +
				                  hostName +
				                  ": RemoteException thrown while getting column families from BAM: ",
				          remoteException);
			}

			onFailure(remoteException.getMessage());
		} else if (type.equals("LoginAuthenticationExceptionException")) {
			LoginAuthenticationExceptionException e = (LoginAuthenticationExceptionException) obj;
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Authentication Stub: " +
				                  hostName +
				                  ": LoginAuthenticationException thrown while authenticating the stub : ",
				          e);
			onFailure(e.getMessage());
		} else if (type.equals("CassandraServiceException")) {
			Exception e = (Exception) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) +
			          " - Getting Column Families: " + hostName +
			          ": Exception thrown while querying for column families: ", e);
			onFailure(e.getMessage());
		} else if (type.equals("CheckColumnFamilyExistenceException")) {
			Exception e = (Exception) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Checking Column Families: " +
			          hostName + ": Exception thrown while column family checking: ", e);
			onFailure(e.getMessage());
		}
	}

	/**
	 * On test success
	 * @param message 
	 */
	private void onSuccess(String message) {
		boolean success = true;
		DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
		Connection connection = dbConnectionManager.getConnection();

		long timestamp = System.currentTimeMillis();
		DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);

		log.info(CaseConverter.splitCamelCase(serviceName) + " - " + TEST_NAME + ": SUCCESS - "+message);
	}

	/**
	 * On test failure
	 * 
	 * @param msg
	 *            error message
	 */
	private void onFailure(String msg) {

		log.error(CaseConverter.splitCamelCase(serviceName) + " - " + TEST_NAME + ": FAILURE  - " +
		          msg);

		if (!errorsReported) {

			boolean success = false;
			DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
			Connection connection = dbConnectionManager.getConnection();

			long timestamp = System.currentTimeMillis();
			DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME,
			                                     success);
			DbConnectionManager.insertFailureDetail(connection, timestamp, serviceName, TEST_NAME,
			                                        msg);

			Mailer mailer = Mailer.getInstance();
			mailer.send(CaseConverter.splitCamelCase(serviceName) + " :FAILURE",
			            CaseConverter.splitCamelCase(TEST_NAME) + ": " + msg, "");
			SMSSender smsSender = SMSSender.getInstance();
			smsSender.send(CaseConverter.splitCamelCase(serviceName) + ": " +
			               CaseConverter.splitCamelCase(TEST_NAME) + ": Failure");

			errorsReported = true;
		}
	}

	/**
	 * Sets Tenant user name
	 * 
	 * @param tenantUser
	 *            Tenant user name
	 */
	public void setTenantUser(String tenantUser) {
		this.tenantUser = tenantUser;
	}

	/**
	 * Sets Tenant user password
	 * 
	 * @param tenantUserPwd
	 *            Tenant user password
	 */
	public void setTenantUserPwd(String tenantUserPwd) {
		this.tenantUserPwd = tenantUserPwd;
	}

	/**
	 * Sets BAM Admin user name
	 * 
	 * @param adminUsername
	 *            admin user name
	 */

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	/**
	 * Sets BAM Admin user password
	 * 
	 * @param adminPassword
	 *            admin user password
	 */
	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	
	/**
	 * @param cassandraKsName
	 *            the cassandraKsName to set
	 */
	public void setCassandraKsName(String cassandraKsName) {
		this.cassandraKsName = cassandraKsName;
	}

	/**
	 * Sets service host
	 * 
	 * @param hostName
	 *            Service host
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Sets Service name
	 * 
	 * @param serviceName
	 *            Service name
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}
