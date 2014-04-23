/**
 * 
 */
package org.wso2.cloud.heartbeat.monitor.modules.cloudmgt.exceptions;

/**
 * @author mihil@wso2.com
 *
 */
public class FalseReturnException extends Exception {

	/**
	 * 
	 */
    private static final long serialVersionUID = 1L;

	/**
	 * This exception can be thrown when a jaggery api call returns false
	 */
	public FalseReturnException() {
		super();
	}

	/**
	 * @param message
	 */
	public FalseReturnException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public FalseReturnException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public FalseReturnException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public FalseReturnException(String message, Throwable cause, boolean enableSuppression,
	                            boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
