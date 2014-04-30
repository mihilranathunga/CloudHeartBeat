=====================================
WSO2 Cloud Monitor-Heartbeat v1.0.0
=====================================

WSO2 Cloud Heartbeat monitor allows system administrators to monitor their Cloud setup and track down
 partially or fully down states of the setup.


Installation & Running
======================
1. Extract the wso2cloud-heartbeat-monitor-1.0.0-bin.zip and go to the extracted directory
2. Configure heartbeat.conf according to the WSO2 Cloud setup
3. Add mysql-connector-java:5.1.21 to the lib folder
4. Import cloud_heartbeat.sql dbscript available in dbscripts folder into your mysql instance
5. Change log4j.properties file if needed.
6. Run the startup.sh or startup.bat as appropriate
	startup.bat file on Windows operating systems and startup.sh file on Unix systems.
    i) ./startup.sh {start|stop|restart|reload|status|version}
    ii) startup.bat {start|stop|restart|reload|status|version}

        Usage: startup.sh [command]

            command:
               	--start                 Start Cloud Heartbeat Monitor
               	--stop                  Stop Cloud Heartbeat Monitor
               	--restart|reload        Restart Cloud Heartbeat Monitor
               	--status                Status of the Cloud Heartbeat Monitor - started|stopped.
               	--version               What version of the Cloud Heartbeat Monitor are you running


For more details, follow heartbeat.conf instructions and user documentation.

WSO2 Cloud Monitor heartbeat Binary Distribution Directory Structure
======================================================
     HEARTBEAT_HOME
            |-- dbscripts <directory>
            |-- lib <directory>
            |-- logs <directory>
            |-- resources <directory>
            |-- heartbeat.conf <file>
            |-- startup.bat <script>
            |-- startup.sh <script>
            |-- wso2cloud-monitor-heartbeat-1.0.0.jar <jar>
            |-- LICENSE.txt <file>
            |-- README.txt <file>


    - dbscripts
      Contains the database creation SQL scripts for mysql databases.

    - lib
      Contains the basic set of libraries required to operate in standalone mode.

    - logs
      Contains the tests' INFO-ERROR log file created.

    - resources
      The repository where Carbon artifacts & Axis2 services and modules deployed in WSO2 Carbon servers are stored.

    - LICENSE.txt
      Apache License 2.0 under which WSO2 Carbon is distributed.

    - README.txt
      This document.

---------------------------------------------------------------------------
(c)  2013, WSO2 Inc.