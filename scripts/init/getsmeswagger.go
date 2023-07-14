// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2023: Nordix Foundation
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
	"github.com/getkin/kin-openapi/openapi3"
	log "github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
	"encoding/json"
	 "io/ioutil"

	"oransc.org/nonrtric/capifcore/internal/invokermanagementapi"
	"oransc.org/nonrtric/capifcore/internal/providermanagementapi"
	"oransc.org/nonrtric/capifcore/internal/publishserviceapi"
    "oransc.org/nonrtric/capifcore/internal/common"
    "oransc.org/nonrtric/capifcore/internal/common29122"
    "oransc.org/nonrtric/capifcore/internal/common29571"
)

func main() {
	var swagger *openapi3.T
	var err error

	swagger,err = providermanagementapi.GetSwagger()
    if err == nil {
        generateSwaggerYaml(swagger, "TS29222_CAPIF_API_Provider_Management_API.yaml")
    }

    swagger,err = publishserviceapi.GetSwagger()
    if err == nil {
        generateSwaggerYaml(swagger, "TS29222_CAPIF_Publish_Service_API.yaml")
    }

    swagger,err = invokermanagementapi.GetSwagger()
    if err == nil {
        generateSwaggerYaml(swagger, "TS29222_CAPIF_API_Invoker_Management_API.yaml")
    }

    swagger,err = common.GetSwagger()
    if err == nil {
        generateSwaggerYaml(swagger, "CommonData.yaml")
    }

    swagger,err = common29122.GetSwagger()
    if err == nil {
        generateSwaggerYaml(swagger, "TS29122_CommonData.yaml")
    }

    swagger,err = common29571.GetSwagger()
    if err == nil {
        generateSwaggerYaml(swagger, "TS29571_CommonData.yaml")
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