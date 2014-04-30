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

package org.wso2.cloud.heartbeat.monitor.modules.common.exceptions;

public class JaggeryAppLoginException extends Exception {

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
}
