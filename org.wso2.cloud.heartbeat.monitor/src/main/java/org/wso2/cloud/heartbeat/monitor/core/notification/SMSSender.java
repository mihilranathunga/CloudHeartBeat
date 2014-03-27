/*
 * Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.wso2.cloud.heartbeat.monitor.core.notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.smslib.*;
import org.smslib.http.BulkSmsHTTPGateway;
import org.smslib.http.ClickatellHTTPGateway;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;
import org.wso2.cloud.heartbeat.monitor.core.configuration.parser.nginx.NodeBuilder;
import org.wso2.cloud.heartbeat.monitor.utils.Constants;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.FileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * SMS notifications implemented in this class.Only "Clickatell" and "BulkSMS", Bulk SMS provider support
 */
public class SMSSender {
    private static final Log log = LogFactory.getLog(SMSSender.class);
    private enum SMSProvider {CLICKATELL, BULKSMS}

    private static SMSSender instance;
    private SMSProvider provider;
    private static boolean alertsOn;
    private static String httpUrl;
    private static String apiId;
    private static String user;
    private static String password;
    private static ArrayList <String> recipients;

    /**
     * Returns instance of the SMSSender if the instance is null, Initialises the SMSSender
     * @return SMSSender instance
     */
    public static synchronized SMSSender getInstance(){
        if(instance==null){
            instance = new SMSSender();
        }
        return instance;
    }

    /**
     * Initializes the SMSSender object
     */
    private SMSSender(){
        Node rootNode = new Node();
        try{
            NodeBuilder.buildNode(rootNode, FileManager.readFile(Constants.HEARTBEAT_CONF_PATH));
            Node sms = rootNode.findChildNodeByName(Constants.NOTIFICATION).findChildNodeByName("sms_http");

            alertsOn = sms.getProperty("alerts").equalsIgnoreCase("true");
            httpUrl = sms.getProperty("http");
            provider = sms.getProperty("provider").equalsIgnoreCase("clickatell") ?
                           SMSProvider.CLICKATELL : SMSProvider.BULKSMS;
            if(provider== SMSProvider.CLICKATELL){
                apiId = sms.getProperty("api_id");
            }
            user = sms.getProperty("user");
            password = sms.getProperty("password");
            recipients = new ArrayList<String>(Arrays.asList((sms.getProperty("recipients")).split(",")));
        } catch (IOException e) {
            log.fatal("SMS Notification: IOException thrown while getting the connection: reading the conf", e);
        } catch (Exception e){
            log.fatal("SMS Notification: Exception thrown while getting the connection: reading the conf", e);
        }
    }

    /**
     * Sends SMS
     * @param text Text to be sent
     */
    public synchronized void send (String text){
        try{
            if(alertsOn){
                if(provider== SMSProvider.CLICKATELL){
                    ClickatellHTTPGateway gateway = new ClickatellHTTPGateway(httpUrl, apiId, user, password);
                    gateway.setOutbound(true);
                    gateway.setSecure(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                } else if(provider == SMSProvider.BULKSMS){
                    BulkSmsHTTPGateway gateway = new BulkSmsHTTPGateway(httpUrl, user, password);
                    gateway.setOutbound(true);
                    Service.getInstance().addGateway(gateway);
                    Service.getInstance().startService();
                }
                for (String recipient : recipients) {
                    recipient = recipient.replace(" ","");
                    OutboundMessage msg = new OutboundMessage(recipient,"Cloud Heartbeat: " + text);
                    Service.getInstance().sendMessage(msg);
                }
                Service.getInstance().stopService();
            } else {
                log.warn("SMS Notification: Notification SMS settings disabled");
            }
        } catch (IOException e) {
            log.error("SMS Notification: IOException thrown while sending a notification sms: ", e);
        } catch (GatewayException e) {
            log.error("SMS Notification: GatewayException thrown while sending a notification sms: ", e);
        } catch (TimeoutException e) {
            log.error("SMS Notification: TimeoutException thrown while sending a notification sms: ", e);
        } catch (InterruptedException e) {
            log.error("SMS Notification: InterruptedException thrown while sending a notification sms: ", e);
        } catch (SMSLibException e) {
            log.error("SMS Notification: SMSLibException thrown while sending a notification sms: ", e);
        }
    }
}
