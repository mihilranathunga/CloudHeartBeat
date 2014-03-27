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

public class ModuleUtils {

    /**
     * Creates hostname with http port
     * @return Host name with http port
     */
    public static String getHostWithHttpPort(String host, String httpPort) {
        return host.contains(":") ? host.split(":")[0] + ":" + httpPort : host;
    }

    /**
     * Creates domain name of the tenant by using user name
     * @return Domain name
     */
    public static String getDomainName(String tenantUser){
        return tenantUser.contains("@") ? tenantUser.split("@")[1] : "";
    }

    /**
     * Creates host name without a port if there is any
     * @return Host name without a port
     */
    public static String hostWithoutPort(String host){
        return host.contains(":") ? host.split(":")[0] : host;
    }
}
