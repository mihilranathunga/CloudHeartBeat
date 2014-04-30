<%@ page import="org.wso2.cloud.heartbeat.monitoring.ui.ServicesHistoryRetriever" %>
<%@ page import="org.wso2.cloud.heartbeat.monitoring.ui.ServicesHistoryRetriever.DailyState" %>
<%@ page import="java.sql.Date" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="javax.servlet.ServletContext"%>
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

<h2>Status History</h2>
<p class="info-section">
    The table below provides a summary of the availability of all cloud services for the past 35 days. Click on the arrow buttons at the top of the table to move forward and backwards through the calendar.
</p>
<div class="history-table">
<%
    DateFormat dateFormat = new SimpleDateFormat("MMM dd");
    DateFormat formatTimeStamp = new SimpleDateFormat("yyyy.MM.dd h:mm a");
    String configPath = getServletConfig().getServletContext().getRealPath("/WEB-INF/heartbeat.conf");
    ServicesHistoryRetriever servicesHistoryRetriever = new ServicesHistoryRetriever(configPath);
    Map <String, Map<Date, DailyState>> servicesHistory = servicesHistoryRetriever.getServicesHistory();
    for(String s : servicesHistory.keySet()){
        System.out.println(s);
    }
%>
<table class="history-main">
    <tr>
        <td class="arrow-left-cell">
            <a class="slide-left" data-value="1" style="visibility:hidden"><<</a>
        </td>
        <td>
            <div class="status-table-wrapper">
                <div class="status-table-wrapper-content">
                    <%  Calendar start = Calendar.getInstance();
                        Calendar end = (Calendar) start.clone();
                        start.add(Calendar.DATE, -1);
                        end.add(Calendar.DATE, -36);
                        int counter = 0;
                        for (; !start.before(end) && !start.getTime().equals(end.getTime()) ; start.add(Calendar.DATE, -1)) {
                            if(counter%7==0){
                                %>
                                <table class="status-table-data">
                                <tr>
                                <%
                            }
                            %>
                            <td><%=dateFormat.format(start.getTime())%></td>
                            <%
                            if(counter%7==6){
                            %>
                            </tr>
                            </table>
                            <%
                            }
                            counter++;
                        }
                    %>
                </div>
            </div>
        </td>
        <td class="arrow-right-cell">
            <a class="slide-right" data-value="1">>></a>
        </td>
    </tr>
    <tr>
        <td>
            <%
                for (String service: servicesHistoryRetriever.getServices()){
                    if(service.equals("Api Manager")){
                        service = "API Manager";
                    }
                    if(service.equals("Ues Server")){
                        service = "UES";
                    }
                    if(service.equals("S 2 Gitblit")){
                        service = "S2 Gitblit";
                    }
                    if(service.equals("Api Store")){
                        service = "API Cloud Store";
                    }
                    if(service.equals("Api Gateway")){
                        service = "API Cloud Gateway";
                    }
                    if(service.equals("Api Publisher")){
                        service = "API Cloud Publisher";
                    }
                    if(service.equals("Api Key Manager")){
                        service = "API Cloud Key Manager";
                    }
                    %>
                    <div class="title-cell"><%=service%></div>
                    <%
                }
            %>
        </td>
        <td>
            <div class="status-table-wrapper">
                <div class="status-table-wrapper-content">
                    <%
                        for(int i=0;i<5;i++){
                    %>
                    <table class="status-table-state" id="slide_<%=(i+1)%>">
                        <%
                            for (String service: servicesHistoryRetriever.getServices()){
                                %>
                                <tr>
                                <%
                                int dayOfWeek = 7;
                                while (dayOfWeek>0){
                                    Entry statusEntry = ((TreeMap) (servicesHistory.get(service))).pollLastEntry();

                                    if(statusEntry!=null){
                                        switch ((DailyState)statusEntry.getValue()){

                                            case NORMAL:
                                                %>
                                                <td><img src="images/live.png" alt="live"></td>
                                                <%
                                            break;

                                            case DISRUPTION:
                                                %>
                                                <td><img src="images/live-with-error.png" alt="live-with-error"></td>
                                                <%
                                            break;

                                            case DOWN:
                                                %>
                                                <td><img src="images/failure.png" alt="failure"></td>
                                                <%
                                            break;

                                            case NA:
                                                %>
                                                <td><img src="images/no-data.png" alt="no-data"></td>
                                                <%
                                            break;
                                        }
                                    } else {
                                        %>
                                        <td><img src="images/no-data.png" alt="no-data"></td>
                                        <%
                                    }
                                    dayOfWeek--;
                                }
                                %>
                                </tr>
                                <%
                            }
                            %>
                            </table>
                            <%
                        }
                        %>
                </div>
            </div>
        </td>
        <td class="right-empty-cell">
        </td>
    </tr>
</table>
<div class="legend">
    <table cellspacing="0">
        <tr>
            <td><img src="images/live.png"  alt="live"/></td>
            <td>Service operating normally</td>
            <td><img src="images/failure.png"  alt="failure"/></td>
            <td>Service Down</td>
            <td><img src="images/live-with-error.png"  alt="live-with-error"/></td>
            <td>Service Disruption</td>
        </tr>
    </table>
</div>
</div>
<script type="text/javascript">
    registerElementsHistory();
</script>