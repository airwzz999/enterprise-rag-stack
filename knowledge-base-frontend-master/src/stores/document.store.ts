import { create } from 'zustand';
import { Document, DocumentFilter } from '@/types';
import { documentService } from '@/services';

interface DocumentState {
  documents: Document[];
  currentDocument: Document | null;
  prevDocument: Document | null;
  nextDocument: Document | null;
  isLoading: boolean;
  total: number;
  currentPage: number;
  pageSize: number;
  filter: DocumentFilter;
  fetchingDocumentId: string | null;

  // Actions
  fetchDocuments: (filter?: DocumentFilter) => Promise<void>;
  fetchDocument: (id: string) => Promise<void>;
  fetchAdjacentDocuments: (id: string) => Promise<void>;
  createDocument: (data: Partial<Document>) => Promise<Document>;
  updateDocument: (id: string, data: Partial<Document>) => Promise<void>;
  deleteDocument: (id: string) => Promise<void>;
  likeDocument: (id: string) => Promise<void>;
  setCurrentDocument: (document: Document | null) => void;
  setFilter: (filter: Partial<DocumentFilter>) => void;
  reset: () => void;
}

const defaultFilter: DocumentFilter = {
  page: 1,
  pageSize: 12,
  // Note: the backend does not currently support the sortBy and sortOrder parameters
};

export const useDocumentStore = create<DocumentState>((set, get) => ({
  documents: [],
  currentDocument: null,
  prevDocument: null,
  nextDocument: null,
  isLoading: false,
  total: 0,
  currentPage: 1,
  pageSize: 12,
  filter: defaultFilter,
  fetchingDocumentId: null,

  fetchDocuments: async (filter?: DocumentFilter) => {
    // Prevent duplicate requests
    const currentState = get();
    if (currentState.isLoading) {
      return;
    }

    set({ isLoading: true });
    try {
      const currentFilter = get().filter;
      const finalFilter = { ...currentFilter, ...filter };
      const response = await documentService.getDocuments(finalFilter);

      // Only update the data, not the filter (to avoid an infinite loop)
      set({
        documents: response.list,
        total: response.total,
        currentPage: response.page,
        pageSize: response.pageSize,
        isLoading: false,
      });

      // Only update the filter if new filter params were passed in
      if (filter && Object.keys(filter).length > 0) {
        set({ filter: finalFilter });
      }
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  fetchDocument: async (id: string) => {
    const currentState = get();
    // Prevent duplicate requests: skip if the same document is already being fetched
    if (currentState.fetchingDocumentId === id) {
      return;
    }
    set({ isLoading: true, fetchingDocumentId: id });
    try {
      const document = await documentService.getDocument(id);
      set({
        currentDocument: document,
        isLoading: false,
        fetchingDocumentId: null,
      });
      // Fetch adjacent documents
      get().fetchAdjacentDocuments(id);
    } catch (error) {
      set({ isLoading: false, fetchingDocumentId: null });
      throw error;
    }
  },

  fetchAdjacentDocuments: async (id: string) => {
    try {
      const neighbors = await documentService.getDocumentNeighbors(id);
      set({
        prevDocument: neighbors.prevId ? { id: neighbors.prevId, title: neighbors.prevTitle } as Document : null,
        nextDocument: neighbors.nextId ? { id: neighbors.nextId, title: neighbors.nextTitle } as Document : null,
      });
    } catch (error) {
      // Set to null if the fetch fails
      set({
        prevDocument: null,
        nextDocument: null,
      });
    }
  },

  createDocument: async (data: any) => {
    // The backend returns the document ID (Long type)
    const documentId = await documentService.createDocument(data);
    // Construct a basic document object containing the ID returned by the backend
    const document: Document = {
      id: String(documentId),
      title: data.title,
      content: data.content || '',
      summary: data.summary,
      categoryId: data.categoryId ? String(data.categoryId) : undefined,
      tags: data.tags ? data.tags.split(',').filter(Boolean) : [],
      status: data.status === 1 ? 'published' : data.status === 2 ? 'archived' : 'draft',
      authorId: '1', // Fetched from the backend
      authorName: 'Current User', // Fetched from the backend
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    set((state) => ({
      documents: [document, ...state.documents],
      total: state.total + 1,
    }));
    return document;
  },

  updateDocument: async (id: string, data: Partial<Document>) => {
    const updatedDocument = await documentService.updateDocument(id, data);
    set((state) => ({
      documents: state.documents.map((doc) =>
        doc.id === id ? updatedDocument : doc
      ),
      currentDocument:
        state.currentDocument?.id === id ? updatedDocument : state.currentDocument,
    }));
  },

  deleteDocument: async (id: string) => {
    await documentService.deleteDocument(id);
    set((state) => ({
      documents: state.documents.filter((doc) => doc.id !== id),
      total: state.total - 1,
      currentDocument:
        state.currentDocument?.id === id ? null : state.currentDocument,
    }));
  },

  likeDocument: async (id: string) => {
    const currentDoc = get().currentDocument;
    const isCurrentlyLiked = currentDoc?.id === id ? currentDoc.isLiked : false;

    if (isCurrentlyLiked) {
      await documentService.unlikeDocument(id);
    } else {
      await documentService.likeDocument(id);
    }

    // 1. Optimistically toggle the isLiked state first to give the user instant feedback
    set((state) => ({
      documents: state.documents.map((doc) =>
        doc.id === id ? { ...doc, isLiked: !doc.isLiked } : doc
      ),
      currentDocument:
        state.currentDocument?.id === id
          ? { ...state.currentDocument, isLiked: !state.currentDocument.isLiked }
          : state.currentDocument,
    }));

    // 2. Refetch document details from the backend to get the real likeCount and isLiked from the database
    try {
      const document = await documentService.getDocument(id);
      set((state) => ({
        documents: state.documents.map((doc) =>
          doc.id === id
            ? { ...doc, likeCount: document.likeCount, isLiked: document.isLiked }
            : doc
        ),
        currentDocument:
          state.currentDocument?.id === id
            ? { ...state.currentDocument, likeCount: document.likeCount, isLiked: document.isLiked }
            : state.currentDocument,
      }));
    } catch {
      // Do not throw if the backend fetch fails; keep the optimistically updated isLiked state
    }
  },

  setCurrentDocument: (document: Document | null) => {
    set({ currentDocument: document });
  },

  setFilter: (filter: Partial<DocumentFilter>) => {
    set((state) => {
      // Create a new filter object, but only update the provided fields
      const newFilter = { ...state.filter, ...filter };
      // Only update when the filter has actually changed
      const hasChanged = (Object.keys(filter) as Array<keyof DocumentFilter>).some(
        key => JSON.stringify(state.filter[key]) !== JSON.stringify(filter[key])
      );

      if (hasChanged) {
        return { filter: newFilter };
      }
      return {};
    });
  },

  reset: () => {
    set({
      documents: [],
      currentDocument: null,
      prevDocument: null,
      nextDocument: null,
      total: 0,
      currentPage: 1,
      filter: defaultFilter,
    });
  },
}));
