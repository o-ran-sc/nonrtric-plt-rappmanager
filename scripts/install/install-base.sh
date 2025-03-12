#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

echo "######### Installing base components #########"

echo "Installing helm..."
snap install helm --classic
HELM_VERSION=$(helm version --short)
echo "Helm version $HELM_VERSION installed."

ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
CM_VERSION="v0.16.2"
CM_PORT="8879"
HELM_LOCAL_REPO="$ROOT_DIR/chartstorage"

echo "Installing ChartMuseum binary..."
pushd /tmp
wget https://get.helm.sh/chartmuseum-$CM_VERSION-linux-amd64.tar.gz
tar xvfz chartmuseum-$CM_VERSION-linux-amd64.tar.gz
sudo mv /tmp/linux-amd64/chartmuseum /usr/local/bin/chartmuseum
popd

echo "Starting ChartMuseum on port $CM_PORT..."
nohup chartmuseum --port=$CM_PORT --storage="local" --context-path=/charts --storage-local-rootdir=$HELM_LOCAL_REPO >/dev/null 2>&1 &
echo $! > $ROOT_DIR/CM_PID.txt

echo "Install yq..."
snap install yq

echo "Creating kubernetes namespace..."
kubectl create ns kserve-test

