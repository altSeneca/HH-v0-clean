'use client';

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useDOBLookup, formatDOBCertificationForForm } from '@/lib/hooks/use-dob-lookup';
import { DOBLookupValues, dobLookupSchema } from '@/lib/schemas/certification';
import { CertificationFormValues } from '@/lib/schemas/certification';

interface DOBVerificationProps {
  onWorkerFound: (prefillData: Partial<CertificationFormValues>) => void;
  onSkip: () => void;
}

/**
 * NYC DOB Training Connect verification component
 * Allows lookup by SST number, certification number, or name
 */
export function DOBVerification({ onWorkerFound, onSkip }: DOBVerificationProps) {
  const { lookupWorker, isLoading, error, data, isAvailable, clearError, clearData } = useDOBLookup();
  const [lookupType, setLookupType] = useState<'sst' | 'cert' | 'name'>('sst');
  const [formData, setFormData] = useState<DOBLookupValues>({
    sst_number: '',
    certification_number: '',
    first_name: '',
    last_name: '',
  });
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleInputChange = (field: keyof DOBLookupValues, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setValidationError(null);
    clearError();
  };

  const handleLookup = async () => {
    setValidationError(null);

    // Build lookup request based on type
    let lookupRequest: DOBLookupValues = {};

    if (lookupType === 'sst') {
      lookupRequest.sst_number = formData.sst_number;
    } else if (lookupType === 'cert') {
      lookupRequest.certification_number = formData.certification_number;
    } else {
      lookupRequest.first_name = formData.first_name;
      lookupRequest.last_name = formData.last_name;
    }

    // Validate with Zod
    const result = dobLookupSchema.safeParse(lookupRequest);
    if (!result.success) {
      const zodError = result.error as any;
      setValidationError(zodError.errors?.[0]?.message || 'Validation failed');
      return;
    }

    const response = await lookupWorker(result.data);

    // If worker not found, show message but don't block
    if (response && !response.found) {
      setValidationError('No records found in NYC DOB Training Connect');
    }
  };

  const handleSelectCertification = (index: number) => {
    if (!data || !data.certifications[index]) return;

    const cert = data.certifications[index];
    const prefillData = formatDOBCertificationForForm(cert);

    // Add worker name if available
    if (data.worker) {
      prefillData.holderName = `${data.worker.first_name} ${data.worker.last_name}`;
      prefillData.sstNumber = data.worker.sst_number;
    }

    onWorkerFound(prefillData);
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
        <h2 className="text-2xl font-bold text-gray-900">NYC DOB Verification</h2>
        <p className="text-gray-600">Verify worker certifications with NYC DOB Training Connect</p>
      </div>

      {/* API Unavailable Warning */}
      {!isAvailable && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded"
          role="alert"
        >
          <div className="flex items-start">
            <div className="flex-shrink-0">
              <svg
                className="h-5 w-5 text-amber-500"
                fill="currentColor"
                viewBox="0 0 20 20"
                aria-hidden="true"
              >
                <path
                  fillRule="evenodd"
                  d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm font-medium text-amber-800">
                NYC DOB lookup service is currently unavailable. You can skip this step and enter information manually.
              </p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Lookup Type Selector */}
      <div className="flex gap-2 p-1 bg-gray-100 rounded-lg">
        <button
          type="button"
          onClick={() => {
            setLookupType('sst');
            clearData();
            setValidationError(null);
          }}
          className={`flex-1 py-3 px-4 rounded-md font-semibold transition-colors ${
            lookupType === 'sst'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          SST Number
        </button>
        <button
          type="button"
          onClick={() => {
            setLookupType('cert');
            clearData();
            setValidationError(null);
          }}
          className={`flex-1 py-3 px-4 rounded-md font-semibold transition-colors ${
            lookupType === 'cert'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Cert Number
        </button>
        <button
          type="button"
          onClick={() => {
            setLookupType('name');
            clearData();
            setValidationError(null);
          }}
          className={`flex-1 py-3 px-4 rounded-md font-semibold transition-colors ${
            lookupType === 'name'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Name
        </button>
      </div>

      {/* Lookup Form */}
      <div className="space-y-4">
        <AnimatePresence mode="wait">
          {/* SST Number Input */}
          {lookupType === 'sst' && (
            <motion.div
              key="sst"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className="space-y-2"
            >
              <label htmlFor="sstNumber" className="block text-sm font-semibold text-gray-900">
                SST Card Number
              </label>
              <input
                id="sstNumber"
                type="text"
                value={formData.sst_number}
                onChange={(e) => handleInputChange('sst_number', e.target.value)}
                maxLength={8}
                placeholder="12345678"
                className="w-full px-4 py-4 text-lg font-mono border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                aria-label="SST Card Number (8 digits)"
              />
            </motion.div>
          )}

          {/* Certification Number Input */}
          {lookupType === 'cert' && (
            <motion.div
              key="cert"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className="space-y-2"
            >
              <label htmlFor="certNumber" className="block text-sm font-semibold text-gray-900">
                Certification Number
              </label>
              <input
                id="certNumber"
                type="text"
                value={formData.certification_number}
                onChange={(e) => handleInputChange('certification_number', e.target.value)}
                placeholder="ABC-123456"
                className="w-full px-4 py-4 text-lg font-mono border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                aria-label="Certification Number"
              />
            </motion.div>
          )}

          {/* Name Inputs */}
          {lookupType === 'name' && (
            <motion.div
              key="name"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className="space-y-4"
            >
              <div className="space-y-2">
                <label htmlFor="firstName" className="block text-sm font-semibold text-gray-900">
                  First Name
                </label>
                <input
                  id="firstName"
                  type="text"
                  value={formData.first_name}
                  onChange={(e) => handleInputChange('first_name', e.target.value)}
                  placeholder="John"
                  className="w-full px-4 py-4 text-lg border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                  aria-label="First Name"
                />
              </div>
              <div className="space-y-2">
                <label htmlFor="lastName" className="block text-sm font-semibold text-gray-900">
                  Last Name
                </label>
                <input
                  id="lastName"
                  type="text"
                  value={formData.last_name}
                  onChange={(e) => handleInputChange('last_name', e.target.value)}
                  placeholder="Doe"
                  className="w-full px-4 py-4 text-lg border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                  aria-label="Last Name"
                />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Validation/API Errors */}
        {(validationError || error) && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="bg-red-50 border-l-4 border-red-500 p-4 rounded"
            role="alert"
          >
            <p className="text-sm font-medium text-red-800">{validationError || error}</p>
          </motion.div>
        )}

        {/* Lookup Button */}
        <button
          type="button"
          onClick={handleLookup}
          disabled={isLoading || !isAvailable}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-semibold py-4 px-6 rounded-lg transition-colors duration-200 text-lg"
        >
          {isLoading ? (
            <span className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              Searching...
            </span>
          ) : (
            'Lookup in NYC DOB'
          )}
        </button>
      </div>

      {/* Results */}
      {data && data.found && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="space-y-4"
        >
          {/* Worker Info */}
          {data.worker && (
            <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4">
              <h3 className="font-semibold text-green-900 mb-2">Worker Found</h3>
              <div className="space-y-1 text-sm">
                <p>
                  <span className="font-medium">Name:</span> {data.worker.first_name} {data.worker.last_name}
                </p>
                <p>
                  <span className="font-medium">SST Number:</span> {data.worker.sst_number}
                </p>
                <p>
                  <span className="font-medium">Card Status:</span>{' '}
                  <span
                    className={`font-semibold ${
                      data.worker.status === 'active' ? 'text-green-700' : 'text-red-700'
                    }`}
                  >
                    {data.worker.status.toUpperCase()}
                  </span>
                </p>
              </div>
            </div>
          )}

          {/* Certifications List */}
          {data.certifications.length > 0 && (
            <div className="space-y-2">
              <h3 className="font-semibold text-gray-900">Certifications ({data.certifications.length})</h3>
              <div className="space-y-2">
                {data.certifications.map((cert, index) => (
                  <motion.button
                    key={cert.certification_id}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.1 }}
                    onClick={() => handleSelectCertification(index)}
                    className="w-full text-left bg-white border-2 border-gray-300 hover:border-blue-500 rounded-lg p-4 transition-colors"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h4 className="font-semibold text-gray-900">{cert.certification_type}</h4>
                      <span
                        className={`text-xs font-semibold px-2 py-1 rounded ${
                          cert.status === 'valid'
                            ? 'bg-green-100 text-green-800'
                            : cert.status === 'expired'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {cert.status.toUpperCase()}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600 space-y-1">
                      <p>Number: {cert.certification_number}</p>
                      <p>Expires: {new Date(cert.expiration_date).toLocaleDateString()}</p>
                      <p className="text-xs text-gray-500">Click to auto-fill form</p>
                    </div>
                  </motion.button>
                ))}
              </div>
            </div>
          )}
        </motion.div>
      )}

      {/* Skip Button */}
      <div className="pt-4 border-t-2 border-gray-200">
        <button
          type="button"
          onClick={onSkip}
          className="w-full bg-white hover:bg-gray-50 text-gray-900 font-semibold py-4 px-6 rounded-lg border-2 border-gray-300 transition-colors duration-200 text-lg"
        >
          Skip DOB Verification
        </button>
      </div>
    </motion.div>
  );
}
