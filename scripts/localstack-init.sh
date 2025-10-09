#!/bin/bash

# ============================================================================
# Localstack Initialization Script
# Sets up S3 buckets, SES emails, and other AWS resources for testing
# ============================================================================

set -e

echo "=========================================="
echo "Initializing Localstack for HazardHawk Phase 2 Testing"
echo "=========================================="

# Wait for Localstack to be ready
echo "Waiting for Localstack to be ready..."
until curl -s http://localhost:4566/_localstack/health | grep -q "running"; do
  echo "  Still waiting..."
  sleep 2
done
echo "✓ Localstack is ready!"

# ============================================================================
# S3 Configuration
# ============================================================================
echo ""
echo "Setting up S3 buckets..."

# Create certification documents bucket
awslocal s3 mb s3://hazardhawk-test-certifications
echo "  ✓ Created bucket: hazardhawk-test-certifications"

# Create general documents bucket
awslocal s3 mb s3://hazardhawk-test-documents
echo "  ✓ Created bucket: hazardhawk-test-documents"

# Create PTP documents bucket
awslocal s3 mb s3://hazardhawk-test-ptp
echo "  ✓ Created bucket: hazardhawk-test-ptp"

# Configure CORS for certification bucket
awslocal s3api put-bucket-cors \
  --bucket hazardhawk-test-certifications \
  --cors-configuration '{
    "CORSRules": [
      {
        "AllowedOrigins": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
        "AllowedHeaders": ["*"],
        "ExposeHeaders": ["ETag"],
        "MaxAgeSeconds": 3000
      }
    ]
  }'
echo "  ✓ Configured CORS for certifications bucket"

# Configure CORS for documents bucket
awslocal s3api put-bucket-cors \
  --bucket hazardhawk-test-documents \
  --cors-configuration '{
    "CORSRules": [
      {
        "AllowedOrigins": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
        "AllowedHeaders": ["*"],
        "ExposeHeaders": ["ETag"],
        "MaxAgeSeconds": 3000
      }
    ]
  }'
echo "  ✓ Configured CORS for documents bucket"

# Upload test certification files
echo ""
echo "Uploading test certification files..."

# Create a temporary test file
cat > /tmp/test-osha10.txt << 'CERTEOF'
OSHA 10-Hour Construction Safety Certification

Certificate Holder: John Doe
Certificate Number: OSHA10-2025-123456
Completion Date: January 15, 2025
Expiration Date: Never (awareness training)
Training Provider: Safety First Training Center
Instructor: Jane Smith

This certifies that the above-named individual has successfully completed
the OSHA 10-Hour Construction Safety Training program.

Topics covered:
- Fall Protection
- Electrical Safety
- Personal Protective Equipment
- Hazard Communication
- Materials Handling
- Scaffolding Safety
- Ladder Safety
- Stairways and Ladders
- Tools - Hand and Power
- Health Hazards in Construction
CERTEOF

awslocal s3 cp /tmp/test-osha10.txt s3://hazardhawk-test-certifications/samples/osha10.txt
echo "  ✓ Uploaded sample OSHA-10 certification"

# Create a test OSHA-30 file
cat > /tmp/test-osha30.txt << 'CERTEOF'
OSHA 30-Hour Construction Safety Certification

Certificate Holder: Jane Smith
Certificate Number: OSHA30-2025-789012
Completion Date: February 1, 2025
Expiration Date: Never (awareness training)
Training Provider: Construction Safety Institute
Instructor: Bob Johnson

This certifies that the above-named individual has successfully completed
the OSHA 30-Hour Construction Safety Training program for supervisors.
CERTEOF

awslocal s3 cp /tmp/test-osha30.txt s3://hazardhawk-test-certifications/samples/osha30.txt
echo "  ✓ Uploaded sample OSHA-30 certification"

# List buckets to confirm
echo ""
echo "S3 Buckets created:"
awslocal s3 ls

# ============================================================================
# SES Configuration (for email notifications)
# ============================================================================
echo ""
echo "Setting up SES (Simple Email Service)..."

# Verify email addresses for testing
awslocal ses verify-email-identity --email-address test@hazardhawk.com
awslocal ses verify-email-identity --email-address admin@hazardhawk.com
awslocal ses verify-email-identity --email-address worker@hazardhawk.com

echo "  ✓ Verified email: test@hazardhawk.com"
echo "  ✓ Verified email: admin@hazardhawk.com"
echo "  ✓ Verified email: worker@hazardhawk.com"

# ============================================================================
# SNS Configuration (for push notifications)
# ============================================================================
echo ""
echo "Setting up SNS (Simple Notification Service)..."

# Create SNS topic for certification expiration alerts
CERT_EXPIRY_TOPIC=$(awslocal sns create-topic \
  --name hazardhawk-certification-expiring \
  --query 'TopicArn' \
  --output text)

echo "  ✓ Created SNS topic: $CERT_EXPIRY_TOPIC"

# Create SNS topic for safety alerts
SAFETY_ALERT_TOPIC=$(awslocal sns create-topic \
  --name hazardhawk-safety-alerts \
  --query 'TopicArn' \
  --output text)

echo "  ✓ Created SNS topic: $SAFETY_ALERT_TOPIC"

# Subscribe a test endpoint
awslocal sns subscribe \
  --topic-arn "$CERT_EXPIRY_TOPIC" \
  --protocol email \
  --notification-endpoint test@hazardhawk.com

echo "  ✓ Subscribed test@hazardhawk.com to certification alerts"

# ============================================================================
# SQS Configuration (for background jobs)
# ============================================================================
echo ""
echo "Setting up SQS (Simple Queue Service)..."

# Create queue for OCR processing jobs
OCR_QUEUE=$(awslocal sqs create-queue \
  --queue-name hazardhawk-ocr-processing \
  --query 'QueueUrl' \
  --output text)

echo "  ✓ Created SQS queue: $OCR_QUEUE"

# Create queue for notification jobs
NOTIFICATION_QUEUE=$(awslocal sqs create-queue \
  --queue-name hazardhawk-notifications \
  --query 'QueueUrl' \
  --output text)

echo "  ✓ Created SQS queue: $NOTIFICATION_QUEUE"

# ============================================================================
# Summary
# ============================================================================
echo ""
echo "=========================================="
echo "✓ Localstack initialization complete!"
echo "=========================================="
echo ""
echo "Resources created:"
echo "  • 3 S3 buckets (certifications, documents, ptp)"
echo "  • 3 verified SES email addresses"
echo "  • 2 SNS topics (certification-expiring, safety-alerts)"
echo "  • 2 SQS queues (ocr-processing, notifications)"
echo ""
echo "Access endpoints:"
echo "  • S3:  http://localhost:4566"
echo "  • SES: http://localhost:4566"
echo "  • SNS: http://localhost:4566"
echo "  • SQS: http://localhost:4566"
echo ""
echo "Credentials (for testing):"
echo "  • Access Key: test"
echo "  • Secret Key: test"
echo "  • Region: us-east-1"
echo ""
echo "=========================================="
