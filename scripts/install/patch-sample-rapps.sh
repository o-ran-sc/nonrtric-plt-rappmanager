#!/bin/bash
#  ============LICENSE_START===============================================
#  Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
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

CWD=$(pwd)
export WORKSPACE="$CWD/../../sample-rapp-generator"

IP_ADDRESS=$(hostname -I | sort -n -t . -k 1,1 -k 2,2 -k 3,3 -k 4,4 | awk '{print $1}')
echo "IP Address : $IP_ADDRESS"

CHART_REPO_GET_URI=${CHART_REPO_GET_URI:-'http://'$IP_ADDRESS':8879/charts'}
CHART_REPO_POST_URI=${CHART_REPO_POST_URI:-'http://'$IP_ADDRESS':8879/charts/api/charts'}

echo "Replacing charts repo post url in yaml files....."
echo "Chart repository post URI : $CHART_REPO_POST_URI"
for file in $(find $WORKSPACE -type f -name "*.yaml"); do
  sed -i "s|UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI|${CHART_REPO_POST_URI}|g" "$file"
  if grep -q "$CHART_REPO_POST_URI" "$file"; then
    echo "$file is updated."
  fi
done

echo "Replacing charts repo get url & cluster-ip in json files....."
echo "Chart repository get URI : $CHART_REPO_GET_URI"
for file in $(find $WORKSPACE -type f -name "*.json"); do
  sed -i "s|UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI|${CHART_REPO_GET_URI}|g" "$file"
  if grep -q "$CHART_REPO_GET_URI" "$file"; then
    echo "$file is updated."
  fi

  sed -i "s/UPDATE_THIS_CLUSTER_IP/$IP_ADDRESS/g" "$file"
  if grep -q "$IP_ADDRESS" "$file"; then
    echo "UPDATE_THIS_CLUSTER_IP updated in file: $file "
  fi
done


echo "Patching Sample rApps completed."
