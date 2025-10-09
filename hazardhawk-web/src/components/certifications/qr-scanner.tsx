'use client';

/**
 * QR Code Scanner Component for HazardHawk Certification Upload
 * Uses html5-qrcode library to scan QR codes containing worker session data
 */

import { useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import { WorkerQRData } from '@/types/certification';
import { parseQRCodeData, validateQRCodeData } from '@/lib/utils/qr-utils';

interface QRScannerProps {
  onScanSuccess: (data: WorkerQRData) => void;
  onScanError?: (error: string) => void;
  className?: string;
}

type ScannerState =
  | { type: 'idle' }
  | { type: 'requesting-permission' }
  | { type: 'starting' }
  | { type: 'scanning' }
  | { type: 'success'; data: WorkerQRData }
  | { type: 'error'; message: string };

export function QRScanner({ onScanSuccess, onScanError, className = '' }: QRScannerProps) {
  const [state, setState] = useState<ScannerState>({ type: 'idle' });
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const scannerElementId = useRef(`qr-scanner-${Math.random().toString(36).substring(7)}`);
  const isScanning = useRef(false);

  // Cleanup function to stop scanner
  const stopScanner = async () => {
    if (scannerRef.current && isScanning.current) {
      try {
        await scannerRef.current.stop();
        isScanning.current = false;
      } catch (error) {
        console.error('Error stopping scanner:', error);
      }
    }
  };

  // Start scanning function
  const startScanning = async () => {
    if (isScanning.current) {
      console.log('Scanner already running');
      return;
    }

    setState({ type: 'requesting-permission' });

    try {
      // Initialize scanner if not already done
      if (!scannerRef.current) {
        scannerRef.current = new Html5Qrcode(scannerElementId.current);
      }

      setState({ type: 'starting' });

      // Configure scanner
      const config = {
        fps: 10, // Frames per second for scanning
        qrbox: { width: 250, height: 250 }, // Scanning area
        aspectRatio: 1.0,
      };

      // Start scanning
      await scannerRef.current.start(
        { facingMode: 'environment' }, // Use back camera on mobile
        config,
        (decodedText: string) => {
          // Success callback - QR code decoded
          handleQRCodeScanned(decodedText);
        },
        (errorMessage: string) => {
          // Error callback - called frequently during scanning, can be ignored
          // Only log if it's not a "No QR code found" type message
          if (!errorMessage.includes('NotFoundException')) {
            console.debug('QR Scan error:', errorMessage);
          }
        }
      );

      isScanning.current = true;
      setState({ type: 'scanning' });
    } catch (error) {
      isScanning.current = false;
      const errorMessage = getErrorMessage(error);
      setState({ type: 'error', message: errorMessage });
      onScanError?.(errorMessage);
    }
  };

  // Handle QR code scan success
  const handleQRCodeScanned = async (decodedText: string) => {
    console.log('QR Code scanned:', decodedText);

    // Stop scanning immediately to prevent multiple scans
    await stopScanner();

    try {
      // Parse and validate QR code data
      const workerData = parseQRCodeData(decodedText);

      setState({ type: 'success', data: workerData });
      onScanSuccess(workerData);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Invalid QR code data';
      setState({ type: 'error', message: errorMessage });
      onScanError?.(errorMessage);
    }
  };

  // Get user-friendly error message
  const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
      if (error.message.includes('Permission denied')) {
        return 'Camera permission denied. Please enable camera access in your browser settings.';
      }
      if (error.message.includes('NotFoundError')) {
        return 'No camera found. Please ensure your device has a camera.';
      }
      if (error.message.includes('NotAllowedError')) {
        return 'Camera access not allowed. Please grant permission to use the camera.';
      }
      if (error.message.includes('NotReadableError')) {
        return 'Camera is already in use by another application.';
      }
      return error.message;
    }
    return 'Failed to access camera. Please try again.';
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopScanner();
    };
  }, []);

  // Reset scanner state
  const resetScanner = () => {
    setState({ type: 'idle' });
  };

  return (
    <div className={`qr-scanner-container ${className}`}>
      {/* Scanner preview area */}
      <div className="relative bg-gray-900 rounded-lg overflow-hidden">
        <div
          id={scannerElementId.current}
          className="w-full min-h-[300px] md:min-h-[400px]"
        />

        {/* Overlay messages */}
        {state.type === 'idle' && (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-900">
            <div className="text-center p-6">
              <svg
                className="w-24 h-24 mx-auto mb-4 text-gray-600"
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
              <p className="text-gray-400 text-lg">Ready to scan QR code</p>
            </div>
          </div>
        )}

        {state.type === 'requesting-permission' && (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-900 bg-opacity-90">
            <div className="text-center p-6">
              <div className="animate-spin rounded-full h-16 w-16 border-4 border-gray-600 border-t-blue-500 mx-auto mb-4"></div>
              <p className="text-gray-300 text-lg">Requesting camera access...</p>
            </div>
          </div>
        )}

        {state.type === 'starting' && (
          <div className="absolute inset-0 flex items-center justify-center bg-gray-900 bg-opacity-90">
            <div className="text-center p-6">
              <div className="animate-spin rounded-full h-16 w-16 border-4 border-gray-600 border-t-blue-500 mx-auto mb-4"></div>
              <p className="text-gray-300 text-lg">Starting camera...</p>
            </div>
          </div>
        )}

        {state.type === 'scanning' && (
          <div className="absolute top-0 left-0 right-0 p-4 bg-gradient-to-b from-black to-transparent">
            <p className="text-white text-center text-sm font-medium">
              Position QR code within the frame
            </p>
          </div>
        )}

        {state.type === 'success' && (
          <div className="absolute inset-0 flex items-center justify-center bg-green-900 bg-opacity-90">
            <div className="text-center p-6">
              <svg
                className="w-20 h-20 mx-auto mb-4 text-green-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <p className="text-green-100 text-xl font-semibold mb-2">QR Code Scanned!</p>
              {state.data.workerName && (
                <p className="text-green-200">Worker: {state.data.workerName}</p>
              )}
            </div>
          </div>
        )}

        {state.type === 'error' && (
          <div className="absolute inset-0 flex items-center justify-center bg-red-900 bg-opacity-90">
            <div className="text-center p-6 max-w-md">
              <svg
                className="w-16 h-16 mx-auto mb-4 text-red-400"
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
              <p className="text-red-100 text-lg font-semibold mb-2">Scan Failed</p>
              <p className="text-red-200 text-sm">{state.message}</p>
            </div>
          </div>
        )}
      </div>

      {/* Control buttons */}
      <div className="mt-6 flex gap-4 justify-center">
        {state.type === 'idle' && (
          <button
            onClick={startScanning}
            className="px-8 py-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-lg transition-colors text-lg min-w-[200px]"
          >
            Start Scanning
          </button>
        )}

        {state.type === 'scanning' && (
          <button
            onClick={stopScanner}
            className="px-8 py-4 bg-red-600 hover:bg-red-700 text-white font-semibold rounded-lg shadow-lg transition-colors text-lg min-w-[200px]"
          >
            Stop Scanning
          </button>
        )}

        {state.type === 'error' && (
          <button
            onClick={resetScanner}
            className="px-8 py-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-lg transition-colors text-lg min-w-[200px]"
          >
            Try Again
          </button>
        )}

        {state.type === 'success' && (
          <button
            onClick={resetScanner}
            className="px-8 py-4 bg-gray-600 hover:bg-gray-700 text-white font-semibold rounded-lg shadow-lg transition-colors text-lg min-w-[200px]"
          >
            Scan Another
          </button>
        )}
      </div>

      {/* Help text */}
      <div className="mt-6 p-4 bg-gray-100 rounded-lg">
        <h3 className="font-semibold text-gray-900 mb-2">How to scan:</h3>
        <ol className="list-decimal list-inside space-y-1 text-sm text-gray-700">
          <li>Click "Start Scanning" and allow camera access</li>
          <li>Point your camera at the QR code</li>
          <li>Hold steady until the code is recognized</li>
          <li>You'll be redirected to the upload form automatically</li>
        </ol>
      </div>
    </div>
  );
}
