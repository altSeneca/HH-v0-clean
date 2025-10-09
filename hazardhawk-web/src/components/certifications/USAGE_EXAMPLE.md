# Certification Components Usage Example

This document demonstrates how to use the OCR review, manual entry, and DOB verification components together.

## Component Flow

1. **Upload Document** → Process with OCR
2. **OCR Review** → Display extracted data with confidence scores
3. **DOB Verification** (optional) → Verify with NYC DOB Training Connect
4. **Manual Entry** → Edit or manually enter certification details
5. **Submit** → Save certification to database

## Example Implementation

```tsx
'use client';

import { useState } from 'react';
import { useOCR } from '@/lib/hooks/use-ocr';
import { useDOBLookup } from '@/lib/hooks/use-dob-lookup';
import { OCRReview, ManualEntryForm, DOBVerification } from '@/components/certifications';
import { certificationsApi } from '@/lib/api/certifications';
import type { CertificationFormValues } from '@/lib/schemas/certification';
import type { OCRReviewData } from '@/types/certification';

type Step = 'ocr-review' | 'dob-verification' | 'manual-entry' | 'success';

export function CertificationUploadFlow({ workerId, documentUrl }: { workerId: string; documentUrl: string }) {
  const [step, setStep] = useState<Step>('ocr-review');
  const [ocrData, setOcrData] = useState<OCRReviewData | null>(null);
  const [prefillData, setPrefillData] = useState<Partial<CertificationFormValues>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { extractOCR, isLoading: isOCRLoading } = useOCR();

  // Load OCR data on mount
  useEffect(() => {
    const loadOCR = async () => {
      const result = await extractOCR(documentUrl);
      if (result) {
        setOcrData({
          holderName: result.holderName,
          certificationType: result.certificationType,
          certificationNumber: result.certificationNumber,
          expirationDate: result.expirationDate,
          issueDate: result.issueDate,
          confidence: result.confidence,
          documentUrl,
        });
      }
    };
    loadOCR();
  }, [documentUrl, extractOCR]);

  // Handle OCR confirmation
  const handleOCRConfirm = async () => {
    if (!ocrData) return;

    setIsSubmitting(true);
    try {
      await certificationsApi.submitCertification({
        workerId,
        holderName: ocrData.holderName,
        certificationType: ocrData.certificationType,
        certificationNumber: ocrData.certificationNumber,
        expirationDate: ocrData.expirationDate,
        issueDate: ocrData.issueDate,
        documentUrl,
        ocrConfidence: ocrData.confidence,
        submittedVia: 'web',
      });
      setStep('success');
    } catch (error) {
      console.error('Submission failed:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle edit from OCR review
  const handleOCREdit = () => {
    if (ocrData) {
      setPrefillData({
        workerId,
        holderName: ocrData.holderName,
        certificationType: ocrData.certificationType,
        certificationNumber: ocrData.certificationNumber,
        expirationDate: ocrData.expirationDate,
        issueDate: ocrData.issueDate,
      });
    }
    setStep('dob-verification');
  };

  // Handle DOB worker found
  const handleDOBWorkerFound = (data: Partial<CertificationFormValues>) => {
    setPrefillData({ ...prefillData, ...data });
    setStep('manual-entry');
  };

  // Handle DOB skip
  const handleDOBSkip = () => {
    setStep('manual-entry');
  };

  // Handle manual form submission
  const handleManualSubmit = async (data: CertificationFormValues) => {
    setIsSubmitting(true);
    try {
      await certificationsApi.submitCertification({
        ...data,
        documentUrl,
        ocrConfidence: ocrData?.confidence || 0,
        submittedVia: 'web',
      });
      setStep('success');
    } catch (error) {
      console.error('Submission failed:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Render appropriate step
  if (isOCRLoading) {
    return <div>Processing document...</div>;
  }

  if (step === 'ocr-review' && ocrData) {
    return (
      <OCRReview
        data={ocrData}
        onConfirm={handleOCRConfirm}
        onEdit={handleOCREdit}
        isSubmitting={isSubmitting}
      />
    );
  }

  if (step === 'dob-verification') {
    return (
      <DOBVerification
        onWorkerFound={handleDOBWorkerFound}
        onSkip={handleDOBSkip}
      />
    );
  }

  if (step === 'manual-entry') {
    return (
      <ManualEntryForm
        workerId={workerId}
        prefill={prefillData}
        onSubmit={handleManualSubmit}
        onCancel={() => setStep('ocr-review')}
        isSubmitting={isSubmitting}
      />
    );
  }

  if (step === 'success') {
    return <div>Certification submitted successfully!</div>;
  }

  return null;
}
```

## Individual Component Usage

### OCR Review Component

```tsx
import { OCRReview } from '@/components/certifications';

<OCRReview
  data={{
    holderName: "John Doe",
    certificationType: "osha-30",
    certificationNumber: "ABC-123456",
    expirationDate: "2025-12-31",
    issueDate: "2024-01-15",
    confidence: 92,
    documentUrl: "https://s3.../cert.pdf"
  }}
  onConfirm={() => console.log('Confirmed')}
  onEdit={() => console.log('Edit clicked')}
  isSubmitting={false}
/>
```

### DOB Verification Component

```tsx
import { DOBVerification } from '@/components/certifications';

<DOBVerification
  onWorkerFound={(data) => console.log('Worker found:', data)}
  onSkip={() => console.log('Skipped')}
/>
```

### Manual Entry Form Component

```tsx
import { ManualEntryForm } from '@/components/certifications';

<ManualEntryForm
  workerId="worker-123"
  prefill={{
    holderName: "John Doe",
    certificationType: "osha-30"
  }}
  onSubmit={(data) => console.log('Form submitted:', data)}
  onCancel={() => console.log('Cancelled')}
  isSubmitting={false}
/>
```

## Custom Hooks Usage

### useOCR Hook

```tsx
import { useOCR } from '@/lib/hooks/use-ocr';

function MyComponent() {
  const { extractOCR, isLoading, error, data } = useOCR();

  const handleExtract = async () => {
    const result = await extractOCR('https://s3.../document.pdf');
    if (result) {
      console.log('OCR Data:', result);
    }
  };

  return (
    <div>
      <button onClick={handleExtract} disabled={isLoading}>
        {isLoading ? 'Processing...' : 'Extract OCR'}
      </button>
      {error && <p>Error: {error}</p>}
      {data && <pre>{JSON.stringify(data, null, 2)}</pre>}
    </div>
  );
}
```

### useDOBLookup Hook

```tsx
import { useDOBLookup } from '@/lib/hooks/use-dob-lookup';

function MyComponent() {
  const { lookupWorker, isLoading, data, isAvailable } = useDOBLookup();

  const handleLookup = async () => {
    const result = await lookupWorker({ sst_number: '12345678' });
    if (result?.found) {
      console.log('Worker certifications:', result.certifications);
    }
  };

  return (
    <div>
      <button onClick={handleLookup} disabled={isLoading || !isAvailable}>
        Lookup Worker
      </button>
      {!isAvailable && <p>DOB API is unavailable</p>}
      {data?.found && <p>Found {data.certifications.length} certifications</p>}
    </div>
  );
}
```

## Form Validation

All forms use Zod schemas from `@/lib/schemas/certification`:

```tsx
import { certificationFormSchema, dobLookupSchema } from '@/lib/schemas/certification';

// Validate certification form data
const result = certificationFormSchema.safeParse(formData);
if (result.success) {
  // Data is valid
  console.log(result.data);
} else {
  // Show errors
  console.error(result.error);
}
```

## Accessibility Features

All components include:
- ARIA labels and descriptions
- Keyboard navigation support
- Screen reader announcements
- Error message associations
- Focus management
- Touch-friendly inputs (large targets, mobile-optimized date pickers)

## Error Handling

- **OCR failures**: Hook returns null and sets error message
- **DOB API unavailable**: Graceful degradation, allows workflow to continue
- **Form validation**: Real-time error messages with clear feedback
- **Network errors**: User-friendly error messages, retry options

## Styling

All components use Tailwind CSS with:
- Responsive design (mobile-first)
- Framer Motion animations
- High contrast for accessibility
- Construction-friendly UI (large touch targets)
