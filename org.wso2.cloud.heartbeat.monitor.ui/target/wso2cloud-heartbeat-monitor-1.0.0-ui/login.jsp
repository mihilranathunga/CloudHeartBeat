<%@ page import="org.wso2.cloud.heartbeat.monitoring.ui.Authentication" %>
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

<%
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    out.println("Checking login<br>");
    if (username == null || password == null) {

        out.print("Invalid paramters ");
    }

    // Here you put the check on the username and password
    String configPath = getServletConfig().getServletContext().getRealPath("/WEB-INF/heartbeat.conf");
    Authentication authentication = new Authentication(configPath);
    if(authentication.checkLogin(username.trim(), password.trim())){
        out.println("Welcome " + username + " <a href=\"index.jsp\">Back to main</a>");
        session.setAttribute("username", username);
        response.sendRedirect("index.jsp");
    }

    else
    {
        session.setAttribute("error","Invalid username and password");
        response.sendRedirect("index.jsp");
    }
%>
