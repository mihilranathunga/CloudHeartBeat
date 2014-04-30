<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%--
  ~ Copyright 2005-2012 WSO2, Inc. http://www.wso2.org
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  --%>
<%@ page import="javax.servlet.ServletContext"%>
<%@ page import="org.wso2.cloud.heartbeat.monitoring.ui.ServicesStatusRetriever" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.wso2.cloud.heartbeat.monitoring.ui.ServiceHealth" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.text.DateFormat" %>

<h2>Current Status - <% DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  (Z/z)");
    Date date = new Date();
    String timeZone = dateFormat.format(date);
    out.print(timeZone);%>
</h2>
<p class="info-section">
    The table below shows the availability of WSO2 Cloud services. "Date and Time" column indicates the last checked date and time of a individual service. If you are experiencing a real-time, operational issue with one of our services that is not described below, please contact us via "Support" link.
</p>

<div class="main" id="mainTable">
    <div class="service-status">
<table cellspacing="0" border="0" class="content">
    <tr class="heading">
        <td class="status" ></td>
        <td class="service" >Service</td>
        <td class="date-time" >Date and Time</td>
        <td class="info" colspan="2">Details</td>
    </tr>

    <%
        String configPath = getServletConfig().getServletContext().getRealPath("/WEB-INF/heartbeat.conf");
        ServicesStatusRetriever servicesStatusRetriever = new ServicesStatusRetriever(configPath);
        ArrayList <ServiceHealth> serviceHealths = servicesStatusRetriever.getServiceHealths();
        String serviceName;
        for (ServiceHealth serviceHealth: serviceHealths){
            serviceName = serviceHealth.getServiceName();
            if(serviceName.equals("Api Manager")){
               serviceName = "API Manager";
            }
            if(serviceName.equals("Ues Server")){
                serviceName = "UES";
            }
            if(serviceName.equals("S 2 Gitblit")){
                serviceName = "S2 Gitblit";
            }
            if(serviceName.equals("Api Store")){
                serviceName = "API Cloud Store";
            }
            if(serviceName.equals("Api Gateway")){
                serviceName = "API Cloud Gateway";
            }
            if(serviceName.equals("Api Publisher")){
                serviceName = "API Cloud Publisher";
            }
            if(serviceName.equals("Api Key Manager")){
                serviceName = "API Cloud Key Manager";
            }
    %>
    <tr class="parent-row">
        <td class="status" data-service="<%=serviceHealth.getServiceName()%>">
            <%--<a onclick="showNotes(this)" class="showNotesLink">--%>
            <% ServiceHealth.Status status = serviceHealth.getServiceStatus();
                switch (status){
                    case SUCCESS:
            %>
            <img src="images/live.png" alt="live">
            <% break;
                case FAILURE:
            %>
            <img src="images/failure.png" alt="failure">
            <% break;

                case PROBLEM:
            %>
            <img src="images/live-with-error.png" alt="warning">
            <% break;
            }
            %>
        </td>
        <td class="service"><a class="expandingLink"><%=serviceName%><i class="close"></i></a></td>
        <td class="date-time"><%=serviceHealth.getLastTestDateTime()%></td>
        <td class="info">
            <%
                switch (status){
                    case SUCCESS:
                        out.print("Service operating normally");
                        break;

                    case FAILURE:
                        out.print("Service down");
                        break;

                    case PROBLEM:
                        out.print("Service disruption");
                        break;
                }
            %></td>
    </tr>
    <% if(!serviceHealth.getSuccessTests().isEmpty()){
        for (Map.Entry<String, String> statusEntry : serviceHealth.getSuccessTests().entrySet()) {

    %>
    <tr class="child-row" style="display: none;">
        <td class="status status-sub"><img src="images/live.png" alt="live"></td>
        <td class="service-sub"><%=statusEntry.getKey()%></td>
        <td class="date-time-sub"><%=statusEntry.getValue()%></td>
        <td>Success</td>
    </tr>
    <%
            }
        }
        if(!serviceHealth.getFailureTests().isEmpty()){
            for (Map.Entry<String, String> statusEntry : serviceHealth.getFailureTests().entrySet()) {
    %>
    <tr class="child-row" style="display: none;">
        <td class="status status-sub"><img src="images/failure.png" alt="failure"></td>
        <td class="service-sub"><%=statusEntry.getKey()%></td>
        <td class="date-time-sub"><%=statusEntry.getValue()%></td>
        <td>Failure:&nbsp;&nbsp;<%=serviceHealth.getFailureDetails().get(statusEntry.getKey())%></td>
    </tr>
    <%
        }
    %>
    <%
            }
        }
    %>
</table>
    </div>
    <div class="legend">
        <table cellspacing="0">
            <tr>
                <td><img src="images/live.png"  alt="live"/></td>
                <td>Service operating normally</td>
                <td><img src="images/failure.png" alt="failure"/></td>
                <td>Service Down</td>
                <td><img src="images/live-with-error.png" alt="live-with-error"/></td>
                <td>Service Disruption</td>
            </tr>
        </table>
    </div>
</div>
<script type="text/javascript">
    registerTableEvents();
</script>