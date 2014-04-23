package org.wso2.cloud.heartbeat.monitor.modules.cloudmgt;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.cloud.heartbeat.monitor.core.clients.authentication.JaggeryAppAuthenticatorClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.https.HttpsJaggeryClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.JagApiProperties;
import org.wso2.cloud.heartbeat.monitor.core.notification.Mailer;
import org.wso2.cloud.heartbeat.monitor.core.notification.SMSSender;
import org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.exceptions.FalseReturnException;
import org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.exceptions.JaggeryAppLoginException;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class ChangePassswordTest implements Job {

	private static final Log log = LogFactory.getLog(ChangePassswordTest.class);

	private final String TEST_NAME = "ChangePassswordTest";

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

	private String tenantUserTempPwd;

	/**
	 * @param jobExecutionContext
	 *            "managementHostName", "hostName" ,"tenantUser",
	 *            "tenantUserPwd" "httpPort"
	 *            "deploymentWaitTime" params passed via JobDataMap.
	 * @throws org.quartz.JobExecutionException
	 */
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		setCompleteTestName(CaseConverter.splitCamelCase(serviceName) +
		                    " - Change Passsword Test : ");
		initWebAppTest();
		if (!errorsReported) {
			changePassword();
		}
		if (!errorsReported) {
			resetPasssword();
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

			if (!loginStatus) {
				throw new JaggeryAppLoginException(
				                                   "Login failure to cloudmgt jaggery app. Returned false as login status.");
			}
			isTenantAdmin = true;
		} catch (JaggeryAppLoginException je) {
			countNoOfRequests("JaggeryAppLoginException", je, "initWebAppTest");
		} catch (Exception ee) {
			countNoOfRequests("ExecutionException", ee, "initWebAppTest");
		}
		requestCount = 0;
	}

	/**
	 * Change Tenant Password
	 */
	private void changePassword() {

		try {
			if (loginStatus) {
				String url = hostName + JagApiProperties.CHANGE_PASSWD_CLOUDMGT_TENANT_URL_SFX;
				Map<String, String> params = new HashMap<String, String>();
				params.put("action", "changePassword");
				params.put("oldPassword", tenantUserPwd);
				params.put("password", tenantUserTempPwd);
				String result = HttpsJaggeryClient.httpPost(url, params);
				if (result.equals("false")) {
					throw new FalseReturnException("Change Password returned status as false");
				} else if (result.equals("true")) {
					log.info(TEST_NAME + " : Change Password Success");
				}
			} else {
				throw new JaggeryAppLoginException(
				                                   "Login failure to cloudmgt jaggery app. Returned false as login status");
			}
		} catch (JaggeryAppLoginException je) {
			countNoOfRequests("JaggeryAppLoginException", je, "changePassword");
		} catch (FalseReturnException fe) {
			countNoOfRequests("FalseReturnException", fe, "changePassword");
		} catch (Exception ee) {
			countNoOfRequests("ExecutionException", ee, "changePassword");
		}
		requestCount = 0;
	}

	/**
	 * Reset the tenant password
	 */
	private void resetPasssword() {

		try {
			if (loginStatus) {
				String url = hostName + JagApiProperties.CHANGE_PASSWD_CLOUDMGT_TENANT_URL_SFX;
				Map<String, String> params = new HashMap<String, String>();
				params.put("action", "changePassword");
				params.put("oldPassword", tenantUserTempPwd);
				params.put("password", tenantUserPwd);
				String result = HttpsJaggeryClient.httpPost(url, params);

				if (result.equals("false")) {
					throw new FalseReturnException(
					                               "Reset Password returned status as false. New Password for user:" +
					                                       tenantUser +
					                                       " is \'" +
					                                       tenantUserTempPwd + "\'");
				} else if (result.equals("true")) {
					log.info(TEST_NAME + " :Reset Password Success");
					onSuccess();
				}
			} else {
				throw new JaggeryAppLoginException(
				                                   "Login failure to cloudmgt jaggery app. Returned false as a login status. New Password for user:" +
				                                           tenantUser +
				                                           " is \'" +
				                                           tenantUserTempPwd + "\'");
			}
		} catch (FalseReturnException fe) {
			countNoOfRequests("FalseReturnException", fe, "resetPasssword");
		} catch (JaggeryAppLoginException je) {
			countNoOfRequests("JaggeryAppLoginException", je, "resetPasssword");
		} catch (Exception ee) {
			countNoOfRequests("ExecutionException", ee, "changePassword");
		}
		requestCount = 0;
	}

	private void countNoOfRequests(String type, Object obj, String method) {

		requestCount++;
		log.info("Retrying :" + method + " count: " + requestCount + " type: " + type);
		if (requestCount == 3) {
			System.out.println("3 times retried, handling error" + method);
			handleError(type, obj, method);
			requestCount = 0;
		} else {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// Exception ignored
			}

			if (type.equals("JaggeryAppLoginException")) {
				if (method.equals("initWebAppTest")) {
					initWebAppTest();
				} else if (method.equals("changePassword")) {
					changePassword();
				} else if (method.equals("resetPasssword")) {
					resetPasssword();
				}
			} else if (type.equals("FalseReturnException")) {
				if (method.equals("changePassword")) {
					changePassword();
				} else if (method.equals("resetPasssword")) {
					resetPasssword();
				}
			} else if (type.equals("ExecutionException")) {
				if (method.equals("initWebAppTest")) {
					initWebAppTest();
				} else if (method.equals("changePassword")) {
					changePassword();
				} else if (method.equals("resetPasssword")) {
					resetPasssword();
				}
			}

		}
	}

	private void handleError(String type, Object obj, String method) {
		if (type.equals("JaggeryAppLoginException")) {
			JaggeryAppLoginException jaggeryAppLoginException = (JaggeryAppLoginException) obj;
			if (method.equals("initWebAppTest")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Initiate Test: " +
				          hostName, jaggeryAppLoginException);
			} else if (method.equals("changePassword")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Change Password: " +
				          hostName, jaggeryAppLoginException);
			} else if (method.equals("resetPasssword")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Reset Password: " +
				          hostName, jaggeryAppLoginException);
			}
			onFailure(jaggeryAppLoginException.getMessage());
		} else if (type.equals("FalseReturnException")) {
			FalseReturnException falseReturnException = (FalseReturnException) obj;
			if (method.equals("changePassword")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Change Password: " +
				          hostName, falseReturnException);
			} else if (method.equals("resetPasssword")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Reset Password: " +
				          hostName, falseReturnException);
			}
			onFailure(falseReturnException.getMessage());
		} else if (type.equals("ExecutionException")) {
			Exception exception = (Exception) obj;
			if (method.equals("initWebAppTest")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Initiate Test: " +
				          hostName, exception);
			} else if (method.equals("changePassword")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Change Password: " +
				          hostName, exception);
			} else if (method.equals("resetPasssword")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Reset Password: " +
				          hostName, exception);
			}
			onFailure(exception.getMessage());
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

		log.error(completeTestName + "FAILURE  - " + msg);
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
	 * Sets temporary new password to tenant
	 * 
	 * @param tenantUserTempPwd
	 *            the tenantUserTempPwd to set
	 */
	public void setTenantUserTempPwd(String tenantUserTempPwd) {
		this.tenantUserTempPwd = tenantUserTempPwd;
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