#!/bin/bash
#  ============LICENSE_START===============================================
#  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

echo "######### Patching Sample rApps #########"

# Default values
IP_ADDRESS=$(hostname -I | sort -n -t . -k 1,1 -k 2,2 -k 3,3 -k 4,4 | awk '{print $1}')
PORT=8879

# Parse optional arguments
while getopts "i:p:r:" opt; do
  case $opt in
    i) IP_ADDRESS="$OPTARG" ;;
    p) PORT="$OPTARG" ;;
    r) RAPP="$OPTARG" ;;
    *) echo "Usage: $0 [-i IP_ADDRESS] [-p PORT] [-r RAPP]" >&2; exit 1 ;;
  esac
done

echo "IP Address : $IP_ADDRESS"
echo "Port : $PORT"
echo "Rapp Directory: $RAPP"


echo "Replacing hardcoded variables inside sample-rapp-generator:"
echo "UPDATE_THIS_MACHINE_IP"
echo "UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI"
echo "UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI"
echo "UPDATE_THIS_ADDRESS"

CWD=$(pwd)
export WORKSPACE="$CWD/../../sample-rapp-generator"

CHARTREPO_IP_PORT=${CHART_REPO_GET_URI:-'http://'$IP_ADDRESS':'$PORT''}
echo "CHARTREPO_IP_PORT: $CHARTREPO_IP_PORT"
CHART_REPO_GET_URI=${CHART_REPO_GET_URI:-'http://'$IP_ADDRESS':'$PORT'/charts'}
CHART_REPO_POST_URI=${CHART_REPO_POST_URI:-'http://'$IP_ADDRESS':'$PORT'/api/charts'}

echo "Replacing charts repo post url in yaml files....."
echo "Chart repository post URI : $CHART_REPO_POST_URI"
for file in $(find $WORKSPACE/$RAPP -type f -name "*.yaml"); do
  sed -i "s|UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI|${CHART_REPO_POST_URI}|g" "$file"
  if grep -q "$CHART_REPO_POST_URI" "$file"; then
    echo "$file is updated."
  fi
done

echo "Replacing charts repo get url and machine ip in json files....."
echo "Chart repository get URI : $CHART_REPO_GET_URI"
for file in $(find $WORKSPACE/$RAPP -type f \( -name "*.yaml" -o -name "*.json" \)); do
  sed -i "s|UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI|${CHART_REPO_GET_URI}|g" "$file"
  if grep -q "$CHART_REPO_GET_URI" "$file"; then
    echo "$file is updated."
  fi

  sed -i "s/UPDATE_THIS_MACHINE_IP/$IP_ADDRESS/g" "$file"
  if grep -q "$IP_ADDRESS" "$file"; then
    echo "UPDATE_THIS_MACHINE_IP updated in file: $file"
  fi

  sed -i "s|UPDATE_THIS_ADDRESS|${CHARTREPO_IP_PORT}|g" "$file"
  if grep -q "$CHARTREPO_IP_PORT" "$file"; then
    echo "UPDATE_THIS_ADDRESS updated in file: $file"
  fi

done
echo "UPDATE_THIS_MACHINE_IP=$IP_ADDRESS"
echo "UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI=$CHART_REPO_GET_URI"
echo "UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI=$CHART_REPO_POST_URI"
echo "Patching Sample rApps completed."
