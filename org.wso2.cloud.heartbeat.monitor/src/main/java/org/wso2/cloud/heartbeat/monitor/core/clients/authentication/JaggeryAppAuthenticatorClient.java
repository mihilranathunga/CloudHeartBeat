/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.cloud.heartbeat.monitor.core.clients.authentication;

import org.wso2.cloud.heartbeat.monitor.core.clients.https.HttpsJaggeryClient;
import org.wso2.cloud.heartbeat.monitor.core.clients.utils.JagApiProperties;

import java.util.HashMap;
import java.util.Map;

public class JaggeryAppAuthenticatorClient {

    private String loginUrl;
    private String logOutUrl;


    public JaggeryAppAuthenticatorClient(String hostName) {
        loginUrl = hostName + JagApiProperties.LOGIN_URL_SFX;
        logOutUrl = hostName + JagApiProperties.LOGOUT_URL_SFX;
    }

    public boolean login(String userName, String password){
        Map<String, String> params = new HashMap<String, String>();
        params.put("action", "login");
        params.put("userName", userName);
        params.put("password", password);
        String value = HttpsJaggeryClient.httpPostLogin(loginUrl, params);
        if (!"false".equals(value)) {
            return true;
        }else{
            return false;
        }

    }

    public void logout(){
        Map<String, String> params = new HashMap<String, String>();
        params.put("action", "logout");
        HttpsJaggeryClient.httpPost(logOutUrl, params);
    }

}
