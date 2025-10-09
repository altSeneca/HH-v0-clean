# Phase 2 - Backend API Requirements

**Document Version**: 1.0
**Date**: October 8, 2025
**Phase**: Crew Management - Phase 2 (Certification Management)
**Status**: Specification Complete - Implementation Needed

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [File Upload Endpoints](#file-upload-endpoints)
3. [OCR Processing Endpoints](#ocr-processing-endpoints)
4. [Notification Configuration](#notification-configuration)
5. [Infrastructure Requirements](#infrastructure-requirements)
6. [Background Jobs](#background-jobs)
7. [Security Requirements](#security-requirements)
8. [API Request/Response Examples](#api-requestresponse-examples)
9. [Error Handling](#error-handling)
10. [Monitoring & Logging](#monitoring--logging)

---

## Executive Summary

Phase 2 (Certification Management) client-side services have been implemented with stub integrations. This document specifies the required backend API endpoints, infrastructure configuration, and integration requirements for full functionality.

### Service Integration Status

| Service | Client Status | Backend Status | Priority |
|---------|--------------|----------------|----------|
| FileUploadService | ✅ Complete | ⏳ Needed | P0 |
| OCRService | ✅ Complete | ⏳ Needed | P0 |
| NotificationService | ✅ Complete | ⏳ Needed | P1 |
| Background Jobs | ⏳ Spec Only | ⏳ Needed | P1 |

### Key Technologies

- **File Storage**: AWS S3 with CloudFront CDN
- **OCR Processing**: Google Cloud Document AI
- **Email**: SendGrid
- **SMS**: Twilio
- **Push Notifications**: Firebase Cloud Messaging (FCM) / Apple Push Notification Service (APNs)
- **Database**: PostgreSQL (schema already deployed)

---

## File Upload Endpoints

### 1. Generate Presigned Upload URL

**Purpose**: Provide secure, time-limited S3 upload URLs to clients without exposing AWS credentials.

#### Endpoint
```
POST /api/storage/presigned-url
```

#### Request Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Request Body
```json
{
  "bucket": "hazardhawk-certifications",
  "key": "certifications/1728400000000-osha-30.pdf",
  "contentType": "application/pdf",
  "expirationSeconds": 3600,
  "operation": "putObject"
}
```

**Field Descriptions**:
- `bucket` (required): S3 bucket name
- `key` (required): Object key/path in S3
- `contentType` (required): MIME type of file being uploaded
- `expirationSeconds` (optional): URL validity duration (default: 3600, max: 86400)
- `operation` (required): S3 operation (`putObject`, `getObject`, `deleteObject`)

#### Response (200 OK)
```json
{
  "presignedUrl": "https://hazardhawk-certifications.s3.us-east-1.amazonaws.com/certifications/1728400000000-osha-30.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...",
  "expiresAt": "2025-10-08T16:00:00Z"
}
```

#### Error Responses
```json
// 400 Bad Request - Invalid parameters
{
  "error": "invalid_request",
  "message": "Content-Type is required",
  "code": "MISSING_CONTENT_TYPE"
}

// 401 Unauthorized - Invalid/expired token
{
  "error": "unauthorized",
  "message": "Invalid authentication token",
  "code": "INVALID_TOKEN"
}

// 403 Forbidden - Insufficient permissions
{
  "error": "forbidden",
  "message": "User does not have permission to upload to this bucket",
  "code": "INSUFFICIENT_PERMISSIONS"
}

// 429 Too Many Requests - Rate limit exceeded
{
  "error": "rate_limit_exceeded",
  "message": "Maximum 100 presigned URLs per minute exceeded",
  "code": "RATE_LIMIT_EXCEEDED",
  "retryAfter": 60
}
```

#### Rate Limiting
- **Per User**: 100 requests per minute
- **Per Company**: 500 requests per minute
- **Global**: 5,000 requests per minute

#### Implementation Notes
- Use AWS SDK's `getSignedUrl()` or equivalent
- Validate bucket name against whitelist
- Sanitize object keys to prevent path traversal attacks
- Log all presigned URL generations for audit trail
- Set appropriate CORS headers for client uploads

---

### 2. Delete Uploaded File

**Purpose**: Delete files from S3 storage (certifications, thumbnails).

#### Endpoint
```
DELETE /api/storage/files
```

#### Request Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Request Body
```json
{
  "bucket": "hazardhawk-certifications",
  "key": "certifications/1728400000000-osha-30.pdf"
}
```

#### Response (204 No Content)
No body returned on success.

#### Error Responses
```json
// 404 Not Found - File does not exist
{
  "error": "not_found",
  "message": "File not found in specified bucket",
  "code": "FILE_NOT_FOUND"
}

// 403 Forbidden - User does not own file
{
  "error": "forbidden",
  "message": "User does not have permission to delete this file",
  "code": "INSUFFICIENT_PERMISSIONS"
}
```

#### Implementation Notes
- Verify user has permission to delete file (owns certification or is admin)
- Delete both main file and associated thumbnail if exists
- Update database to remove URLs from `worker_certifications` table
- Implement soft delete with 30-day retention before permanent deletion
- Log all deletions for audit compliance

---

### 3. Check File Existence

**Purpose**: Verify if a file exists in S3 without downloading it.

#### Endpoint
```
HEAD /api/storage/files?bucket={bucket}&key={key}
```

#### Request Headers
```
Authorization: Bearer <JWT_TOKEN>
```

#### Query Parameters
- `bucket` (required): S3 bucket name
- `key` (required): Object key/path

#### Response (200 OK)
```
HTTP/1.1 200 OK
Content-Type: application/pdf
Content-Length: 1048576
Last-Modified: Tue, 08 Oct 2025 14:30:00 GMT
ETag: "abc123def456"
```

#### Response (404 Not Found)
```
HTTP/1.1 404 Not Found
```

#### Implementation Notes
- Use AWS SDK's `headObject()` method
- Return S3 metadata in response headers
- Cache results for 5 minutes to reduce S3 API calls
- Validate user has permission to check this file

---

## OCR Processing Endpoints

### 1. Submit Document for OCR Extraction

**Purpose**: Process certification documents using Google Document AI to extract structured data.

#### Endpoint
```
POST /api/ocr/extract-certification
```

#### Request Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

#### Request Body
```json
{
  "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf",
  "workerProfileId": "550e8400-e29b-41d4-a716-446655440000",
  "expectedCertificationType": "OSHA_30"
}
```

**Field Descriptions**:
- `documentUrl` (required): S3/CDN URL of document to process
- `workerProfileId` (required): Worker profile ID for context
- `expectedCertificationType` (optional): Hint for intelligent extraction

#### Response (200 OK) - Synchronous Processing
```json
{
  "processingId": "ocr_abc123def456",
  "status": "completed",
  "extractedData": {
    "holderName": "John Doe",
    "certificationType": "OSHA_30",
    "certificationNumber": "12345678",
    "issueDate": "2024-06-15",
    "expirationDate": "2029-06-15",
    "issuingAuthority": "OSHA",
    "confidence": 0.92,
    "needsReview": false,
    "rawText": "OSHA 30-Hour Construction Safety Certificate\n\nThis certifies that John Doe...",
    "extractedFields": {
      "holderName": {"value": "John Doe", "confidence": 0.98},
      "certificationType": {"value": "OSHA 30", "confidence": 0.95},
      "certificationNumber": {"value": "12345678", "confidence": 0.88},
      "issueDate": {"value": "2024-06-15", "confidence": 0.92},
      "expirationDate": {"value": "2029-06-15", "confidence": 0.90},
      "issuingAuthority": {"value": "OSHA", "confidence": 0.95}
    }
  },
  "processingTimeMs": 2340
}
```

#### Response (202 Accepted) - Asynchronous Processing
```json
{
  "processingId": "ocr_abc123def456",
  "status": "processing",
  "webhookUrl": "https://app.hazardhawk.com/api/webhooks/ocr-complete",
  "estimatedCompletionSeconds": 15
}
```

#### Error Responses
```json
// 400 Bad Request - Invalid document format
{
  "error": "invalid_document",
  "message": "Document format not supported. Supported: PDF, PNG, JPG, JPEG",
  "code": "UNSUPPORTED_FORMAT",
  "supportedFormats": ["PDF", "PNG", "JPG", "JPEG"]
}

// 413 Payload Too Large - File size exceeds limit
{
  "error": "file_too_large",
  "message": "Document size exceeds 10MB limit",
  "code": "FILE_SIZE_EXCEEDED",
  "maxSizeBytes": 10485760
}

// 503 Service Unavailable - Google Document AI unavailable
{
  "error": "ocr_service_unavailable",
  "message": "OCR service temporarily unavailable",
  "code": "EXTERNAL_SERVICE_ERROR",
  "retryAfter": 30
}
```

#### Processing Strategy

**Synchronous** (preferred for small files):
- Files < 5MB
- Processing time < 10 seconds
- Return results immediately

**Asynchronous** (for large files):
- Files > 5MB
- Processing time > 10 seconds
- Return 202 Accepted with webhook callback

#### Implementation Notes
- Use Google Cloud Document AI Processor API
- Create custom processor for construction certifications
- Map extracted text to standard certification type codes (see `CertificationTypeCodes` in client)
- Calculate confidence score as average of individual field confidences
- Set `needsReview = true` if confidence < 0.85
- Store raw OCR text in `worker_certifications.ocr_metadata`
- Implement retry logic (3 attempts with exponential backoff)
- Cache results for 24 hours by document URL

---

### 2. Validate Document Format

**Purpose**: Pre-validate document before OCR processing to save API calls.

#### Endpoint
```
POST /api/ocr/validate-document
```

#### Request Body
```json
{
  "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf"
}
```

#### Response (200 OK)
```json
{
  "isValid": true,
  "format": "PDF",
  "sizeBytes": 1048576,
  "errorMessage": null
}
```

#### Response (400 Bad Request)
```json
{
  "isValid": false,
  "format": null,
  "sizeBytes": 15728640,
  "errorMessage": "File size exceeds 10MB maximum"
}
```

#### Validation Rules
- **Supported Formats**: PDF, PNG, JPG, JPEG
- **Max File Size**: 10MB
- **Min File Size**: 1KB
- **URL Accessibility**: Must return 200 OK on HEAD request

---

### 3. Batch OCR Processing

**Purpose**: Process multiple certifications concurrently (worker onboarding with multiple certs).

#### Endpoint
```
POST /api/ocr/batch-extract
```

#### Request Body
```json
{
  "documents": [
    {
      "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf",
      "expectedCertificationType": "OSHA_30"
    },
    {
      "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400001000-forklift.jpg",
      "expectedCertificationType": "FORKLIFT"
    }
  ]
}
```

#### Response (200 OK)
```json
{
  "batchId": "batch_xyz789",
  "results": [
    {
      "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf",
      "status": "success",
      "extractedData": { /* same as single extraction */ }
    },
    {
      "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400001000-forklift.jpg",
      "status": "failed",
      "error": "Low image quality - unable to extract text"
    }
  ],
  "totalDocuments": 2,
  "successCount": 1,
  "failureCount": 1
}
```

#### Implementation Notes
- Process documents in parallel (max 10 concurrent)
- Return partial results if some documents fail
- Maintain order of input array in results
- Implement circuit breaker if >50% of batch fails

---

## Notification Configuration

### 1. Email Notifications (SendGrid)

#### Account Setup
1. Create SendGrid account: https://signup.sendgrid.com/
2. Verify sender domain or email address
3. Generate API key with `Mail Send` permission
4. Configure environment variable: `SENDGRID_API_KEY`

#### Sender Configuration
```
From Email: noreply@hazardhawk.com
From Name: HazardHawk Safety Team
Reply-To: support@hazardhawk.com
```

#### Rate Limits
- **Free Tier**: 100 emails/day
- **Essentials Plan**: 50,000 emails/month
- **Recommended**: Pro Plan (100,000 emails/month)

#### Email Templates

**Template 1: Certification Expiration Warning (90 days)**
```
Subject: [Action Required] Certification Expiring in {{days_until_expiration}} Days

Body:
Hi {{worker_name}},

Your {{certification_type}} certification is expiring soon.

Certificate Details:
- Type: {{certification_type}}
- Certification Number: {{certification_number}}
- Expiration Date: {{expiration_date}}
- Days Remaining: {{days_until_expiration}}

Please renew your certification before {{expiration_date}} to maintain compliance
and continue working on site.

Upload your renewed certification here:
{{upload_link}}

If you have questions, contact your safety manager: {{safety_manager_email}}

Best regards,
HazardHawk Safety Team
```

**Template 2: Certification Expired (0 days)**
```
Subject: [URGENT] Certification Expired - Immediate Action Required

Body:
Hi {{worker_name}},

Your {{certification_type}} certification has expired as of {{expiration_date}}.

IMPORTANT: You cannot work on projects requiring this certification until it is renewed.

Please upload your renewed certification immediately:
{{upload_link}}

Contact your safety manager for assistance: {{safety_manager_email}}

Best regards,
HazardHawk Safety Team
```

**Template 3: Certification Verified**
```
Subject: Certification Approved - {{certification_type}}

Body:
Hi {{worker_name}},

Great news! Your {{certification_type}} certification has been verified and approved.

Certificate Details:
- Type: {{certification_type}}
- Certification Number: {{certification_number}}
- Valid Until: {{expiration_date}}

You can now be assigned to projects requiring this certification.

Best regards,
HazardHawk Safety Team
```

---

### 2. SMS Notifications (Twilio)

#### Account Setup
1. Create Twilio account: https://www.twilio.com/try-twilio
2. Purchase phone number (e.g., +1-555-SAFETY)
3. Configure environment variables:
   - `TWILIO_ACCOUNT_SID`
   - `TWILIO_AUTH_TOKEN`
   - `TWILIO_PHONE_NUMBER`

#### Rate Limits
- **Trial**: 500 messages
- **Recommended**: Standard Plan with at least 1,000 messages/month

#### SMS Templates

**Template 1: Certification Expiring Soon**
```
HazardHawk: Your {{cert_type}} certification expires in {{days}} days ({{exp_date}}).
Renew now: {{short_link}}
```

**Template 2: Certification Expired**
```
HazardHawk URGENT: Your {{cert_type}} certification expired {{exp_date}}.
Upload renewal immediately: {{short_link}}
```

**Character Limits**:
- Single SMS: 160 characters
- Multi-part SMS: 1,600 characters (max)
- Recommend keeping under 160 to avoid multi-part charges

---

### 3. Push Notifications (FCM/APNs)

#### Firebase Cloud Messaging (Android)

**Setup**:
1. Create Firebase project: https://console.firebase.google.com/
2. Add Android app with package name: `com.hazardhawk`
3. Download `google-services.json`
4. Configure environment variable: `FCM_SERVER_KEY`

**Notification Payload**:
```json
{
  "to": "{{fcm_device_token}}",
  "notification": {
    "title": "Certification Expiring Soon",
    "body": "Your OSHA 30 certification expires in 30 days",
    "icon": "ic_notification",
    "color": "#FF5722",
    "sound": "default"
  },
  "data": {
    "type": "certification_expiring",
    "certificationId": "550e8400-e29b-41d4-a716-446655440000",
    "daysUntilExpiration": 30,
    "action": "view_certification"
  }
}
```

#### Apple Push Notification Service (iOS)

**Setup**:
1. Create APNs certificate in Apple Developer Portal
2. Upload certificate to Firebase
3. Configure environment variable: `APNS_AUTH_KEY`

**Notification Payload**:
```json
{
  "aps": {
    "alert": {
      "title": "Certification Expiring Soon",
      "body": "Your OSHA 30 certification expires in 30 days"
    },
    "badge": 1,
    "sound": "default"
  },
  "certificationId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "certification_expiring"
}
```

---

### 4. Notification Service API

#### Send Certification Expiration Alert

**Endpoint**:
```
POST /api/notifications/certification-expiring
```

**Request Body**:
```json
{
  "workerId": "550e8400-e29b-41d4-a716-446655440000",
  "certificationId": "660e8400-e29b-41d4-a716-446655440001",
  "daysUntilExpiration": 30,
  "channels": ["email", "sms", "push"]
}
```

**Response (200 OK)**:
```json
{
  "notificationId": "notif_abc123",
  "deliveryResults": {
    "email": {
      "status": "sent",
      "provider": "sendgrid",
      "messageId": "sg_xyz789"
    },
    "sms": {
      "status": "sent",
      "provider": "twilio",
      "messageId": "SM1234567890"
    },
    "push": {
      "status": "sent",
      "provider": "fcm",
      "messageId": "fcm_abcdef"
    }
  },
  "totalChannels": 3,
  "successfulChannels": 3,
  "failedChannels": 0
}
```

#### Implementation Notes
- Send via all available channels simultaneously
- Track delivery status per channel
- Retry failed deliveries (max 3 attempts with exponential backoff)
- Store notification history in `notification_logs` table (create if needed)
- Respect user notification preferences (allow opt-out per channel)

---

## Infrastructure Requirements

### 1. AWS S3 Configuration

#### Bucket Structure
```
hazardhawk-certifications/
├── certifications/
│   ├── 1728400000000-osha-30.pdf
│   ├── 1728400001000-forklift-cert.jpg
│   └── ...
└── thumbnails/
    ├── 1728400001000-forklift-cert.jpg
    └── ...

hazardhawk-worker-photos/
├── profiles/
│   ├── 1728400000000-john-doe.jpg
│   └── ...
└── thumbnails/
    └── ...
```

#### CORS Configuration
```json
[
  {
    "AllowedOrigins": [
      "https://app.hazardhawk.com",
      "https://hazardhawk.com",
      "http://localhost:3000"
    ],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

#### Lifecycle Policies
```json
{
  "Rules": [
    {
      "Id": "DeleteRejectedCertificationsAfter30Days",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "certifications/rejected/"
      },
      "Expiration": {
        "Days": 30
      }
    },
    {
      "Id": "TransitionOldCertificationsToGlacier",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "certifications/"
      },
      "Transitions": [
        {
          "Days": 365,
          "StorageClass": "GLACIER"
        }
      ]
    }
  ]
}
```

#### Bucket Policies
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCloudFrontAccess",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::cloudfront:user/CloudFront Origin Access Identity {{OAI_ID}}"
      },
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::hazardhawk-certifications/*"
    },
    {
      "Sid": "AllowBackendUpload",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::{{ACCOUNT_ID}}:role/hazardhawk-backend-role"
      },
      "Action": ["s3:PutObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::hazardhawk-certifications/*"
    }
  ]
}
```

---

### 2. CloudFront CDN Configuration

#### Distribution Settings
```
Origin Domain: hazardhawk-certifications.s3.us-east-1.amazonaws.com
Origin Access: Origin Access Identity (OAI)
Viewer Protocol Policy: Redirect HTTP to HTTPS
Allowed HTTP Methods: GET, HEAD
Cached HTTP Methods: GET, HEAD
Cache Policy: CachingOptimized
TTL: Min 0, Max 31536000, Default 86400
Compress Objects: Yes
Price Class: Use All Edge Locations
```

#### Custom Domain (Optional)
```
CNAME: cdn.hazardhawk.com
SSL Certificate: ACM certificate for *.hazardhawk.com
```

#### Cache Behaviors
| Path Pattern | Origin | TTL | Query Strings |
|-------------|--------|-----|---------------|
| `/certifications/*` | S3 | 1 day | None |
| `/thumbnails/*` | S3 | 7 days | None |

---

### 3. Google Cloud Document AI

#### Project Setup
1. Create Google Cloud project
2. Enable Document AI API
3. Create service account with role: `roles/documentai.apiUser`
4. Download service account key JSON
5. Configure environment variable: `GOOGLE_APPLICATION_CREDENTIALS`

#### Custom Processor Configuration

**Processor Type**: Form Parser (pre-trained)

**Custom Fields** (train with sample certifications):
- `holder_name` (person name)
- `certification_type` (classification)
- `certification_number` (alphanumeric)
- `issue_date` (date)
- `expiration_date` (date)
- `issuing_authority` (organization)

**Training Data Requirements**:
- Minimum 50 sample certifications per type
- Include variations: scanned, photographed, digital PDFs
- Rotate images to test orientation handling
- Include poor quality samples for robustness

#### API Quotas
- **Free Tier**: 1,000 pages/month
- **Recommended**: Standard pricing ($0.65 per page for first 1M pages)
- **Rate Limit**: 120 requests/minute per project

#### Environment Variables
```bash
GOOGLE_CLOUD_PROJECT_ID=hazardhawk-production
GOOGLE_DOCUMENT_AI_PROCESSOR_ID=abc123def456
GOOGLE_DOCUMENT_AI_LOCATION=us
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

---

## Background Jobs

### 1. Daily Certification Expiration Check

**Purpose**: Identify certifications expiring soon and send alerts at predefined thresholds.

#### Job Schedule
```
Cron: 0 2 * * * (Daily at 2:00 AM UTC)
Timeout: 30 minutes
Concurrency: 1 (prevent overlapping runs)
```

#### Job Logic (Pseudocode)
```python
def daily_expiration_check():
    thresholds = [90, 60, 30, 14, 7, 3, 0]  # Days before expiration

    for days_until_expiration in thresholds:
        target_date = today() + days(days_until_expiration)

        # Find certifications expiring on target date
        certifications = db.query("""
            SELECT wc.*, wp.email, wp.phone, wp.name, ct.name as cert_type_name
            FROM worker_certifications wc
            JOIN worker_profiles wp ON wc.worker_profile_id = wp.id
            JOIN certification_types ct ON wc.certification_type_id = ct.id
            WHERE wc.expiration_date = :target_date
              AND wc.status = 'verified'
              AND wc.expiration_date IS NOT NULL
        """, target_date=target_date)

        for cert in certifications:
            # Check if alert already sent for this threshold
            if alert_already_sent(cert.id, days_until_expiration):
                continue

            # Send multi-channel notification
            send_notification(
                worker_id=cert.worker_profile_id,
                certification_id=cert.id,
                days_until_expiration=days_until_expiration,
                channels=['email', 'sms', 'push']
            )

            # Mark alert as sent
            mark_alert_sent(cert.id, days_until_expiration)

        # Special handling for expired certifications (day 0)
        if days_until_expiration == 0:
            for cert in certifications:
                # Update status to expired
                db.execute("""
                    UPDATE worker_certifications
                    SET status = 'expired'
                    WHERE id = :cert_id
                """, cert_id=cert.id)

                # Notify safety manager and project manager
                notify_management_team(cert)

                # Remove worker from tasks requiring this certification
                remove_worker_from_tasks_requiring_cert(cert)
```

#### Alert Tracking Table
```sql
CREATE TABLE certification_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certification_id UUID NOT NULL REFERENCES worker_certifications(id) ON DELETE CASCADE,
    days_before_expiration INTEGER NOT NULL,
    alert_sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    channels_used JSONB NOT NULL, -- ["email", "sms", "push"]
    delivery_status JSONB NOT NULL, -- {email: "sent", sms: "failed", push: "sent"}

    UNIQUE(certification_id, days_before_expiration)
);

CREATE INDEX idx_cert_alerts_cert_id ON certification_alerts(certification_id);
CREATE INDEX idx_cert_alerts_sent_at ON certification_alerts(alert_sent_at DESC);
```

#### Job Monitoring
- **Success Metric**: >99% of alerts delivered
- **Alert If**: Job fails 2 consecutive runs
- **Dashboard Metrics**:
  - Certifications expiring per threshold
  - Notification delivery rate per channel
  - Job execution time
  - Failed deliveries (require manual intervention)

---

### 2. Weekly Certification Report

**Purpose**: Send weekly summary to safety managers.

#### Job Schedule
```
Cron: 0 8 * * 1 (Every Monday at 8:00 AM UTC)
```

#### Report Contents
```
Subject: Weekly Certification Report - {{company_name}}

Summary:
- Certifications expiring this week: {{count_expiring_this_week}}
- Certifications expiring this month: {{count_expiring_this_month}}
- Expired certifications: {{count_expired}}
- Pending verification: {{count_pending_verification}}

Action Required:
{{#each pending_verifications}}
  - {{worker_name}}: {{cert_type}} (uploaded {{days_ago}} days ago)
{{/each}}

Expiring This Week:
{{#each expiring_this_week}}
  - {{worker_name}}: {{cert_type}} (expires {{expiration_date}})
{{/each}}

View full report: {{dashboard_link}}
```

---

## Security Requirements

### 1. File Upload Security

#### Virus Scanning
- **Service**: AWS S3 + ClamAV Lambda or Cloud-based scanner (e.g., Sophos, MetaDefender)
- **Trigger**: On S3 PUT event
- **Action**: Quarantine infected files, notify admin, mark certification as rejected

**Lambda Function** (pseudocode):
```python
def scan_uploaded_file(event):
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = event['Records'][0]['s3']['object']['key']

    # Download file to /tmp
    s3.download_file(bucket, key, '/tmp/file')

    # Scan with ClamAV
    result = scan_file('/tmp/file')

    if result.is_infected():
        # Move to quarantine bucket
        s3.copy_object(
            source={'Bucket': bucket, 'Key': key},
            destination={'Bucket': f'{bucket}-quarantine', 'Key': key}
        )
        s3.delete_object(Bucket=bucket, Key=key)

        # Notify admin
        send_email(
            to='security@hazardhawk.com',
            subject='Virus Detected in Upload',
            body=f'File {key} quarantined. Virus: {result.virus_name}'
        )

        # Update database
        db.execute("""
            UPDATE worker_certifications
            SET status = 'rejected',
                rejection_reason = 'Virus detected during upload'
            WHERE document_url LIKE :key
        """, key=f'%{key}%')
```

#### File Validation
```python
ALLOWED_MIME_TYPES = [
    'application/pdf',
    'image/jpeg',
    'image/jpg',
    'image/png'
]

ALLOWED_EXTENSIONS = ['.pdf', '.jpg', '.jpeg', '.png']

MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB

def validate_upload(file_path, content_type):
    # Check MIME type
    if content_type not in ALLOWED_MIME_TYPES:
        raise ValidationError('Invalid file type')

    # Check file extension
    ext = os.path.splitext(file_path)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise ValidationError('Invalid file extension')

    # Check file size
    size = os.path.getsize(file_path)
    if size > MAX_FILE_SIZE:
        raise ValidationError('File too large')

    # Check magic number (prevents extension spoofing)
    magic_number = read_magic_number(file_path)
    if not is_valid_magic_number(magic_number, content_type):
        raise ValidationError('File type mismatch')
```

---

### 2. API Authentication & Authorization

#### JWT Token Validation
```
Header: Authorization: Bearer <JWT_TOKEN>

Token Claims:
{
  "sub": "user_id",
  "company_id": "company_uuid",
  "role": "worker|safety_lead|admin",
  "permissions": ["upload:certification", "view:certification", ...],
  "iat": 1728400000,
  "exp": 1728486400
}
```

#### Permission Matrix
| Endpoint | Worker | Safety Lead | Admin |
|----------|--------|-------------|-------|
| POST /api/storage/presigned-url | ✅ Own files | ✅ All files | ✅ All files |
| DELETE /api/storage/files | ✅ Own files | ✅ All files | ✅ All files |
| POST /api/ocr/extract-certification | ✅ Own certs | ✅ All certs | ✅ All certs |
| POST /api/notifications/* | ❌ No | ✅ Yes | ✅ Yes |

---

### 3. Rate Limiting

#### Global Rate Limits
```
Per IP: 1,000 requests/minute
Per User: 500 requests/minute
Per Company: 2,000 requests/minute
```

#### Endpoint-Specific Limits
| Endpoint | Rate Limit |
|----------|-----------|
| POST /api/storage/presigned-url | 100/min per user |
| POST /api/ocr/extract-certification | 20/min per user |
| POST /api/notifications/* | 10/min per user |

#### Implementation (Redis-based)
```python
def check_rate_limit(user_id, endpoint):
    key = f'rate_limit:{endpoint}:{user_id}'
    count = redis.incr(key)

    if count == 1:
        redis.expire(key, 60)  # 1 minute TTL

    if count > get_limit_for_endpoint(endpoint):
        raise RateLimitExceeded(retry_after=60)
```

---

### 4. Data Encryption

#### In Transit
- **TLS 1.3** for all API endpoints
- **HTTPS only** (redirect HTTP to HTTPS)
- **Certificate**: Let's Encrypt or AWS ACM

#### At Rest
- **S3**: Server-side encryption with AES-256 (SSE-S3)
- **Database**: Transparent Data Encryption (TDE) for PostgreSQL
- **Backup Files**: Encrypted with AWS KMS

---

## API Request/Response Examples

### Example 1: Upload Certification Flow

#### Step 1: Request Presigned URL
```bash
curl -X POST https://api.hazardhawk.com/api/storage/presigned-url \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "bucket": "hazardhawk-certifications",
    "key": "certifications/1728400000000-osha-30.pdf",
    "contentType": "application/pdf",
    "expirationSeconds": 3600,
    "operation": "putObject"
  }'
```

**Response**:
```json
{
  "presignedUrl": "https://hazardhawk-certifications.s3.us-east-1.amazonaws.com/certifications/1728400000000-osha-30.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&...",
  "expiresAt": "2025-10-08T16:00:00Z"
}
```

---

#### Step 2: Upload File to S3
```bash
curl -X PUT "https://hazardhawk-certifications.s3.us-east-1.amazonaws.com/certifications/1728400000000-osha-30.pdf?X-Amz-Algorithm=..." \
  -H "Content-Type: application/pdf" \
  --data-binary @osha-30-certificate.pdf
```

**Response**:
```
HTTP/1.1 200 OK
ETag: "abc123def456"
```

---

#### Step 3: Submit for OCR Processing
```bash
curl -X POST https://api.hazardhawk.com/api/ocr/extract-certification \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf",
    "workerProfileId": "550e8400-e29b-41d4-a716-446655440000",
    "expectedCertificationType": "OSHA_30"
  }'
```

**Response**:
```json
{
  "processingId": "ocr_abc123def456",
  "status": "completed",
  "extractedData": {
    "holderName": "John Doe",
    "certificationType": "OSHA_30",
    "certificationNumber": "12345678",
    "issueDate": "2024-06-15",
    "expirationDate": "2029-06-15",
    "issuingAuthority": "OSHA",
    "confidence": 0.92,
    "needsReview": false
  }
}
```

---

#### Step 4: Save to Database (Backend Logic)
```sql
INSERT INTO worker_certifications (
    worker_profile_id,
    company_id,
    certification_type_id,
    certification_number,
    issue_date,
    expiration_date,
    issuing_authority,
    document_url,
    thumbnail_url,
    status,
    ocr_confidence,
    ocr_metadata
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    '660e8400-e29b-41d4-a716-446655440001',
    (SELECT id FROM certification_types WHERE code = 'OSHA_30'),
    '12345678',
    '2024-06-15',
    '2029-06-15',
    'OSHA',
    'https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf',
    'https://cdn.hazardhawk.com/thumbnails/1728400000000-osha-30.pdf',
    'pending_verification',
    92.00,
    '{"holderName": {"value": "John Doe", "confidence": 0.98}, ...}'::jsonb
);
```

---

### Example 2: Send Expiration Alert

```bash
curl -X POST https://api.hazardhawk.com/api/notifications/certification-expiring \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": "550e8400-e29b-41d4-a716-446655440000",
    "certificationId": "660e8400-e29b-41d4-a716-446655440001",
    "daysUntilExpiration": 30,
    "channels": ["email", "sms"]
  }'
```

**Response**:
```json
{
  "notificationId": "notif_abc123",
  "deliveryResults": {
    "email": {
      "status": "sent",
      "provider": "sendgrid",
      "messageId": "sg_xyz789",
      "sentAt": "2025-10-08T15:30:00Z"
    },
    "sms": {
      "status": "sent",
      "provider": "twilio",
      "messageId": "SM1234567890",
      "sentAt": "2025-10-08T15:30:01Z"
    }
  },
  "totalChannels": 2,
  "successfulChannels": 2,
  "failedChannels": 0
}
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "error": "error_code",
  "message": "Human-readable error message",
  "code": "MACHINE_READABLE_CODE",
  "details": {
    "field": "certification_number",
    "reason": "Field is required"
  },
  "timestamp": "2025-10-08T15:30:00Z",
  "requestId": "req_abc123def456"
}
```

### HTTP Status Codes
| Status | Meaning | When to Use |
|--------|---------|-------------|
| 200 OK | Success | Successful GET, POST, PUT requests |
| 201 Created | Resource created | Successful resource creation |
| 204 No Content | Success (no body) | Successful DELETE requests |
| 400 Bad Request | Invalid input | Validation errors, malformed JSON |
| 401 Unauthorized | Not authenticated | Missing or invalid auth token |
| 403 Forbidden | Not authorized | Valid token but insufficient permissions |
| 404 Not Found | Resource not found | File, certification, or user doesn't exist |
| 409 Conflict | Resource conflict | Duplicate certification upload |
| 413 Payload Too Large | File too large | File exceeds 10MB limit |
| 422 Unprocessable Entity | Validation error | Business logic validation failure |
| 429 Too Many Requests | Rate limit exceeded | User exceeded request quota |
| 500 Internal Server Error | Server error | Unexpected backend error |
| 503 Service Unavailable | Service down | External service (S3, OCR) unavailable |

### Error Codes
```
Authentication:
- INVALID_TOKEN: JWT token invalid or expired
- MISSING_TOKEN: Authorization header missing
- TOKEN_EXPIRED: Token expired, refresh required

Authorization:
- INSUFFICIENT_PERMISSIONS: User lacks required permission
- RESOURCE_NOT_OWNED: User doesn't own resource

Validation:
- MISSING_FIELD: Required field missing
- INVALID_FORMAT: Field format invalid (e.g., email, date)
- FILE_TOO_LARGE: File exceeds size limit
- UNSUPPORTED_FORMAT: File format not supported

Storage:
- S3_UPLOAD_FAILED: S3 upload failed
- FILE_NOT_FOUND: File doesn't exist in S3
- BUCKET_NOT_FOUND: S3 bucket doesn't exist

OCR:
- OCR_PROCESSING_FAILED: Document AI processing failed
- LOW_CONFIDENCE: OCR confidence below threshold
- DOCUMENT_UNREADABLE: Document quality too poor

Notifications:
- EMAIL_SEND_FAILED: SendGrid delivery failed
- SMS_SEND_FAILED: Twilio delivery failed
- PUSH_SEND_FAILED: FCM/APNs delivery failed

Rate Limiting:
- RATE_LIMIT_EXCEEDED: Too many requests
```

---

## Monitoring & Logging

### 1. Application Logs

#### Log Format (JSON)
```json
{
  "timestamp": "2025-10-08T15:30:00.123Z",
  "level": "INFO",
  "service": "certification-service",
  "requestId": "req_abc123def456",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "companyId": "660e8400-e29b-41d4-a716-446655440001",
  "action": "ocr_extraction",
  "message": "OCR processing completed",
  "metadata": {
    "documentUrl": "https://cdn.hazardhawk.com/certifications/1728400000000-osha-30.pdf",
    "confidence": 0.92,
    "processingTimeMs": 2340
  }
}
```

#### Log Levels
- **DEBUG**: Detailed diagnostic information
- **INFO**: General informational messages
- **WARN**: Warning messages (non-critical errors)
- **ERROR**: Error messages (failures requiring attention)
- **FATAL**: Critical errors (service unavailable)

---

### 2. Key Metrics to Track

#### API Performance
- **Request Rate**: Requests per second per endpoint
- **Response Time**: p50, p95, p99 latency per endpoint
- **Error Rate**: 4xx and 5xx errors per endpoint
- **Success Rate**: % of 2xx responses

#### File Upload Metrics
- **Upload Success Rate**: % of successful S3 uploads
- **Average Upload Time**: Time from presigned URL to upload complete
- **File Size Distribution**: Histogram of uploaded file sizes
- **Compression Ratio**: Original size vs compressed size

#### OCR Performance
- **OCR Success Rate**: % of successful extractions
- **Average Confidence Score**: Mean confidence across all extractions
- **Processing Time**: Time per document (by size/format)
- **Low Confidence Rate**: % of extractions with confidence < 0.85

#### Notification Delivery
- **Email Delivery Rate**: % of emails successfully sent
- **SMS Delivery Rate**: % of SMS successfully sent
- **Push Notification Rate**: % of push notifications delivered
- **Channel Preference**: Which channel has highest engagement

#### Background Jobs
- **Job Success Rate**: % of cron jobs completing successfully
- **Job Duration**: Time to complete daily expiration check
- **Certifications Processed**: Number of certifications checked per run
- **Alerts Sent**: Number of expiration alerts sent per threshold

---

### 3. Alerting Rules

#### Critical Alerts (PagerDuty)
```
- API error rate > 5% for 5 minutes
- OCR service down for 2 minutes
- S3 upload success rate < 90% for 10 minutes
- Background job failed 2 consecutive runs
- Database connection pool exhausted
```

#### Warning Alerts (Slack)
```
- API p95 latency > 1 second for 10 minutes
- OCR confidence < 0.7 for > 20% of documents
- Email delivery rate < 95% for 30 minutes
- SMS delivery rate < 90% for 30 minutes
- Disk usage > 80%
```

---

### 4. Dashboards

#### Operations Dashboard (Grafana)
```
- Requests per second (all endpoints)
- Average response time (all endpoints)
- Error rate by status code
- Active database connections
- CPU and memory usage
```

#### Certification Management Dashboard
```
- Certifications uploaded today/week/month
- Certifications pending verification
- Certifications expiring in next 30 days
- Expired certifications count
- OCR extraction success rate
- Average OCR confidence score
```

#### Notification Dashboard
```
- Alerts sent per channel (email, SMS, push)
- Delivery success rate per channel
- Failed deliveries requiring retry
- Cost per channel (SendGrid, Twilio)
```

---

## Implementation Checklist

### Phase 1: Infrastructure Setup (Week 1)
- [ ] Create AWS S3 buckets (`hazardhawk-certifications`, `hazardhawk-worker-photos`)
- [ ] Configure S3 CORS policies
- [ ] Set up S3 lifecycle rules
- [ ] Create CloudFront distribution
- [ ] Configure custom domain for CDN (optional)
- [ ] Enable S3 server-side encryption

### Phase 2: External Services (Week 1)
- [ ] Create SendGrid account and verify sender
- [ ] Generate SendGrid API key
- [ ] Create email templates in SendGrid
- [ ] Create Twilio account and purchase phone number
- [ ] Generate Twilio credentials
- [ ] Create Firebase project for FCM
- [ ] Upload APNs certificate to Firebase

### Phase 3: Google Document AI (Week 1-2)
- [ ] Create Google Cloud project
- [ ] Enable Document AI API
- [ ] Create service account and download key
- [ ] Create custom processor for certifications
- [ ] Train processor with 50+ sample certifications
- [ ] Test processor accuracy (target >85%)

### Phase 4: Backend API Development (Week 2)
- [ ] Implement `/api/storage/presigned-url` endpoint
- [ ] Implement `/api/storage/files` DELETE endpoint
- [ ] Implement `/api/storage/files` HEAD endpoint
- [ ] Implement `/api/ocr/extract-certification` endpoint
- [ ] Implement `/api/ocr/validate-document` endpoint
- [ ] Implement `/api/ocr/batch-extract` endpoint
- [ ] Implement `/api/notifications/certification-expiring` endpoint
- [ ] Add authentication middleware (JWT validation)
- [ ] Add authorization middleware (permission checks)
- [ ] Implement rate limiting (Redis-based)

### Phase 5: Background Jobs (Week 2)
- [ ] Implement daily expiration check cron job
- [ ] Create `certification_alerts` table
- [ ] Implement alert deduplication logic
- [ ] Test job with mock data (all thresholds)
- [ ] Set up job monitoring and alerting

### Phase 6: Security (Week 2)
- [ ] Implement virus scanning Lambda function
- [ ] Configure S3 event notifications for new uploads
- [ ] Implement file validation (MIME type, size, magic number)
- [ ] Set up API rate limiting
- [ ] Configure TLS 1.3 for all endpoints
- [ ] Enable database encryption at rest
- [ ] Conduct security audit and penetration test

### Phase 7: Testing (Week 3)
- [ ] Write unit tests for all endpoints (>80% coverage)
- [ ] Write integration tests for OCR flow
- [ ] Write integration tests for notification flow
- [ ] Load test API endpoints (target: 1,000 req/s)
- [ ] Test background job with 10,000+ certifications
- [ ] Test rate limiting with concurrent users

### Phase 8: Monitoring & Deployment (Week 3)
- [ ] Set up application logging (JSON format)
- [ ] Configure log aggregation (CloudWatch, Loki, etc.)
- [ ] Create Grafana dashboards
- [ ] Set up PagerDuty alerting
- [ ] Configure Slack notifications
- [ ] Deploy to staging environment
- [ ] Run end-to-end tests in staging
- [ ] Deploy to production with feature flags
- [ ] Monitor for 24 hours before full rollout

---

## Support & Troubleshooting

### Common Issues

#### Issue 1: Presigned URL Upload Fails
**Symptoms**: Client receives 403 Forbidden when uploading to presigned URL

**Causes**:
- CORS misconfiguration
- URL expired
- Content-Type mismatch
- Bucket policy doesn't allow upload

**Solutions**:
```bash
# Check CORS configuration
aws s3api get-bucket-cors --bucket hazardhawk-certifications

# Verify bucket policy
aws s3api get-bucket-policy --bucket hazardhawk-certifications

# Test presigned URL
curl -X PUT "<presigned-url>" -H "Content-Type: application/pdf" --data-binary @test.pdf
```

---

#### Issue 2: OCR Extraction Low Confidence
**Symptoms**: `needsReview: true` for >50% of documents

**Causes**:
- Poor image quality (low resolution, blurry)
- Handwritten text
- Processor not trained for this certification type

**Solutions**:
- Guide users to capture high-quality images (min 300 DPI)
- Improve lighting and focus in camera UI
- Add more training samples to Document AI processor
- Use multi-page PDFs instead of photos when possible

---

#### Issue 3: Notification Delivery Failures
**Symptoms**: `deliveryResults.email.status = "failed"`

**Causes**:
- SendGrid API key invalid or expired
- Email address bounced or unsubscribed
- Rate limit exceeded

**Solutions**:
```bash
# Verify SendGrid API key
curl -H "Authorization: Bearer $SENDGRID_API_KEY" https://api.sendgrid.com/v3/user/profile

# Check bounce list
curl -H "Authorization: Bearer $SENDGRID_API_KEY" https://api.sendgrid.com/v3/suppression/bounces

# Retry failed deliveries
POST /api/notifications/retry
{
  "notificationId": "notif_abc123"
}
```

---

## Conclusion

This document provides comprehensive specifications for backend API integration required for Phase 2 Certification Management. All client-side services have been implemented and are awaiting backend endpoints.

**Next Steps**:
1. Backend team reviews this document
2. Backend team estimates implementation timeline (recommended: 2-3 weeks)
3. Infrastructure team provisions AWS, Google Cloud, SendGrid, and Twilio resources
4. Backend team implements endpoints following specifications
5. Integration testing with client-side services
6. Security audit and load testing
7. Production deployment

**Questions or Clarifications**:
Contact the frontend development team or refer to client-side service implementations in:
- `/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/FileUploadService.kt`
- `/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/OCRService.kt`
- `/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/NotificationService.kt`

---

**Document Version**: 1.0
**Last Updated**: October 8, 2025
**Maintained By**: HazardHawk Development Team
**Review Cycle**: Monthly or after major API changes
