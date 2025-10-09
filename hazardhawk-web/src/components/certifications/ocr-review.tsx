'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { OCRReviewData } from '@/types/certification';
import { CERTIFICATION_TYPES } from '@/types/certification';

interface OCRReviewProps {
  data: OCRReviewData;
  onConfirm: () => void;
  onEdit: () => void;
  isSubmitting?: boolean;
}

/**
 * Displays OCR extracted data with confidence indicators
 * Allows user to confirm data or switch to manual entry
 */
export function OCRReview({ data, onConfirm, onEdit, isSubmitting = false }: OCRReviewProps) {
  const [imageLoaded, setImageLoaded] = useState(false);

  // Get confidence badge color and label
  const getConfidenceBadge = (confidence: number) => {
    if (confidence >= 85) {
      return { color: 'bg-green-500', label: 'High Confidence', textColor: 'text-green-700' };
    } else if (confidence >= 60) {
      return { color: 'bg-amber-500', label: 'Medium Confidence', textColor: 'text-amber-700' };
    } else {
      return { color: 'bg-red-500', label: 'Low Confidence', textColor: 'text-red-700' };
    }
  };

  const badge = getConfidenceBadge(data.confidence);

  // Format certification type for display
  const certTypeLabel =
    CERTIFICATION_TYPES.find((type) => type.value === data.certificationType)?.label ||
    data.certificationType;

  // Format dates for display
  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
    } catch {
      return dateStr;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ duration: 0.3 }}
      className="w-full max-w-2xl mx-auto p-6 space-y-6"
    >
      {/* Header */}
      <div className="text-center space-y-2">
        <h2 className="text-2xl font-bold text-gray-900">Review Extracted Information</h2>
        <p className="text-gray-600">Verify the information extracted from your document</p>
      </div>

      {/* Confidence Badge */}
      <motion.div
        initial={{ scale: 0.9 }}
        animate={{ scale: 1 }}
        transition={{ delay: 0.2 }}
        className="flex justify-center"
      >
        <div className={`inline-flex items-center gap-2 px-4 py-2 rounded-full ${badge.color} bg-opacity-10 border-2 border-${badge.color}`}>
          <div className={`w-3 h-3 rounded-full ${badge.color}`}></div>
          <span className={`font-semibold ${badge.textColor}`}>
            {badge.label} ({Math.round(data.confidence)}%)
          </span>
        </div>
      </motion.div>

      {/* Document Preview */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3 }}
        className="relative bg-gray-100 rounded-lg overflow-hidden"
      >
        {!imageLoaded && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}
        <img
          src={data.documentUrl}
          alt="Certification Document"
          className="w-full h-auto max-h-96 object-contain"
          onLoad={() => setImageLoaded(true)}
        />
      </motion.div>

      {/* Extracted Fields */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.4 }}
        className="bg-white border-2 border-gray-200 rounded-lg p-6 space-y-4"
      >
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Extracted Data</h3>

        {/* Name */}
        <div className="space-y-1">
          <label className="block text-sm font-medium text-gray-700">Holder Name</label>
          <p className="text-lg font-semibold text-gray-900">{data.holderName}</p>
        </div>

        {/* Certification Type */}
        <div className="space-y-1">
          <label className="block text-sm font-medium text-gray-700">Certification Type</label>
          <p className="text-lg font-semibold text-gray-900">{certTypeLabel}</p>
        </div>

        {/* Certification Number */}
        <div className="space-y-1">
          <label className="block text-sm font-medium text-gray-700">Certification Number</label>
          <p className="text-lg font-mono font-semibold text-gray-900">{data.certificationNumber}</p>
        </div>

        {/* Expiration Date */}
        <div className="space-y-1">
          <label className="block text-sm font-medium text-gray-700">Expiration Date</label>
          <p className="text-lg font-semibold text-gray-900">{formatDate(data.expirationDate)}</p>
        </div>

        {/* Issue Date (if available) */}
        {data.issueDate && (
          <div className="space-y-1">
            <label className="block text-sm font-medium text-gray-700">Issue Date</label>
            <p className="text-lg font-semibold text-gray-900">{formatDate(data.issueDate)}</p>
          </div>
        )}
      </motion.div>

      {/* Low Confidence Warning */}
      {data.confidence < 60 && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.5 }}
          className="bg-red-50 border-l-4 border-red-500 p-4 rounded"
          role="alert"
        >
          <div className="flex items-start">
            <div className="flex-shrink-0">
              <svg
                className="h-5 w-5 text-red-500"
                fill="currentColor"
                viewBox="0 0 20 20"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clipRule="evenodd"
                />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-red-800">
                Low confidence detection. Please review carefully or consider manual entry.
              </p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Action Buttons */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
        className="flex flex-col sm:flex-row gap-4 pt-4"
      >
        <button
          onClick={onConfirm}
          disabled={isSubmitting}
          className="flex-1 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-semibold py-4 px-6 rounded-lg transition-colors duration-200 text-lg"
        >
          {isSubmitting ? (
            <span className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              Submitting...
            </span>
          ) : (
            'Confirm & Submit'
          )}
        </button>

        <button
          onClick={onEdit}
          disabled={isSubmitting}
          className="flex-1 bg-white hover:bg-gray-50 disabled:bg-gray-100 disabled:cursor-not-allowed text-gray-900 font-semibold py-4 px-6 rounded-lg border-2 border-gray-300 transition-colors duration-200 text-lg"
        >
          Edit Manually
        </button>
      </motion.div>
    </motion.div>
  );
}
