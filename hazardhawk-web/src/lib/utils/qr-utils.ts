/**
 * QR Code utilities for HazardHawk certification system
 * Handles QR code generation, parsing, and validation
 */

import QRCode from 'qrcode';
import { z } from 'zod';
import { WorkerQRData } from '@/types/certification';

/**
 * Zod schema for validating WorkerQRData from QR codes
 * Ensures all required fields are present and properly formatted
 */
export const WorkerQRDataSchema = z.object({
  workerId: z.string().min(1, 'Worker ID is required'),
  projectId: z.string().min(1, 'Project ID is required'),
  companyId: z.string().min(1, 'Company ID is required'),
  workerName: z.string().optional(),
  uploadSessionId: z.string().min(1, 'Upload session ID is required'),
});

/**
 * Parse and validate QR code data
 * @param qrData - Raw string data from QR code scan
 * @returns Validated WorkerQRData object
 * @throws Error if data is invalid or doesn't match schema
 */
export function parseQRCodeData(qrData: string): WorkerQRData {
  try {
    // Parse JSON string
    const parsed = JSON.parse(qrData);

    // Validate against schema
    const validated = WorkerQRDataSchema.parse(parsed);

    return validated;
  } catch (error) {
    if (error instanceof z.ZodError) {
      // Create user-friendly error message from Zod validation errors
      const fieldErrors = error.issues.map(err => `${err.path.join('.')}: ${err.message}`);
      throw new Error(`Invalid QR code data: ${fieldErrors.join(', ')}`);
    }

    if (error instanceof SyntaxError) {
      throw new Error('Invalid QR code format: not valid JSON');
    }

    throw new Error('Failed to parse QR code data');
  }
}

/**
 * Validate QR code data without throwing errors
 * Useful for checking data before processing
 * @param qrData - Raw string data from QR code scan
 * @returns Validation result with success flag and data or errors
 */
export function validateQRCodeData(qrData: string): {
  success: boolean;
  data?: WorkerQRData;
  errors?: string[];
} {
  try {
    const parsed = JSON.parse(qrData);
    const result = WorkerQRDataSchema.safeParse(parsed);

    if (result.success) {
      return {
        success: true,
        data: result.data,
      };
    } else {
      return {
        success: false,
        errors: result.error.issues.map(err => `${err.path.join('.')}: ${err.message}`),
      };
    }
  } catch (error) {
    return {
      success: false,
      errors: ['Invalid JSON format'],
    };
  }
}

/**
 * Generate QR code as data URL (base64 PNG)
 * Used for testing and admin QR code generation
 * @param data - WorkerQRData to encode in QR code
 * @param options - QR code generation options
 * @returns Promise resolving to base64 data URL
 */
export async function generateQRCode(
  data: WorkerQRData,
  options?: {
    width?: number;
    margin?: number;
    errorCorrectionLevel?: 'L' | 'M' | 'Q' | 'H';
  }
): Promise<string> {
  try {
    // Validate data before encoding
    const validated = WorkerQRDataSchema.parse(data);

    // Convert to JSON string
    const jsonString = JSON.stringify(validated);

    // Generate QR code
    const dataUrl = await QRCode.toDataURL(jsonString, {
      width: options?.width || 512,
      margin: options?.margin || 2,
      errorCorrectionLevel: options?.errorCorrectionLevel || 'M',
      color: {
        dark: '#000000',
        light: '#FFFFFF',
      },
    });

    return dataUrl;
  } catch (error) {
    if (error instanceof z.ZodError) {
      const fieldErrors = error.issues.map(err => `${err.path.join('.')}: ${err.message}`);
      throw new Error(`Invalid worker data: ${fieldErrors.join(', ')}`);
    }
    throw new Error('Failed to generate QR code');
  }
}

/**
 * Generate QR code as SVG string
 * Useful for high-quality display and printing
 * @param data - WorkerQRData to encode in QR code
 * @param options - QR code generation options
 * @returns Promise resolving to SVG string
 */
export async function generateQRCodeSVG(
  data: WorkerQRData,
  options?: {
    width?: number;
    margin?: number;
    errorCorrectionLevel?: 'L' | 'M' | 'Q' | 'H';
  }
): Promise<string> {
  try {
    // Validate data before encoding
    const validated = WorkerQRDataSchema.parse(data);

    // Convert to JSON string
    const jsonString = JSON.stringify(validated);

    // Generate QR code as SVG
    const svg = await QRCode.toString(jsonString, {
      type: 'svg',
      width: options?.width || 512,
      margin: options?.margin || 2,
      errorCorrectionLevel: options?.errorCorrectionLevel || 'M',
      color: {
        dark: '#000000',
        light: '#FFFFFF',
      },
    });

    return svg;
  } catch (error) {
    if (error instanceof z.ZodError) {
      const fieldErrors = error.issues.map(err => `${err.path.join('.')}: ${err.message}`);
      throw new Error(`Invalid worker data: ${fieldErrors.join(', ')}`);
    }
    throw new Error('Failed to generate QR code SVG');
  }
}

/**
 * Check if a string looks like valid QR code data
 * Quick validation without full parsing
 * @param data - String to check
 * @returns True if data appears to be valid QR code JSON
 */
export function isValidQRCodeFormat(data: string): boolean {
  try {
    const parsed = JSON.parse(data);
    return (
      typeof parsed === 'object' &&
      parsed !== null &&
      'workerId' in parsed &&
      'projectId' in parsed &&
      'companyId' in parsed &&
      'uploadSessionId' in parsed
    );
  } catch {
    return false;
  }
}

/**
 * Create a test/demo QR code data object
 * Useful for development and testing
 * @param overrides - Optional field overrides
 * @returns WorkerQRData object with test data
 */
export function createTestQRData(overrides?: Partial<WorkerQRData>): WorkerQRData {
  return {
    workerId: 'test-worker-001',
    projectId: 'test-project-001',
    companyId: 'test-company-001',
    workerName: 'John Doe',
    uploadSessionId: `session-${Date.now()}`,
    ...overrides,
  };
}
