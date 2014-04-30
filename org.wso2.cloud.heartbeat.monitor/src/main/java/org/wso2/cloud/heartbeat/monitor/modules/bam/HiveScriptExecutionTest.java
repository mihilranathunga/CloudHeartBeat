package org.wso2.cloud.heartbeat.monitor.modules.bam;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ServiceContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResult;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResultRow;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.cassandra.mgt.stub.ks.CassandraKeyspaceAdminCassandraServerManagementException;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.ColumnFamilyInformation;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.CassandraKeyspaceAdminClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.HiveExecutionServiceClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.RemoteTenantManagerServiceClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.modules.ues.UESTenantLoginTest;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

public class HiveScriptExecutionTest implements Job {

	private static final Log log = LogFactory.getLog(HiveScriptExecutionTest.class);

	private final String TEST_NAME = "HiveScriptExecutionTest";

	/**
	 * Parameters set by values in HeartBeat.conf
	 */

	private String tenantUser;
	private String tenantUserPwd;

	private String adminUsername;
	private String adminPassword;

	private String isAdminUsername;
	private String isAdminPassword;

	private String cassandraHost;
	private String cassandraPort;
	private String cassandraKsName;
	private String cassandraKsUsername;
	private String cassandraKsPassword;
	private String serverKey;

	private String hostName;
	private String serviceName;
	private String completeTestName;

	private String identityServerHost;

	/**
	 * Parameters used by this class
	 */

	private int tenantID;
	private String cassandraCfName;

	private CarbonAuthenticatorClient carbonAuthenticatorClient;
	private HiveExecutionServiceClient hiveClient;

	private CarbonAuthenticatorClient identityServerAuthenticatorClient;
	private RemoteTenantManagerServiceClient rTSMClient;

	private String sessionCookie = null;
	private String isSessionCookie = null;

	private int requestCount = 0;
	private boolean queryExecutionSuccessfull = false;
	private boolean errorsReported;

	private String hiveQuery;

	private CassandraKeyspaceAdminClient keyspaceClient;

	private ColumnFamilyInformation columnFamily;

	/**
	 * @param jobExecutionContext
	 *            "hostName" ,"tenantUser", "tenantUserPwd" "serviceName" params
	 *            passed via JobDataMap.
	 * @throws org.quartz.JobExecutionException
	 */
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		setCompleteTestName(CaseConverter.splitCamelCase(serviceName) + " - Hive Script Execution Test : ");
		if (!errorsReported) {
			log.info("Tenant : " + tenantUser + " ,TenantPw : " + tenantUserPwd);
			log.info("BAM -Host: " + hostName + ",Service: " + serviceName + " ,adminUser: " +
			         adminUsername + " ,adminPw: " + adminPassword);
			
			connectToBAMServer();
		}		
		if (!errorsReported) {
			log.info("IS -Host: " + identityServerHost + " ,isAdmin: " + isAdminUsername +
			         " ,isAdminPw: " + isAdminPassword);
			
			getTenantIDFromISServer();
		}
		if (!errorsReported) {
			
			log.info("Cassandra -Host: " + cassandraHost + " ,Cassandra -Port: " + cassandraPort +
			         " ,Cassandra KS name: " + cassandraKsName + " ,Cassandra KS usname: " +
			         cassandraKsUsername + " ,Cassandra KS Pw: " + cassandraKsPassword);
			
			initializeHiveExecutionTest();
		}
		if(!errorsReported){
			log.info("Cassandra Column Family name : "+cassandraCfName);
			getColumnFamily();
		}
		if (!errorsReported) {
			executeQuery();
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
			hiveClient = new HiveExecutionServiceClient(hostName, sessionCookie);
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
	 * Gets Tenant ID from the Provided Identity Server
	 * 
	 */

	private void getTenantIDFromISServer() {

		try {
			identityServerAuthenticatorClient = new CarbonAuthenticatorClient(identityServerHost);
			isSessionCookie = identityServerAuthenticatorClient.login(tenantUser, tenantUserPwd,
			                                                          identityServerHost);

			if (isSessionCookie.equals(null)) {
				throw new LoginAuthenticationExceptionException("Session Cookie not recieved");
			}

			rTSMClient = new RemoteTenantManagerServiceClient(identityServerHost, isSessionCookie);

			tenantID = rTSMClient.getTenantIdofUser(tenantUser);
		} catch (AxisFault axisFault) {
			countNoOfRequests("AxisFault", axisFault, "getTenantIDFromISServer");
		} catch (RemoteException re) {
			countNoOfRequests("RemoteException", re, "getTenantIDFromISServer");
		} catch (LoginAuthenticationExceptionException lae) {
			countNoOfRequests("LoginAuthenticationExceptionException", lae,
			                  "getTenantIDFromISServer");
		}
		requestCount = 0;
	}

	/**
	 * Initialize test variables
	 * 
	 */

	private void initializeHiveExecutionTest() {

		try {
			
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd");
			String curDate = format.format(cal.getTime());
			cassandraCfName = "log_" + tenantID + "_" + serverKey + "_" + curDate;
			log.info("Server Key: " + serverKey + " ,Column Family Name: " + cassandraCfName);
			hiveQuery = 
					"CREATE EXTERNAL TABLE IF NOT EXISTS LogEventInfo (" 
						+ "key STRING, tenantID INT,serverName STRING, appName STRING, priority STRING,logTime DOUBLE,logger STRING,message STRING)" 
					    + "STORED BY 'org.apache.hadoop.hive.cassandra.CassandraStorageHandler' " 
						+ "WITH SERDEPROPERTIES (" 
					    + "\"cassandra.host\" = \"" + cassandraHost + "\"," 
						+ "\"cassandra.port\" = \"" + cassandraPort + "\","
					    + "\"cassandra.ks.name\" = \"" + cassandraKsName +"\"," 
					    + "\"cassandra.ks.username\" = \"" + cassandraKsUsername + "\"," 
					    + "\"cassandra.ks.password\" = \"" + cassandraKsPassword + "\"," 
					    + "\"cassandra.cf.name\" = \"" + cassandraCfName + "\"," 
					    + "\"cassandra.columns.mapping\" = \":key,payload_tenantID,payload_serverName,payload_appName,payload_priority,payload_logTime,payload_logger,payload_message\");"
					 + " SELECT key, tenantID, serverName, serverName, logTime , logger,  message FROM LogEventInfo;"
					 + "DROP TABLE IF EXISTS LogEventInfo;";
		} catch (Exception e) {
			countNoOfRequests("TestSetupException", e, "initializeHiveExecutionTest");
		}
		requestCount = 0;
	}
	
	/**
	 *Check if column family exists
	 */
	
	private void getColumnFamily(){

			try {
	            columnFamily = keyspaceClient.getColumnFamilyof(cassandraKsName, cassandraCfName);
            } catch (RemoteException e) {
            	countNoOfRequests("CassandraServiceException", e, "getColumnFamily");
            } catch (CassandraKeyspaceAdminCassandraServerManagementException e) {
            	countNoOfRequests("RemoteException", e, "getColumnFamily");
            }
		requestCount = 0;
	}

	/**
	 * Executes Query
	 */

	private void executeQuery() {

		try {
			QueryResult[] response = hiveClient.executeHiveScript(null, hiveQuery);

			QueryResultRow[] resultRows;
			String[] columnValues;

			for (int i = 0; i < response.length; i++) {
				if (response[i].isResultRowsSpecified()) {
					resultRows = response[i].getResultRows();

					log.info("Result for : " + response[i].getQuery() + " Response : " +
					         response[i].isResultRowsSpecified());

					for (int j = 0; j < resultRows.length; j++) {

						columnValues = resultRows[j].getColumnValues();

						for (int k = 0; k < columnValues.length; k++) {
							System.out.print(columnValues[k] + " ");
						}
						System.out.println();
					}

					queryExecutionSuccessfull = true;
				} else {
					// log.info("No Result for : " + response[i].getQuery());
				}
			}
			
			if (queryExecutionSuccessfull) {
				onSuccess();
			}
		} catch (Exception e) {
			log.error("Exception in Executing Query :" + e.getMessage());
			countNoOfRequests("ExecuteQuery", e, "executeQuery");
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
				} else if (method.equals("getTenantIDFromISServer")) {
					getTenantIDFromISServer();
				} else if (method.equals("getColumnFamily")) {
					getColumnFamily();
				}
			} else if (type.equals("TestSetupException")) {
				initializeHiveExecutionTest();
			} else if (type.equals("CassandraServiceException")) {
				getColumnFamily();
			} else if (type.equals("ExecuteQuery")) {
				executeQuery();
			}
		}
	}

	private void handleError(String type, Object obj, String method) {
		if (type.equals("AxisFault")) {
			AxisFault axisFault = (AxisFault) obj;
			if (method.equals("connectToBAMServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Authentication Stub: " +
				                  hostName +
				                  ": AxisFault thrown while authenticating the stub from BAM: ",
				          axisFault);
			} else if (method.equals("getTenantIDFromISServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Getting Tenant ID: " +
				                  hostName + ": AxisFault thrown while getting Tenant ID from IS: ",
				          axisFault);
			}
			onFailure(axisFault.getMessage());
		} else if (type.equals("RemoteException")) {
			RemoteException remoteException = (RemoteException) obj;
			if (method.equals("connectToBAMServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Authentication Stub: " +
				                  hostName +
				                  ": RemoteException thrown while authenticating the stub : ",
				          remoteException);
			} else if (method.equals("getTenantIDFromISServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Getting Tenant ID: " +
				                  hostName +
				                  ": RemoteException thrown while getting Tenant ID from IS: ",
				          remoteException);
			} else if (method.equals("getColumnFamily")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Getting Column Family: " +
		                  hostName +
		                  ": RemoteException thrown while getting Column Family from BAM: ",
		          remoteException);
	}

			onFailure(remoteException.getMessage());
		} else if (type.equals("LoginAuthenticationExceptionException")) {
			LoginAuthenticationExceptionException e = (LoginAuthenticationExceptionException) obj;
			if (method.equals("connectToBAMServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Authentication Stub: " +
				                  hostName +
				                  ": LoginAuthenticationException thrown while authenticating the stub : ",
				          e);
			} else if (method.equals("getTenantIDFromISServer")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Getting Tenant ID: " +
				                  hostName +
				                  ": LoginAuthenticationException thrown while getting Tenant ID from IS: ",
				          e);
			}

			onFailure(e.getMessage());
		} else if (type.equals("TestSetupException")) {
			Exception e = (Exception) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) +
			          " - Initializing Test Variables: " + hostName +
			          ": Exception thrown while setting up test: ", e);
			onFailure(e.getMessage());
		} else if (type.equals("CassandraServiceException")) {
			Exception e = (Exception) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Getting Column Family: " +
			          hostName + ": Exception thrown while getting Column Family from BAM: ", e);
			onFailure(e.getMessage());
		}else if (type.equals("ExecuteQuery")) {
			Exception e = (Exception) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Query Execution: " +
			          hostName + ": Exception thrown while executing query: ", e);
			onFailure(e.getMessage());
		}
	}

	/**
	 * On test success
	 */
	private void onSuccess() {
		boolean success = true;
		DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
		Connection connection = dbConnectionManager.getConnection();

		long timestamp = System.currentTimeMillis();
		DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);

		log.info(completeTestName+ "SUCCESS");
	}

	/**
	 * On test failure
	 * 
	 * @param msg
	 *            error message
	 */
	private void onFailure(String msg) {

		log.error(completeTestName+ "FAILURE  - " +
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
	 * Sets identity Server admin username
	 * 
	 * @param isAdminUsername
	 */

	public void setIsAdminUsername(String isAdminUsername) {
		this.isAdminUsername = isAdminUsername;
	}

	/**
	 * Sets identity Server admin password
	 * 
	 * @param isAdminPassword
	 */

	public void setIsAdminPassword(String isAdminPassword) {
		this.isAdminPassword = isAdminPassword;
	}

	/**
	 * Sets Cassandra hostname
	 * 
	 * @param cassandraHost
	 *            cassandra host name
	 */

	public void setCassandraHost(String cassandraHost) {
		this.cassandraHost = cassandraHost;
	}

	/**
	 * @param cassandraPort
	 *            the cassandraPort to set
	 */
	public void setCassandraPort(String cassandraPort) {
		this.cassandraPort = cassandraPort;
	}

	/**
	 * @param cassandraKsName
	 *            the cassandraKsName to set
	 */
	public void setCassandraKsName(String cassandraKsName) {
		this.cassandraKsName = cassandraKsName;
	}

	/**
	 * @param cassandraKsUsername
	 *            the cassandraKsUsername to set
	 */
	public void setCassandraKsUsername(String cassandraKsUsername) {
		this.cassandraKsUsername = cassandraKsUsername;
	}

	/**
	 * @param cassandraKsPassword
	 *            the cassandraKsPassword to set
	 */
	public void setCassandraKsPassword(String cassandraKsPassword) {
		this.cassandraKsPassword = cassandraKsPassword;
	}

	/**
	 * @param serverKey
	 *            the serverKey to set
	 */
	public void setServerKey(String serverKey) {
		this.serverKey = serverKey;
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
	
	/**
     * Sets Display Service name
     * @param completeTestName Service name
     */
    public void setCompleteTestName(String completeTestName) {
        this.completeTestName = completeTestName;
    }

	/**
	 * Sets identity Server Hostname
	 * 
	 * @param identityServerHost
	 */

	public void setIdentityServerHost(String identityServerHost) {
		this.identityServerHost = identityServerHost;
	}

}
