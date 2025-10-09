'use client';

/**
 * Certification Upload Form Page
 * Receives worker/project data from QR code scan via URL parameters
 */

import { Suspense, useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { UploadWizard } from '@/components/certifications/upload-wizard';
import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

function UploadPageContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [mounted, setMounted] = useState(false);

  // Extract QR data from URL parameters
  const workerId = searchParams.get('workerId');
  const projectId = searchParams.get('projectId');
  const companyId = searchParams.get('companyId');
  const sessionId = searchParams.get('sessionId');
  const workerName = searchParams.get('workerName');

  useEffect(() => {
    setMounted(true);
  }, []);

  const handleComplete = (certificationId: string) => {
    console.log('Upload complete:', certificationId);
    // Redirect to scanner page with success message
    router.push(`/upload/${sessionId}?success=true&certId=${certificationId}`);
  };

  const handleCancel = () => {
    // Return to scanner page
    router.push(`/upload/${sessionId}`);
  };

  if (!mounted) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-gray-300 border-t-blue-600"></div>
      </div>
    );
  }

  // Validate required parameters
  if (!workerId || !projectId || !companyId || !sessionId) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg p-8">
          <div className="flex flex-col items-center text-center">
            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mb-6">
              <AlertCircle className="w-10 h-10 text-red-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-3">Invalid Upload Link</h2>
            <p className="text-gray-600 mb-6">
              The upload link is missing required information. Please scan the QR code again.
            </p>
            <Button
              onClick={() => router.push(`/upload/${sessionId || 'start'}`)}
              size="lg"
              className="w-full"
            >
              Scan QR Code
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <UploadWizard
        workerId={workerId}
        projectId={projectId}
        workerName={workerName || undefined}
        onComplete={handleComplete}
        onCancel={handleCancel}
      />
    </div>
  );
}

export default function UploadPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-gray-300 border-t-blue-600"></div>
        </div>
      }
    >
      <UploadPageContent />
    </Suspense>
  );
}
