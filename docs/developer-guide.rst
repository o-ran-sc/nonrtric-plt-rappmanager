.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2023-2024 OpenInfra Foundation Europe. All rights reserved.

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC rApp Manager.

Additional developer guides are available on the `O-RAN SC NONRTRIC Developer wiki <https://wiki.o-ran-sc.org/display/RICNR/Release+I>`_.

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

* rappmanager.acm.baseurl=http://policy-clamp-runtime-acm.default:6969/onap/policy/clamp/acm/v2/
* rappmanager.sme.baseurl=http://servicemanager:8095
* rappmanager.dme.baseurl=http://informationservice:9082
* rappmanager.rapps.env.smeDiscoveryEndpoint=http://servicemanager:8095/service-apis/v1/allServiceAPIs


Start in Docker
+++++++++++++++

To build and deploy the rApp Manager, go to the *rappmanager/rapp-manager-application* folder and run the
following command:

    +-----------------------------+
    | mvn clean install           |
    +-----------------------------+

Then start the container by running the following command:

    +-------------------------------------+
    | docker run nonrtric-plt-rappmanager |
    +-------------------------------------+

Kubernetes deployment
+++++++++++++++++++++

Non-RT RIC can be also deployed in a Kubernetes cluster, `it/dep repository <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_.
hosts deployment and integration artifacts. Instructions and helm charts to deploy the Non-RT-RIC functions in the
OSC NONRTRIC integrated test environment can be found in the *./nonrtric* directory.

For more information on installation of NonRT-RIC in Kubernetes, see `Deploy NONRTRIC in Kubernetes <https://wiki.o-ran-sc.org/display/RICNR/Release+I+-+Run+in+Kubernetes>`_.

For more information see `Integration and Testing documentation in the O-RAN-SC <https://docs.o-ran-sc.org/projects/o-ran-sc-it-dep/en/latest/index.html>`_.

