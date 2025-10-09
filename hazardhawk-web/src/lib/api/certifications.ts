import apiClient from './client';
import {
  PresignedUrlParams,
  PresignedUrlResponse,
  OCRExtractionRequest,
  OCRExtractionResponse,
  CertificationSubmission,
  CertificationRecord,
  FilterParams,
  PendingQueueResponse,
  ApprovalRequest,
  RejectionRequest,
} from '@/types/api';

export const certificationsApi = {
  // File upload flow
  getPresignedUrl: async (params: PresignedUrlParams): Promise<PresignedUrlResponse> => {
    const response = await apiClient.post<PresignedUrlResponse>('/api/storage/presigned-url', params);
    return response.data;
  },

  uploadToS3: async (url: string, file: File, onProgress?: (progress: number) => void): Promise<void> => {
    await apiClient.put(url, file, {
      headers: {
        'Content-Type': file.type,
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(progress);
        }
      },
    });
  },

  // OCR processing
  extractOCR: async (request: OCRExtractionRequest): Promise<OCRExtractionResponse> => {
    const response = await apiClient.post<OCRExtractionResponse>('/api/ocr/extract-certification', request);
    return response.data;
  },

  // Certification submission
  submitCertification: async (data: CertificationSubmission): Promise<CertificationRecord> => {
    const response = await apiClient.post<CertificationRecord>('/api/certifications', data);
    return response.data;
  },

  // Verification queue (admin)
  getPendingQueue: async (filter?: FilterParams): Promise<PendingQueueResponse> => {
    const response = await apiClient.get<PendingQueueResponse>('/api/certifications/pending', {
      params: filter,
    });
    return response.data;
  },

  // Approval/rejection
  approve: async (request: ApprovalRequest): Promise<CertificationRecord> => {
    const { certificationId, ...body } = request;
    const response = await apiClient.post<CertificationRecord>(
      `/api/certifications/${certificationId}/approve`,
      body
    );
    return response.data;
  },

  reject: async (request: RejectionRequest): Promise<CertificationRecord> => {
    const { certificationId, ...body } = request;
    const response = await apiClient.post<CertificationRecord>(
      `/api/certifications/${certificationId}/reject`,
      body
    );
    return response.data;
  },

  // Get single certification
  getCertification: async (id: string): Promise<CertificationRecord> => {
    const response = await apiClient.get<CertificationRecord>(`/api/certifications/${id}`);
    return response.data;
  },
};

export default certificationsApi;
