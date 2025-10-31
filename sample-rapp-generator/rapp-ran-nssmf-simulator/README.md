5G RAN NSSMF Simulator Rapp

5G RAN NSSMF Simulator Rapp implements 5G RAN NSSMF Interfaces for subscriptions, NSSI creation, modifiction, get and delete apis according to 3GPP 28.532

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

#### Implementation Details
- **Subscription Storage**: In-memory HashMap (subscriptionMap)
- **ID Generation**: Sequential integer starting from 1
- **3GPP Version**: v17.0.0
- **Server Port**: 8080
- **Logging**: Comprehensive logging for all operations

Directory Structure

src - contains source code for the Rapp
Dockerfile - contains instructions to build a Docker image for the Rapp
