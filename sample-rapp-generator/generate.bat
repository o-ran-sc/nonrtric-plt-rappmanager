:: ============LICENSE_START===============================================
::  Copyright (C) 2023 Nordix Foundation. All rights reserved.
::  Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
::  ========================================================================
::  Licensed under the Apache License, Version 2.0 (the "License");
::  you may not use this file except in compliance with the License.
::  You may obtain a copy of the License at
::
::       http://www.apache.org/licenses/LICENSE-2.0
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

if [%1]==[] goto usage
SET DIRECTORY=%1
if %DIRECTORY:~-1%==\ (
    SET DIRECTORY=%DIRECTORY:~0,-1%
)

SET HELM_DIR=%DIRECTORY%/Artifacts/Deployment/HELM
SET CSARFILE=%DIRECTORY%.csar
SET ZIPFILE=%DIRECTORY%.zip
SET EXCLUDE_DIRS=

if exist %DIRECTORY% (
    if exist "%HELM_DIR%" (
        echo Helm directory exists: %HELM_DIR%
        for /d %%D in ("%HELM_DIR%\*") do (
            if exist "%%D" (
                echo Checking directory: %%D
                echo Packaging Helm chart in directory: %%~nxD
                helm package %%D -d %HELM_DIR%
                SET EXCLUDE_DIRS=!EXCLUDE_DIRS!--exclude=Artifacts/Deployment/HELM/%%~nxD/*
                echo Excluding directory: %HELM_DIR%\%%~nxD
            )
        )
    ) else (
        echo Helm directory %HELM_DIR% doesn't exist.
    )

    del %CSARFILE% 2>nul
    pushd %DIRECTORY%
    tar !EXCLUDE_DIRS! -a -cf ..\%ZIPFILE% *
    popd
    rename %ZIPFILE% %CSARFILE%
    @echo rApp package %CSARFILE% generated.
) else (
    @echo Directory %DIRECTORY% doesn't exist.
)
goto :eof

:usage
@echo USAGE: %0% ^<rApp-resource-folder-name^>
