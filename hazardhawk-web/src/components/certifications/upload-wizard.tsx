'use client';

import { useEffect, useCallback } from 'react';
import { Camera, Upload, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useUploadStore } from '@/lib/stores/upload-store';
import { useUpload } from '@/lib/hooks/use-upload';
import { certificationsApi } from '@/lib/api/certifications';
import { Button } from '@/components/ui/button';
import { CameraCapture } from './camera-capture';
import { FileUploader } from './file-uploader';
import { UploadProgress } from '../shared/upload-progress';
import { UploadFile } from '@/types/certification';

interface UploadWizardProps {
  workerId: string;
  projectId: string;
  workerName?: string;
  onComplete?: (certificationId: string) => void;
  onCancel?: () => void;
}

export function UploadWizard({
  workerId,
  projectId,
  workerName,
  onComplete,
  onCancel,
}: UploadWizardProps) {
  const { state, setState, currentFile, setFile, setWorkerInfo } = useUploadStore();
  const { uploadFile, uploading, progress, error: uploadError } = useUpload();

  // Initialize worker info
  useEffect(() => {
    setWorkerInfo(workerId, projectId);
    setState({ type: 'source-selection' });
  }, [workerId, projectId, setWorkerInfo, setState]);

  // Handle source selection
  const handleSourceSelect = useCallback(
    (source: 'camera' | 'file') => {
      if (source === 'camera') {
        setState({ type: 'camera' });
      } else {
        setState({ type: 'file-selection' });
      }
    },
    [setState]
  );

  // Handle file/photo capture
  const handleFileSelect = useCallback(
    async (file: UploadFile) => {
      setFile(file);

      // Start upload
      setState({
        type: 'uploading',
        progress: 0,
        fileName: file.name,
      });

      try {
        const bucket = 'hazardhawk-certifications'; // Configure this
        const keyPrefix = `${projectId}/${workerId}`;

        const result = await uploadFile(file.file, bucket, keyPrefix, {
          onProgress: (uploadProgress) => {
            setState({
              type: 'uploading',
              progress: uploadProgress,
              fileName: file.name,
            });
          },
        });

        // Start OCR processing
        setState({ type: 'processing' });

        const ocrResult = await certificationsApi.extractOCR({
          documentUrl: result.fileUrl,
        });

        // Show OCR review
        setState({
          type: 'ocr-review',
          data: {
            holderName: ocrResult.holderName,
            certificationType: ocrResult.certificationType,
            certificationNumber: ocrResult.certificationNumber,
            expirationDate: ocrResult.expirationDate,
            issueDate: ocrResult.issueDate,
            confidence: ocrResult.confidence,
            documentUrl: result.fileUrl,
          },
        });
      } catch (err) {
        console.error('Upload or OCR failed:', err);
        setState({
          type: 'error',
          message: err instanceof Error ? err.message : 'Upload failed',
          canRetry: true,
        });
      }
    },
    [setState, setFile, uploadFile, projectId, workerId]
  );

  // Handle OCR review confirmation
  const handleOCRConfirm = useCallback(async () => {
    if (state.type !== 'ocr-review') return;

    setState({ type: 'submitting' });

    try {
      const result = await certificationsApi.submitCertification({
        workerId,
        holderName: state.data.holderName,
        certificationType: state.data.certificationType,
        certificationNumber: state.data.certificationNumber,
        expirationDate: state.data.expirationDate,
        issueDate: state.data.issueDate,
        documentUrl: state.data.documentUrl,
        ocrConfidence: state.data.confidence,
        submittedVia: 'web',
      });

      setState({
        type: 'success',
        certificationId: result.id,
      });

      onComplete?.(result.id);
    } catch (err) {
      console.error('Submission failed:', err);
      setState({
        type: 'error',
        message: 'Failed to submit certification. Please try again.',
        canRetry: false,
      });
    }
  }, [state, setState, workerId, onComplete]);

  // Handle manual entry
  const handleManualEntry = useCallback(() => {
    if (state.type === 'ocr-review') {
      setState({
        type: 'manual-entry',
        prefill: {
          workerId,
          holderName: state.data.holderName,
          certificationType: state.data.certificationType,
          certificationNumber: state.data.certificationNumber,
          expirationDate: state.data.expirationDate,
          issueDate: state.data.issueDate,
        },
      });
    } else {
      setState({ type: 'manual-entry' });
    }
  }, [state, setState, workerId]);

  // Handle retry
  const handleRetry = useCallback(() => {
    setState({ type: 'source-selection' });
  }, [setState]);

  // Handle cancel
  const handleCancel = useCallback(() => {
    onCancel?.();
  }, [onCancel]);

  // Render based on state
  switch (state.type) {
    case 'idle':
      return null;

    case 'source-selection':
      return (
        <div className="flex flex-col h-full bg-white">
          <div className="flex-1 flex flex-col items-center justify-center p-6">
            <h1 className="text-3xl font-bold text-gray-900 mb-2 text-center">
              Upload Certification
            </h1>
            {workerName && (
              <p className="text-lg text-gray-600 mb-8 text-center">
                For: {workerName}
              </p>
            )}

            <p className="text-base text-gray-700 mb-8 text-center max-w-md">
              Choose how you'd like to upload your certification document
            </p>

            <div className="w-full max-w-md space-y-4">
              <Button
                onClick={() => handleSourceSelect('camera')}
                size="lg"
                className="w-full h-20 text-lg bg-yellow-500 hover:bg-yellow-600 text-black font-semibold"
              >
                <Camera className="w-6 h-6 mr-3" />
                Take Photo
              </Button>

              <Button
                onClick={() => handleSourceSelect('file')}
                size="lg"
                variant="outline"
                className="w-full h-20 text-lg border-2"
              >
                <Upload className="w-6 h-6 mr-3" />
                Upload File
              </Button>
            </div>
          </div>

          <div className="p-6 border-t border-gray-200">
            <Button onClick={handleCancel} variant="outline" size="lg" className="w-full">
              Cancel
            </Button>
          </div>
        </div>
      );

    case 'camera':
      return (
        <CameraCapture
          onCapture={handleFileSelect}
          onCancel={() => setState({ type: 'source-selection' })}
        />
      );

    case 'file-selection':
      return (
        <FileUploader
          onFileSelect={handleFileSelect}
          onCancel={() => setState({ type: 'source-selection' })}
        />
      );

    case 'uploading':
      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-6 bg-gray-50">
          <UploadProgress
            fileName={state.fileName}
            fileSize={currentFile?.size}
            progress={state.progress}
            showCancel={false}
          />
        </div>
      );

    case 'processing':
      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-6 bg-gray-50">
          <div className="text-center">
            <div className="inline-block animate-spin rounded-full h-16 w-16 border-4 border-yellow-500 border-t-transparent mb-6" />
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Processing Document
            </h2>
            <p className="text-gray-600">
              Extracting information from your certification...
            </p>
          </div>
        </div>
      );

    case 'ocr-review':
      return (
        <div className="flex flex-col h-full bg-white">
          <div className="flex-1 overflow-y-auto p-6">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Review Information
            </h2>
            <p className="text-sm text-gray-600 mb-6">
              Please verify the extracted information is correct
            </p>

            {/* Document Preview */}
            {currentFile?.preview && (
              <div className="mb-6">
                <img
                  src={currentFile.preview}
                  alt="Document"
                  className="w-full max-w-md mx-auto rounded-lg border border-gray-200 shadow-sm"
                />
              </div>
            )}

            {/* Extracted Data */}
            <div className="bg-gray-50 rounded-lg p-4 space-y-4 mb-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Holder Name
                </label>
                <p className="text-lg text-gray-900">{state.data.holderName}</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Certification Type
                </label>
                <p className="text-lg text-gray-900">{state.data.certificationType}</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Certification Number
                </label>
                <p className="text-lg text-gray-900">{state.data.certificationNumber}</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Expiration Date
                </label>
                <p className="text-lg text-gray-900">
                  {new Date(state.data.expirationDate).toLocaleDateString()}
                </p>
              </div>

              {state.data.issueDate && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Issue Date
                  </label>
                  <p className="text-lg text-gray-900">
                    {new Date(state.data.issueDate).toLocaleDateString()}
                  </p>
                </div>
              )}

              {/* Confidence Score */}
              <div className="pt-4 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-700">
                    Confidence Score
                  </span>
                  <span
                    className={`text-sm font-semibold ${
                      state.data.confidence >= 0.8
                        ? 'text-green-600'
                        : state.data.confidence >= 0.6
                        ? 'text-yellow-600'
                        : 'text-orange-600'
                    }`}
                  >
                    {Math.round(state.data.confidence * 100)}%
                  </span>
                </div>
              </div>
            </div>

            {/* Warning for low confidence */}
            {state.data.confidence < 0.8 && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
                <div className="flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm font-medium text-yellow-900 mb-1">
                      Low Confidence Detection
                    </p>
                    <p className="text-sm text-yellow-800">
                      Some information may not be accurate. Please review carefully
                      or enter manually.
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="border-t border-gray-200 p-6 space-y-3">
            <Button
              onClick={handleOCRConfirm}
              size="lg"
              className="w-full h-16 text-lg bg-green-600 hover:bg-green-700"
            >
              <CheckCircle2 className="w-5 h-5 mr-2" />
              Confirm & Submit
            </Button>

            <Button
              onClick={handleManualEntry}
              variant="outline"
              size="lg"
              className="w-full h-16 text-lg"
            >
              Edit Information
            </Button>

            <Button onClick={handleRetry} variant="ghost" className="w-full">
              Upload Different Document
            </Button>
          </div>
        </div>
      );

    case 'submitting':
      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-6 bg-gray-50">
          <div className="text-center">
            <div className="inline-block animate-spin rounded-full h-16 w-16 border-4 border-green-600 border-t-transparent mb-6" />
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Submitting Certification
            </h2>
            <p className="text-gray-600">Please wait...</p>
          </div>
        </div>
      );

    case 'success':
      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-6 bg-gray-50">
          <div className="text-center max-w-md">
            <div className="inline-flex items-center justify-center w-20 h-20 bg-green-100 rounded-full mb-6">
              <CheckCircle2 className="w-12 h-12 text-green-600" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Success!</h2>
            <p className="text-lg text-gray-600 mb-8">
              Your certification has been submitted and is pending verification.
            </p>
            <Button onClick={handleCancel} size="lg" className="w-full">
              Done
            </Button>
          </div>
        </div>
      );

    case 'error':
      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-6 bg-gray-50">
          <div className="text-center max-w-md">
            <div className="inline-flex items-center justify-center w-20 h-20 bg-red-100 rounded-full mb-6">
              <AlertCircle className="w-12 h-12 text-red-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Upload Failed</h2>
            <p className="text-gray-600 mb-8">{state.message}</p>
            <div className="space-y-3">
              {state.canRetry && (
                <Button onClick={handleRetry} size="lg" className="w-full">
                  Try Again
                </Button>
              )}
              <Button onClick={handleCancel} variant="outline" size="lg" className="w-full">
                Cancel
              </Button>
            </div>
          </div>
        </div>
      );

    case 'manual-entry':
      // Manual entry form component
      // For now, return placeholder
      return (
        <div className="flex flex-col items-center justify-center min-h-screen p-6">
          <p className="text-gray-600">Manual entry component not yet implemented</p>
          <Button onClick={handleCancel} className="mt-4">
            Cancel
          </Button>
        </div>
      );

    default:
      return null;
  }
}
