#!/bin/sh
# ----------------------------------------------------------------------------
#  Copyright 2005-2013 WSO2, Inc. http://www.wso2.org
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# ----------------------------------------------------------------------------
# init script for WSO2 Cloud Heartbeat Monitor


# Check the application status
#
check_status() {
  # Running ps with some arguments to check if the PID exists

  s="$(ps -ef | grep 'java -jar wso2cloud-heartbeat-monitor-1.0.0.jar'| grep -v "grep" | awk '{print$2}')"

  # If something was returned by the ps command, this function returns the PID
  if [ $s ] ; then
    return $s
  fi

  # In any another case, return 0
  return 0
}

# Starts the application
start() {

  # At first checks if the application is already started calling the check_status function
  check_status

  pid=$?

  if [ $pid -ne 0 ] ; then
    echo "WSO2 Cloud - Heartbeat Monitor: Already started"
    exit 1
  fi

  # If the application isn't running, starts it
  echo -n "WSO2 Cloud - Heartbeat Monitor: "

  java -jar wso2cloud-heartbeat-monitor-1.0.0.jar &
  echo "Started"
}

# Stops the application
stop() {

  # Checks the application status
  check_status

  pid=$?

  if [ $pid -eq 0 ] ; then
    echo "WSO2 Cloud - Heartbeat Monitor: Already stopped"
    exit 1
  fi

  # Kills the application process
  echo -n "WSO2 Cloud - Heartbeat Monitor: "
  kill -9 $pid &
  echo "Stopped"
}

# Show the application status
status() {

  # The check_status function, again...
  check_status

  # If the PID was returned means the application is running
  if [ $? -ne 0 ] ; then
    echo "WSO2 Cloud - Heartbeat Monitor: Running"
  else
    echo "WSO2 Cloud - Heartbeat Monitor: Stopped"
  fi

}

#show application version
version() {
echo "WSO2 Cloud - Heartbeat Monitor v1.0.0"
}

# Main logic, a simple case to call functions
case "$1" in
  start)
    start
    ;;
  stop)
    stop
    ;;
  status)
    status
    ;;
  restart|reload)
    stop
    start
    ;;
  version)
    version
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|reload|status|version}"
    exit 1
esac

exit 0