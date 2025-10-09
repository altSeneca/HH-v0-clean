'use client';

import { useForm } from 'react-hook-form';
import { motion } from 'framer-motion';
import { certificationFormSchema, CertificationFormValues } from '@/lib/schemas/certification';
import { CERTIFICATION_TYPES } from '@/types/certification';

interface ManualEntryFormProps {
  workerId: string;
  prefill?: Partial<CertificationFormValues>;
  onSubmit: (data: CertificationFormValues) => void;
  onCancel?: () => void;
  isSubmitting?: boolean;
}

/**
 * Manual certification entry form with validation
 * Supports pre-filling data from OCR or DOB lookup
 */
export function ManualEntryForm({
  workerId,
  prefill,
  onSubmit,
  onCancel,
  isSubmitting = false,
}: ManualEntryFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
    setError,
    clearErrors,
  } = useForm<CertificationFormValues>({
    defaultValues: {
      workerId,
      holderName: prefill?.holderName || '',
      certificationType: prefill?.certificationType || 'osha-10',
      certificationNumber: prefill?.certificationNumber || '',
      expirationDate: prefill?.expirationDate || '',
      issueDate: prefill?.issueDate || '',
      sstNumber: prefill?.sstNumber || '',
    },
  });

  // Manual validation using Zod schema
  const onFormSubmit = (data: CertificationFormValues) => {
    clearErrors();
    const result = certificationFormSchema.safeParse(data);

    if (!result.success) {
      // Set errors from Zod validation
      const zodError = result.error as any;
      if (zodError.errors) {
        zodError.errors.forEach((error: any) => {
          const field = error.path[0] as keyof CertificationFormValues;
          setError(field, { type: 'manual', message: error.message });
        });
      }
      return;
    }

    onSubmit(result.data);
  };

  const selectedType = watch('certificationType');

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
        <h2 className="text-2xl font-bold text-gray-900">Enter Certification Details</h2>
        <p className="text-gray-600">Manually enter worker certification information</p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
        {/* Holder Name */}
        <div className="space-y-2">
          <label htmlFor="holderName" className="block text-sm font-semibold text-gray-900">
            Holder Name <span className="text-red-500">*</span>
          </label>
          <input
            id="holderName"
            type="text"
            {...register('holderName')}
            className={`w-full px-4 py-4 text-lg border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all ${
              errors.holderName ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="John Doe"
            aria-invalid={errors.holderName ? 'true' : 'false'}
            aria-describedby={errors.holderName ? 'holderName-error' : undefined}
          />
          {errors.holderName && (
            <p id="holderName-error" className="text-sm text-red-600 font-medium" role="alert">
              {errors.holderName.message}
            </p>
          )}
        </div>

        {/* Certification Type */}
        <div className="space-y-2">
          <label htmlFor="certificationType" className="block text-sm font-semibold text-gray-900">
            Certification Type <span className="text-red-500">*</span>
          </label>
          <select
            id="certificationType"
            {...register('certificationType')}
            className={`w-full px-4 py-4 text-lg border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all bg-white ${
              errors.certificationType ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={errors.certificationType ? 'true' : 'false'}
            aria-describedby={errors.certificationType ? 'certificationType-error' : undefined}
          >
            {CERTIFICATION_TYPES.map((type) => (
              <option key={type.value} value={type.value}>
                {type.label}
              </option>
            ))}
          </select>
          {errors.certificationType && (
            <p id="certificationType-error" className="text-sm text-red-600 font-medium" role="alert">
              {errors.certificationType.message}
            </p>
          )}
        </div>

        {/* Certification Number */}
        <div className="space-y-2">
          <label htmlFor="certificationNumber" className="block text-sm font-semibold text-gray-900">
            Certification Number <span className="text-red-500">*</span>
          </label>
          <input
            id="certificationNumber"
            type="text"
            {...register('certificationNumber')}
            className={`w-full px-4 py-4 text-lg font-mono border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all ${
              errors.certificationNumber ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="ABC-123456"
            aria-invalid={errors.certificationNumber ? 'true' : 'false'}
            aria-describedby={errors.certificationNumber ? 'certificationNumber-error' : undefined}
          />
          {errors.certificationNumber && (
            <p id="certificationNumber-error" className="text-sm text-red-600 font-medium" role="alert">
              {errors.certificationNumber.message}
            </p>
          )}
        </div>

        {/* Expiration Date */}
        <div className="space-y-2">
          <label htmlFor="expirationDate" className="block text-sm font-semibold text-gray-900">
            Expiration Date <span className="text-red-500">*</span>
          </label>
          <input
            id="expirationDate"
            type="date"
            {...register('expirationDate')}
            className={`w-full px-4 py-4 text-lg border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all ${
              errors.expirationDate ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={errors.expirationDate ? 'true' : 'false'}
            aria-describedby={errors.expirationDate ? 'expirationDate-error' : undefined}
          />
          {errors.expirationDate && (
            <p id="expirationDate-error" className="text-sm text-red-600 font-medium" role="alert">
              {errors.expirationDate.message}
            </p>
          )}
        </div>

        {/* Issue Date (Optional) */}
        <div className="space-y-2">
          <label htmlFor="issueDate" className="block text-sm font-semibold text-gray-900">
            Issue Date <span className="text-gray-500 text-xs">(Optional)</span>
          </label>
          <input
            id="issueDate"
            type="date"
            {...register('issueDate')}
            className={`w-full px-4 py-4 text-lg border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all ${
              errors.issueDate ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={errors.issueDate ? 'true' : 'false'}
            aria-describedby={errors.issueDate ? 'issueDate-error' : undefined}
          />
          {errors.issueDate && (
            <p id="issueDate-error" className="text-sm text-red-600 font-medium" role="alert">
              {errors.issueDate.message}
            </p>
          )}
        </div>

        {/* SST Number (Optional, shown for NYC certifications) */}
        {selectedType === 'sst' && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
            className="space-y-2"
          >
            <label htmlFor="sstNumber" className="block text-sm font-semibold text-gray-900">
              SST Card Number <span className="text-gray-500 text-xs">(Optional - for NYC DOB lookup)</span>
            </label>
            <input
              id="sstNumber"
              type="text"
              {...register('sstNumber')}
              maxLength={8}
              className={`w-full px-4 py-4 text-lg font-mono border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all ${
                errors.sstNumber ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="12345678"
              aria-invalid={errors.sstNumber ? 'true' : 'false'}
              aria-describedby={errors.sstNumber ? 'sstNumber-error' : undefined}
            />
            {errors.sstNumber && (
              <p id="sstNumber-error" className="text-sm text-red-600 font-medium" role="alert">
                {errors.sstNumber.message}
              </p>
            )}
          </motion.div>
        )}

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 pt-6">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex-1 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-semibold py-4 px-6 rounded-lg transition-colors duration-200 text-lg"
          >
            {isSubmitting ? (
              <span className="flex items-center justify-center gap-2">
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                Submitting...
              </span>
            ) : (
              'Submit Certification'
            )}
          </button>

          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              disabled={isSubmitting}
              className="flex-1 bg-white hover:bg-gray-50 disabled:bg-gray-100 disabled:cursor-not-allowed text-gray-900 font-semibold py-4 px-6 rounded-lg border-2 border-gray-300 transition-colors duration-200 text-lg"
            >
              Cancel
            </button>
          )}
        </div>
      </form>
    </motion.div>
  );
}
