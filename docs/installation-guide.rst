.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.

Installation Guide
==================

Abstract
--------

This document describes how to install the Non-RT RIC components, their dependencies and required system resources.

Software Installation and Deployment
------------------------------------

Install with Helm
+++++++++++++++++

Helm charts and an example recipe are provided in the `it/dep repo <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_,
under "nonrtric". By modifying the variables named "installXXX" in the beginning of the example recipe file, which
components that will be installed can be controlled. Then the components can be installed and started by running the
following command:

      .. code-block:: bash

        bin/deploy-nonrtric -f nonrtric/RECIPE_EXAMPLE/example_recipe.yaml

Install with dependent components
+++++++++++++++++++++++++++++++++

The scripts for the deployments of rApp Manager and its dependent components are available in *rappmanager/scripts/install* directory.

ACM components should be configured with couple of other components for the participants to work.

In case some of the installation is already setup or not set by the installation scripts, the below environment variables can be used to set the configurations ACM through installation scripts.

+--------------------+--------------------------------------------+----------------------------------------------+
| **Variable Name**  | **Description**                            | **Default Value**                            |
+--------------------+--------------------------------------------+----------------------------------------------+
| CHART_REPO_HOST    | Address of the chart repository.           | http://IP_ADDRESS:8879/charts                |
|                    |                                            |                                              |
|                    | It will be used by Kubernetes participant. | IP_ADDRESS: IP of the host in which          |
|                    |                                            | the installation scripts are running.        |
+--------------------+--------------------------------------------+----------------------------------------------+
| A1PMS_HOST         | Address of the A1PMS.                      | http://policymanagementservice.nonrtric:9080 |
|                    |                                            |                                              |
|                    | It will be accessed from A1PMS participant.|                                              |
+--------------------+--------------------------------------------+----------------------------------------------+

All components can be installed using the command below,

      .. code-block:: bash

        ./install-all.sh

Individual components can be installed using the commands below,

To install the tools required for other installer scripts.

      .. code-block:: bash

        ./install-base.sh

To install the ACM, and it's related components.

      .. code-block:: bash

        ./install-acm.sh

To install the Kserve, and it's related components.

      .. code-block:: bash

        ./install-kserve.sh

To installs the NONRTRIC components.

      .. code-block:: bash

        ./install-nonrtric.sh


Uninstallation
++++++++++++++
To uninstall all the components

      .. code-block:: bash

        ./uninstall-all.sh
