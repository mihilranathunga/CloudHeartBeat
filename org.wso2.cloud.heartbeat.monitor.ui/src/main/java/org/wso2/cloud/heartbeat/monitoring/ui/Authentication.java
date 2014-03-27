/*
 * Copyright 2005-2012 WSO2, Inc. http://www.wso2.org
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

package org.wso2.cloud.heartbeat.monitoring.ui;

import org.wso2.cloud.heartbeat.monitoring.ui.configuration.Node;
import org.wso2.cloud.heartbeat.monitoring.ui.configuration.parser.nginx.NodeBuilder;
import org.wso2.cloud.heartbeat.monitoring.ui.utils.FileManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

public class Authentication {

    private static final Log log = LogFactory.getLog(Authentication.class);

    private String configPath;
    private Node rootNode;

    public Authentication (String configPath){
        this.configPath = configPath;
        this.rootNode = new Node();
        readNodes();
    }

    private void readNodes() {
        try {
            NodeBuilder.buildNode(rootNode, FileManager.readFile(configPath));
        } catch (IOException e) {
            log.error("Heartbeat - Monitor - IOException thrown while reading the configuration file: ", e);
        }
    }

    public boolean checkLogin(String userName ,String password){
        Node user = rootNode.findChildNodeByName("admin_user");
        return (user.getProperty("user")).equals(userName) && (user.getProperty("password")).equals(password);
    }
}
