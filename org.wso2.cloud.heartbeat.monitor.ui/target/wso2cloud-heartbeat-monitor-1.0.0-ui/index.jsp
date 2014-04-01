<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
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
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%--<%--%>
    <%--boolean loggedIn = false;--%>
    <%--String user = "";--%>
    <%--if(session.getAttribute( "username")!=null){--%>
        <%--loggedIn = true;--%>
        <%--user = (String)session.getAttribute( "username");--%>
    <%--}--%>
<%--%>--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="content-type" content="text/html;charset=utf-8" />
    <meta name="generator" content="Adobe GoLive" />
    <title>WSO2 Cloud Service Health Dashboard</title>
    <link rel="stylesheet" href="css/redmond/jquery-ui-1.9.2.custom.min.css" />
    <link type="text/css" href="css/style.css" rel="stylesheet"  media="all" />
    <script type="text/javascript" src="js/jquery-1.8.3.min.js"></script>
    <script src="js/jquery-ui-1.9.2.custom.min.js"></script>
    <script type="text/javascript" src="js/gimiks.js"></script>
    <script type="text/javascript">
        var onFullScreen =false;
        $(document).ready(function() {
           $.get('service_status.jsp', function(responseText) {
                $('#status').html(responseText);

            });
            $.ajaxSetup({ cache: false });
            setInterval(function() {
                $.get('service_status.jsp', function(responseText) {
                    $('#status').html(responseText);

                });
            }, 15000);
        });
        $(document).ready(function() {
            $.get('service_history.jsp', function(responseText) {
                $('#history').html(responseText);

            });
            $.ajaxSetup({ cache: false });
            setInterval(function() {
                $.get('service_history.jsp', function(responseText) {
                    $('#history').html(responseText);

                });
            }, 600000);
        });
    </script>
</head>

<body>
<div class="middle">
    <div class="header">
        <ul class="header-links">
            <li class="wso2">
                <a href="http://wso2.com" target="_blank"></a>
            </li>
            <li class="contact">
                <a href="http://wso2.com/contact/" target="_blank">Contact</a>
            </li>
            <li class="support">
                <a href="http://wso2.com/support/" target="_blank">Support</a>
            </li>
        </ul>

    </div>
</div>
<div class="title">
    <table class="title_table">
        <tr>
            <td class="title-image"><a target="_blank" href="https://cloudpreview.wso2.com/"><img src="images/logo.png" /></a></td>
            <td class="title_text">Service Health Dashboard</td>
        </tr>
    </table>
</div>
<div class="full-screen" id="full-screen"></div>
<div id="main-content">

    <div id="content">

        <div id="status" class="status_div"></div>
        <div id="history" class="history_div"></div>
    </div>
    <div class="footer">
        &copy;2013WSO2
    </div>
</div>
</body>

</html>
