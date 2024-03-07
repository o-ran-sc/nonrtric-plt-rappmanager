// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2023: Nordix Foundation
//   Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
//   %%
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   ========================LICENSE_END===================================
//

package main

import (
	"encoding/json"
	"fmt"
	"github.com/getkin/kin-openapi/openapi3"
	log "github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
	"io/ioutil"
	"os"

	"oransc.org/nonrtric/capifcore/internal/common"
	"oransc.org/nonrtric/capifcore/internal/common29122"
	"oransc.org/nonrtric/capifcore/internal/common29571"
	"oransc.org/nonrtric/capifcore/internal/invokermanagementapi"
	"oransc.org/nonrtric/capifcore/internal/providermanagementapi"
	"oransc.org/nonrtric/capifcore/internal/publishserviceapi"
)

type fn func() (swagger *openapi3.T, err error)

var smeOpenApiFileLocation string = "../../openapi/sme/"

func main() {

	var err error
	fmt.Println("Generating SME openapi spec...")
	os.MkdirAll(smeOpenApiFileLocation, 0755)
	if err == nil {
		generateAndCopySwagger("TS29222_CAPIF_API_Provider_Management_API.yaml", providermanagementapi.GetSwagger)
		generateAndCopySwagger("TS29222_CAPIF_Publish_Service_API.yaml", publishserviceapi.GetSwagger)
		generateAndCopySwagger("TS29222_CAPIF_API_Invoker_Management_API.yaml", invokermanagementapi.GetSwagger)
		generateAndCopySwagger("CommonData.yaml", common.GetSwagger)
		generateAndCopySwagger("TS29122_CommonData.yaml", common29122.GetSwagger)
		generateAndCopySwagger("TS29571_CommonData.yaml", common29571.GetSwagger)
	}
}

func generateSwaggerYaml(swagger *openapi3.T, filename string) {
	jsondataarr, jsondataarrerr := json.Marshal(&swagger)
	if jsondataarrerr != nil {
		log.Fatalf("Error loading json data from swagger \n: %s", jsondataarrerr)
	}

	var data map[string]interface{}
	if err := json.Unmarshal(jsondataarr, &data); err != nil {
		log.Fatalf("Error loading json data to map \n: %s", jsondataarrerr)
		log.Fatal(err)
	}

	yamldataarr, yamldataarrerr := yaml.Marshal(&data)
	if yamldataarrerr != nil {
		log.Fatalf("Error loading json data map to array \n: %s", yamldataarrerr)
	}

	err2 := ioutil.WriteFile(filename, yamldataarr, 0755)
	if err2 != nil {
		log.Fatalf("Error writing provider yaml \n: %s", err2)
	}
}

func copy(srcFile string, targetFile string) error {
	data, err := os.ReadFile(srcFile)
	if err != nil {
		return err
	}
	err = os.WriteFile(targetFile, data, 0644)
	if err != nil {
		return err
	}
	return nil
}

func generateAndCopySwagger(openApiFileName string, getSwagger fn) {
	fmt.Printf("Generating %s...\n", openApiFileName)
	swagger, err := getSwagger()
	if err == nil {
		generateSwaggerYaml(swagger, openApiFileName)
		fmt.Printf("Copying %s to %s \n", openApiFileName, smeOpenApiFileLocation + openApiFileName)
		copy(openApiFileName, smeOpenApiFileLocation +openApiFileName)
	}
}
