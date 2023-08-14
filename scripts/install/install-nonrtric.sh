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

echo "######### Installing NONRTRIC components #########"

git clone "https://gerrit.o-ran-sc.org/r/it/dep"

ENABLED_SERVICES=(installPms installA1controller installA1simulator installInformationservice)
DISABLED_SERVICES=(installControlpanel installRappcatalogueservice installRappcatalogueenhancedservice installNonrtricgateway installKong installDmaapadapterservice installDmaapmediatorservice installHelmmanager installOrufhrecovery installRansliceassurance installRanpm)

RECEIPE_FILE="dep/nonrtric/RECIPE_EXAMPLE/example_recipe.yaml"

for element in "${ENABLED_SERVICES[@]}"; do
  echo "Enabling service $element"
  yq eval ".nonrtric.$element"="true" -i $RECEIPE_FILE
done

for element in "${DISABLED_SERVICES[@]}"; do
  echo "Disabling service $element"
  yq eval ".nonrtric.$element"="false" -i $RECEIPE_FILE
done

sudo dep/bin/deploy-nonrtric -f $RECEIPE_FILE

while [[ $TIME -lt 2000 ]]; do
  NONRTRIC_PODS=$(kubectl get pods -n nonrtric --field-selector=status.phase!=Running --no-headers)
  if [[ -z "$NONRTRIC_PODS" ]]; then
    echo "All NONRTRIC Components are running."
    kubectl get pods -n nonrtric
    break
  fi

  echo "Waiting for NONRTRIC Components to be running..."
  echo "These pods are not running"
  echo "$NONRTRIC_PODS"
  TIME=$(expr $TIME + 5)
  sleep 5
done

echo "NONRTRIC component installation completed..."
