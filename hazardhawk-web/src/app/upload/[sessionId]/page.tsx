'use client';

/**
 * Certification Upload Landing Page with QR Code Scanner
 * Dynamic route: /upload/[sessionId]
 *
 * Flow:
 * 1. Worker opens this page (can be any sessionId initially)
 * 2. QR scanner loads and prompts for camera access
 * 3. After successful scan, extracts worker/project data
 * 4. Transitions to upload wizard with pre-filled context
 */

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { QRScanner } from '@/components/certifications/qr-scanner';
import { WorkerQRData } from '@/types/certification';

type PageState =
  | { type: 'scanning' }
  | { type: 'scanned'; data: WorkerQRData }
  | { type: 'transitioning' }
  | { type: 'error'; message: string };

export default function UploadSessionPage() {
  const router = useRouter();
  const [state, setState] = useState<PageState>({ type: 'scanning' });
  const [mounted, setMounted] = useState(false);

  // Handle client-side mounting
  useEffect(() => {
    setMounted(true);
  }, []);

  // Handle successful QR code scan
  const handleScanSuccess = async (data: WorkerQRData) => {
    console.log('QR scan successful:', data);
    setState({ type: 'scanned', data });

    // Wait a moment to show success state
    setTimeout(() => {
      setState({ type: 'transitioning' });

      // Redirect to upload wizard with query parameters
      // We pass the worker/project data via URL params for the upload form
      const uploadUrl = new URL('/upload', window.location.origin);
      uploadUrl.searchParams.set('workerId', data.workerId);
      uploadUrl.searchParams.set('projectId', data.projectId);
      uploadUrl.searchParams.set('companyId', data.companyId);
      uploadUrl.searchParams.set('sessionId', data.uploadSessionId);
      if (data.workerName) {
        uploadUrl.searchParams.set('workerName', data.workerName);
      }

      // Navigate to upload wizard
      router.push(uploadUrl.pathname + uploadUrl.search);
    }, 1500);
  };

  // Handle scan error
  const handleScanError = (error: string) => {
    console.error('QR scan error:', error);
    setState({ type: 'error', message: error });
  };

  // Don't render until mounted (avoid hydration issues)
  if (!mounted) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-gray-300 border-t-blue-600"></div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-4xl mx-auto px-4 py-6">
          <div className="flex items-center gap-3">
            {/* HazardHawk Logo/Icon */}
            <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
              <svg
                className="w-8 h-8 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">HazardHawk</h1>
              <p className="text-sm text-gray-600">Certification Upload Portal</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Welcome Section */}
        {state.type === 'scanning' && (
          <>
            <div className="mb-8 text-center">
              <h2 className="text-3xl font-bold text-gray-900 mb-3">
                Upload Your Certification
              </h2>
              <p className="text-lg text-gray-600 max-w-2xl mx-auto">
                Scan the QR code provided by your safety lead to begin uploading your
                certification documents.
              </p>
            </div>

            {/* QR Scanner */}
            <div className="bg-white rounded-xl shadow-lg p-6 mb-8">
              <QRScanner
                onScanSuccess={handleScanSuccess}
                onScanError={handleScanError}
              />
            </div>

            {/* Info Cards */}
            <div className="grid md:grid-cols-2 gap-6 mb-8">
              <div className="bg-blue-50 border-2 border-blue-200 rounded-lg p-6">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
                    <svg
                      className="w-6 h-6 text-white"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                      />
                    </svg>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">Need a QR Code?</h3>
                    <p className="text-sm text-gray-700">
                      Ask your safety lead or project manager to generate a QR code for you
                      from the HazardHawk mobile app.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-green-50 border-2 border-green-200 rounded-lg p-6">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 bg-green-600 rounded-lg flex items-center justify-center flex-shrink-0">
                    <svg
                      className="w-6 h-6 text-white"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                      />
                    </svg>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">Secure Upload</h3>
                    <p className="text-sm text-gray-700">
                      Your certification documents are encrypted and securely stored in
                      compliance with industry standards.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

        {/* Success State */}
        {state.type === 'scanned' && (
          <div className="bg-white rounded-xl shadow-lg p-8 text-center">
            <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <svg
                className="w-12 h-12 text-green-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-3">QR Code Scanned!</h2>
            {state.data.workerName && (
              <p className="text-lg text-gray-600 mb-2">
                Welcome, <span className="font-semibold">{state.data.workerName}</span>
              </p>
            )}
            <p className="text-gray-500">Preparing upload form...</p>
          </div>
        )}

        {/* Transitioning State */}
        {state.type === 'transitioning' && (
          <div className="bg-white rounded-xl shadow-lg p-8 text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-4 border-gray-200 border-t-blue-600 mx-auto mb-6"></div>
            <h2 className="text-2xl font-bold text-gray-900 mb-3">Loading Upload Form</h2>
            <p className="text-gray-600">Please wait...</p>
          </div>
        )}

        {/* Error State */}
        {state.type === 'error' && (
          <div className="bg-white rounded-xl shadow-lg p-8">
            <div className="text-center mb-6">
              <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
                <svg
                  className="w-12 h-12 text-red-600"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-3">Scan Failed</h2>
              <p className="text-red-600 mb-6">{state.message}</p>
              <button
                onClick={() => setState({ type: 'scanning' })}
                className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow transition-colors"
              >
                Try Again
              </button>
            </div>
          </div>
        )}

        {/* Footer Help */}
        <div className="mt-8 text-center text-sm text-gray-500">
          <p>
            Having trouble? Contact your safety lead or email{' '}
            <a href="mailto:support@hazardhawk.com" className="text-blue-600 hover:underline">
              support@hazardhawk.com
            </a>
          </p>
        </div>
      </div>
    </main>
  );
}
