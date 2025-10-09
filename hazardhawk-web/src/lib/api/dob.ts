import axios from 'axios';
import { DOBLookupRequest, DOBLookupResponse } from '@/types/api';

const DOB_API_URL = process.env.NEXT_PUBLIC_DOB_API_URL || 'https://dob-trainingconnect.cityofnewyork.us/api';
const DOB_API_KEY = process.env.DOB_API_KEY;

const dobClient = axios.create({
  baseURL: DOB_API_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    ...(DOB_API_KEY && { 'X-API-Key': DOB_API_KEY }),
  },
});

export const dobApi = {
  /**
   * Look up worker certifications in NYC DOB Training Connect system
   * Can search by SST number, certification number, or name
   */
  lookupWorker: async (request: DOBLookupRequest): Promise<DOBLookupResponse> => {
    try {
      const response = await dobClient.post<DOBLookupResponse>('/lookup/worker', request);
      return response.data;
    } catch (error) {
      console.error('DOB API lookup failed:', error);
      // Return empty response if API fails
      return {
        found: false,
        certifications: [],
      };
    }
  },

  /**
   * Look up worker by SST card number
   */
  lookupBySST: async (sstNumber: string): Promise<DOBLookupResponse> => {
    return dobApi.lookupWorker({ sst_number: sstNumber });
  },

  /**
   * Look up worker by certification number
   */
  lookupByCertNumber: async (certNumber: string): Promise<DOBLookupResponse> => {
    return dobApi.lookupWorker({ certification_number: certNumber });
  },

  /**
   * Look up worker by name
   */
  lookupByName: async (firstName: string, lastName: string): Promise<DOBLookupResponse> => {
    return dobApi.lookupWorker({ first_name: firstName, last_name: lastName });
  },
};

export default dobApi;
