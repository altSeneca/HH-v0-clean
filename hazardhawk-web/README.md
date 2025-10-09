# HazardHawk Web Certification Portal

A Next.js web application for uploading and verifying construction worker safety certifications. Designed to complement the HazardHawk Android app with browser-based access for workers who don't have the app installed.

## Features

### For Workers
- 📱 **QR Code Access** - Scan QR code from safety lead to access upload portal
- 📷 **Camera Capture** - Take photos of certifications using web camera
- 📄 **File Upload** - Upload certification images or PDFs (max 10MB)
- 🤖 **AI OCR** - Automatic extraction of certification data using Google Document AI
- 🏛️ **NYC DOB Integration** - Verify SST certifications with NYC Department of Buildings
- ✏️ **Manual Entry** - Edit or manually enter certification details
- ✅ **Instant Feedback** - Know immediately when upload is successful

### For Admins
- 📊 **Dashboard** - View statistics on pending, approved, and rejected certifications
- 📋 **Verification Queue** - Review all pending certifications in one place
- 🔍 **Document Viewer** - Zoom, pan, and rotate uploaded documents
- ⌨️ **Keyboard Shortcuts** - Approve (A), Reject (R), Skip (S), Navigate (←→)
- 🎯 **Filtering & Sorting** - Find certifications by worker, type, or confidence score
- 💬 **Rejection Reasons** - Provide clear feedback to workers

## Tech Stack

- **Framework**: Next.js 15.5 (App Router)
- **Language**: TypeScript 5.9 (strict mode)
- **Styling**: Tailwind CSS 4.1
- **State**: Zustand + TanStack Query
- **Forms**: React Hook Form + Zod
- **Animations**: Framer Motion
- **File Upload**: AWS S3 presigned URLs
- **Camera**: react-webcam
- **QR Codes**: html5-qrcode + qrcode

## Getting Started

### Prerequisites
- Node.js 18+
- npm or yarn
- Backend API running (see API Integration section)

### Installation

```bash
# Clone the repository
cd hazardhawk-web

# Install dependencies
npm install

# Set up environment variables
cp .env.local.example .env.local
# Edit .env.local with your API endpoints and credentials

# Run development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build for Production

```bash
# Build the application
npm run build

# Start production server
npm start
```

## Project Structure

```
hazardhawk-web/
├── src/
│   ├── app/              # Next.js App Router pages
│   ├── components/       # React components
│   ├── lib/             # Utilities, hooks, API clients
│   ├── types/           # TypeScript type definitions
│   └── styles/          # Global CSS
├── public/              # Static assets
├── .env.local           # Environment variables
└── package.json         # Dependencies
```

## Environment Variables

Create a `.env.local` file:

```bash
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000

# AWS S3
NEXT_PUBLIC_S3_BUCKET=hazardhawk-certifications
NEXT_PUBLIC_S3_REGION=us-east-1

# NYC DOB Training Connect
NEXT_PUBLIC_DOB_API_URL=https://dob-trainingconnect.cityofnewyork.us/api
DOB_API_KEY=your-api-key-here

# Google Document AI
GOOGLE_DOCUMENT_AI_PROJECT_ID=your-project-id
GOOGLE_DOCUMENT_AI_LOCATION=us
GOOGLE_DOCUMENT_AI_PROCESSOR_ID=your-processor-id
```

## Routes

| Route | Description |
|-------|-------------|
| `/` | Home page (redirects to upload) |
| `/upload` | Worker certification upload form |
| `/upload/[sessionId]` | QR code scanner landing page |
| `/test-qr` | Test QR code generator (dev tool) |
| `/admin/verify` | Admin verification dashboard |

## API Integration

The web app expects these backend endpoints:

### File Upload
```
POST /api/storage/presigned-url
Body: { bucket, key, contentType }
Response: { uploadUrl, fileUrl, key }
```

### OCR Processing
```
POST /api/ocr/extract-certification
Body: { documentUrl }
Response: { holderName, certificationType, certificationNumber, ... }
```

### Certification Management
```
POST /api/certifications
GET  /api/certifications/pending
POST /api/certifications/:id/approve
POST /api/certifications/:id/reject
```

See `/src/types/api.ts` for complete type definitions.

## Development

### Available Scripts

```bash
npm run dev      # Start development server
npm run build    # Build for production
npm run start    # Start production server
npm run lint     # Run ESLint
npm run test     # Run tests (Vitest)
npm run test:e2e # Run E2E tests (Playwright)
```

### Testing

```bash
# Unit tests
npm run test

# E2E tests
npm run test:e2e

# Test QR code workflow
npm run dev
# Visit http://localhost:3000/test-qr
# Generate a QR code
# Scan it at http://localhost:3000/upload/scan
```

## Mobile Support

The application is fully responsive and works on:
- iOS Safari (iOS 14+)
- Chrome Mobile (Android 8+)
- Desktop browsers (Chrome, Safari, Firefox)

### Camera Access
Camera features require HTTPS in production (browsers require secure context for camera API).

## Deployment

### Vercel (Recommended)
```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel
```

### Docker
```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
```

### AWS Amplify / Other Platforms
Follow standard Next.js deployment guidelines.

## NYC DOB Integration

### SST Card Verification
The app can verify NYC Site Safety Training (SST) cards by:
1. Scanning SST number from card via OCR
2. Looking up in NYC DOB Training Connect database
3. Auto-populating worker info if found
4. Displaying certification status (valid/expired/revoked)

### API Requirements
- NYC DOB API key (apply at https://dob-trainingconnect.cityofnewyork.us)
- Authorized IP addresses (whitelist your server)
- Rate limits apply (implement caching)

## Security

- All API requests use HTTPS
- File uploads validated (type, size)
- S3 presigned URLs expire after 15 minutes
- Input validation with Zod schemas
- CORS configured for API
- XSS protection (React escapes by default)

## Performance

- **Build size**: ~200 kB (largest route)
- **First Load JS**: ~234 kB (with code splitting)
- **Static pages**: 5/6 routes pre-rendered
- **Image compression**: Client-side (before upload)
- **Caching**: TanStack Query (60s stale time)

## Browser Compatibility

| Browser | Version | Camera | File Upload | QR Scan |
|---------|---------|--------|-------------|---------|
| Chrome  | 90+     | ✅     | ✅          | ✅      |
| Safari  | 14+     | ✅     | ✅          | ✅      |
| Firefox | 88+     | ✅     | ✅          | ✅      |
| iOS Safari | 14+ | ✅     | ✅          | ✅      |
| Chrome Mobile | 90+ | ✅   | ✅          | ✅      |

## Troubleshooting

### Camera not working
- Ensure HTTPS (required for camera API in production)
- Check browser permissions
- Try different browser

### File upload fails
- Check file size (max 10MB)
- Verify S3 bucket permissions
- Check CORS configuration

### QR code won't scan
- Ensure good lighting
- Hold camera steady
- Try generating a new QR code at `/test-qr`

### Build errors
```bash
# Clear Next.js cache
rm -rf .next

# Clear node_modules
rm -rf node_modules
npm install

# Rebuild
npm run build
```

## Support

For issues or questions:
1. Check the implementation log: `/docs/implementation/20251009-071400-web-certification-portal-implementation.md`
2. Review the API documentation: `/src/types/api.ts`
3. Open an issue in the main HazardHawk repository

## License

Proprietary - HazardHawk Construction Safety Platform

## Related Documentation

- [Implementation Log](../docs/implementation/20251009-071400-web-certification-portal-implementation.md)
- [Original Plan](../docs/plan/20251008-210500-web-certification-upload-plan.md)
- [Backend API Requirements](../docs/implementation/phase2-backend-api-requirements.md)
