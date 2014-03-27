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

package org.wso2.cloud.heartbeat.monitor.core.notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;
import org.wso2.cloud.heartbeat.monitor.core.configuration.parser.nginx.NodeBuilder;
import org.wso2.cloud.heartbeat.monitor.core.notification.utils.MailAuthenticator;
import org.wso2.cloud.heartbeat.monitor.utils.Constants;
import org.wso2.cloud.heartbeat.monitor.utils.fileutils.FileManager;

import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;
import java.io.IOException;
import java.util.Properties;

/**
 * Mail notifications implemented in this class
 */
public class Mailer {
    private static final Log log = LogFactory.getLog(Mailer.class);
    private static Mailer instance;

    private static boolean alertsOn;
    private static boolean authEnabled;
    private static String mailUser;
    private static String mailUserPwd;
    private static String smtpServer;
    private static String smtpPort;
    private static String from;
    private static String toAddresses;

    /**
     * Returns instance of the Mailer object if the instance is null Initializes Mailer
     * @return Mailer instance
     */
    public static Mailer getInstance(){
        if(instance==null){
            instance = new Mailer();
        }
        return instance;
    }

    /**
     * Initializes Mailer object
     */
    private Mailer() {
        Node rootNode = new Node();
        try {
            NodeBuilder.buildNode(rootNode, FileManager.readFile(Constants.HEARTBEAT_CONF_PATH));
            Node notification = rootNode.findChildNodeByName(Constants.NOTIFICATION).findChildNodeByName("email");
            if(notification != null ){
                alertsOn = notification.getProperty("alerts").equalsIgnoreCase("true");
                authEnabled = notification.getProperty("auth").equalsIgnoreCase("enabled");
                mailUser = notification.getProperty("mail_user");
                mailUserPwd = notification.getProperty("mail_user_pwd");
                smtpServer = notification.getProperty("smtp_server");
                smtpPort = notification.getProperty("smtp_port");

                from = notification.getProperty("from_address");
                toAddresses = notification.getProperty("to_addresses");
            }
        } catch (IOException e) {
            log.fatal("Mail Notification: IOException thrown while getting the connection: reading " +
                      "the conf", e);
        } catch (Exception e){
            log.fatal("Mail Notification: Exception thrown while getting the connection: reading the" +
                      " conf: verify your notification settings", e);
        }
    }

    /**
     * Sends mail with specified params
     * @param subject Subject of the mail
     * @param text1 Text body part one
     * @param text2 Text body part two
     */
    public void send(String subject, String text1, String text2){

        if(alertsOn){
            Properties props = new Properties();
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.host", smtpServer);
            props.put("mail.smtp.port", smtpPort);

            Session session;
            if(authEnabled){
                props.put("mail.smtp.auth", "true");
                session = Session.getInstance(props, new MailAuthenticator(mailUser, mailUserPwd));
            } else {
                session = Session.getDefaultInstance(props);
            }
            Message simpleMessage = new MimeMessage(session);
            InternetAddress fromAddress = null;

            String [] recipients = toAddresses.split(",");
            InternetAddress[] addressTo= new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                try {
                    addressTo[i] = new InternetAddress(recipients[i]);
                } catch (AddressException e) {
                    log.fatal("Mail Notification: AddressException thrown while sending alerts", e);
                }
            }
            try {
                fromAddress = new InternetAddress(from);
            } catch (AddressException e) {
                log.fatal("Mail Notification: AddressException thrown while sending alerts", e);
            }

            try {
                simpleMessage.setFrom(fromAddress);
                simpleMessage.setRecipients(RecipientType.TO, addressTo);
                simpleMessage.setSubject("Cloud Heartbeat: "+ subject);

                Multipart multipart = new MimeMultipart();

                BodyPart part1 = new MimeBodyPart();
                part1.setContent(text1, "text/html");

                BodyPart part2 = new MimeBodyPart();
                part2.setContent(text2, "text/html");

                multipart.addBodyPart(part1);
                multipart.addBodyPart(part2);

                simpleMessage.setContent(multipart);
                Transport.send(simpleMessage);
            } catch (MessagingException e) {
                log.fatal("Mail Notification: MessagingException thrown while getting the connection:" +
                          " sending alerts", e);
            }
        } else {
            log.warn("Mail Notification: Notification settings disabled");
        }
    }
 }
