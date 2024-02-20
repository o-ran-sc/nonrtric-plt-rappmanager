#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023-2024 OpenInfra Foundation Europe. All rights reserved.
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

echo "######### Installing Rapp Manager #########"

./install-base.sh

echo "Installing Kserve components..."
./install-kserve.sh

echo "Installing NONRTRIC components..."
./install-nonrtric.sh "$@"

echo "Installing ACM components..."
./install-acm.sh

echo "Patching Kserve..."
./patch-kserve.sh

echo "Patching Sample rApps..."
./patch-sample-rapps.sh

echo "Rapp Manager installation completed."