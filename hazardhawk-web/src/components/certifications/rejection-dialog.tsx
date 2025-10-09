'use client';

import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface RejectionDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => void;
  workerName: string;
  isSubmitting?: boolean;
}

const COMMON_REJECTION_REASONS = [
  'Expired certification',
  'Document quality too poor to verify',
  'Information does not match records',
  'Wrong certification type',
  'Document appears altered or fake',
  'Missing required information',
  'Name mismatch',
];

/**
 * Modal dialog for rejecting certifications
 * Includes common rejection reasons as quick-select chips
 * Keyboard shortcut: Escape to close
 */
export function RejectionDialog({
  isOpen,
  onClose,
  onConfirm,
  workerName,
  isSubmitting = false,
}: RejectionDialogProps) {
  const [reason, setReason] = useState('');
  const [selectedChip, setSelectedChip] = useState<string | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Focus textarea when dialog opens
  useEffect(() => {
    if (isOpen && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [isOpen]);

  // Handle Escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, isSubmitting, onClose]);

  // Handle chip selection
  const handleChipSelect = (chipReason: string) => {
    if (selectedChip === chipReason) {
      // Deselect
      setSelectedChip(null);
      setReason('');
    } else {
      // Select
      setSelectedChip(chipReason);
      setReason(chipReason);
    }
  };

  // Handle confirm
  const handleConfirm = () => {
    if (reason.trim()) {
      onConfirm(reason.trim());
    }
  };

  // Handle close
  const handleClose = () => {
    if (!isSubmitting) {
      setReason('');
      setSelectedChip(null);
      onClose();
    }
  };

  // Prevent body scroll when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={handleClose}
            className="fixed inset-0 bg-black bg-opacity-50 z-50"
          />

          {/* Dialog */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ type: 'spring', duration: 0.3 }}
              className="bg-white rounded-lg shadow-2xl w-full max-w-lg pointer-events-auto"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-center justify-between p-6 border-b border-gray-200">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-red-100 rounded-lg">
                    <AlertTriangle className="text-red-600" size={24} />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">Reject Certification</h2>
                    <p className="text-sm text-gray-600 mt-1">For: {workerName}</p>
                  </div>
                </div>
                <button
                  onClick={handleClose}
                  disabled={isSubmitting}
                  className="text-gray-400 hover:text-gray-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  aria-label="Close"
                >
                  <X size={24} />
                </button>
              </div>

              {/* Content */}
              <div className="p-6 space-y-4">
                {/* Quick select chips */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Common Reasons (click to select)
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {COMMON_REJECTION_REASONS.map((chipReason) => (
                      <button
                        key={chipReason}
                        onClick={() => handleChipSelect(chipReason)}
                        disabled={isSubmitting}
                        className={`px-3 py-1.5 text-sm rounded-full border-2 transition-all ${
                          selectedChip === chipReason
                            ? 'bg-red-50 border-red-500 text-red-700 font-medium'
                            : 'bg-white border-gray-300 text-gray-700 hover:border-red-300 hover:bg-red-50'
                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                      >
                        {chipReason}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Custom reason textarea */}
                <div>
                  <label htmlFor="rejection-reason" className="block text-sm font-medium text-gray-700 mb-2">
                    Rejection Reason
                  </label>
                  <textarea
                    ref={textareaRef}
                    id="rejection-reason"
                    value={reason}
                    onChange={(e) => {
                      setReason(e.target.value);
                      setSelectedChip(null);
                    }}
                    disabled={isSubmitting}
                    placeholder="Provide a detailed reason for rejection..."
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500 disabled:bg-gray-100 disabled:cursor-not-allowed resize-none"
                  />
                  <p className="text-xs text-gray-500 mt-1">
                    This reason will be shared with the worker.
                  </p>
                </div>

                {/* Warning */}
                <div className="bg-red-50 border-l-4 border-red-500 p-3 rounded">
                  <p className="text-sm text-red-700">
                    <strong>Note:</strong> The worker will be notified of this rejection and will need to resubmit
                    their certification.
                  </p>
                </div>
              </div>

              {/* Footer */}
              <div className="flex items-center justify-end gap-3 p-6 border-t border-gray-200 bg-gray-50">
                <Button
                  variant="outline"
                  onClick={handleClose}
                  disabled={isSubmitting}
                  className="min-w-24"
                >
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleConfirm}
                  disabled={isSubmitting || !reason.trim()}
                  className="min-w-24"
                >
                  {isSubmitting ? (
                    <span className="flex items-center gap-2">
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      Rejecting...
                    </span>
                  ) : (
                    'Confirm Rejection'
                  )}
                </Button>
              </div>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
