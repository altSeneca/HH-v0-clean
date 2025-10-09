'use client';

import { useState, useRef, useCallback } from 'react';
import Webcam from 'react-webcam';
import { Camera, RotateCcw, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { UploadFile } from '@/types/certification';

interface CameraCaptureProps {
  onCapture: (file: UploadFile) => void;
  onCancel: () => void;
}

export function CameraCapture({ onCapture, onCancel }: CameraCaptureProps) {
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('environment');
  const [captured, setCaptured] = useState<string | null>(null);
  const webcamRef = useRef<Webcam>(null);

  const handleUserMediaError = useCallback((error: string | DOMException) => {
    console.error('Camera access error:', error);
    setHasPermission(false);
  }, []);

  const handleUserMedia = useCallback(() => {
    setHasPermission(true);
  }, []);

  const capture = useCallback(() => {
    const imageSrc = webcamRef.current?.getScreenshot();
    if (imageSrc) {
      setCaptured(imageSrc);
    }
  }, []);

  const retake = useCallback(() => {
    setCaptured(null);
  }, []);

  const confirm = useCallback(async () => {
    if (!captured) return;

    // Convert base64 to File object
    const response = await fetch(captured);
    const blob = await response.blob();
    const timestamp = Date.now();
    const file = new File([blob], `certification-${timestamp}.jpg`, {
      type: 'image/jpeg',
      lastModified: timestamp,
    });

    const uploadFile: UploadFile = {
      file,
      preview: captured,
      type: 'image',
      size: file.size,
      name: file.name,
    };

    onCapture(uploadFile);
  }, [captured, onCapture]);

  const toggleCamera = useCallback(() => {
    setFacingMode((prev) => (prev === 'user' ? 'environment' : 'user'));
  }, []);

  if (hasPermission === false) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] px-4 text-center">
        <Camera className="w-16 h-16 text-gray-400 mb-4" />
        <h3 className="text-lg font-semibold text-gray-900 mb-2">
          Camera Permission Required
        </h3>
        <p className="text-sm text-gray-600 mb-6">
          Please allow camera access to capture your certification document.
        </p>
        <Button onClick={onCancel} variant="outline">
          Go Back
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* Camera View */}
      <div className="relative flex-1 bg-black">
        {!captured ? (
          <>
            <Webcam
              ref={webcamRef}
              audio={false}
              screenshotFormat="image/jpeg"
              videoConstraints={{
                facingMode,
                width: { ideal: 1920 },
                height: { ideal: 1080 },
              }}
              onUserMedia={handleUserMedia}
              onUserMediaError={handleUserMediaError}
              className="w-full h-full object-cover"
            />

            {/* Document Frame Overlay */}
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="relative w-[90%] max-w-[500px] aspect-[1.586/1]">
                {/* Semi-transparent overlay */}
                <div className="absolute inset-0 border-4 border-white rounded-lg shadow-lg" />

                {/* Corner guides */}
                <div className="absolute top-0 left-0 w-8 h-8 border-t-4 border-l-4 border-yellow-400 rounded-tl-lg" />
                <div className="absolute top-0 right-0 w-8 h-8 border-t-4 border-r-4 border-yellow-400 rounded-tr-lg" />
                <div className="absolute bottom-0 left-0 w-8 h-8 border-b-4 border-l-4 border-yellow-400 rounded-bl-lg" />
                <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-yellow-400 rounded-br-lg" />

                {/* Instruction text */}
                <div className="absolute -bottom-12 left-0 right-0 text-center">
                  <p className="text-white text-sm font-medium bg-black/50 px-4 py-2 rounded-lg">
                    Align certification document within frame
                  </p>
                </div>
              </div>
            </div>
          </>
        ) : (
          <img src={captured} alt="Captured" className="w-full h-full object-contain" />
        )}

        {/* Top Bar */}
        <div className="absolute top-0 left-0 right-0 p-4 flex justify-between items-center">
          <Button
            onClick={onCancel}
            variant="ghost"
            size="icon"
            className="bg-black/50 hover:bg-black/70 text-white"
          >
            <X className="h-6 w-6" />
          </Button>

          {!captured && (
            <Button
              onClick={toggleCamera}
              variant="ghost"
              size="icon"
              className="bg-black/50 hover:bg-black/70 text-white"
            >
              <RotateCcw className="h-6 w-6" />
            </Button>
          )}
        </div>
      </div>

      {/* Controls */}
      <div className="bg-white border-t border-gray-200 p-6">
        {!captured ? (
          <Button
            onClick={capture}
            size="lg"
            className="w-full h-20 text-lg bg-yellow-500 hover:bg-yellow-600 text-black font-semibold"
          >
            <Camera className="w-6 h-6 mr-2" />
            Capture Document
          </Button>
        ) : (
          <div className="flex gap-3">
            <Button
              onClick={retake}
              variant="outline"
              size="lg"
              className="flex-1 h-20 text-lg"
            >
              <RotateCcw className="w-5 h-5 mr-2" />
              Retake
            </Button>
            <Button
              onClick={confirm}
              size="lg"
              className="flex-1 h-20 text-lg bg-green-600 hover:bg-green-700"
            >
              Use Photo
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
