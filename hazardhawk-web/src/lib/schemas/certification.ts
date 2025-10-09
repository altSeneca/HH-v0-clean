import { z } from 'zod';

// Helper to validate future dates
const futureDate = z.string().refine(
  (dateStr) => {
    const date = new Date(dateStr);
    const now = new Date();
    now.setHours(0, 0, 0, 0); // Reset to start of day
    return date >= now;
  },
  { message: 'Expiration date must be in the future' }
);

// Helper to validate past or present dates
const pastOrPresentDate = z.string().refine(
  (dateStr) => {
    const date = new Date(dateStr);
    const now = new Date();
    now.setHours(23, 59, 59, 999); // End of today
    return date <= now;
  },
  { message: 'Issue date cannot be in the future' }
);

// Helper for date format validation (YYYY-MM-DD or ISO)
const dateString = z.string().regex(
  /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}(\.\d{3})?Z?)?$/,
  'Invalid date format. Use YYYY-MM-DD'
);

// Certification types from constants
export const certificationTypeSchema = z.enum([
  'osha-10',
  'osha-30',
  'sst',
  'scaffold',
  'confined-space',
  'fall-protection',
  'hazmat',
  'first-aid',
  'other',
]);

// Manual certification entry form schema
export const certificationFormSchema = z.object({
  workerId: z.string().min(1, 'Worker ID is required'),
  holderName: z
    .string()
    .min(2, 'Name must be at least 2 characters')
    .max(100, 'Name must be less than 100 characters')
    .regex(/^[a-zA-Z\s'-]+$/, 'Name can only contain letters, spaces, hyphens, and apostrophes'),
  certificationType: certificationTypeSchema,
  certificationNumber: z
    .string()
    .min(3, 'Certification number must be at least 3 characters')
    .max(50, 'Certification number must be less than 50 characters')
    .regex(/^[A-Z0-9-]+$/i, 'Certification number can only contain letters, numbers, and hyphens'),
  expirationDate: dateString.pipe(futureDate),
  issueDate: dateString.pipe(pastOrPresentDate).optional(),
  sstNumber: z
    .string()
    .regex(/^\d{8}$/, 'SST number must be 8 digits')
    .optional()
    .or(z.literal('')),
});

// DOB lookup form schema
export const dobLookupSchema = z.object({
  sst_number: z
    .string()
    .regex(/^\d{8}$/, 'SST number must be 8 digits')
    .optional()
    .or(z.literal('')),
  certification_number: z.string().optional().or(z.literal('')),
  first_name: z.string().optional().or(z.literal('')),
  last_name: z.string().optional().or(z.literal('')),
}).refine(
  (data) => {
    // At least one field must be filled
    return (
      data.sst_number ||
      data.certification_number ||
      (data.first_name && data.last_name)
    );
  },
  {
    message: 'Please provide SST number, certification number, or both first and last name',
    path: ['sst_number'],
  }
);

// OCR confirmation schema (simplified version of manual form)
export const ocrConfirmationSchema = z.object({
  holderName: z.string().min(1, 'Name is required'),
  certificationType: certificationTypeSchema,
  certificationNumber: z.string().min(1, 'Certification number is required'),
  expirationDate: z.string().min(1, 'Expiration date is required'),
  issueDate: z.string().optional(),
  sstNumber: z.string().optional().or(z.literal('')),
});

// Type inference
export type CertificationFormValues = z.infer<typeof certificationFormSchema>;
export type DOBLookupValues = z.infer<typeof dobLookupSchema>;
export type OCRConfirmationValues = z.infer<typeof ocrConfirmationSchema>;
