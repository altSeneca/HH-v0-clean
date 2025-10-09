import { useState, useCallback } from 'react';
import { certificationsApi } from '@/lib/api/certifications';
import imageCompression from 'browser-image-compression';

interface UploadOptions {
  onProgress?: (progress: number) => void;
  onSuccess?: (fileUrl: string) => void;
  onError?: (error: Error) => void;
  compressImages?: boolean;
  maxSizeMB?: number;
}

interface UploadResult {
  fileUrl: string;
  key: string;
}

export function useUpload() {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<Error | null>(null);

  const compressImage = async (file: File, maxSizeMB: number = 1): Promise<File> => {
    if (!file.type.startsWith('image/')) {
      return file;
    }

    const options = {
      maxSizeMB,
      maxWidthOrHeight: 1920,
      useWebWorker: true,
      fileType: 'image/jpeg' as const,
    };

    try {
      const compressedFile = await imageCompression(file, options);
      // Preserve original file name with jpg extension
      const newFileName = file.name.replace(/\.[^/.]+$/, '.jpg');
      return new File([compressedFile], newFileName, {
        type: 'image/jpeg',
        lastModified: Date.now(),
      });
    } catch (err) {
      console.warn('Image compression failed, using original:', err);
      return file;
    }
  };

  const uploadFile = useCallback(
    async (
      file: File,
      bucket: string,
      keyPrefix: string,
      options: UploadOptions = {}
    ): Promise<UploadResult> => {
      const {
        onProgress,
        onSuccess,
        onError,
        compressImages = true,
        maxSizeMB = 1,
      } = options;

      setUploading(true);
      setProgress(0);
      setError(null);

      try {
        // Step 1: Compress image if needed (0-20%)
        let fileToUpload = file;
        if (compressImages) {
          setProgress(5);
          fileToUpload = await compressImage(file, maxSizeMB);
          setProgress(20);
        }

        // Step 2: Get presigned URL (20-30%)
        const timestamp = Date.now();
        const safeFileName = fileToUpload.name.replace(/[^a-zA-Z0-9.-]/g, '_');
        const key = `${keyPrefix}/${timestamp}-${safeFileName}`;

        const { uploadUrl, fileUrl } = await certificationsApi.getPresignedUrl({
          bucket,
          key,
          contentType: fileToUpload.type,
        });

        setProgress(30);

        // Step 3: Upload to S3 (30-100%)
        await certificationsApi.uploadToS3(uploadUrl, fileToUpload, (uploadProgress) => {
          // Map 0-100% to 30-100%
          const adjustedProgress = 30 + (uploadProgress * 70) / 100;
          setProgress(adjustedProgress);
          onProgress?.(adjustedProgress);
        });

        setProgress(100);
        setUploading(false);

        onSuccess?.(fileUrl);
        return { fileUrl, key };
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Upload failed');
        setError(error);
        setUploading(false);
        onError?.(error);
        throw error;
      }
    },
    []
  );

  const reset = useCallback(() => {
    setUploading(false);
    setProgress(0);
    setError(null);
  }, []);

  return {
    uploadFile,
    uploading,
    progress,
    error,
    reset,
  };
}
