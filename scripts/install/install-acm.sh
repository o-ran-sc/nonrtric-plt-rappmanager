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

echo "######### Installing ACM components #########"

ENABLE_COMPONENTS=(policy-models-simulator policy-clamp-runtime-acm policy-clamp-ac-kserve-ppnt policy-clamp-ac-k8s-ppnt policy-clamp-ac-a1pms-ppnt)
DISABLE_COMPONENTS=(policy-api policy-pap policy-apex-pdp policy-pdpd-cl policy-xacml-pdp policy-distribution policy-clamp-ac-pf-ppnt policy-clamp-ac-http-ppnt)

ACM_VALUES_FILE="docker/helm/policy/values.yaml"
A1PMS_CONFIGURATION_FILE="docker/helm/policy/components/policy-clamp-ac-a1pms-ppnt/resources/config/A1pmsParticipantParameters.yaml"
K8S_CONFIGURATION_FILE="docker/helm/policy/components/policy-clamp-ac-k8s-ppnt/values.yaml"
K8S_VERSIONS_FILE="docker/compose/get-k8s-versions.sh"

IP_ADDRESS=$(hostname -I | awk '{print $1}')
echo "IP Address : $IP_ADDRESS"

git clone "https://gerrit.onap.org/r/policy/docker"

CWD=$(pwd)
export WORKSPACE="$CWD/docker"

#Temporary workaround. Should be removed once this gets fixed in policy/docker repo
echo "Update policy-db-migrator version..."
yq eval '.dbmigrator.image="onap/policy-db-migrator:3.0-SNAPSHOT-latest"' -i $ACM_VALUES_FILE
sed -i 's/component=policy-clamp-ac-kserve-ppnt/component=policy-clamp-ac-sim-ppnt/g' $K8S_VERSIONS_FILE
PARAMETER_STRING="metrics:\n  security:\n    disabled: false"
echo -e "$PARAMETER_STRING" >> docker/helm/policy/components/policy-clamp-runtime-acm/resources/config/acRuntimeParameters.yaml

echo "Updating policy docker image versions..."
bash $K8S_VERSIONS_FILE

echo "Enabling the access for the clusterroles..."
kubectl apply -f resources/acm-role-binding.yaml

for element in "${ENABLE_COMPONENTS[@]}"; do
  echo "Enabling component $element"
  yq eval ".$element.enabled"="true" -i $ACM_VALUES_FILE
done

for element in "${DISABLE_COMPONENTS[@]}"; do
  echo "Disabling component $element"
  yq eval ".$element.enabled"="false" -i $ACM_VALUES_FILE
done

echo "Updating A1PMS Participant"
yq eval '.a1pms.baseUrl="http://policymanagementservice.nonrtric:9080"' -i $A1PMS_CONFIGURATION_FILE

echo "Updating the k8s participant repo list"
yq eval '.repoList.helm.repos += {"repoName":"local","address":"http://'$IP_ADDRESS':8879/charts"}' -i $K8S_CONFIGURATION_FILE

echo "Building policy helm charts..."
helm dependency build docker/helm/policy/

echo "Installing policy helm charts..."
helm install csit-policy docker/helm/policy/ -n default

while [[ $TIME -lt 2000 ]]; do
  NONRTRIC_PODS=$(kubectl get pods -n default --field-selector=status.phase!=Running,status.phase!=Succeeded --no-headers)
  if [[ -z "$NONRTRIC_PODS" ]]; then
    echo "All ACM Components are running."
    kubectl get pods -n default
    break
  fi

  echo "Waiting for ACM Components to be running..."
  echo "These pods are not running"
  echo "$NONRTRIC_PODS"
  TIME=$(expr $TIME + 5)
  sleep 5
done

echo "ACM Components Installation Completed."