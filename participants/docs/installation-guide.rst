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
