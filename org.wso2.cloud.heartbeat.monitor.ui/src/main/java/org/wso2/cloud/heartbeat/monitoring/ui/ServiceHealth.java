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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServiceHealth {

    public ServiceHealth(String serviceName) {
        this.serviceName = serviceName;
        this.successTests = new HashMap<String, String>();
        this.failureTests = new HashMap<String, String>();
        this.failureDetails = new HashMap<String, String>();
        this.testTimestamps = new ArrayList<Timestamp>();
        formatTestDate = new SimpleDateFormat("yyyy.MM.dd h:mm a");               //tests' date and time
    }

    public void addSuccessTest(String testName, Timestamp dateTime) {
        testTimestamps.add(dateTime);
        successTests.put(checkConventions(testName), formatTestDate.format(dateTime));
    }

    public void addFailureTest(String testName, Timestamp dateTime) {
        testTimestamps.add(dateTime);
        failureTests.put(checkConventions(testName), formatTestDate.format(dateTime));
    }

    public void addFailureTestDetails(String testName,  String detail, Timestamp timestamp) {
        failureDetails.put(checkConventions(testName),detail);
    }

    private String checkConventions(String testName) {
        if(testName.contains("Axis 2")){
            return testName.replace("Axis 2", "Axis2");
        } else if (testName.contains("Jaxrs")){
            return testName.replace("Jaxrs", "JAXRS");
        } else if (testName.contains("Jaxws")){
            return testName.replace("Jaxws", "JAXWS");
        } else if (testName.contains("Bpel")){
            return testName.replace("Bpel", "BPEL");
        } else if (testName.contains("Jms")){
            return testName.replace("Jms", "JMS");
        } else return testName;
    }

    public Map <String, String> getSuccessTests() {
        return successTests;
    }

    public Map <String, String> getFailureTests () {
        return failureTests;
    }

    public Map <String, String> getFailureDetails () {
        return failureDetails;
    }

    public String getLastTestDateTime(){
        if(!testTimestamps.isEmpty()){
            Collections.sort(testTimestamps, Collections.reverseOrder());
            return  formatTestDate.format(testTimestamps.get(0));
        } else {
            return "Not Available";
        }
    }

    public Status getServiceStatus() {
        if(successTests.isEmpty()){
            return Status.FAILURE;
        } else if(failureTests.isEmpty()){
            return Status.SUCCESS;
        } else {
            return Status.PROBLEM;
        }
    }

    public String getServiceName(){
        return serviceName;
    }

    public enum Status {SUCCESS, FAILURE, PROBLEM}

    private Map <String, String> successTests;
    private Map <String, String> failureTests;
    private Map <String, String> failureDetails;
    private String serviceName;
    private ArrayList<Timestamp> testTimestamps;
    private DateFormat formatTestDate;
}
