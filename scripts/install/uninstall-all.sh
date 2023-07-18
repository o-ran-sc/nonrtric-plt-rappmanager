#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

echo "Uninstalling ACM Components..."
helm uninstall csit-policy -n default

echo "Uninstalling NONRTRIC Components..."
sudo dep/bin/undeploy-nonrtric

echo "Uninstalling Kserve Components..."
kubectl delete -f https://github.com/kserve/kserve/releases/download/v0.10.0/kserve.yaml
kubectl delete -f https://github.com/kserve/kserve/releases/download/v0.10.0/kserve-runtimes.yaml
kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.0/cert-manager.yaml
kubectl delete ns cert-manager
helm uninstall istiod -n istio-system
helm uninstall istio-base -n istio-system
kubectl delete ns istio-system

rm -rf dep/
rm -rf docker/

sudo rm -fr /dockerdata-nfs/* /tmp/dockerdata-nfs
