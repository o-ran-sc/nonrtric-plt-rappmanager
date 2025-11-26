# 5G RAN NSSMF Simulator RAPP

The 5G RAN NSSMF Simulator RAPP implements 5G RAN Network Slice Subnet Management Function (NSSMF) interfaces for subscriptions, NSSI creation, modification, get and delete APIs according to 3GPP 28.532 standards.

This comprehensive package includes both a **Spring Boot application** that provides the NSSMF simulator functionality and a **Helm chart** for Kubernetes deployment.

## Overview

The RAN NSSMF Simulator RAPP consists of three main components:

1. **Spring Boot Application** (`ran-nssmf-simulator/`): A fully functional NSSMF simulator that implements 3GPP-compliant REST APIs
2. **Helm Chart** (`ran-nssmf-simulator-rapp/Artifacts/Deployment/HELM/`): Kubernetes deployment configuration for containerized orchestration
3. **CSAR Package** (`ran-nssmf-simulator-rapp/`): Cloud Service Archive for non-RT RIC rApp Manager deployment

This RAPP is designed to facilitate testing, development, and integration of O-RAN components by providing a mock NSSMF that simulates real network slice management operations without requiring actual network infrastructure.

## CSAR Package Structure

The RAN NSSMF Simulator includes a **CSAR (Cloud Service Archive)** package that enables deployment through the non-RT RIC rApp Manager. The CSAR package contains:

### Core CSAR Files
- **`asd.mf`**: ETSI entry manifest file with package metadata and file inventory
- **`TOSCA-Metadata/TOSCA.meta`**: TOSCA metadata with entry definitions and version information
- **`Definitions/asd.yaml`**: Application Service Descriptor defining rApp properties and deployment artifacts
- **`Definitions/asd_types.yaml`**: TOSCA type definitions for the rApp

### Deployment Artifacts
- **`Artifacts/Deployment/HELM/ran-nssmf-simulator-rapp-0.1.0.tgz`**: Packaged Helm chart for Kubernetes deployment

### Service Management Files
- **`Files/Acm/definition/compositions.json`**: Automation Composition definitions for ONAP Policy CLAMP integration
- **`Files/Acm/instances/k8s-instance.json`**: Kubernetes instance configuration
- **`Files/Sme/providers/provider-function-1.json`**: Service Management Entity provider configuration
- **`Files/Sme/serviceapis/api-set-1.json`**: API service definitions exposing RAN NSSMF Simulator endpoints

### rApp Manager Integration
The CSAR package enables:
- **Package Upload**: Upload rApp packages to non-RT RIC rApp Manager
- **Automated Deployment**: Deploy rApp instances using uploaded CSAR packages
- **Service Discovery**: Automatic registration of rApp APIs with Service Management Entity
- **Lifecycle Management**: Complete rApp lifecycle control through rApp Manager

### API Exposure
The rApp exposes the following APIs through SME:
- **Base URI**: `/3GPPManagement`
- **Operations**: GET, POST, PUT, DELETE
- **Protocol**: HTTP/1.1
- **Service Endpoint**: `ran-nssmf-simulator-rapp.smo.svc.cluster.local:8080`

## API Documentation

### Network Slice Subnet Management API

#### Get Network Slice Subnet
- **URL**: `http://localhost:8080/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/{subnetId}`
- **Method**: `GET`
- **Content-Type**: `application/json`

#### Modify Network Slice Subnet
- **URL**: `http://localhost:8080/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/{subnetId}`
- **Method**: `PUT`
- **Content-Type**: `application/json`
- **Request Body**: NetworkSliceSubnetDTO with updated slice profile characteristics

#### Supported Subnet IDs
The simulator supports 6 pre-configured network slice subnets with different characteristics:

1. `9090d36f-6af5-4cfd-8bda-7a3c88fa82fa` - SST: 1, SD: 000001
2. `9090d36f-6af5-4cfd-8bda-7a3c88fa82fb` - SST: 1, SD: 000002 (with RRU resource allocation)
3. `9090d36f-6af5-4cfd-8bda-7a3c88fa82fc` - SST: 2, SD: 000003
4. `9090d36f-6af5-4cfd-8bda-7a3c88fa82fd` - SST: 2, SD: 000004 (with RRU resource allocation)
5. `9090d36f-6af5-4cfd-8bda-7a3c88fa82fe` - SST: 3, SD: 000005
6. `9090d36f-6af5-4cfd-8bda-7a3c88fa82ff` - SST: 1, SD: 000006 (with RRU resource allocation)

#### Response Structure
```json
{
  "id": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fa",
  "attributes": {
    "operationalState": "enabled",
    "administrativeState": "UNLOCKED",
    "networkSliceSubnetType": "RAN_SLICESUBNET",
    "managedFunctionRef": [
      "2c000978-15e3-4393-984e-a20d32c96004-AUPF_200000",
      "2c000978-15e3-4393-984e-a20d32c96004-DU_200000",
      "2c000978-15e3-4393-984e-a20d32c96004-ACPF_200000"
    ],
    "networkSliceSubnetRef": [],
    "sliceProfileList": [
      {
        "sliceProfileId": "2f1ca17d-5c44-4355-bfed-e9800a2996c1",
        "extensions": {
          "state": "IN_SERVICE"
        },
        "pLMNInfoList": [
          {
            "PLMNId": {
              "mcc": "330",
              "mnc": "220"
            },
            "SNSSAI": {
              "sst": 1,
              "sd": "000001"
            }
          }
        ],
        "RANSliceSubnetProfile": {
          "coverageAreaTAList": [1, 2],
          "resourceSharingLevel": "shared",
          "RRU.PrbDl": 1024,
          "RRU.PrbUl": 3096
        }
      }
    ]
  }
}
```

#### Example Usage

**cURL Command (GET):**
```bash
curl -X GET \
  http://localhost:8080/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/9090d36f-6af5-4cfd-8bda-7a3c88fa82fa \
  -H 'Content-Type: application/json'
```

**cURL Command (PUT):**
```bash
curl -X PUT \
  http://localhost:8080/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/9090d36f-6af5-4cfd-8bda-7a3c88fa82fa \
  -H 'Content-Type: application/json' \
  -d '{
    "id": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fa",
    "attributes": {
      "operationalState": "enabled",
      "administrativeState": "UNLOCKED",
      "networkSliceSubnetType": "RAN_SLICESUBNET",
      "sliceProfileList": [
        {
          "sliceProfileId": "2f1ca17d-5c44-4355-bfed-e9800a2996c1",
          "pLMNInfoList": [
            {
              "PLMNId": {
                "mcc": "330",
                "mnc": "220"
              },
              "SNSSAI": {
                "sst": 1,
                "sd": "000001"
              }
            }
          ]
        }
      ]
    }
  }'
```

**Response Codes:**
- `200 OK`: Successfully retrieved or modified network slice subnet
- `404 Not Found`: Subnet ID not found
- `400 Bad Request`: Invalid request body or validation errors

#### Key Features
- **3GPP Compliance**: Implements 3GPP 28.532 Network Slice Subnet Management
- **Pre-configured Data**: 6 different slice configurations with varying characteristics
- **SNSSAI Support**: Different Slice/Service Type (SST) and Slice Differentiator (SD) combinations
- **RAN Resource Allocation**: Configurable RRU PRB (Physical Resource Block) allocations
- **PLMN Support**: Public Land Mobile Network identifiers
- **Managed Function References**: Links to network functions (AUPF, DU, ACPF)

### File Data Reporting Subscription API

#### Endpoint
- **URL**: `http://localhost:8080/3GPPManagement/FileDataReportingMnS/v17.0.0/subscriptions`
- **Method**: `POST`
- **Content-Type**: `application/json`

#### Request Body
```json
{
  "consumerReference": "https://callback-url.com/notifications"
}
```

**Request Validation:**
- `consumerReference` is required and cannot be null or empty
- Must be a valid URL for receiving callback notifications

#### Response
**Status Code**: `201 Created`
**Headers**: 
- `Location`: URL of the created subscription resource

**Response Body:**
```json
{
  "consumerReference": "https://callback-url.com/notifications"
}
```

#### Functionality
- Creates a new subscription for file data reporting notifications
- Generates a unique subscription ID automatically
- Stores subscription details in memory
- Returns the subscription location in the `Location` header
- Logs all subscription operations for debugging
- Sends automated file ready notifications every 5 minutes to all active subscribers

#### Example Usage

**cURL Command:**
```bash
curl -X POST \
  http://localhost:8080/3GPPManagement/FileDataReportingMnS/v17.0.0/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{
    "consumerReference": "https://my-callback-service.com/webhook"
  }'
```

**Example Response:**
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/3GPPManagement/FileDataReportingMnS/v17.0.0/subscriptions/1
Content-Type: application/json

{
  "consumerReference": "https://my-callback-service.com/webhook"
}
```

### File Ready Notification API

#### Automated Notifications
The simulator automatically sends file ready notifications to all subscribed endpoints every 5 minutes.

**Notification Endpoint**: The callback URL provided during subscription
**Method**: `POST`
**Content-Type**: `application/json`

#### Notification Body Structure
```json
{
  "notificationHeader": {
    "notificationId": "notif-1234567890",
    "notificationType": "notifyFileReady",
    "eventTime": {
      "dateTime": "2025-01-03T12:09:48.123"
    }
  },
  "fileInfoList": [
    {
      "fileLocation": "http://example.com/files/sample-performance-data-1234567890.csv",
      "fileSize": 1024,
      "fileReadyTime": {
        "dateTime": "2025-01-03T12:09:48.123"
      },
      "fileExpirationTime": {
        "dateTime": "2025-01-04T12:09:48.123"
      },
      "fileCompression": "gzip",
      "fileFormat": "CSV",
      "fileDataType": "Performance",
      "jobId": "job-1234567890"
    }
  ],
  "additionalText": "Sample file ready notification from NSSMF simulator"
}
```

#### Notification Fields Description

**NotificationHeader:**
- `notificationId`: Unique identifier for the notification
- `notificationType`: Type of notification (always "notifyFileReady")
- `eventTime`: Timestamp when the notification was generated

**FileInfo:**
- `fileLocation`: URL where the file can be accessed
- `fileSize`: Size of the file in bytes
- `fileReadyTime`: Timestamp when the file became available
- `fileExpirationTime`: Timestamp when the file will expire
- `fileCompression`: Compression format used (e.g., "gzip")
- `fileFormat`: File format (e.g., "CSV", "JSON")
- `fileDataType`: Type of data in the file (e.g., "Performance")
- `jobId`: Unique identifier for the job that generated the file

#### Implementation Details
- **Subscription Storage**: In-memory HashMap (subscriptionMap)
- **ID Generation**: Sequential integer starting from 1
- **3GPP Version**: v17.0.0
- **Server Port**: 8080
- **Logging**: Comprehensive logging for all operations
- **Notification Schedule**: Every 5 minutes (300,000 ms)
- **Data Models**: Complete DTO structure for 3GPP compliance

## Data Transfer Objects (DTOs)

### File Data Reporting DTOs
- **SubscriptionRequestDTO**: Handles subscription requests with callback URI validation
- **SubscriptionDTO**: Response object for subscription confirmation
- **NotifyFileReadyDTO**: Complete notification payload sent to subscribers
- **NotificationHeaderDTO**: Standard 3GPP notification header with metadata
- **FileInfoDTO**: Detailed file information including location, size, and timestamps
- **DateTimeDTO**: Standardized date/time format using ISO 8601

### Network Slice Subnet DTOs
- **NetworkSliceSubnetDTO**: Main network slice subnet representation with ID and attributes
- **NetworkSliceSubnetAttributesDTO**: Slice subnet attributes including operational state, administrative state, and slice profiles
- **SliceProfileDTO**: Response DTO for slice profile modification operations (placeholder for 3GPP TS28541_SliceNrm.yaml compliance)
- **SliceProfileItemDTO**: Individual slice profile configuration with PLMN info and RAN profile
- **SliceProfileExtensionsDTO**: Slice profile extensions including service state
- **PlmnInfoListDTO**: PLMN information list containing network identifiers
- **PlmnIdDTO**: Public Land Mobile Network identifier (MCC/MNC)
- **SnssaiDTO**: Single-Network Slice Selection Assistance Information (SST/SD)
- **RanSliceSubnetProfileDTO**: RAN-specific slice profile including coverage area and resource allocation

### DTO Features
- **Lombok Integration**: Automatic getter/setter generation and builder patterns
- **Validation**: Jakarta validation annotations for request validation
- **Builder Pattern**: Fluent API for object creation
- **Sample Data**: Static factory methods for creating test data
- **JSON Property Mapping**: Custom JSON property annotations for 3GPP compliance
- **3GPP Compliance**: All DTOs follow 3GPP 28.532 standards

## Technical Architecture

### Framework & Technology Stack
- **Spring Boot 3.5.5** with Java 21
- **Gradle** build system
- **Lombok** for reducing boilerplate code
- **Jakarta Validation** for request validation
- **Spring Web** for REST API endpoints
- **Spring Scheduling** for periodic notification tasks

### Key Components
- **FileDataReportingMnSController**: REST controller handling subscriptions and notifications
- **NetworkSliceSubnetController**: REST controller handling network slice subnet management operations (GET and PUT)
- **Request Validation**: Jakarta validation annotations for input validation on modification requests
- **Scheduled Notifications**: Automated file ready notifications every 5 minutes
- **RestTemplate**: HTTP client for sending callback notifications
- **In-memory Storage**: HashMap-based subscription management
- **Pre-configured Slice Data**: Mock network slice subnet configurations for testing
- **Comprehensive Logging**: Detailed logging for all operations including modification requests

## Directory Structure

```
├── ran-nssmf-simulator/                          # Spring Boot Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/org/oransc/ran/nssmf/simulator/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── FileDataReportingMnSController.java
│   │   │   │   │   └── NetworkSliceSubnetController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── DateTimeDTO.java
│   │   │   │   │   ├── FileInfoDTO.java
│   │   │   │   │   ├── NetworkSliceSubnetAttributesDTO.java
│   │   │   │   │   ├── NetworkSliceSubnetDTO.java
│   │   │   │   │   ├── NotificationHeaderDTO.java
│   │   │   │   │   ├── NotifyFileReadyDTO.java
│   │   │   │   │   ├── PlmnIdDTO.java
│   │   │   │   │   ├── PlmnInfoListDTO.java
│   │   │   │   │   ├── RanSliceSubnetProfileDTO.java
│   │   │   │   │   ├── SliceProfileExtensionsDTO.java
│   │   │   │   │   ├── SliceProfileItemDTO.java
│   │   │   │   │   ├── SnssaiDTO.java
│   │   │   │   │   ├── SubscriptionDTO.java
│   │   │   │   │   └── SubscriptionRequestDTO.java
│   │   │   │   └── RanNssmfSimulatorApplication.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/org/oransc/ran_nssmf_simulator/
│   ├── build.gradle
│   ├── gradlew
│   └── gradlew.bat
└── ran-nssmf-simulator-rapp/                     # CSAR Package for rApp Manager
    ├── asd.mf                                    # ETSI Entry Manifest
    ├── Definitions/                              # TOSCA Definitions
    │   ├── asd.yaml                              # Application Service Descriptor
    │   └── asd_types.yaml                        # TOSCA Type Definitions
    ├── Files/                                    # Service Management Files
    │   ├── Acm/                                  # Automation Composition
    │   │   ├── definition/
    │   │   │   └── compositions.json             # ONAP Policy CLAMP definitions
    │   │   └── instances/
    │   │       └── k8s-instance.json             # Kubernetes instance config
    │   └── Sme/                                  # Service Management Entity
    │       ├── providers/
    │       │   └── provider-function-1.json      # SME provider configuration
    │       └── serviceapis/
    │           └── api-set-1.json                # API service definitions
    ├── TOSCA-Metadata/                           # TOSCA Metadata
    │   └── TOSCA.meta                            # TOSCA metadata file
    └── Artifacts/                                # Deployment Artifacts
        └── Deployment/
            └── HELM/
                └── ran-nssmf-simulator-rapp/     # Helm Chart
                    ├── Chart.yaml
                    ├── values.yaml
                    ├── .helmignore
                    └── templates/
                        ├── _helpers.tpl
                        ├── deployment.yaml
                        ├── service.yaml
                        ├── serviceaccount.yaml
                        ├── NOTES.txt
                        └── tests/
                            └── test-connection.yaml
```

## Configuration

### Application Properties
- `spring.application.name=ran-nssmf-simulator`
- `mns.fileDataReporting.version=v17.0.0`
- `server.port=8080`
- `spring.jackson.serialization.fail-on-empty-beans=false`

### Build Configuration
- **Java Version**: 21
- **Spring Boot Version**: 3.5.5
- **Dependency Management**: Spring Boot Gradle Plugin
- **Testing**: JUnit 5 with Spring Boot Test

## Deployment

### Docker Support
- **Dockerfile**: Contains instructions to build a Docker image for the Rapp
- **Containerization**: Ready for deployment in containerized environments
- **Port Exposure**: 8080 for HTTP access

### Running the Application

**Using Gradle:**
```bash
./gradlew bootRun
```

**Using Java:**
```bash
./gradlew build
java -jar build/libs/ran-nssmf-simulator-0.0.1-SNAPSHOT.jar
```

**Using Docker:**
```bash
docker build -t ran-nssmf-simulator .
docker run -p 8080:8080 ran-nssmf-simulator
```

### Kubernetes Deployment with Helm

The RAN NSSMF Simulator includes a comprehensive Helm chart for deployment in Kubernetes clusters. The Helm chart is located in the `ran-nssmf-simulator-rapp/Artifacts/Deployment/HELM/ran-nssmf-simulator-rapp/` directory.

#### Prerequisites
- **Kubernetes Cluster**: v1.20+ with access to configure deployments and services
- **Helm 3**: Package manager for Kubernetes (v3.0+)
- **kubectl**: Kubernetes command-line tool configured for your cluster

#### Helm Chart Configuration

**Chart Details:**
- **Chart Name**: `ran-nssmf-simulator-rapp`
- **Chart Version**: 0.1.0
- **App Version**: 1.16.0
- **API Version**: v2 (Helm 3 compatible)

**Key Configuration Options** (in `values.yaml`):
- **Replica Count**: Number of pod replicas (default: 1)
- **Image Repository**: `<DOCKER_REGISTRY>/ran-nssmf-simulator-rapp`
- **Image Tag**: `1.0.0`
- **Service Type**: ClusterIP (default) - configurable to NodePort, LoadBalancer, or ClusterIP
- **Service Port**: 8080
- **Resource Limits**: Configurable CPU and memory limits
- **Auto-scaling**: Optional horizontal pod autoscaling support
- **Environment Variables**: Configurable application settings including SME discovery endpoint

#### Deployment Steps

**1. Build and Push Docker Image:**
```bash
# Build the Docker image
docker build -t <DOCKER_REGISTRY>/ran-nssmf-simulator-rapp:1.0.0 .

# Push to your container registry
docker push <DOCKER_REGISTRY>/ran-nssmf-simulator-rapp:1.0.0
```

**2. Install the Helm Chart:**
```bash
# Navigate to the Helm chart directory
cd ran-nssmf-simulator-rapp/Artifacts/Deployment/HELM/ran-nssmf-simulator-rapp/

# Install the chart (default namespace)
helm install ran-nssmf-simulator .

# Install in a specific namespace
helm install ran-nssmf-simulator . --namespace nssmf --create-namespace

# Install with custom values
helm install ran-nssmf-simulator . --values custom-values.yaml
```

**3. Verify Deployment:**
```bash
# Check pod status
kubectl get pods -l app.kubernetes.io/name=ran-nssmf-simulator-rapp

# Check service status
kubectl get svc ran-nssmf-simulator-rapp

# Check deployment logs
kubectl logs -l app.kubernetes.io/name=ran-nssmf-simulator-rapp
```

**4. Access the Application:**

The access method depends on the service type configured:

**For ClusterIP (default):**
```bash
# Port forward to access locally
kubectl port-forward svc/ran-nssmf-simulator-rapp 8080:8080
# Access at http://localhost:8080
```

**For NodePort:**
```bash
# Get the node port
export NODE_PORT=$(kubectl get svc ran-nssmf-simulator-rapp -o jsonpath='{.spec.ports[0].nodePort}')
export NODE_IP=$(kubectl get nodes -o jsonpath='{.items[0].status.addresses[0].address}')
echo "Access at http://$NODE_IP:$NODE_PORT"
```

**For LoadBalancer:**
```bash
# Get the external IP
export SERVICE_IP=$(kubectl get svc ran-nssmf-simulator-rapp -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "Access at http://$SERVICE_IP:8080"
```

#### Customization Options

**Custom Values File Example** (`custom-values.yaml`):
```yaml
replicaCount: 3
image:
  repository: my-registry.com/ran-nssmf-simulator-rapp
  tag: "2.0.0"
service:
  type: LoadBalancer
  port: 8080
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi
environment:
  appId: "my-nssmf-app"
  smeDiscoveryEndpoint: "http://my-service-manager:8095/service-apis/v1/allServiceAPIs"
```

#### Helm Operations

**Uninstall Deployment:**
```bash
helm uninstall ran-nssmf-simulator
```

**API Testing:**
```bash
# Test health endpoint
kubectl port-forward svc/ran-nssmf-simulator-rapp 8080:8080 &
curl http://localhost:8080/actuator/health

# Test network slice subnet API
curl http://localhost:8080/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/9090d36f-6af5-4cfd-8bda-7a3c88fa82fa
```

## Testing and Integration

### Health Checks
- **Spring Boot Actuator**: Available for health monitoring
- **Endpoint**: `http://localhost:8080/actuator/health`

### Logging
- **Comprehensive Logging**: All subscription and notification operations are logged
- **Debug Information**: Detailed logs for troubleshooting integration issues
- **Error Handling**: Graceful error handling for failed notifications

## Use Cases

This simulator is designed to:
1. **Test Integration**: Allow other RIC components to test NSSMF integration
2. **Development Support**: Provide a mock NSSMF for development without real network equipment
3. **Standard Compliance**: Demonstrate 3GPP 28.532 interface compliance
4. **Callback Testing**: Enable testing of asynchronous notification mechanisms
5. **Performance Testing**: Support load testing with multiple subscribers
6. **API Validation**: Validate client implementations against 3GPP standards
7. **Network Slice Testing**: Test network slice subnet retrieval and management operations
8. **SNSSAI Validation**: Validate Slice/Service Type and Slice Differentiator handling
9. **RAN Resource Management**: Test RAN resource allocation and configuration scenarios
10. **PLMN Configuration**: Test Public Land Mobile Network identifier handling
