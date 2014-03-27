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

    String username=(String)session.getAttribute("username");
   if(username!=null)
       {
          out.println(username+" loged out, <a href=\"index.jsp\">Back</a>");
           session.removeAttribute("username");

       }
    else
        {
        out.println("You are already not login <a href=\"index.jsp\">Back</a>");
    }


    response.sendRedirect("index.jsp");
%>
