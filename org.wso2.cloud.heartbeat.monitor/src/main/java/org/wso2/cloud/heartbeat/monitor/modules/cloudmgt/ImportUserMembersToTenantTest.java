/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.exceptions.FalseReturnException;
import org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.exceptions.JaggeryAppLoginException;
import org.wso2.cloud.heartbeat.monitor.utils.DbConnectionManager;
import org.wso2.cloud.heartbeat.monitor.utils.ModuleUtils;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.CaseConverter;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class ImportUserMembersToTenantTest implements Job {

	private static final Log log = LogFactory.getLog(ImportUserMembersToTenantTest.class);

	private final String TEST_NAME = "ImportUserMembersToTenantTest";

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

	private String memberName = "heartbeatMember";
	private String memberUserName;
	private String memberDefaultPassword = "password";

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
		                    " - Import User Members To Tenant Test : ");
		initWebAppTest();
		if (!errorsReported) {
			addMemberToTenant();
		}
		if (!errorsReported) {
			updateMember();
		}
		if (!errorsReported) {
			loginWithMember();
		}

		if (!errorsReported) {
			deleteMember();
		}
	}

	/**
	 * Initializes Web application service test
	 */
	private void initWebAppTest() {
		errorsReported = false;

		try {
			hostName = "https://" + hostName;

			memberUserName = memberName + "@" + ModuleUtils.getDomainName(tenantUser);

			authenticatorClient = new JaggeryAppAuthenticatorClient(hostName, "cloudmgt");
			loginStatus = authenticatorClient.login(tenantUser, tenantUserPwd);

			if (!loginStatus) {
				throw new JaggeryAppLoginException("Login failure to cloudmgt jaggery app. Returned false as login status");
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
	 * import Member to tenant
	 */
	private void addMemberToTenant() {
		try {
	        if (loginStatus) {
	        	String url = hostName + JagApiProperties.ADD_USER_TO_CLOUDMGT_TENANT_URL_SFX;
	        	Map<String, String> params = new HashMap<String, String>();
	        	params.put("action", "bulkImportUsers");
	        	params.put("users", memberName);
	        	params.put("defaultPassword", memberDefaultPassword);
	        	String result = HttpsJaggeryClient.httpPost(url, params);
	        	if (result.equals("false")) {
	        		throw new FalseReturnException("Add Member to Tenant returned status as false");
	        	} else if (result.equals("true")) {
            		log.info(TEST_NAME+" : Import Member Success");
            	}
	        } else {
	        	throw new JaggeryAppLoginException("Login failure to cloudmgt jaggery app. Returned false as login status");
	        }
        } catch (JaggeryAppLoginException je) {
        	countNoOfRequests("JaggeryAppLoginException", je ,"addMemberToTenant");
        } catch (FalseReturnException fe) {
        	countNoOfRequests("FalseReturnException", fe ,"addMemberToTenant");
        } catch (Exception ee) {
        	countNoOfRequests("ExecutionException", ee ,"addMemberToTenant");
        }
		requestCount = 0;
	}

	/**
	 * Updates the member with 'developer' role
	 */
	private void updateMember() {

		try {
	        if (loginStatus) {
	        	String url = hostName + JagApiProperties.ADD_USER_TO_CLOUDMGT_TENANT_URL_SFX;
	        	Map<String, String> params = new HashMap<String, String>();
	        	params.put("action", "updateUserRoles");
	        	params.put("rolesToAdd", "developer");
	        	params.put("rolesToDelete", "");
	        	params.put("userName", memberName);
	        	String result = HttpsJaggeryClient.httpPost(url, params);
	        	if (result.equals("false")) {
	        		throw new FalseReturnException("Update Member returned status as false");
	        	} else if (result.equals("true")) {
	        		log.info(TEST_NAME+" : Update Member Success");
	        	}
	        } else {
	        	throw new JaggeryAppLoginException("Login failure to cloudmgt jaggery app. Returned false as login status");
	        }
        } catch (JaggeryAppLoginException je) {
        	countNoOfRequests("JaggeryAppLoginException", je ,"updateMember");
        } catch (FalseReturnException fe) {
        	countNoOfRequests("FalseReturnException", fe ,"updateMember");
        } catch (Exception ee) {
        	countNoOfRequests("ExecutionException", ee ,"updateMember");
        }
		requestCount = 0;
	}

	/**
	 * Log in with the created member
	 */
	private void loginWithMember() {
		try {
	        authenticatorClient.logout();
	        loginStatus = false;
	        isTenantAdmin = false;
	        loginStatus = authenticatorClient.login(memberUserName, memberDefaultPassword);
	        if (!loginStatus) {
	        	throw new JaggeryAppLoginException("Imported Member Login Failure to cloudmgt jaggery app. Returned false as login status");
	        }
	        authenticatorClient.logout();
        } catch (JaggeryAppLoginException je) {
        	countNoOfRequests("JaggeryAppLoginException", je ,"loginWithMember");
        } catch (Exception ee) {
        	countNoOfRequests("ExecutionException", ee ,"loginWithMember");
        }
	}
	
	/**
	 * Delete Imported Member from tenant
	 * Before doing this tenant needs to be relogged to jaggery app
	 */

	private void deleteMember() {
		try {
	        loginStatus = authenticatorClient.login(tenantUser, tenantUserPwd);
	        isTenantAdmin = true;
	        if (loginStatus) {
	        	String url = hostName + JagApiProperties.ADD_USER_TO_CLOUDMGT_TENANT_URL_SFX;
	        	Map<String, String> params = new HashMap<String, String>();
	        	params.put("action", "deleteUserFromTenant");
	        	params.put("userName", memberName);
	        	String result = HttpsJaggeryClient.httpPost(url, params);
	        	if (result.equals("true")) {
	        		log.info(TEST_NAME+" : Delete Member Success");
	        		onSuccess();
	        	} else if (result.equals("false")) {
	        		throw new FalseReturnException("Delete Member returned status as false");
	        	}
	        } else {
	        	throw new JaggeryAppLoginException("Login failure to cloudmgt jaggery app. Returned false as login status");
	        }
        } catch (JaggeryAppLoginException je) {
        	countNoOfRequests("JaggeryAppLoginException", je ,"deleteMember");
        } catch (FalseReturnException fe) {
        	countNoOfRequests("FalseReturnException", fe ,"deleteMember");
        } catch (Exception ee) {
        	countNoOfRequests("ExecutionException", ee ,"deleteMember");
        }
		requestCount = 0;
	}
	
	/**
	 * Retries the Methods if errors occur
	 * 
	 * @param type Exception type
	 * @param obj  Exception object
	 * @param method Method Name
	 */

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
				} else if (method.equals("addMemberToTenant")) {
					addMemberToTenant();
				} else if (method.equals("updateMember")) {
					updateMember();
				} else if (method.equals("loginWithMember")) {
					loginWithMember();
				} else if (method.equals("deleteMember")) {
					deleteMember();
				}
			} else if (type.equals("FalseReturnException")) {

				if (method.equals("addMemberToTenant")) {
					addMemberToTenant();
				} else if (method.equals("updateMember")) {
					updateMember();
				} else if (method.equals("deleteMember")) {
					deleteMember();
				}
			} else if (type.equals("ExecutionException")) {
				if (method.equals("initWebAppTest")) {
					initWebAppTest();
				} else if (method.equals("addMemberToTenant")) {
					addMemberToTenant();
				} else if (method.equals("updateMember")) {
					updateMember();
				} else if (method.equals("loginWithMember")) {
					loginWithMember();
				} else if (method.equals("deleteMember")) {
					deleteMember();
				}
			}

		}
	}
	
	/**
	 * Handles errors when retrying failed
	 * 
	 * @param type Exception type
	 * @param obj  Exception object
	 * @param method Method Name
	 */

	private void handleError(String type, Object obj, String method) {
		if (type.equals("JaggeryAppLoginException")) {
			JaggeryAppLoginException jaggeryAppLoginException = (JaggeryAppLoginException) obj;
			if (method.equals("initWebAppTest")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Initiate Test: " +
				          hostName, jaggeryAppLoginException);
			} else if (method.equals("addMemberToTenant")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Import Member: " +
				          hostName, jaggeryAppLoginException);
			} else if (method.equals("updateMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Update Member: " +
				          hostName, jaggeryAppLoginException);
			} else if (method.equals("loginWithMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Login with Member: " +
				          hostName, jaggeryAppLoginException);
			} else if (method.equals("deleteMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Delete Member: " +
				          hostName, jaggeryAppLoginException);
			}
			onFailure(jaggeryAppLoginException.getMessage());
		} else if (type.equals("FalseReturnException")) {
			FalseReturnException falseReturnException = (FalseReturnException) obj;
			if (method.equals("addMemberToTenant")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Import Member: " +
				          hostName, falseReturnException);
			} else if (method.equals("updateMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Update Member: " +
				          hostName, falseReturnException);
			} else if (method.equals("deleteMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Delete Member: " +
				          hostName, falseReturnException);
			}
			onFailure(falseReturnException.getMessage());
		} else if (type.equals("ExecutionException")) {
			Exception exception = (Exception) obj;
			if (method.equals("initWebAppTest")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Initiate Test: " +
				          hostName, exception);
			} else if (method.equals("addMemberToTenant")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Import Member: " +
				          hostName, exception);
			} else if (method.equals("updateMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Update Member: " +
				          hostName, exception);
			} else if (method.equals("loginWithMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Login with Member: " +
				          hostName, exception);
			} else if (method.equals("deleteMember")) {
				log.error(CaseConverter.splitCamelCase(serviceName) + " - Delete Member: " +
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
		log.error(completeTestName + "FAILURE  - " +msg);
		
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
