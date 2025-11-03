5G RAN NSSMF Simulator Rapp

5G RAN NSSMF Simulator Rapp implements 5G RAN NSSMF Interfaces for subscriptions, NSSI creation, modification, get and delete APIs according to 3GPP 28.532

## API Documentation

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

### Core DTOs
- **SubscriptionRequestDTO**: Handles subscription requests with callback URI validation
- **SubscriptionDTO**: Response object for subscription confirmation
- **NotifyFileReadyDTO**: Complete notification payload sent to subscribers
- **NotificationHeaderDTO**: Standard 3GPP notification header with metadata
- **FileInfoDTO**: Detailed file information including location, size, and timestamps
- **DateTimeDTO**: Standardized date/time format using ISO 8601

### DTO Features
- **Lombok Integration**: Automatic getter/setter generation and builder patterns
- **Validation**: Jakarta validation annotations for request validation
- **Builder Pattern**: Fluent API for object creation
- **Sample Data**: Static factory methods for creating test data

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
- **Scheduled Notifications**: Automated file ready notifications every 5 minutes
- **RestTemplate**: HTTP client for sending callback notifications
- **In-memory Storage**: HashMap-based subscription management

## Directory Structure

```
src/
├── main/
│   ├── java/org/oransc/ran/nssmf/simulator/
│   │   ├── controller/
│   │   │   └── FileDataReportingMnSController.java
│   │   ├── dto/
│   │   │   ├── DateTimeDTO.java
│   │   │   ├── FileInfoDTO.java
│   │   │   ├── NotificationHeaderDTO.java
│   │   │   ├── NotifyFileReadyDTO.java
│   │   │   ├── SubscriptionDTO.java
│   │   │   └── SubscriptionRequestDTO.java
│   │   └── RanNssmfSimulatorApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/org/oransc/ran_nssmf_simulator/
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
