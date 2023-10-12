:: ============LICENSE_START===============================================
::  Copyright (C) 2023 Nordix Foundation. All rights reserved.
::  ========================================================================
::  Licensed under the Apache License, Version 2.0 (the "License");
::  you may not use this file except in compliance with the License.
::  You may obtain a copy of the License at
::
::       http:\\www.apache.org\licenses\LICENSE-2.0
::
::  Unless required by applicable law or agreed to in writing, software
::  distributed under the License is distributed on an "AS IS" BASIS,
::  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
::  See the License for the specific language governing permissions and
::  limitations under the License.
::  ============LICENSE_END=================================================
::

@echo off
set SME_LOCATION="..\sme\capifcore"
set SME_OPENAPI_LOCATION="..\..\openapi\sme"
cp ..\scripts\init\getsmeswagger.go %SME_LOCATION%
cd %SME_LOCATION%

echo Generating SME openapi spec...

IF EXIST getsmeswagger.go (
  echo Generating...
  go run getsmeswagger.go

  echo Copying generated specs...
  mkdir %SME_OPENAPI_LOCATION%
  echo Copying CommonData.yaml
  mv CommonData.yaml %SME_OPENAPI_LOCATION%
  echo Copying TS29122_CommonData.yaml
  mv TS29122_CommonData.yaml %SME_OPENAPI_LOCATION%
  echo Copying TS29571_CommonData.yaml
  mv TS29571_CommonData.yaml %SME_OPENAPI_LOCATION%
  echo Copying TS29222_CAPIF_API_Invoker_Management_API.yaml
  mv TS29222_CAPIF_API_Invoker_Management_API.yaml %SME_OPENAPI_LOCATION%
  echo Copying TS29222_CAPIF_API_Provider_Management_API.yaml
  mv TS29222_CAPIF_API_Provider_Management_API.yaml %SME_OPENAPI_LOCATION%
  echo Copying TS29222_CAPIF_Publish_Service_API.yaml
  mv TS29222_CAPIF_Publish_Service_API.yaml %SME_OPENAPI_LOCATION%
) ELSE (
  echo Unable to find the openapi spec generator.
)
