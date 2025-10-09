import { create } from 'zustand';
import { UploadState, UploadFile } from '@/types/certification';

interface UploadStore {
  // State
  state: UploadState;
  currentFile: UploadFile | null;
  workerId: string | null;
  projectId: string | null;

  // Actions
  setState: (state: UploadState) => void;
  setFile: (file: UploadFile | null) => void;
  setWorkerInfo: (workerId: string, projectId: string) => void;
  reset: () => void;
}

const initialState: UploadState = { type: 'idle' };

export const useUploadStore = create<UploadStore>((set) => ({
  // Initial state
  state: initialState,
  currentFile: null,
  workerId: null,
  projectId: null,

  // Actions
  setState: (state) => set({ state }),

  setFile: (file) => set({ currentFile: file }),

  setWorkerInfo: (workerId, projectId) => set({ workerId, projectId }),

  reset: () =>
    set({
      state: initialState,
      currentFile: null,
      workerId: null,
      projectId: null,
    }),
}));
