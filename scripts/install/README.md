## Installation of Rapp Manager

Rapp Manager application requires the following components,

* ONAP ACM
* OSC SME


### Pre-requisites

The installation scripts do not handle the required installations listed below.

* Kubernetes cluster(v1.24.6)
* GIT

### Installation

> **"dev"** mode installation can be used to deploy snapshot images of rApp Manager and DME Participant.
To initiate the dev mode installation, provide "dev" as an argument when executing the script.

All the components can be installed as shown below,

```./install-all.sh``` (or) ```./install-all.sh dev```

Individual components can be installed using the commands below,

```./install-base.sh``` - Installs the tools required for other installer scripts.

```./install-acm.sh``` - Installs the ACM, and it's related components.

```./install-kserve.sh``` - Installs the Kserve, and it's related components.

```./install-nonrtric.sh``` or ```./install-nonrtric.sh dev``` - Installs the NONRTRIC components.

> **These scripts are specifically designed for a fresh environment.**

### Uninstallation

```./uninstall-all.sh``` - Uninstalls all the components



