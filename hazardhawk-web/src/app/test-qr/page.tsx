'use client';

/**
 * Test QR Code Generator Page
 * For development and testing purposes only
 * Allows generating test QR codes to verify scanner functionality
 */

import { useState, useEffect } from 'react';
import { generateQRCode, createTestQRData } from '@/lib/utils/qr-utils';
import { WorkerQRData } from '@/types/certification';

export default function TestQRPage() {
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string>('');
  const [formData, setFormData] = useState<WorkerQRData>(createTestQRData());
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const handleGenerate = async () => {
    setIsGenerating(true);
    setError('');

    try {
      const dataUrl = await generateQRCode(formData, {
        width: 512,
        errorCorrectionLevel: 'M',
      });
      setQrDataUrl(dataUrl);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate QR code');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleDownload = () => {
    if (!qrDataUrl) return;

    const link = document.createElement('a');
    link.href = qrDataUrl;
    link.download = `hazardhawk-qr-${formData.uploadSessionId}.png`;
    link.click();
  };

  const handleRandomize = () => {
    setFormData(createTestQRData({
      uploadSessionId: `session-${Date.now()}`,
    }));
    setQrDataUrl('');
  };

  if (!mounted) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-gray-300 border-t-blue-600"></div>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 py-8">
      <div className="max-w-4xl mx-auto px-4">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-3xl font-bold text-gray-900">
              QR Code Generator (Test)
            </h1>
            <a
              href="/upload/scan"
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow transition-colors"
            >
              Test Scanner
            </a>
          </div>
          <p className="text-gray-600">
            Generate test QR codes for development and testing. Scan with the upload page
            to verify functionality.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-8">
          {/* Form */}
          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Worker Session Data
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Worker ID *
                </label>
                <input
                  type="text"
                  value={formData.workerId}
                  onChange={(e) =>
                    setFormData({ ...formData, workerId: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="worker-001"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Project ID *
                </label>
                <input
                  type="text"
                  value={formData.projectId}
                  onChange={(e) =>
                    setFormData({ ...formData, projectId: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="project-001"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Company ID *
                </label>
                <input
                  type="text"
                  value={formData.companyId}
                  onChange={(e) =>
                    setFormData({ ...formData, companyId: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="company-001"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Worker Name (Optional)
                </label>
                <input
                  type="text"
                  value={formData.workerName || ''}
                  onChange={(e) =>
                    setFormData({ ...formData, workerName: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="John Doe"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Upload Session ID *
                </label>
                <input
                  type="text"
                  value={formData.uploadSessionId}
                  onChange={(e) =>
                    setFormData({ ...formData, uploadSessionId: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="session-123456"
                />
              </div>

              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-sm text-red-600">{error}</p>
                </div>
              )}

              <div className="flex gap-3">
                <button
                  onClick={handleGenerate}
                  disabled={isGenerating}
                  className="flex-1 px-4 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 text-white font-semibold rounded-lg shadow transition-colors"
                >
                  {isGenerating ? 'Generating...' : 'Generate QR Code'}
                </button>
                <button
                  onClick={handleRandomize}
                  className="px-4 py-3 bg-gray-600 hover:bg-gray-700 text-white font-semibold rounded-lg shadow transition-colors"
                >
                  Randomize
                </button>
              </div>
            </div>
          </div>

          {/* QR Code Display */}
          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Generated QR Code
            </h2>

            {qrDataUrl ? (
              <div className="space-y-4">
                <div className="bg-gray-50 p-4 rounded-lg border-2 border-gray-200">
                  <img
                    src={qrDataUrl}
                    alt="Generated QR Code"
                    className="w-full max-w-sm mx-auto"
                  />
                </div>

                <div className="space-y-2">
                  <button
                    onClick={handleDownload}
                    className="w-full px-4 py-3 bg-green-600 hover:bg-green-700 text-white font-semibold rounded-lg shadow transition-colors"
                  >
                    Download QR Code
                  </button>

                  <a
                    href="/upload/scan"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block text-center px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow transition-colors"
                  >
                    Test with Scanner
                  </a>
                </div>

                {/* Data Preview */}
                <div className="p-4 bg-gray-50 rounded-lg border border-gray-200">
                  <h3 className="font-semibold text-sm text-gray-700 mb-2">
                    Encoded Data:
                  </h3>
                  <pre className="text-xs text-gray-600 overflow-x-auto">
                    {JSON.stringify(formData, null, 2)}
                  </pre>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                <div className="text-center">
                  <svg
                    className="w-16 h-16 mx-auto mb-3 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z"
                    />
                  </svg>
                  <p className="text-gray-500">
                    Fill in the form and click "Generate QR Code"
                  </p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Instructions */}
        <div className="mt-8 bg-yellow-50 border-2 border-yellow-200 rounded-lg p-6">
          <h3 className="font-semibold text-yellow-900 mb-3">Testing Instructions:</h3>
          <ol className="list-decimal list-inside space-y-2 text-sm text-yellow-800">
            <li>Fill in the worker session data above (or use default test values)</li>
            <li>Click "Generate QR Code" to create a QR code</li>
            <li>Download the QR code or open the scanner in a new tab</li>
            <li>
              Scan the QR code with your phone or display it on another screen to test
            </li>
            <li>Verify that the scanner correctly extracts all the data</li>
          </ol>
        </div>
      </div>
    </main>
  );
}
