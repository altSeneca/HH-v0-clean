import { useState, useCallback } from 'react';
import { certificationsApi } from '@/lib/api/certifications';
import { OCRExtractionResponse } from '@/types/api';

interface UseOCRResult {
  extractOCR: (documentUrl: string) => Promise<OCRExtractionResponse | null>;
  isLoading: boolean;
  error: string | null;
  data: OCRExtractionResponse | null;
  clearError: () => void;
}

/**
 * Custom hook for OCR processing of certification documents
 * Handles loading states, error handling, and data parsing
 */
export function useOCR(): UseOCRResult {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<OCRExtractionResponse | null>(null);

  const extractOCR = useCallback(async (documentUrl: string): Promise<OCRExtractionResponse | null> => {
    if (!documentUrl) {
      setError('Document URL is required');
      return null;
    }

    setIsLoading(true);
    setError(null);
    setData(null);

    try {
      const result = await certificationsApi.extractOCR({ documentUrl });

      // Validate response has required fields
      if (!result.holderName || !result.certificationType || !result.certificationNumber) {
        throw new Error('OCR extraction incomplete - missing required fields');
      }

      setData(result);
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error
        ? err.message
        : 'Failed to process document with OCR';

      console.error('OCR extraction error:', err);
      setError(errorMessage);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    extractOCR,
    isLoading,
    error,
    data,
    clearError,
  };
}
