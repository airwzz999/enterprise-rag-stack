import { create } from 'zustand';
import { fileManagementService, FileMetadata, FileStatistics } from '@/services/file-management.service';
import type { EntityId } from '@/types';

/**
 * File management center state management
 */
interface FileManagementState {
  files: FileMetadata[];
  statistics: FileStatistics | null;
  isLoading: boolean;
  error: string | null;
  currentCategory: string;
  searchKeyword: string;
  selectedFiles: EntityId[];

  // Action methods
  loadFileList: (category?: string) => Promise<void>;
  loadStatistics: () => Promise<void>;
  uploadFile: (file: File, isPublic?: boolean) => Promise<FileMetadata>;
  deleteFile: (fileId: EntityId) => Promise<void>;
  batchDeleteFiles: (fileIds: EntityId[]) => Promise<number>;
  renameFile: (fileId: EntityId, newFileName: string) => Promise<void>;
  updateFilePermission: (fileId: EntityId, isPublic: boolean) => Promise<void>;
  copyFile: (fileId: EntityId) => Promise<FileMetadata>;
  searchFiles: (keyword: string) => Promise<void>;
  setSelectedFiles: (fileIds: EntityId[]) => void;
  setCurrentCategory: (category: string) => void;
  setError: (error: string | null) => void;
  clearError: () => void;
}

export const useFileManagementStore = create<FileManagementState>((set, get) => ({
  files: [],
  statistics: null,
  isLoading: false,
  error: null,
  currentCategory: 'all',
  searchKeyword: '',
  selectedFiles: [],

  // Load file list
  loadFileList: async (category?: string) => {
    set({ isLoading: true, error: null });
    try {
      const targetCategory = category || get().currentCategory;
      let files: FileMetadata[];

      if (targetCategory === 'all') {
        files = await fileManagementService.getFileList();
      } else {
        files = await fileManagementService.getFileListByCategory(targetCategory);
      }

      set({
        files,
        currentCategory: targetCategory,
        isLoading: false,
        selectedFiles: [],
      });
    } catch (error: any) {
      console.error('Failed to load file list:', error);
      set({
        error: error.message || 'Failed to load file list',
        isLoading: false,
      });
    }
  },

  // Load statistics
  loadStatistics: async () => {
    try {
      const statistics = await fileManagementService.getFileStatistics();
      set({ statistics });
    } catch (error: any) {
      console.error('Failed to load statistics:', error);
      set({ error: error.message || 'Failed to load statistics' });
    }
  },

  // Upload file
  uploadFile: async (file: File, isPublic: boolean = false) => {
    set({ isLoading: true, error: null });
    try {
      const metadata = await fileManagementService.uploadFile(file, isPublic);

      // Refresh the file list
      await get().loadFileList();

      // Refresh statistics
      await get().loadStatistics();

      set({ isLoading: false });
      return metadata;
    } catch (error: any) {
      console.error('Failed to upload file:', error);
      set({
        error: error.message || 'Failed to upload file',
        isLoading: false,
      });
      throw error;
    }
  },

  // Delete file
  deleteFile: async (fileId: EntityId) => {
    set({ isLoading: true, error: null });
    try {
      await fileManagementService.deleteFile(fileId);

      // Remove the file from the list
      set((state) => ({
        files: state.files.filter((f) => f.id !== fileId),
        isLoading: false,
      }));

      // Refresh statistics
      await get().loadStatistics();
    } catch (error: any) {
      console.error('Failed to delete file:', error);
      set({
        error: error.message || 'Failed to delete file',
        isLoading: false,
      });
      throw error;
    }
  },

  // Batch delete files
  batchDeleteFiles: async (fileIds: EntityId[]) => {
    set({ isLoading: true, error: null });
    try {
      const count = await fileManagementService.batchDeleteFiles(fileIds);

      // Remove the files from the list
      set((state) => ({
        files: state.files.filter((f) => !fileIds.includes(f.id)),
        isLoading: false,
        selectedFiles: [],
      }));

      // Refresh statistics
      await get().loadStatistics();

      return count;
    } catch (error: any) {
      console.error('Failed to batch delete files:', error);
      set({
        error: error.message || 'Failed to batch delete files',
        isLoading: false,
      });
      throw error;
    }
  },

  // Rename file
  renameFile: async (fileId: EntityId, newFileName: string) => {
    set({ isLoading: true, error: null });
    try {
      await fileManagementService.renameFile(fileId, newFileName);

      // Update the filename in the list
      set((state) => ({
        files: state.files.map((f) =>
          f.id === fileId ? { ...f, fileName: newFileName } : f
        ),
        isLoading: false,
      }));
    } catch (error: any) {
      console.error('Failed to rename file:', error);
      set({
        error: error.message || 'Failed to rename file',
        isLoading: false,
      });
      throw error;
    }
  },

  // Update file permission
  updateFilePermission: async (fileId: EntityId, isPublic: boolean) => {
    set({ isLoading: true, error: null });
    try {
      await fileManagementService.updateFilePermission(fileId, isPublic);

      // Update the permission setting in the list
      set((state) => ({
        files: state.files.map((f) =>
          f.id === fileId ? { ...f, isPublic } : f
        ),
        isLoading: false,
      }));
    } catch (error: any) {
      console.error('Failed to update file permission:', error);
      set({
        error: error.message || 'Failed to update file permission',
        isLoading: false,
      });
      throw error;
    }
  },

  // Copy file
  copyFile: async (fileId: EntityId) => {
    set({ isLoading: true, error: null });
    try {
      const newFile = await fileManagementService.copyFile(fileId);

      // Refresh the file list
      await get().loadFileList();

      // Refresh statistics
      await get().loadStatistics();

      set({ isLoading: false });
      return newFile;
    } catch (error: any) {
      console.error('Failed to copy file:', error);
      set({
        error: error.message || 'Failed to copy file',
        isLoading: false,
      });
      throw error;
    }
  },

  // Search files
  searchFiles: async (keyword: string) => {
    set({ isLoading: true, error: null, searchKeyword: keyword });
    try {
      const files = await fileManagementService.searchFiles(keyword);
      set({
        files,
        isLoading: false,
      });
    } catch (error: any) {
      console.error('Failed to search files:', error);
      set({
        error: error.message || 'Failed to search files',
        isLoading: false,
      });
    }
  },

  // Set selected files
  setSelectedFiles: (fileIds: EntityId[]) => {
    set({ selectedFiles: fileIds });
  },

  // Set current category
  setCurrentCategory: (category: string) => {
    set({ currentCategory: category });
  },

  // Set error message
  setError: (error: string | null) => {
    set({ error });
  },

  // Clear error message
  clearError: () => {
    set({ error: null });
  },
}));

export default useFileManagementStore;
