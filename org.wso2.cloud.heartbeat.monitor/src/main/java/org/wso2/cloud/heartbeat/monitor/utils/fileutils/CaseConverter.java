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

package org.wso2.cloud.heartbeat.monitor.utils.fileutils;

/**
 * Case conversions implemented in this class
 */
public class CaseConverter {

    /**
     * Splits String into human readable string 
     * (i.e TenantLogin ---> Tenant Login) 
     * @param s String to be converted
     * @return Converted String
     */
    public static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                              "(?<=[A-Z])(?=[A-Z][a-z])",
                              "(?<=[^A-Z])(?=[A-Z])",
                              "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    /**
     * Converts underscore case into title case
     * (i.e tenant_login ---> TenantLogin)
     * @param underscoreCaseString Underscore case string
     * @return Title case string
     */
    public static String underscoreToTitleCase(String underscoreCaseString){
        String[] parts = underscoreCaseString.split("_");
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : parts){
            camelCaseString.append(toProperCase(part));
        }
        return camelCaseString.toString();
    }

    /**
     * Converts underscore case into camel case
     * (i.e tenant_login ---> tenantLogin)
     * @param underscoreCaseString Underscore case string
     * @return Camel case string
     */
    public static String underscoreToCamelCase(String underscoreCaseString){
        String[] parts = underscoreCaseString.split("_");
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : parts){
            camelCaseString.append(toProperCase(part));
        }
        return camelCaseString.substring(0,1).toLowerCase() + camelCaseString.substring(1);
    }

    /**
     * Converts string to proper case
     * (i.e tenant ---> Tenant)
     * @param s String to be converted
     * @return Proper case string
     */
    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
               s.substring(1).toLowerCase();
    }
}
