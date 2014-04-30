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


public class FalseReturnException extends Exception {

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
}
