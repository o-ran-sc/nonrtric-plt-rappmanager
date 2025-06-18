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

echo "######### Installing Kserve #########"

KSERVE_CRD_RESPONSE=$(kubectl get crd inferenceservice.serving.kserve.io)
if [ -z "$KSERVE_CRD_RESPONSE" ]; then
  echo "Adding istio helm repository"
  helm repo add istio https://istio-release.storage.googleapis.com/charts
  helm repo update

  echo "Installing Istio..."
  kubectl create namespace istio-system
  helm install istio-base istio/base -n istio-system --set defaultRevision=default
  helm install istiod istio/istiod -n istio-system --wait

  echo "Installing Istio Kserve ingress..."
  kubectl apply -f resources/kserve-istio-ingress.yaml

  echo "Installing Cert Manager v1.12.0 ..."
  kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.12.0/cert-manager.yaml

  while [[ $TIME -lt 20 ]]; do
    CERT_MANAGER_PODS=$(kubectl get pods -n cert-manager --field-selector=status.phase!=Running --no-headers)
    if [[ -z "$CERT_MANAGER_PODS" ]]; then
      echo "Cert manager is running."
      break
    fi

    echo "Waiting for cert manager to be running..."
    echo "These pods are not running"
    echo "$CERT_MANAGER_PODS"
    TIME=$(expr $TIME + 3)
    sleep 3
  done
  echo "Waiting for cert-manager to get initialized..."

  echo "Installing Kserve v0.11.2 ..."
  kubectl apply -f https://github.com/kserve/kserve/releases/download/v0.11.2/kserve.yaml
  kubectl apply -f https://github.com/kserve/kserve/releases/download/v0.11.2/kserve-runtimes.yaml
  echo "Patching Kserve ..."
  kubectl patch configmap/inferenceservice-config -n kserve --type=strategic -p '{"data": {"deploy": "{\"defaultDeploymentMode\": \"RawDeployment\"}"}}'
  echo "Kserve installation completed."
else
  echo "Kserve already installed."
fi
