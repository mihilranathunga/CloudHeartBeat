package org.wso2.cloud.heartbeat.monitor.modules.cloudmgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.JaggeryAppAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.https.HttpsJaggeryClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.JagApiProperties;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class UserFunctionsTest implements Job{

	private static final Log log = LogFactory.getLog(UserFunctionsTest.class);

	private final String TEST_NAME = "UserFunctionsTest";

	private String hostName;
	private String tenantUser;
	private String tenantUserPwd;
	private int deploymentWaitTime;
	private String serviceName;

	private String completeTestName;

	private boolean errorsReported;
	private int requestCount = 0;

	private JaggeryAppAuthenticatorClient authenticatorClient;
	private boolean isTenantAdmin = false;
	private boolean loginStatus = false;

	private String newPwd = "mihil1234";
	private String oldPwd = "wso2mihil";

	/**
	 * @param jobExecutionContext
	 *            "managementHostName", "hostName" ,"tenantUser",
	 *            "tenantUserPwd" "httpPort"
	 *            "deploymentWaitTime" params passed via JobDataMap.
	 * @throws org.quartz.JobExecutionException
	 */
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		setCompleteTestName(CaseConverter.splitCamelCase(serviceName) + " - User Functions Test : ");
		initWebAppTest();
		if (!errorsReported) {
			changePassword();
		}
		if (!errorsReported) {
			resetPasssword();
		}
		if(!errorsReported){
			onSuccess();
		}
	}

	/**
	 * Initializes Web application service test
	 */
	private void initWebAppTest() {
		errorsReported = false;
		try {
			hostName = "https://" + hostName;

			authenticatorClient = new JaggeryAppAuthenticatorClient(hostName, "cloudmgt");
			loginStatus = authenticatorClient.login(tenantUser, tenantUserPwd);
		} catch (Exception e) {
			log.error("Login error - cloud mgt " + e.getMessage());
		}
		if (loginStatus) {

			log.info("Looged into cloud mgt successfully");

		} else {
			log.error("Initial logging in didn't work");
		}
		isTenantAdmin = true;
	}

	/**
	 * Change Tenant Password
	 */
	private void changePassword() {

			if (loginStatus) {
				String url = hostName + JagApiProperties.CHANGE_PASSWD_CLOUDMGT_TENANT_URL_SFX;
				Map<String, String> params = new HashMap<String, String>();
				params.put("action", "changePassword");
				params.put("oldPassword", oldPwd);
				params.put("password", newPwd);
				String result = HttpsJaggeryClient.httpPost(url, params);
				if (result.equals("false")) {
					log.error("Change Password returned as false in method \'change Password\', retrying..");
					countNoOfRequests("Function error", "changePassword");
				} else if (result.equals("true")) {
					log.info("Change Password Success!");
				}
			} else {
				log.error("Not logged in method : changePassword");
				countNoOfRequests("LoginError","changePassword");
				
			}
	}

	/**
	 * Logging into tenant
	 */
	private void login(String method) {
		authenticatorClient.logout();
		loginStatus = false;
		isTenantAdmin = false;

		loginStatus = authenticatorClient.login(tenantUser, tenantUserPwd);

		if (!loginStatus) {
			log.error("Login error at method : " + method);
		}
	}

	/**
	 * Reset the tenant password
	 */
	private void resetPasssword() {

			if (loginStatus) {
				String url = hostName + JagApiProperties.CHANGE_PASSWD_CLOUDMGT_TENANT_URL_SFX;
				Map<String, String> params = new HashMap<String, String>();
				params.put("action", "changePassword");
				params.put("oldPassword", newPwd);
				params.put("password", oldPwd);
				String result = HttpsJaggeryClient.httpPost(url, params);

				if (result.equals("false")) {
					log.error("Change Password returned as false, in method \'Reset Password\' retrying..");
					countNoOfRequests("Function error","resetPasssword");
				} else if (result.equals("true")) {
					log.info("Reset Password Success!");
					requestCount=0;
				}
			} else {
				log.error("Not logged in method : resetPassword");
				countNoOfRequests("LoginError","resetPasssword");
			}
	}

	private void countNoOfRequests(String type, String method) {

		requestCount++;
		System.out.println("Retrying :" + method + " count: " + requestCount + " type: " + type);
		if (requestCount == 3) {
			System.out.println("3 times retried, handling error" + method);
			handleError(type, method);
			requestCount = 0;
		} else {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// Exception ignored
			}

			// Logs only if this login needs to be a tenant admin login,
			// therefore ignores 'loginWithMember'
			if (type.equals("LoginError") && !method.equals(method)) {
				login(method);
			}
			else if(type.equals("Function error") && !method.equals("changePassword")){
				changePassword();
			}
			else if(type.equals("Function error") && !method.equals("resetPasssword")){
				resetPasssword();
			}

		}
	}

	private void handleError(String type, String method) {
		if (type.equals("Function error")) {
			String msg = null;
			// Which method gave the error
			if (method.equals("changePassword")) {
				msg = "Changing Tenant Password";
				onFailure(msg);
			} else if (method.equals("resetPassword")) {
				msg = "Resetting Original Password";
				if (isTenantAdmin) {
					msg =
					      "Tenant Admin User Function test failure in cloudmgt jaggery app while " +
					              msg;
				}
				log.error(completeTestName + msg);
				onFailure(msg);
			}
		} else if (type.equals("FailedMemberAddition")) {
			log.error(completeTestName + "Failed to add member into tenant.");
			onFailure("Failed to add member into tenant");
		}
	}

	/**
	 * On success
	 */
	private void onSuccess() {
		boolean success = true;
		DbConnectionManager dbConnectionManager = DbConnectionManager.getInstance();
		Connection connection = dbConnectionManager.getConnection();

		long timestamp = System.currentTimeMillis();
		DbConnectionManager.insertLiveStatus(connection, timestamp, serviceName, TEST_NAME, success);

		log.info(completeTestName + "SUCCESS");
	}

	/**
	 * On failure
	 * 
	 * @param msg
	 *            fault message
	 */
	private void onFailure(String msg) {
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
			mailer.send(CaseConverter.splitCamelCase(serviceName) + ": FAILURE",
			            CaseConverter.splitCamelCase(TEST_NAME) + ": " + msg, "");

			SMSSender smsSender = SMSSender.getInstance();
			smsSender.send(CaseConverter.splitCamelCase(serviceName) + ": " +
			               CaseConverter.splitCamelCase(TEST_NAME) + ": Failure");
			errorsReported = true;
		}
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
	 * Sets deployment waiting time
	 * 
	 * @param deploymentWaitTime
	 *            Deployment wait time
	 */
	public void setDeploymentWaitTime(String deploymentWaitTime) {
		this.deploymentWaitTime =
		                          Integer.parseInt(deploymentWaitTime.split("s")[0].replace(" ", "")) * 1000;
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
	 * 
	 * @param completeTestName
	 *            Service name
	 */
	public void setCompleteTestName(String completeTestName) {
		this.completeTestName = completeTestName;
	}

}
