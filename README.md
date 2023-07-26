# Rapp Manager (This is a prototype)
Rapp manager is an application which lifecycle manages the Rapp.

<mark>**Rapp packaging model used here is purely a prototype**</mark>

## Architecture

![Image](docs/images/architecture.png "Rapp Manager Architecture")

### Rapp States

![Image](docs/images/rapp-states.png "Rapp States")

### Rapp Instance States

![Image](docs/images/rapp-instance-states.png "Rapp Instance States")

### Events responsible for Rapp Instance State Transition

![Image](docs/images/rapp-state-events.png "Rapp Manager State Events")

## Integrations

### ONAP ACM

ONAP ACM is used here as a backend of Rapp manager to lifecycle manage the deployment items as part of Rapp.

ONAP ACM related details can be found [here](https://docs.onap.org/projects/onap-policy-parent/en/london/clamp/clamp.html).

### Integration of SME (CAPIF)

This integration is based on the CAPIF function developed as part of ORAN-SC. It is available [here](https://github.com/o-ran-sc/nonrtric-plt-sme/blob/master/capifcore/README.md)

## Flow Diagrams

### Application Lifecycle

![Image](docs/images/application-lifecycle.png "Rapp Manager Application Lifecycle")

### Rapp Flow

![Image](docs/images/rapp-flow.png "Rapp Flow")

### Rapp Instance Flow

![Image](docs/images/rapp-instance-flow.png "Rapp Instance Flow")


