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

echo "######### Installing NONRTRIC components #########"

ENABLED_SERVICES=(installPms installA1controller installA1simulator installInformationservice installrAppmanager installDmeParticipant installCapifcore installServicemanager installKong)
DISABLED_SERVICES=(installControlpanel installRappcatalogueservice installRappcatalogueenhancedservice installNonrtricgateway installDmaapadapterservice installDmaapmediatorservice installHelmmanager installOrufhrecovery installRansliceassurance installRanpm)

if [[ "${ENABLED_SERVICES[@]}" =~ "installRanpm" ]]; then
    git clone --recursive "https://gerrit.o-ran-sc.org/r/it/dep"
else
    git clone "https://gerrit.o-ran-sc.org/r/it/dep"
fi

RECEIPE_FILE="dep/nonrtric/RECIPE_EXAMPLE/example_recipe.yaml"

for element in "${ENABLED_SERVICES[@]}"; do
  echo "Enabling service $element"
  yq eval ".nonrtric.$element"="true" -i $RECEIPE_FILE
done

for element in "${DISABLED_SERVICES[@]}"; do
  echo "Disabling service $element"
  yq eval ".nonrtric.$element"="false" -i $RECEIPE_FILE
done

# Dev mode installation configuration
if [[ "$1" == "dev" ]]; then
  SNAPSHOT_REPO='nexus3.o-ran-sc.org:10003/o-ran-sc'
  RAPP_MANAGER_VERSION=$(grep -oPm2 "(?<=<version>)[^<]+" "../../pom.xml" | tail -n1)
  DME_PARTICIPANT_VERSION=$(grep -oPm2 "(?<=<version>)[^<]+" "../../participants/pom.xml" | tail -n1)
  echo "Rapp Manager Version: $RAPP_MANAGER_VERSION"
  echo "DME Participant Version: $DME_PARTICIPANT_VERSION"
  yq eval ".rappmanager.rappmanager.image.registry"=\"$SNAPSHOT_REPO\" -i $RECEIPE_FILE
  yq eval ".rappmanager.rappmanager.image.tag"=\"$RAPP_MANAGER_VERSION\" -i $RECEIPE_FILE
  yq eval ".rappmanager.rappmanager.imagePullPolicy=\"Always\"" -i $RECEIPE_FILE
  yq eval ".dmeparticipant.dmeparticipant.image.registry"=\"$SNAPSHOT_REPO\" -i $RECEIPE_FILE
  yq eval ".dmeparticipant.dmeparticipant.image.tag"=\"$DME_PARTICIPANT_VERSION\" -i $RECEIPE_FILE
  yq eval ".dmeparticipant.dmeparticipant.imagePullPolicy=\"Always\"" -i $RECEIPE_FILE
fi

# There is no real way to disable the PV through the recipe file because kongstorage is deployed standalone.
# The below command will not work
#yq eval ".kongstorage.kongpv.enabled"="false" -i $RECEIPE_FILE
# So, we have to make the change to disable PV in the values.yaml file of kongstorage directly after pulling it
# from the git repo. This can be set to false if you want to use the default storage class.
yq eval ".kongpv.enabled"="true" -i dep/nonrtric/helm/kongstorage/values.yaml

sudo dep/bin/deploy-nonrtric -f $RECEIPE_FILE

while [[ $TIME -lt 2000 ]]; do
  NONRTRIC_PODS=$(kubectl get pods -n nonrtric --field-selector=status.phase!=Running,status.phase!=Succeeded --no-headers)
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
