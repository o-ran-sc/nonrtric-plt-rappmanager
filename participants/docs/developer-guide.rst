.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC DME Participant.

Additional developer guides are available on the `O-RAN SC NONRTRIC Developer wiki <https://wiki.o-ran-sc.org/display/RICNR/Release+I>`_.

The DME Participant is a Java 17 web application built using the Spring Framework. Using Spring Boot
dependencies, it runs as a standalone application.

Its main functionality is to work with ACM to lifecycle manage DME.

Start standalone
++++++++++++++++

The project uses Maven. To start the DME Participant as a freestanding application, run the following
command in the *rappmanager/participants/participant-impl-dme* directory:

    +-----------------------------+
    | mvn spring-boot:run         |
    +-----------------------------+

There are a few service endpoints that needs to be available to run. These are referred to from the application.yaml file.
The following properties have to be modified:

* dme.baseUrl=http://informationservice:9082


Start in Docker
+++++++++++++++

To build and deploy the DME Participant, go to the *rappmanager/participants/participant-impl-dme* folder and run the
following command:

    +-----------------------------+
    | mvn clean install           |
    +-----------------------------+

Then start the container by running the following command:

    +----------------------------------------+
    | docker run nonrtric-plt-dmeparticipant |
    +----------------------------------------+

Kubernetes deployment
+++++++++++++++++++++

Non-RT RIC can be also deployed in a Kubernetes cluster, `it/dep repository <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_.
hosts deployment and integration artifacts. Instructions and helm charts to deploy the Non-RT-RIC functions in the
OSC NONRTRIC integrated test environment can be found in the *./nonrtric* directory.

For more information on installation of NonRT-RIC in Kubernetes, see `Deploy NONRTRIC in Kubernetes <https://wiki.o-ran-sc.org/display/RICNR/Release+I+-+Run+in+Kubernetes>`_.

For more information see `Integration and Testing documentation in the O-RAN-SC <https://docs.o-ran-sc.org/projects/o-ran-sc-it-dep/en/latest/index.html>`_.

