package org.wso2.cloud.heartbeat.monitor.modules.bam;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceHiveExecutionException;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResult;
import org.wso2.carbon.analytics.hive.stub.HiveExecutionServiceStub.QueryResultRow;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.CarbonAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.service.HiveExecutionServiceClient;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.modules.ues.UESTenantLoginTest;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.rmi.RemoteException;
import java.sql.Connection;

public class HiveScriptExecutionTest implements Job {

	private static final Log log = LogFactory.getLog(UESTenantLoginTest.class);
	private final String TEST_NAME = "HiveScriptExecutionTest";

	private String tenantUser;
	private String tenantUserPwd;
	private String hostName;
	private String serviceName;
	private int requestCount = 0;

	private CarbonAuthenticatorClient carbonAuthenticatorClient;
	private HiveExecutionServiceClient hiveClient;

	private String sessionCookie;

	private String userName = "admin";
	private String password = "admin";

	private String hiveQuery =
	"DROP TABLE request;"
			
	+"CREATE EXTERNAL TABLE IF NOT EXISTS request("
	+"request_id INT, device_type STRING, purpose STRING, requirements STRING, employee_employee_id INT, resolved INT)"
    +"STORED BY 'org.wso2.carbon.hadoop.hive.jdbc.storage.JDBCStorageHandler'"
    +"TBLPROPERTIES ("
    +"'wso2.carbon.datasource.name'='HWDREPO',"
    +"'hive.jdbc.update.on.duplicate' = 'true' ,"
    +"'hive.jdbc.primary.key.fields' = 'request_id' ,"
    +"'hive.jdbc.table.create.query' ="
    +"'CREATE TABLE request (request_id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,"
    +"device_type varchar(45) NOT NULL, purpose varchar(400) NOT NULL,requirements varchar(1000) NOT NULL, employee_employee_id int(11) NOT NULL, resolved int(11) NOT NULL, request_date date)');"
    
    +"select request_id,device_type,purpose from request;";


	/**
	 * @param jobExecutionContext
	 *            "hostName" ,"tenantUser", "tenantUserPwd" "serviceName" params
	 *            passed via JobDataMap.
	 * @throws org.quartz.JobExecutionException
	 */
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		initializeLoginTest();
		executeQuery();
	}

	private void initializeLoginTest() {

		log.info("Host Name: " + hostName + " ,userName :" + tenantUser + " ,userPassword" +
		         tenantUserPwd);
		try {
			carbonAuthenticatorClient = new CarbonAuthenticatorClient(hostName);
			sessionCookie = carbonAuthenticatorClient.login(userName, password, hostName);

			log.info("Session Cookie :" + sessionCookie);
			hiveClient = new HiveExecutionServiceClient(hostName, sessionCookie);
		} catch (AxisFault axisFault) {
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			          ": AxisFault thrown while initiating the test : ", axisFault);
		} catch (RemoteException rme) {
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			          ": RemoteException thrown while initiating the test : ", rme);
		} catch (LoginAuthenticationExceptionException lae) {
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			                  ": LoginAuthenticationExceptionException thrown while initiating the test : ",
			          lae);
		}
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

					log.info("Result is there : "+response[i].isResultRowsSpecified());	

					for (int j = 0; j < resultRows.length; j++) {

						columnValues = resultRows[j].getColumnValues();

						for (int k = 0; k < columnValues.length; k++) {
							System.out.print(columnValues[k] + " ");
						}
						System.out.println();
					}
				} else {
					log.info("There's no result");
					log.info(response[i].getQuery());
				}
			}
		}

		// log.info("Query Executed - "+response.toString());

		catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HiveExecutionServiceHiveExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Exception in Execute Query :" + e.getMessage());
		}
	}

	/**
	 * checks login for a service
	 */
	private void login() {
		try {
			boolean loginStatus =
			                      carbonAuthenticatorClient.checkLogin(tenantUser, tenantUserPwd,
			                                                           hostName);
			if (loginStatus) {
				onSuccess();
			} else {
				countNoOfLoginRequests("LoginError", null);
			}
		} catch (RemoteException e) {
			countNoOfLoginRequests("RemoteException", e);
		} catch (LoginAuthenticationExceptionException e) {
			countNoOfLoginRequests("LoginAuthenticationExceptionException", e);
		} catch (Exception e) {
			countNoOfLoginRequests("Exception", e);
		}
	}

	private void countNoOfLoginRequests(String type, Object obj) {
		requestCount++;
		if (requestCount == 3) {
			handleError(type, obj);
			requestCount = 0;
		} else {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// Exception ignored
			}
			login();
		}
	}

	private void handleError(String type, Object obj) {
		if (type.equals("LoginError")) {
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			          ": Login failure. Returned false as a login status by Server");
			onFailure("Tenant login failure");
		} else if (type.equals("AxisFault")) {
			AxisFault axisFault = (AxisFault) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			          ": AxisFault thrown while authenticating the stub : ", axisFault);
			onFailure(axisFault.getMessage());
		} else if (type.equals("RemoteException")) {
			RemoteException remoteException = (RemoteException) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			                  ": RemoteException thrown while login from Heartbeat tenant : ",
			          remoteException);
			onFailure(remoteException.getMessage());
		} else if (type.equals("LoginAuthenticationExceptionException")) {
			LoginAuthenticationExceptionException e = (LoginAuthenticationExceptionException) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			                  ": LoginAuthenticationException thrown while login from Heartbeat tenant : ",
			          e);
			onFailure(e.getMessage());
		} else if (type.equals("Exception")) {
			Exception e = (Exception) obj;
			log.error(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: " + hostName +
			          ": Exception thrown while login from Heartbeat tenant : ", e);
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

		log.info(CaseConverter.splitCamelCase(serviceName) + " - Tenant Login: SUCCESS");
	}

	/**
	 * On test failure
	 * 
	 * @param msg
	 *            error message
	 */
	private void onFailure(String msg) {

		boolean success = false;
		DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
		Connection connection = dbConnectionManager.getConnection();

		long timestamp = System.currentTimeMillis();
		DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);
		DbConnectionManager.insertFailureDetail(connection, timestamp, serviceName, TEST_NAME, msg);

		Mailer mailer = Mailer.getInstance();
		mailer.send(CaseConverter.splitCamelCase(serviceName) + " :FAILURE",
		            CaseConverter.splitCamelCase(TEST_NAME) + ": " + msg, "");
		SMSSender smsSender = SMSSender.getInstance();
		smsSender.send(CaseConverter.splitCamelCase(serviceName) + ": " +
		               CaseConverter.splitCamelCase(TEST_NAME) + ": Failure");
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
