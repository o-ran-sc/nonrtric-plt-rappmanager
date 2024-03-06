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

SAMPLE_RAPP_GENERATOR="sample-rapp-generator"
RAPP_GENERATOR_CMD="./generate.sh"
RAPP_BASE_PACKAGE="rapp-all"
TEST_RESOURCES="/src/test/resources/"
ACM_TEST_RESOURCES="../rapp-manager-acm$TEST_RESOURCES"
APPLICATION_TEST_RESOURCES="../rapp-manager-application$TEST_RESOURCES"
MODELS_TEST_RESOURCES="../rapp-manager-models$TEST_RESOURCES"
DME_TEST_RESOURCES="../rapp-manager-dme$TEST_RESOURCES"
SME_TEST_RESOURCES="../rapp-manager-sme$TEST_RESOURCES"
CHART_MUSEUM_GET_URI="http://localhost:8879/charts"
CHART_MUSEUM_POST_URI="http://localhost:8879/charts/api/charts"

function copy_package() {
  target_locations=("${@:2}")
  for target_location in "${target_locations[@]}"; do
    echo "Copying $1 to $target_location.."
    cp -f "$1" "$target_location"
  done
}

function generate_package() {
  target_locations=("${@:2}")
  package_name="$1.csar"
  $RAPP_GENERATOR_CMD "$1"
  copy_package "$package_name" "${target_locations[@]}"
  rm -r "$1" "$package_name"
}

function update_chart_museum_uri() {
  find "$1" -type f -exec sed -i "s|UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI|$CHART_MUSEUM_POST_URI|g" {} \;
  find "$1" -type f -exec sed -i "s|UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI|$CHART_MUSEUM_GET_URI|g" {} \;
}

sudo apt-get -y install zip jq
sudo snap install yq


cd $SAMPLE_RAPP_GENERATOR

echo "Generating valid rApp package..."
VALID_RAPP_PACKAGE_FOLDER_NAME="valid-rapp-package"
cp -r $RAPP_BASE_PACKAGE $VALID_RAPP_PACKAGE_FOLDER_NAME
update_chart_museum_uri "$VALID_RAPP_PACKAGE_FOLDER_NAME"
generate_package $VALID_RAPP_PACKAGE_FOLDER_NAME $ACM_TEST_RESOURCES $DME_TEST_RESOURCES $SME_TEST_RESOURCES $MODELS_TEST_RESOURCES $APPLICATION_TEST_RESOURCES

echo "Generating valid rApp package without artifacts..."
VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME="valid-rapp-package-no-artifacts"
cp -r $RAPP_BASE_PACKAGE $VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME
update_chart_museum_uri "$VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME"
yq eval 'del(.topology_template.node_templates.applicationServiceDescriptor.artifacts)' -i $VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME/Definitions/asd.yaml
generate_package $VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME $APPLICATION_TEST_RESOURCES

echo "Generating invalid rApp package..."
INVALID_RAPP_PACKAGE_FOLDER_NAME="invalid-rapp-package"
cp -r $RAPP_BASE_PACKAGE $INVALID_RAPP_PACKAGE_FOLDER_NAME
rm -r $INVALID_RAPP_PACKAGE_FOLDER_NAME/Files $INVALID_RAPP_PACKAGE_FOLDER_NAME/Artifacts
generate_package $INVALID_RAPP_PACKAGE_FOLDER_NAME $MODELS_TEST_RESOURCES $APPLICATION_TEST_RESOURCES

echo "Generating invalid rApp package without tosca..."
INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME="invalid-rapp-package-no-tosca"
cp -r $RAPP_BASE_PACKAGE $INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME
rm -r $INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME/TOSCA-Metadata/TOSCA.meta
generate_package $INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME $MODELS_TEST_RESOURCES

echo "Generating invalid rApp package without asd yaml..."
INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME="invalid-rapp-package-no-asd-yaml"
cp -r $RAPP_BASE_PACKAGE $INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME
rm -r $INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME/Definitions/asd.yaml
generate_package $INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME $MODELS_TEST_RESOURCES

echo "Generating invalid rApp package without ACM composition..."
INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME="invalid-rapp-package-no-acm-composition"
cp -r $RAPP_BASE_PACKAGE $INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME
rm -r $INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME/Files/Acm/definition/compositions.json
generate_package $INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME $MODELS_TEST_RESOURCES

echo "Generating invalid rApp package without Artifacts..."
INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME="invalid-rapp-package-missing-artifact"
cp -r $RAPP_BASE_PACKAGE $INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME
rm -r $INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME/Artifacts/Deployment/HELM/or*
generate_package $INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME $MODELS_TEST_RESOURCES

echo "Generating invalid rApp package with empty asd yaml..."
INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME="invalid-rapp-package-empty-asd-yaml"
cp -r $RAPP_BASE_PACKAGE $INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME
truncate -s 0 $INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME/Definitions/asd.yaml
generate_package $INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME $MODELS_TEST_RESOURCES

echo "Generating valid rApp package with new dme info type..."
VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME="valid-rapp-package-new-info-type"
DME_PRODUCER_FILE="$VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME/Files/Dme/infoproducers/json-file-data-producer.json"
cp -r $RAPP_BASE_PACKAGE $VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME
jq '.supported_info_types += ["new-info-type-not-available"]' $DME_PRODUCER_FILE > tmpdataproducer.json
mv tmpdataproducer.json $DME_PRODUCER_FILE
generate_package $VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME $DME_TEST_RESOURCES

