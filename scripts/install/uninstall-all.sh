#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

echo "######### Uninstalling Rapp Manager #########"

ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
echo "Uninstalling ACM Components..."
helm uninstall csit-policy -n default

echo "Removing Kafka components..."
kubectl delete deploy zookeeper-deployment kafka-deployment
kubectl delete svc zookeeper-service kafka

echo "Uninstalling NONRTRIC Components..."
sudo dep/bin/undeploy-nonrtric

echo "Uninstalling Kserve Components..."
kubectl delete -f https://github.com/kserve/kserve/releases/download/v0.11.2/kserve.yaml
kubectl delete -f https://github.com/kserve/kserve/releases/download/v0.11.2/kserve-runtimes.yaml
kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.0/cert-manager.yaml
kubectl delete ns cert-manager
helm uninstall istiod -n istio-system
helm uninstall istio-base -n istio-system
kubectl delete ns istio-system

# Cleanup ChartMuseum
CM_PID_FILE="$ROOT_DIR/CM_PID.txt"
if [ -f $CM_PID_FILE ]; then
  echo "Cleaning up ChartMuseum..."
  PID=$(cat "$CM_PID_FILE")
  echo "Killing ChartMuseum with PID $PID"
  kill $PID
  rm $CM_PID_FILE
  echo "ChartMuseum cleanup completed"
fi

rm -rf "$ROOT_DIR/chartstorage"
rm -rf dep/
rm -rf docker/

sudo rm -fr /dockerdata-nfs/* /tmp/dockerdata-nfs
