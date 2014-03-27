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

package org.wso2.cloud.heartbeat.monitor.core.schedule.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronExpression;
import org.wso2.cloud.heartbeat.monitor.core.configuration.Node;

/**
 * Utilities for Quartz triggers implemented in this class
 */
public class TriggerUtils {

    private static final Log log = LogFactory.getLog(TriggerUtils.class);

    public enum TriggerType {CRON, SIMPLE}

    /**
     * Trigger types in underscore case format
     */
    public static final String CRON_TRIGGER = "cron_trigger";
    public static final String SIMPLE_TRIGGER = "simple_trigger";

    /**
     * for simple trigger specifications
     */
    public static final String MINUTES = "m";
    public static final String HOURS = "h";

    /**
     * Returns trigger type required a node
     * @param node Service node
     * @return Quartz trigger type
     * @throws Exception
     */
    public static TriggerType getTriggerType(Node node) throws Exception {
        boolean simpleTrigger = !(node.getProperty(SIMPLE_TRIGGER) == null ||
                                 (node.getProperty(SIMPLE_TRIGGER) != null && node.getProperty(SIMPLE_TRIGGER).isEmpty()));
        boolean cronTrigger =!(node.getProperty(CRON_TRIGGER) == null ||
                              (node.getProperty(CRON_TRIGGER) != null && node.getProperty(CRON_TRIGGER).isEmpty()));

        if(!simpleTrigger && cronTrigger){
            return TriggerType.CRON;
        } else if (simpleTrigger && !cronTrigger){
            return TriggerType.SIMPLE;
        } else {
            log.error("Trigger type duplication detected in configuration: "  + node.getName());
            throw new Exception("Trigger type duplication detected in configuration: " + node.getName());
        }
    }

    /**
     * Checks validity of a Cron expression
     * @param expression Cron expression
     * @return True if the Cron expression is valid
     */
    public static boolean isValidCronExpression(String expression){
        return CronExpression.isValidExpression(expression);
    }

    /**
     * Checks validity of a Simple trigger expression
     * @param expression Simple trigger expression
     * @return True if the simple trigger expression is valid
     */
    public static boolean isValidSimpleTriggerExpression(String expression){
       if(expression.endsWith(HOURS)){return isContainsInt(expression.split(HOURS)[0]);}
       else return expression.endsWith(MINUTES) && isContainsInt(expression.split(MINUTES)[0]);
    }

    /**
     * Checks the expression contains an integer
     * @param expression Simple trigger expression after removing hour/minute tag
     * @return True if the expression contains an integer
     */
    private static boolean isContainsInt(String expression) {
        try {
            Integer.parseInt(expression.replace(" ", ""));
            return true;
        } catch (Exception e){
            return false;
        }
    }
}
