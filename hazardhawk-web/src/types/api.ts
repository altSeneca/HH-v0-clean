// API types matching the backend specification

export interface PresignedUrlParams {
  bucket: string;
  key: string;
  contentType: string;
}

export interface PresignedUrlResponse {
  uploadUrl: string;
  fileUrl: string;
  key: string;
}

export interface OCRExtractionRequest {
  documentUrl: string;
}

export interface OCRExtractionResponse {
  holderName: string;
  certificationType: string;
  certificationNumber: string;
  expirationDate: string;
  issueDate?: string;
  issuingAuthority?: string;
  confidence: number;
  extractedAt: string;
}

export interface CertificationSubmission {
  workerId: string;
  holderName: string;
  certificationType: string;
  certificationNumber: string;
  expirationDate: string;
  issueDate?: string;
  documentUrl: string;
  ocrConfidence: number;
  submittedVia: 'web' | 'android' | 'ios';
}

export interface CertificationRecord extends CertificationSubmission {
  id: string;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: string;
  updatedAt: string;
  verifiedBy?: string;
  verifiedAt?: string;
  rejectionReason?: string;
}

export interface FilterParams {
  status?: 'pending' | 'approved' | 'rejected';
  certificationType?: string;
  dateFrom?: string;
  dateTo?: string;
  limit?: number;
  offset?: number;
}

export interface PendingQueueResponse {
  certifications: CertificationRecord[];
  total: number;
  pending: number;
  approved: number;
  rejected: number;
}

export interface ApprovalRequest {
  certificationId: string;
  verifiedBy: string;
}

export interface RejectionRequest {
  certificationId: string;
  reason: string;
  verifiedBy: string;
}

// NYC DOB Training Connect types
export interface DOBWorkerCard {
  sst_number: string;
  first_name: string;
  last_name: string;
  issue_date: string;
  expiration_date: string;
  card_type: string;
  status: 'active' | 'expired' | 'revoked';
}

export interface DOBCertification {
  certification_id: string;
  worker_sst: string;
  certification_type: string;
  certification_number: string;
  issue_date: string;
  expiration_date: string;
  issuing_organization: string;
  status: 'valid' | 'expired' | 'revoked';
}

export interface DOBLookupRequest {
  sst_number?: string;
  certification_number?: string;
  first_name?: string;
  last_name?: string;
}

export interface DOBLookupResponse {
  worker?: DOBWorkerCard;
  certifications: DOBCertification[];
  found: boolean;
}
