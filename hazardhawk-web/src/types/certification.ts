// Frontend-specific certification types

export type UploadSource = 'camera' | 'file';
export type FileType = 'image' | 'pdf';

export interface UploadFile {
  file: File;
  preview?: string;
  type: FileType;
  size: number;
  name: string;
}

export type UploadState =
  | { type: 'idle' }
  | { type: 'source-selection' }
  | { type: 'camera' }
  | { type: 'file-selection' }
  | { type: 'uploading'; progress: number; fileName: string }
  | { type: 'processing' }
  | { type: 'ocr-review'; data: OCRReviewData }
  | { type: 'manual-entry'; prefill?: Partial<CertificationFormData> }
  | { type: 'submitting' }
  | { type: 'success'; certificationId: string }
  | { type: 'error'; message: string; canRetry: boolean };

export interface OCRReviewData {
  holderName: string;
  certificationType: string;
  certificationNumber: string;
  expirationDate: string;
  issueDate?: string;
  confidence: number;
  documentUrl: string;
}

export interface CertificationFormData {
  workerId: string;
  holderName: string;
  certificationType: string;
  certificationNumber: string;
  expirationDate: string;
  issueDate?: string;
  sstNumber?: string; // NYC DOB SST card number
}

export interface WorkerQRData {
  workerId: string;
  projectId: string;
  companyId: string;
  workerName?: string;
  uploadSessionId: string;
}

export const CERTIFICATION_TYPES = [
  { value: 'osha-10', label: 'OSHA 10-Hour' },
  { value: 'osha-30', label: 'OSHA 30-Hour' },
  { value: 'sst', label: 'NYC SST (Site Safety Training)' },
  { value: 'scaffold', label: 'Scaffold User' },
  { value: 'confined-space', label: 'Confined Space' },
  { value: 'fall-protection', label: 'Fall Protection' },
  { value: 'hazmat', label: 'HAZMAT' },
  { value: 'first-aid', label: 'First Aid/CPR' },
  { value: 'other', label: 'Other' },
] as const;

export type CertificationType = typeof CERTIFICATION_TYPES[number]['value'];
