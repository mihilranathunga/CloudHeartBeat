<%@ page import="org.wso2.cloud.heartbeat.monitoring.ui.ServicesStatusRetriever" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.text.DateFormat" %>
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

<%-- Printing all the notes here --%>
<%
    DateFormat formatTimeStamp = new SimpleDateFormat("yyyy.MM.dd h:mm a");
    String serviceName = request.getParameter("serviceName");
    String configPath = getServletConfig().getServletContext().getRealPath("/WEB-INF/heartbeat.conf");
    ServicesStatusRetriever servicesStatusRetriever = new ServicesStatusRetriever(configPath);
    boolean loggedIn = false;
    String user = "";
    if(session.getAttribute( "username")!=null){
        loggedIn = true;
        user = (String)session.getAttribute( "username");
    }
    String note = request.getParameter("note");
    if (note != null) {
        servicesStatusRetriever.addNotes(serviceName, note);
    }
    Map <Timestamp, String> notes = servicesStatusRetriever.getNotes(serviceName);

    if(!notes.isEmpty()){
        for(Entry dailyNote : notes.entrySet()){
             %>
                <div class="info">
                    <span class="yellow"><%=formatTimeStamp.format(dailyNote.getKey())%> </span>
                     <%=dailyNote.getValue()%>
                </div>
             <%
        }

    } else{
        %>
            <div class="info">
                <span class="yellow">Notes Unavailable </span>
            </div>
        <%
    }
%>
<% if (loggedIn) { %>
<div class="add-note">
    <textarea></textarea>
    <button onclick="addNote(this,'<%=serviceName%>')">Add Note</button>
</div>
<% }
%>