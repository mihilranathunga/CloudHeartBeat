/**
 * 
 */
package org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.exceptions;

/**
 * @author mihil@wso2.com
 *
 */
public class JaggeryAppLoginException extends Exception {

	/**
	 * 
	 */
    private static final long serialVersionUID = 1L;

	/**
	 * This class is a custom exception which can be thrown when login Status
	 * of a jaggery app returns false when authenticating a user
	 * using JaggeryAppAuthenticatorClient class
	 */
	public JaggeryAppLoginException() {
		super();
	}

	/**
	 * @param message
	 */
	public JaggeryAppLoginException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public JaggeryAppLoginException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JaggeryAppLoginException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public JaggeryAppLoginException(String message, Throwable cause, boolean enableSuppression,
	                                boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
