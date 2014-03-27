/*
 * Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.cloud.heartbeat.monitor.utils;

import java.io.File;

public class Constants {

    /**
     * Frequently used relative paths
     */
    public static final String HEARTBEAT_CONF_PATH = "heartbeat.conf";
    public static final String LOG4J_PROPERTY_PATH = "resources" + File.separator + "log4j.properties";
    public static final String KEY_STORE_PATH = "resources" + File.separator + "keystores" + File.separator +
            "wso2carbon.jks";
    public static final String TRUST_STORE_PATH = "resources" + File.separator + "truststores" +
            File.separator + "client-truststore.jks";

    /**
     * Frequently used configuration tags
     */
    public static final String HEARTBEAT_TENANT = "heartbeat_tenant";
    public static final String DATA_SOURCE = "data_source";
    public static final String NOTIFICATION = "notification";
    public static final String MODULES = "modules";
    public static final String MANAGER = "manager";
    public static final String GOVERNANCE_REGISTRY = "governance_registry";
    public static final String BUSINESS_RULE_SERVER = "business_rule_server";
    public static final String BUSINESS_PROCESS_SERVER = "business_process_server";
    public static final String DATA_SERVICE_SERVER = "data_service_server";
    public static final String COMPLEX_EVENT_PROCESSOR = "complex_event_processor";
    public static final String STORAGE_SERVER = "storage_server";
    public static final String BUSINESS_ACTIVITY_MONITOR = "business_activity_monitor";
    public static final String TASK_SERVER = "task_server";
    public static final String IDENTITY_SERVER = "identity_server";

    public static final String APPFACTORY = "appfactory";
    public static final String CLOUD_MGT = "cloud_management";

    public static final String STRATOS_CONTROLLER_DEV = "stratos_controller_dev";
    public static final String STRATOS_CONTROLLER_TEST = "stratos_controller_test";
    public static final String STRATOS_CONTROLLER_PROD = "stratos_controller_prod";
    public static final String ESB_DEV = "enterprise_service_bus_dev";
    public static final String ESB_TEST = "enterprise_service_bus_test";
    public static final String ESB_PROD = "enterprise_service_bus_prod";
    public static final String APPLICATION_SERVER_DEV = "application_server_dev";
    public static final String APPLICATION_SERVER_TEST = "application_server_test";
    public static final String APPLICATION_SERVER_PROD = "application_server_prod";
    public static final String BUSINESS_PROCESS_SERVER_DEV = "business_process_server_dev";
    public static final String BUSINESS_PROCESS_SERVER_TEST = "business_process_server_test";
    public static final String BUSINESS_PROCESS_SERVER_PROD = "business_process_server_prod";


    public static final String UES = "ues_server";
    public static final String API_MANAGER = "api_manager";
    public static final String CLOUD_CONTROLLER = "cloud_controller";
    public static final String GITBLIT = "gitblit";
    public static final String S2_GITBLIT = "s2_gitblit";

    public static final String API_GATEWAY = "api_gateway";
    public static final String API_STORE = "api_store";
    public static final String API_PUBLISHER = "api_publisher";
    public static final String API_KEY_MANAGER = "api_key_manager";
    public static final String JENKINS = "jenkins";
}
