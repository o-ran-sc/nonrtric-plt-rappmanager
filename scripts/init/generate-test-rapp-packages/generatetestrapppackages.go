// -
//
//	========================LICENSE_START=================================
//	O-RAN-SC
//	%%
//	Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
//	%%
//	Licensed under the Apache License, Version 2.0 (the "License");
//	you may not use this file except in compliance with the License.
//	You may obtain a copy of the License at
//
//	     http://www.apache.org/licenses/LICENSE-2.0
//
//	Unless required by applicable law or agreed to in writing, software
//	distributed under the License is distributed on an "AS IS" BASIS,
//	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//	See the License for the specific language governing permissions and
//	limitations under the License.
//	========================LICENSE_END===================================
package main

import (
	"archive/zip"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"

	cp "github.com/otiai10/copy"
	"gopkg.in/yaml.v2"
)

func replaceStringsInDirectory(directory, stringToFind, stringToReplace string) error {
	files, err := os.ReadDir(directory)
	checkError(err)
	for _, file := range files {
		filePath := filepath.Join(directory, file.Name())
		if file.IsDir() {
			if err := replaceStringsInDirectory(filePath, stringToFind, stringToReplace); err != nil {
				return err
			}
		} else {
			if err := replaceString(filePath, stringToFind, stringToReplace); err != nil {
				return err
			}
		}
	}
	return nil
}

func replaceString(fileLocation, stringToFind, stringToReplace string) error {
	fileContent, err := os.ReadFile(fileLocation)
	checkError(err)
	if !strings.Contains(string(fileContent), stringToFind) {
		return nil
	}
	updatedContent := strings.ReplaceAll(string(fileContent), stringToFind, stringToReplace)
	err = os.WriteFile(fileLocation, []byte(updatedContent), 0644)
	checkError(err)
	return nil
}

func updateChartMuseumUri(directory string) error {
	CHART_MUSEUM_GET_URI := "http://localhost:8879/charts"
	CHART_MUSEUM_POST_URI := "http://localhost:8879/charts/api/charts"
	err := replaceStringsInDirectory(directory, "UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI", CHART_MUSEUM_POST_URI)
	checkError(err)
	err = replaceStringsInDirectory(directory, "UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI", CHART_MUSEUM_GET_URI)
	checkError(err)
	return nil
}

func generateCsarPackage(directory, fileName string) error {
	csarFile, err := os.Create(fileName)
	checkError(err)
	defer csarFile.Close()
	csarWriter := zip.NewWriter(csarFile)
	defer csarWriter.Close()
	err = filepath.Walk(directory, func(filePath string, fileInfo os.FileInfo, err error) error {
		if fileInfo.IsDir() {
			return nil
		}
		relPath, err := filepath.Rel(directory, filePath)
		checkError(err)
		relPath = strings.ReplaceAll(relPath, "\\", "/")
		zipFile, err := csarWriter.Create(relPath)
		checkError(err)
		fsFile, err := os.Open(filePath)
		checkError(err)
		_, err = io.Copy(zipFile, fsFile)
		checkError(err)
		defer fsFile.Close()
		return nil
	})
	checkError(err)
	return nil
}

func createCsarAndCopy(directory string, paths ...string) error {
	packageName := directory + ".csar"
	err := generateCsarPackage(directory, packageName)
	checkError(err)
	for _, path := range paths {
		fmt.Printf("Copying %s to %s \n", packageName, path)
		copy(packageName, path+packageName)
	}
	os.Remove(packageName)
	os.RemoveAll(directory)
	return nil
}

func copy(srcFile string, targetFile string) {
	data, err := os.ReadFile(srcFile)
	checkError(err)
	err = os.WriteFile(targetFile, data, 0644)
	checkError(err)
}

func checkError(err error) error {
	if err != nil {
		return err
	}
	return nil
}

func removeYamlElement(filePath string, elements ...string) error {
	fileData, err := os.ReadFile(filePath)
	checkError(err)
	var yamlContent map[interface{}]interface{}
	err = yaml.Unmarshal(fileData, &yamlContent)
	checkError(err)
	index := len(elements) - 1
	dataMap := yamlContent
	for i := 0; i < index; i++ {
		resultMap, ok := dataMap[elements[i]].(map[interface{}]interface{})
		if !ok {
			return nil
		}
		dataMap = resultMap
	}
	delete(dataMap, elements[index])
	updatedYamlContent, err := yaml.Marshal(&yamlContent)
	checkError(err)
	err = os.WriteFile(filePath, updatedYamlContent, 0644)
	checkError(err)
	return nil
}

func addJsonElement(filePath string, element string, index string) error {
	fileData, err := os.ReadFile(filePath)
	checkError(err)
	var jsonContent map[string]interface{}
	err = json.Unmarshal(fileData, &jsonContent)
	checkError(err)
	array, ok := jsonContent[index].([]interface{})
	if !ok {
		return nil
	}
	jsonContent[index] = append(array, element)
	updatedJsonContent, err := json.Marshal(&jsonContent)
	checkError(err)
	err = os.WriteFile(filePath, updatedJsonContent, 0644)
	checkError(err)
	return nil
}

func main() {

	SAMPLE_RAPP_GENERATOR := "sample-rapp-generator"
	RAPP_BASE_PACKAGE := "rapp-all"
	TEST_RESOURCES := "/src/test/resources/"
	ACM_TEST_RESOURCES := "../rapp-manager-acm" + TEST_RESOURCES
	APPLICATION_TEST_RESOURCES := "../rapp-manager-application" + TEST_RESOURCES
	MODELS_TEST_RESOURCES := "../rapp-manager-models" + TEST_RESOURCES
	DME_TEST_RESOURCES := "../rapp-manager-dme" + TEST_RESOURCES
	SME_TEST_RESOURCES := "../rapp-manager-sme" + TEST_RESOURCES

	if err := os.Chdir("../../../" + SAMPLE_RAPP_GENERATOR); err != nil {
		fmt.Println("Error changing working directory:", err)
		return
	}

	fmt.Println("Generating valid rApp package... ")
	VALID_RAPP_PACKAGE_FOLDER_NAME := "valid-rapp-package"
	cp.Copy(RAPP_BASE_PACKAGE, VALID_RAPP_PACKAGE_FOLDER_NAME)
	updateChartMuseumUri(VALID_RAPP_PACKAGE_FOLDER_NAME)
	createCsarAndCopy(VALID_RAPP_PACKAGE_FOLDER_NAME, ACM_TEST_RESOURCES, DME_TEST_RESOURCES, SME_TEST_RESOURCES, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating valid rApp package without artifacts...")
	VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME := "valid-rapp-package-no-artifacts"
	cp.Copy(RAPP_BASE_PACKAGE, VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME)
	updateChartMuseumUri(VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME)
	removeYamlElement(VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME+"/Definitions/asd.yaml", "topology_template", "node_templates", "applicationServiceDescriptor", "artifacts")
	createCsarAndCopy(VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating invalid rApp package...")
	INVALID_RAPP_PACKAGE_FOLDER_NAME := "invalid-rapp-package"
	cp.Copy(RAPP_BASE_PACKAGE, INVALID_RAPP_PACKAGE_FOLDER_NAME)
	os.RemoveAll(INVALID_RAPP_PACKAGE_FOLDER_NAME + "/Files")
	os.RemoveAll(INVALID_RAPP_PACKAGE_FOLDER_NAME + "/Artifacts")
	createCsarAndCopy(INVALID_RAPP_PACKAGE_FOLDER_NAME, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating invalid rApp package without tosca...")
	INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME := "invalid-rapp-package-no-tosca"
	cp.Copy(RAPP_BASE_PACKAGE, INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME)
	os.Remove(INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME + "/TOSCA-Metadata/TOSCA.meta")
	createCsarAndCopy(INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating invalid rApp package without asd yaml...")
	INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME := "invalid-rapp-package-no-asd-yaml"
	cp.Copy(RAPP_BASE_PACKAGE, INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME)
	os.Remove(INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME + "/Definitions/asd.yaml")
	createCsarAndCopy(INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating invalid rApp package without ACM composition...")
	INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME := "invalid-rapp-package-no-acm-composition"
	cp.Copy(RAPP_BASE_PACKAGE, INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME)
	os.Remove(INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME + "/Files/Acm/definition/compositions.json")
	createCsarAndCopy(INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating invalid rApp package without Artifacts...")
	INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME := "invalid-rapp-package-missing-artifact"
	cp.Copy(RAPP_BASE_PACKAGE, INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME)
	os.Remove(INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME + "/Artifacts/Deployment/HELM/hello-world-chart-0.1.0.tgz")
	createCsarAndCopy(INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating invalid rApp package with empty asd yaml...")
	INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME := "invalid-rapp-package-empty-asd-yaml"
	cp.Copy(RAPP_BASE_PACKAGE, INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME)
	os.Truncate(INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME+"/Definitions/asd.yaml", 0)
	createCsarAndCopy(INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME, MODELS_TEST_RESOURCES, APPLICATION_TEST_RESOURCES)

	fmt.Println("Generating valid rApp package with new dme info type...")
	VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME := "valid-rapp-package-new-info-type"
	DME_PRODUCER_FILE := VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME + "/Files/Dme/infoproducers/json-file-data-producer.json"
	cp.Copy(RAPP_BASE_PACKAGE, VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME)
	addJsonElement(DME_PRODUCER_FILE, "new-info-type-not-available", "supported_info_types")
	createCsarAndCopy(VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME, DME_TEST_RESOURCES)

}
