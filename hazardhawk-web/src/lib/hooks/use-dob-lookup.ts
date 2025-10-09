import { useState, useCallback } from 'react';
import { dobApi } from '@/lib/api/dob';
import { DOBLookupRequest, DOBLookupResponse, DOBCertification } from '@/types/api';
import { CertificationType } from '@/types/certification';

interface UseDOBLookupResult {
  lookupWorker: (request: DOBLookupRequest) => Promise<DOBLookupResponse | null>;
  isLoading: boolean;
  error: string | null;
  data: DOBLookupResponse | null;
  isAvailable: boolean;
  clearError: () => void;
  clearData: () => void;
}

/**
 * Custom hook for NYC DOB Training Connect API lookups
 * Handles API availability, loading states, and graceful degradation
 */
export function useDOBLookup(): UseDOBLookupResult {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<DOBLookupResponse | null>(null);
  const [isAvailable, setIsAvailable] = useState(true);

  const lookupWorker = useCallback(async (request: DOBLookupRequest): Promise<DOBLookupResponse | null> => {
    // Validate request has at least one search parameter
    if (!request.sst_number && !request.certification_number && !(request.first_name && request.last_name)) {
      setError('Please provide SST number, certification number, or full name');
      return null;
    }

    setIsLoading(true);
    setError(null);
    setData(null);

    try {
      const result = await dobApi.lookupWorker(request);

      // Check if API returned empty response (might indicate API unavailability)
      if (!result.found && result.certifications.length === 0) {
        // This is a normal "not found" response, not an error
        setData(result);
        setIsAvailable(true);
        return result;
      }

      setData(result);
      setIsAvailable(true);
      return result;
    } catch (err) {
      // DOB API errors are handled gracefully
      const errorMessage = err instanceof Error
        ? err.message
        : 'NYC DOB lookup service is currently unavailable';

      console.warn('DOB API lookup failed (non-critical):', err);

      // Mark as unavailable but don't block user workflow
      setError(errorMessage);
      setIsAvailable(false);

      // Return empty response to allow workflow to continue
      const emptyResponse: DOBLookupResponse = {
        found: false,
        certifications: [],
      };

      return emptyResponse;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const clearData = useCallback(() => {
    setData(null);
    setError(null);
  }, []);

  return {
    lookupWorker,
    isLoading,
    error,
    data,
    isAvailable,
    clearError,
    clearData,
  };
}

/**
 * Helper function to format DOB certification data for auto-populating forms
 */
export function formatDOBCertificationForForm(cert: DOBCertification): {
  holderName?: string;
  certificationType: CertificationType;
  certificationNumber: string;
  expirationDate: string;
  issueDate: string;
  sstNumber?: string;
} {
  return {
    certificationType: mapDOBCertType(cert.certification_type),
    certificationNumber: cert.certification_number,
    expirationDate: cert.expiration_date,
    issueDate: cert.issue_date,
  };
}

/**
 * Maps DOB certification types to HazardHawk certification types
 */
function mapDOBCertType(dobType: string): CertificationType {
  const typeMap: Record<string, CertificationType> = {
    'SST': 'sst',
    'OSHA 10': 'osha-10',
    'OSHA 30': 'osha-30',
    'Scaffold': 'scaffold',
    'Confined Space': 'confined-space',
    'Fall Protection': 'fall-protection',
    'HAZMAT': 'hazmat',
    'First Aid': 'first-aid',
  };

  return typeMap[dobType] || 'other';
}
