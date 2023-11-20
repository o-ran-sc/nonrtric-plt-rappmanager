#!/bin/bash
#
# ============LICENSE_START======================================================================
# Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
# ===============================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END========================================================================
#

arg=${1:-"all"}

if [[ "$arg" == "all" || "$arg" == "acm" ]]; then
  echo "++++++++++++++++++++++++++  ACM  ++++++++++++++++++++++++++"

  echo -e "\n**********  A1PMS Participant  **********"
  A1_PMS_HOST=http://$(kubectl get service policymanagementservice -n nonrtric -o jsonpath='{.spec.clusterIP}'):9080
  curl -sS --location "$A1_PMS_HOST/a1-policy/v2/services" --header 'Accept: application/json' | jq

  echo -e "\n\n**********  Kserve Participant  **********"
  kubectl get isvc -A

  echo -e "\n\n**********  Kubernetes Participant  **********"
  kubectl get pods --selector=app=nonrtric-ransliceassurance -n nonrtric
fi

if [[ "$arg" == "all" || "$arg" == "dme" ]]; then
  echo -e "\n\n**********  DME Participant  **********"

  ICS_HOST=http://$(kubectl get service informationservice -n nonrtric -o jsonpath='{.spec.clusterIP}'):9082

  echo -e "\n\n**********  DME Info Types  **********"
  curl -sS --location "$ICS_HOST/data-producer/v1/info-types" --header 'Accept: application/json' | jq

  echo -e "\n\n**********  DME Data Producers  **********"
  curl -sS --location "$ICS_HOST/data-producer/v1/info-producers" --header 'Accept: application/json' | jq

  echo -e "\n\n**********  DME Data Consumers  **********"
  curl -sS --location "$ICS_HOST/data-consumer/v1/info-jobs" --header 'Accept: application/json' | jq
fi

if [[ "$arg" == "all" || "$arg" == "sme" ]]; then
  echo -e "\n\n++++++++++++++++++++++++++  SME  ++++++++++++++++++++++++++"

  CAPIF_HOST=http://$(kubectl get service capifcore -n nonrtric -o jsonpath='{.spec.clusterIP}'):8090

  curl -sS --location "$CAPIF_HOST/service-apis/v1/allServiceAPIs?api-invoker-id=api_invoker_id_Invoker_App_1" --header 'Accept: application/json' | jq

fi

echo -e "\n\n++++++++++++++++++++++++++  Completed  ++++++++++++++++++++++++++"

