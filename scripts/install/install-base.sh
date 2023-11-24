#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
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

echo "Installing chartmuseum..."
curl https://raw.githubusercontent.com/helm/chartmuseum/main/scripts/get-chartmuseum | bash
CHART_MUSEUM_VERSION=$(helm version --short)
echo "Chartmuseum version $CHART_MUSEUM_VERSION is installed."

echo "Install yq..."
snap install yq

echo "Creating kubernetes namespace..."
kubectl create ns kserve-test

