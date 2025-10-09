'use client';

import { useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import { Upload, FileImage, FileText, X, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { UploadFile } from '@/types/certification';

interface FileUploaderProps {
  onFileSelect: (file: UploadFile) => void;
  onCancel: () => void;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ACCEPTED_TYPES = {
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'application/pdf': ['.pdf'],
};

export function FileUploader({ onFileSelect, onCancel }: FileUploaderProps) {
  const [selectedFile, setSelectedFile] = useState<UploadFile | null>(null);
  const [error, setError] = useState<string | null>(null);

  const validateFile = useCallback((file: File): string | null => {
    // Check file size
    if (file.size > MAX_FILE_SIZE) {
      return `File size must be less than ${MAX_FILE_SIZE / 1024 / 1024}MB`;
    }

    // Check file type
    const isValidType = Object.keys(ACCEPTED_TYPES).some((type) =>
      file.type.startsWith(type.split('/')[0])
    );

    if (!isValidType) {
      return 'Please upload an image (JPEG, PNG) or PDF file';
    }

    return null;
  }, []);

  const createUploadFile = useCallback((file: File): UploadFile => {
    const fileType = file.type.startsWith('image/') ? 'image' : 'pdf';
    const preview = fileType === 'image' ? URL.createObjectURL(file) : undefined;

    return {
      file,
      preview,
      type: fileType,
      size: file.size,
      name: file.name,
    };
  }, []);

  const onDrop = useCallback(
    (acceptedFiles: File[], rejectedFiles: any[]) => {
      setError(null);

      if (rejectedFiles.length > 0) {
        const rejection = rejectedFiles[0];
        if (rejection.errors[0]?.code === 'file-too-large') {
          setError(`File is too large. Maximum size is ${MAX_FILE_SIZE / 1024 / 1024}MB`);
        } else if (rejection.errors[0]?.code === 'file-invalid-type') {
          setError('Invalid file type. Please upload an image (JPEG, PNG) or PDF');
        } else {
          setError('Failed to upload file. Please try again.');
        }
        return;
      }

      if (acceptedFiles.length === 0) {
        setError('No file selected');
        return;
      }

      const file = acceptedFiles[0];
      const validationError = validateFile(file);

      if (validationError) {
        setError(validationError);
        return;
      }

      const uploadFile = createUploadFile(file);
      setSelectedFile(uploadFile);
    },
    [validateFile, createUploadFile]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: ACCEPTED_TYPES,
    maxSize: MAX_FILE_SIZE,
    multiple: false,
  });

  const clearFile = useCallback(() => {
    if (selectedFile?.preview) {
      URL.revokeObjectURL(selectedFile.preview);
    }
    setSelectedFile(null);
    setError(null);
  }, [selectedFile]);

  const handleConfirm = useCallback(() => {
    if (selectedFile) {
      onFileSelect(selectedFile);
    }
  }, [selectedFile, onFileSelect]);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="flex flex-col h-full p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-gray-900">Upload Document</h2>
        <Button onClick={onCancel} variant="ghost" size="icon">
          <X className="h-6 w-6" />
        </Button>
      </div>

      {!selectedFile ? (
        <>
          {/* Dropzone */}
          <div
            {...getRootProps()}
            className={`
              flex-1 flex flex-col items-center justify-center
              border-2 border-dashed rounded-lg p-8
              transition-colors cursor-pointer
              ${
                isDragActive
                  ? 'border-yellow-500 bg-yellow-50'
                  : 'border-gray-300 hover:border-gray-400 bg-gray-50'
              }
              ${error ? 'border-red-500 bg-red-50' : ''}
            `}
          >
            <input {...getInputProps()} />

            <Upload
              className={`w-16 h-16 mb-4 ${
                isDragActive ? 'text-yellow-500' : 'text-gray-400'
              }`}
            />

            <p className="text-lg font-semibold text-gray-900 mb-2 text-center">
              {isDragActive ? 'Drop file here' : 'Drag and drop your file here'}
            </p>

            <p className="text-sm text-gray-600 mb-6 text-center">
              or tap to browse files
            </p>

            <div className="text-xs text-gray-500 text-center space-y-1">
              <p>Accepted formats: JPEG, PNG, PDF</p>
              <p>Maximum file size: 10MB</p>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}
        </>
      ) : (
        <>
          {/* File Preview */}
          <div className="flex-1 flex flex-col items-center justify-center">
            {selectedFile.type === 'image' && selectedFile.preview ? (
              <div className="w-full max-w-md">
                <img
                  src={selectedFile.preview}
                  alt="Preview"
                  className="w-full h-auto rounded-lg shadow-lg border border-gray-200"
                />
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center p-8 bg-gray-100 rounded-lg">
                <FileText className="w-20 h-20 text-red-600 mb-4" />
                <p className="text-lg font-semibold text-gray-900">PDF Document</p>
              </div>
            )}

            {/* File Info */}
            <div className="mt-6 p-4 bg-gray-50 rounded-lg w-full max-w-md">
              <div className="flex items-start gap-3">
                {selectedFile.type === 'image' ? (
                  <FileImage className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                ) : (
                  <FileText className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {selectedFile.name}
                  </p>
                  <p className="text-xs text-gray-600 mt-1">
                    {formatFileSize(selectedFile.size)}
                  </p>
                </div>
                <Button
                  onClick={clearFile}
                  variant="ghost"
                  size="icon"
                  className="flex-shrink-0"
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="mt-6 flex gap-3">
            <Button
              onClick={clearFile}
              variant="outline"
              size="lg"
              className="flex-1 h-16 text-lg"
            >
              Choose Different File
            </Button>
            <Button
              onClick={handleConfirm}
              size="lg"
              className="flex-1 h-16 text-lg bg-green-600 hover:bg-green-700"
            >
              Continue
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
