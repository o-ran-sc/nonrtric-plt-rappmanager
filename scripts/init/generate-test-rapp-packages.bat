:: ============LICENSE_START===============================================
::  Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
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
setlocal enabledelayedexpansion

set SAMPLE_RAPP_GENERATOR="sample-rapp-generator"
set RAPP_GENERATOR_CMD="generate.bat"
set RAPP_BASE_PACKAGE="rapp-all"
set TEST_RESOURCES="\src\test\resources\"
set ACM_TEST_RESOURCES="..\rapp-manager-acm%TEST_RESOURCES:"=%"
set APPLICATION_TEST_RESOURCES="..\rapp-manager-application%TEST_RESOURCES:"=%"
set MODELS_TEST_RESOURCES="..\rapp-manager-models%TEST_RESOURCES:"=%"
set DME_TEST_RESOURCES="..\rapp-manager-dme%TEST_RESOURCES:"=%"
set SME_TEST_RESOURCES="..\rapp-manager-sme%TEST_RESOURCES:"=%"
set CHART_MUSEUM_GET_URI="http://localhost:8879/charts"
set CHART_MUSEUM_POST_URI="http://localhost:8879/charts/api/charts"

pushd %SAMPLE_RAPP_GENERATOR%

echo Generating valid rApp package...
set VALID_RAPP_PACKAGE_FOLDER_NAME="valid-rapp-package"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %VALID_RAPP_PACKAGE_FOLDER_NAME%
call :updateChartMuseumUri %VALID_RAPP_PACKAGE_FOLDER_NAME%
call :generatePackage %VALID_RAPP_PACKAGE_FOLDER_NAME% %ACM_TEST_RESOURCES% %DME_TEST_RESOURCES% %SME_TEST_RESOURCES% %MODELS_TEST_RESOURCES% %APPLICATION_TEST_RESOURCES%

echo Generating valid rApp package without artifacts...
set VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME="valid-rapp-package-no-artifacts"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME%
call :updateChartMuseumUri %VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME%
sed -i "/artifacts/,$d" %VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME%/Definitions/asd.yaml
call :generatePackage %VALID_RAPP_PACKAGE_NO_ARTIFACTS_FOLDER_NAME% %APPLICATION_TEST_RESOURCES%

echo Generating invalid rApp package...
set INVALID_RAPP_PACKAGE_FOLDER_NAME="invalid-rapp-package"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %INVALID_RAPP_PACKAGE_FOLDER_NAME%
rmdir /q /s %INVALID_RAPP_PACKAGE_FOLDER_NAME:"=%\Files %INVALID_RAPP_PACKAGE_FOLDER_NAME:"=%\Artifacts
call :generatePackage %INVALID_RAPP_PACKAGE_FOLDER_NAME% %MODELS_TEST_RESOURCES% %APPLICATION_TEST_RESOURCES%

echo Generating invalid rApp package without tosca...
set INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME="invalid-rapp-package-no-tosca"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME%
del /q %INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME:"=%\TOSCA-Metadata\TOSCA.meta
call :generatePackage %INVALID_RAPP_PACKAGE_NO_TOSCA_FOLDER_NAME% %MODELS_TEST_RESOURCES%

echo Generating invalid rApp package without asd yaml...
set INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME="invalid-rapp-package-no-asd-yaml"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME%
del /q %INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME:"=%\Definitions\asd.yaml
call :generatePackage %INVALID_RAPP_PACKAGE_NO_ASD_YAML_FOLDER_NAME% %MODELS_TEST_RESOURCES%

echo Generating invalid rApp package without ACM composition...
set INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME="invalid-rapp-package-no-acm-composition"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME%
del /q %INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME:"=%\Files\Acm\definition\compositions.json
call :generatePackage %INVALID_RAPP_PACKAGE_NO_ACM_COMPOSITION_FOLDER_NAME% %MODELS_TEST_RESOURCES%

echo Generating invalid rApp package without Artifacts...
set INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME="invalid-rapp-package-missing-artifact"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME%
del /q %INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME:"=%\Artifacts\Deployment\HELM\or*
call :generatePackage %INVALID_RAPP_PACKAGE_MISSING_ARTIFACT_FOLDER_NAME% %MODELS_TEST_RESOURCES%

echo Generating invalid rApp package with empty asd yaml...
set INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME="invalid-rapp-package-empty-asd-yaml"
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME%
copy /y nul %INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME:"=%\Definitions\asd.yaml
call :generatePackage %INVALID_RAPP_PACKAGE_EMPTY_ASD_FOLDER_NAME% %MODELS_TEST_RESOURCES%

echo Generating valid rApp package with new dme info type...
set VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME="valid-rapp-package-new-info-type"
set DME_PRODUCER_FILE=%VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME:"=%\Files\Dme\infoproducers\json-file-data-producer.json
xcopy /i /e /Y %RAPP_BASE_PACKAGE% %VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME%
sed -i "s|\"json-file-data-from-filestore\"|\"json-file-data-from-filestore\",\"new-info-type-not-available\"|g" %DME_PRODUCER_FILE%
call :generatePackage %VALID_RAPP_PACKAGE_NEW_INFO_TYPE_FOLDER_NAME% %DME_TEST_RESOURCES%

EXIT /B 0

:CopyPackage
set "file=%~1"
set "args=%*"
set "args=!args:*%2=!"
for %%A in (%args%) do (
    echo Copying %file% to %%A..
    copy %file% %%A
)
EXIT /B 0

:generatePackage
set "dir=%~1"
set "package_name=%dir%.csar"
shift
call %RAPP_GENERATOR_CMD% %dir%
call :copyPackage %package_name% %*
rd /s /q %dir%
del /q %package_name%
EXIT /B 0


:updateChartMuseumUri
set "dir=%~1"
pushd %dir%
for /R %%G in (*) do (
    sed "s|UPDATE_THIS_CHART_MUSEUM_POST_CHARTS_URI|%CHART_MUSEUM_POST_URI%|g" "%%G" > "%%~temp" && (
        move /y "%%~temp" "%%G" > nul
    )
    sed "s|UPDATE_THIS_CHART_MUSEUM_GET_CHARTS_URI|%CHART_MUSEUM_GET_URI%|g" "%%G" > "%%~temp" && (
        move /y "%%~temp" "%%G" > nul
    )
)
popd
EXIT /B 0


