'use client';

import { useState, useMemo, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { StatisticsDashboard } from '@/components/certifications/statistics-dashboard';
import { VerificationQueue } from '@/components/certifications/verification-queue';
import { DocumentViewer } from '@/components/certifications/document-viewer';
import { RejectionDialog } from '@/components/certifications/rejection-dialog';
import { useVerificationShortcuts } from '@/lib/hooks/use-verification-shortcuts';
import { certificationsApi } from '@/lib/api/certifications';
import { CertificationRecord } from '@/types/api';
import { Button } from '@/components/ui/button';
import { CheckCircle, XCircle, SkipForward, ChevronLeft, ChevronRight, Keyboard, ExternalLink } from 'lucide-react';
import { format } from 'date-fns';

/**
 * Admin verification page for reviewing and approving worker certifications
 * Features:
 * - 60/40 split layout (queue/viewer)
 * - Keyboard shortcuts (A=approve, R=reject, S=skip, ←→=navigate)
 * - Real-time statistics
 * - Optimistic updates
 */
export default function VerificationPage() {
  const [selectedCertId, setSelectedCertId] = useState<string | null>(null);
  const [rejectionDialogOpen, setRejectionDialogOpen] = useState(false);
  const [showShortcutsHelp, setShowShortcutsHelp] = useState(false);
  const queryClient = useQueryClient();

  // Fetch pending certifications
  const { data: queueData, isLoading } = useQuery({
    queryKey: ['certifications', 'pending'],
    queryFn: () => certificationsApi.getPendingQueue({ status: 'pending' }),
    refetchInterval: 30000, // Refetch every 30s
  });

  const certifications = queueData?.certifications || [];
  const selectedCert = certifications.find((cert) => cert.id === selectedCertId);

  // Auto-select first certification
  useEffect(() => {
    if (certifications.length > 0 && !selectedCertId) {
      setSelectedCertId(certifications[0].id);
    }
  }, [certifications, selectedCertId]);

  // Calculate statistics
  const statistics = useMemo(() => {
    const totalPending = queueData?.pending || 0;
    const approvedToday = queueData?.approved || 0;
    const rejectedToday = queueData?.rejected || 0;

    // Calculate average processing time (mock for now)
    const avgProcessingTimeMinutes = 5;

    return {
      totalPending,
      approvedToday,
      rejectedToday,
      avgProcessingTimeMinutes,
    };
  }, [queueData]);

  // Approve mutation
  const approveMutation = useMutation({
    mutationFn: (certId: string) =>
      certificationsApi.approve({
        certificationId: certId,
        verifiedBy: 'current-admin', // TODO: Get from auth context
      }),
    onMutate: async (certId) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['certifications', 'pending'] });

      // Snapshot the previous value
      const previousData = queryClient.getQueryData(['certifications', 'pending']);

      // Optimistically update
      queryClient.setQueryData(['certifications', 'pending'], (old: any) => {
        if (!old) return old;
        return {
          ...old,
          certifications: old.certifications.filter((cert: CertificationRecord) => cert.id !== certId),
          pending: old.pending - 1,
          approved: old.approved + 1,
        };
      });

      return { previousData };
    },
    onError: (err, certId, context) => {
      // Rollback on error
      if (context?.previousData) {
        queryClient.setQueryData(['certifications', 'pending'], context.previousData);
      }
      console.error('Failed to approve certification:', err);
    },
    onSuccess: () => {
      // Move to next certification
      moveToNext();
    },
  });

  // Reject mutation
  const rejectMutation = useMutation({
    mutationFn: ({ certId, reason }: { certId: string; reason: string }) =>
      certificationsApi.reject({
        certificationId: certId,
        reason,
        verifiedBy: 'current-admin', // TODO: Get from auth context
      }),
    onMutate: async ({ certId }) => {
      await queryClient.cancelQueries({ queryKey: ['certifications', 'pending'] });
      const previousData = queryClient.getQueryData(['certifications', 'pending']);

      queryClient.setQueryData(['certifications', 'pending'], (old: any) => {
        if (!old) return old;
        return {
          ...old,
          certifications: old.certifications.filter((cert: CertificationRecord) => cert.id !== certId),
          pending: old.pending - 1,
          rejected: old.rejected + 1,
        };
      });

      return { previousData };
    },
    onError: (err, variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(['certifications', 'pending'], context.previousData);
      }
      console.error('Failed to reject certification:', err);
    },
    onSuccess: () => {
      setRejectionDialogOpen(false);
      moveToNext();
    },
  });

  // Navigation helpers
  const getCurrentIndex = () => {
    return certifications.findIndex((cert) => cert.id === selectedCertId);
  };

  const moveToNext = () => {
    const currentIndex = getCurrentIndex();
    if (currentIndex < certifications.length - 1) {
      setSelectedCertId(certifications[currentIndex + 1].id);
    } else if (certifications.length > 0) {
      // Wrap to first
      setSelectedCertId(certifications[0].id);
    }
  };

  const moveToPrevious = () => {
    const currentIndex = getCurrentIndex();
    if (currentIndex > 0) {
      setSelectedCertId(certifications[currentIndex - 1].id);
    } else if (certifications.length > 0) {
      // Wrap to last
      setSelectedCertId(certifications[certifications.length - 1].id);
    }
  };

  // Action handlers
  const handleApprove = () => {
    if (selectedCertId && !approveMutation.isPending) {
      approveMutation.mutate(selectedCertId);
    }
  };

  const handleReject = () => {
    if (selectedCertId && !rejectMutation.isPending) {
      setRejectionDialogOpen(true);
    }
  };

  const handleRejectConfirm = (reason: string) => {
    if (selectedCertId) {
      rejectMutation.mutate({ certId: selectedCertId, reason });
    }
  };

  const handleSkip = () => {
    moveToNext();
  };

  // Keyboard shortcuts
  const { isEnabled: shortcutsEnabled } = useVerificationShortcuts({
    onApprove: handleApprove,
    onReject: handleReject,
    onSkip: handleSkip,
    onNext: moveToNext,
    onPrevious: moveToPrevious,
    disabled: approveMutation.isPending || rejectMutation.isPending,
    modalOpen: rejectionDialogOpen,
  });

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Loading verification queue...</p>
        </div>
      </div>
    );
  }

  // Empty state
  if (certifications.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-7xl mx-auto">
          <div className="mb-6">
            <h1 className="text-3xl font-bold text-gray-900">Certification Verification</h1>
            <p className="text-gray-600 mt-2">Review and approve worker certifications</p>
          </div>

          <StatisticsDashboard data={statistics} />

          <div className="bg-white rounded-lg shadow-sm p-12 text-center">
            <CheckCircle size={64} className="text-green-500 mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-gray-900 mb-2">All caught up!</h2>
            <p className="text-gray-600">There are no pending certifications to review.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 p-6">
        <div className="max-w-[1800px] mx-auto">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Certification Verification</h1>
              <p className="text-gray-600 mt-2">Review and approve worker certifications</p>
            </div>
            <button
              onClick={() => setShowShortcutsHelp(!showShortcutsHelp)}
              className="flex items-center gap-2 px-4 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
            >
              <Keyboard size={18} />
              Keyboard Shortcuts
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-[1800px] mx-auto p-6">
        {/* Statistics */}
        <StatisticsDashboard data={statistics} />

        {/* Shortcuts Help */}
        {showShortcutsHelp && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6"
          >
            <h3 className="font-semibold text-blue-900 mb-3">Keyboard Shortcuts</h3>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-3 text-sm">
              <div className="flex items-center gap-2">
                <kbd className="px-2 py-1 bg-white border border-blue-300 rounded font-mono text-blue-700">A</kbd>
                <span className="text-blue-800">Approve</span>
              </div>
              <div className="flex items-center gap-2">
                <kbd className="px-2 py-1 bg-white border border-blue-300 rounded font-mono text-blue-700">R</kbd>
                <span className="text-blue-800">Reject</span>
              </div>
              <div className="flex items-center gap-2">
                <kbd className="px-2 py-1 bg-white border border-blue-300 rounded font-mono text-blue-700">S</kbd>
                <span className="text-blue-800">Skip</span>
              </div>
              <div className="flex items-center gap-2">
                <kbd className="px-2 py-1 bg-white border border-blue-300 rounded font-mono text-blue-700">←</kbd>
                <span className="text-blue-800">Previous</span>
              </div>
              <div className="flex items-center gap-2">
                <kbd className="px-2 py-1 bg-white border border-blue-300 rounded font-mono text-blue-700">→</kbd>
                <span className="text-blue-800">Next</span>
              </div>
            </div>
          </motion.div>
        )}

        {/* Main Content - 60/40 Split */}
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 h-[calc(100vh-400px)]">
          {/* Queue List - 40% (2/5 columns) */}
          <div className="lg:col-span-2 bg-white rounded-lg shadow-sm overflow-hidden">
            <VerificationQueue
              certifications={certifications}
              selectedId={selectedCertId}
              onSelect={(cert) => setSelectedCertId(cert.id)}
            />
          </div>

          {/* Document Viewer + Actions - 60% (3/5 columns) */}
          <div className="lg:col-span-3 space-y-4">
            {/* Document Viewer */}
            <div className="bg-white rounded-lg shadow-sm overflow-hidden h-[calc(100%-200px)]">
              {selectedCert ? (
                <DocumentViewer documentUrl={selectedCert.documentUrl} />
              ) : (
                <div className="flex items-center justify-center h-full text-gray-500">
                  Select a certification to view
                </div>
              )}
            </div>

            {/* Worker Info + Actions */}
            {selectedCert && (
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-white rounded-lg shadow-sm p-6"
              >
                {/* Worker Info */}
                <div className="mb-6 pb-6 border-b border-gray-200">
                  <h3 className="text-xl font-bold text-gray-900 mb-4">{selectedCert.holderName}</h3>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-gray-600">Certification Type:</span>
                      <p className="font-semibold text-gray-900 mt-1">{selectedCert.certificationType}</p>
                    </div>
                    <div>
                      <span className="text-gray-600">Certification Number:</span>
                      <p className="font-semibold font-mono text-gray-900 mt-1">
                        {selectedCert.certificationNumber}
                      </p>
                    </div>
                    <div>
                      <span className="text-gray-600">Expiration Date:</span>
                      <p className="font-semibold text-gray-900 mt-1">
                        {format(new Date(selectedCert.expirationDate), 'MMM d, yyyy')}
                      </p>
                    </div>
                    <div>
                      <span className="text-gray-600">Uploaded:</span>
                      <p className="font-semibold text-gray-900 mt-1">
                        {format(new Date(selectedCert.createdAt), 'MMM d, yyyy h:mm a')}
                      </p>
                    </div>
                    <div>
                      <span className="text-gray-600">OCR Confidence:</span>
                      <p className="font-semibold text-gray-900 mt-1">
                        {Math.round(selectedCert.ocrConfidence)}%
                      </p>
                    </div>
                    <div>
                      <span className="text-gray-600">Submitted Via:</span>
                      <p className="font-semibold text-gray-900 mt-1 capitalize">
                        {selectedCert.submittedVia}
                      </p>
                    </div>
                  </div>

                  {/* NYC SST Verification Helper */}
                  {selectedCert.certificationType.toLowerCase().includes('sst') && (
                    <div className="mt-4 pt-4 border-t border-gray-200">
                      <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-3">
                        <p className="text-sm text-amber-800">
                          <strong>NYC SST Verification:</strong> Manually verify this card on the official NYC DOB portal before approving.
                        </p>
                      </div>
                      <a
                        href="https://dob-trainingconnect.cityofnewyork.us/"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors"
                      >
                        <ExternalLink className="w-4 h-4" />
                        Verify SST on NYC DOB Portal
                      </a>
                      {selectedCert.certificationNumber && (
                        <p className="text-xs text-gray-500 mt-2 text-center">
                          SST Number: <span className="font-mono font-semibold">{selectedCert.certificationNumber}</span>
                        </p>
                      )}
                    </div>
                  )}
                </div>

                {/* Action Buttons */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  <Button
                    onClick={handleApprove}
                    disabled={approveMutation.isPending || rejectMutation.isPending}
                    className="bg-green-600 hover:bg-green-700 text-white"
                    size="lg"
                  >
                    <CheckCircle size={18} className="mr-2" />
                    Approve (A)
                  </Button>
                  <Button
                    onClick={handleReject}
                    disabled={approveMutation.isPending || rejectMutation.isPending}
                    variant="destructive"
                    size="lg"
                  >
                    <XCircle size={18} className="mr-2" />
                    Reject (R)
                  </Button>
                  <Button
                    onClick={handleSkip}
                    disabled={approveMutation.isPending || rejectMutation.isPending}
                    variant="outline"
                    size="lg"
                  >
                    <SkipForward size={18} className="mr-2" />
                    Skip (S)
                  </Button>
                  <div className="flex gap-2">
                    <Button
                      onClick={moveToPrevious}
                      disabled={approveMutation.isPending || rejectMutation.isPending}
                      variant="outline"
                      size="lg"
                      className="flex-1"
                    >
                      <ChevronLeft size={18} />
                    </Button>
                    <Button
                      onClick={moveToNext}
                      disabled={approveMutation.isPending || rejectMutation.isPending}
                      variant="outline"
                      size="lg"
                      className="flex-1"
                    >
                      <ChevronRight size={18} />
                    </Button>
                  </div>
                </div>

                {/* Progress indicator */}
                <div className="mt-4 text-center text-sm text-gray-600">
                  {getCurrentIndex() + 1} of {certifications.length}
                </div>
              </motion.div>
            )}
          </div>
        </div>
      </div>

      {/* Rejection Dialog */}
      <RejectionDialog
        isOpen={rejectionDialogOpen}
        onClose={() => setRejectionDialogOpen(false)}
        onConfirm={handleRejectConfirm}
        workerName={selectedCert?.holderName || ''}
        isSubmitting={rejectMutation.isPending}
      />
    </div>
  );
}
