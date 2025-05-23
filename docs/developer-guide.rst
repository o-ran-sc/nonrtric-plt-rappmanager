.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2023-2024 OpenInfra Foundation Europe. All rights reserved.
.. Modifications Copyright (c) 2023-2025 Nordix Foundation.

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC rApp Manager.

Additional developer guides are available on the `O-RAN SC NONRTRIC Developer wiki <https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/86802434/Release+K>`_.

The rApp Manager is a Java 17 web application built using the Spring Framework. Using Spring Boot
dependencies, it runs as a standalone application.

Its main functionality is to lifecycle manage rApps.

Start standalone
++++++++++++++++

The project uses Maven. To start the rApp Manager as a freestanding application, run the following
command in the *rappmanager/rapp-manager-application* directory:

    +-----------------------------+
    | mvn spring-boot:run         |
    +-----------------------------+

There are a few service endpoints that needs to be available to run. These are referred to from the application.yaml file.
The following properties have to be modified:

.. code-block:: yaml

    rappmanager.acm.baseurl=http://policy-clamp-runtime-acm.default:6969/onap/policy/clamp/acm/v2/
    rappmanager.sme.baseurl=http://servicemanager:8095
    rappmanager.dme.baseurl=http://informationservice:9082
    rappmanager.rapps.env.smeDiscoveryEndpoint=http://servicemanager:8095/service-apis/v1/allServiceAPIs


Start in Docker
+++++++++++++++

To build and deploy the rApp Manager, go to the *rappmanager* folder and run the
following command:

    +-----------------------------+
    | mvn clean install           |
    +-----------------------------+

.. note::
    The rApp packages for the unit tests are generated as part of the build process at the rappmanager level.

Then start the container by running the following command:

    +----------------------------------------------+
    | docker run o-ran-sc/nonrtric-plt-rappmanager |
    +----------------------------------------------+

Kubernetes deployment
+++++++++++++++++++++

Non-RT RIC can be also deployed in a Kubernetes cluster, `it/dep repository <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_.
hosts deployment and integration artifacts. Instructions and helm charts to deploy the Non-RT-RIC functions in the
OSC NONRTRIC integrated test environment can be found in the *./nonrtric* directory.

The scripts located in *rappmanager/scripts/install* can be used for an automatic installation in the Kubernetes Cluster, see `Automatic Installation in Kubernetes <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-rappmanager/en/latest/installation-guide.html>`_.

For more information on installation of NonRT-RIC in Kubernetes, see `Deploy NONRTRIC in Kubernetes <https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/86802787/Release+K+-+Run+in+Kubernetes>`_.

For more information see `Integration and Testing documentation in the O-RAN-SC <https://docs.o-ran-sc.org/projects/o-ran-sc-it-dep/en/latest/index.html>`_.

