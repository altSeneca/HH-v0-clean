'use client';

import { X, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface UploadProgressProps {
  fileName: string;
  fileSize?: number;
  progress: number;
  onCancel?: () => void;
  showCancel?: boolean;
}

export function UploadProgress({
  fileName,
  fileSize,
  progress,
  onCancel,
  showCancel = true,
}: UploadProgressProps) {
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getProgressStatus = (progress: number): string => {
    if (progress < 20) return 'Preparing...';
    if (progress < 30) return 'Getting upload URL...';
    if (progress < 100) return 'Uploading...';
    return 'Complete!';
  };

  return (
    <div className="w-full max-w-md mx-auto p-6 bg-white rounded-lg shadow-lg border border-gray-200">
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1 min-w-0 pr-4">
          <h3 className="text-lg font-semibold text-gray-900 mb-1">
            {getProgressStatus(progress)}
          </h3>
          <p className="text-sm text-gray-600 truncate">{fileName}</p>
          {fileSize && (
            <p className="text-xs text-gray-500 mt-1">{formatFileSize(fileSize)}</p>
          )}
        </div>

        {showCancel && onCancel && progress < 100 && (
          <Button onClick={onCancel} variant="ghost" size="icon" className="flex-shrink-0">
            <X className="h-5 w-5" />
          </Button>
        )}
      </div>

      {/* Progress Bar */}
      <div className="mb-3">
        <div className="relative w-full h-3 bg-gray-200 rounded-full overflow-hidden">
          <div
            className="absolute top-0 left-0 h-full bg-gradient-to-r from-yellow-400 to-yellow-500 transition-all duration-300 ease-out"
            style={{ width: `${progress}%` }}
          >
            {/* Animated shimmer effect */}
            {progress < 100 && (
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer" />
            )}
          </div>
        </div>
      </div>

      {/* Progress Percentage */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {progress < 100 ? (
            <>
              <Loader2 className="w-4 h-4 text-yellow-600 animate-spin" />
              <span className="text-sm font-medium text-gray-700">
                {Math.round(progress)}%
              </span>
            </>
          ) : (
            <span className="text-sm font-semibold text-green-600">Upload Complete</span>
          )}
        </div>

        {progress < 100 && (
          <span className="text-xs text-gray-500">Please wait...</span>
        )}
      </div>
    </div>
  );
}

// Add shimmer animation to global styles or use inline
// @keyframes shimmer {
//   0% { transform: translateX(-100%); }
//   100% { transform: translateX(100%); }
// }
