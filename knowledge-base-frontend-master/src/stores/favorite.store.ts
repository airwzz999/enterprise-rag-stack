import { create } from 'zustand';
import { favoriteService } from '@/services/favorite.service';

/**
 * Favorites state management
 *
 * @author Top-tier Company Standard
 * @since 1.0.0
 */
interface FavoriteState {
  favorites: Map<string, boolean>; // documentId -> whether favorited
  isLoading: boolean;
  favoriteDocuments: any[]; // List of favorited documents
  toggleFavorite: (documentId: string) => Promise<boolean>;
  checkFavorite: (documentId: string) => Promise<void>;
  loadFavorites: () => Promise<void>;
  isFavorited: (documentId: string) => boolean;
}

export const useFavoriteStore = create<FavoriteState>((set, get) => ({
  favorites: new Map<string, boolean>(),
  isLoading: false,
  favoriteDocuments: [],

  // Toggle favorite status
  toggleFavorite: async (documentId: string) => {
    console.log('🔄 [FavoriteStore] toggleFavorite starting:', documentId);
    console.log('📊 [FavoriteStore] Current state:', get().favorites.get(documentId));

    try {
      const isFavorited = await favoriteService.toggleFavorite(documentId);
      console.log('✅ [FavoriteStore] Backend response:', isFavorited, '(true=favorited, false=not favorited)');

      set((state) => {
        console.log('📝 [FavoriteStore] Map before update:', Array.from(state.favorites.entries()));
        const newFavorites = new Map(state.favorites).set(documentId, isFavorited);
        console.log('📝 [FavoriteStore] Map after update:', Array.from(newFavorites.entries()));
        return {
          favorites: newFavorites,
        };
      });

      return isFavorited;
    } catch (error) {
      console.error('❌ [FavoriteStore] Failed to toggle favorite status:', error);
      throw error;
    }
  },

  // Check favorite status
  checkFavorite: async (documentId: string) => {
    console.log('🔍 [FavoriteStore] checkFavorite starting:', documentId);
    try {
      const isFavorited = await favoriteService.checkFavorite(documentId);
      console.log('✅ [FavoriteStore] checkFavorite returned:', isFavorited);
      set((state) => {
        console.log('📝 [FavoriteStore] checkFavorite state before update:', Array.from(state.favorites.entries()));
        const newFavorites = new Map(state.favorites).set(documentId, isFavorited);
        console.log('📝 [FavoriteStore] checkFavorite state after update:', Array.from(newFavorites.entries()));
        return {
          favorites: newFavorites,
        };
      });
    } catch (error) {
      console.error('Failed to check favorite status:', error);
    }
  },

  // Load favorites list
  loadFavorites: async () => {
    console.log('🔄 [FavoriteStore] loadFavorites starting');
    set({ isLoading: true });
    try {
      const data = await favoriteService.getFavorites();
      console.log('✅ [FavoriteStore] loadFavorites fetched data:', data);

      const favoriteMap = new Map<string, boolean>();

      data.forEach((item: any) => {
        if (item.documentId) {
          const docId = String(item.documentId);
          favoriteMap.set(docId, true);
          console.log('📝 [FavoriteStore] Added to favorites map:', docId, '= true');
        }
      });

      console.log('📊 [FavoriteStore] loadFavorites final map:', Array.from(favoriteMap.entries()));

      set({
        favoriteDocuments: data,
        favorites: favoriteMap,
        isLoading: false,
      });
    } catch (error) {
      console.error('Failed to load favorites list:', error);
      set({ isLoading: false });
    }
  },

  // Determine whether a document is favorited
  isFavorited: (documentId: string) => {
    return get().favorites.get(documentId) === true;
  },
}));

export default useFavoriteStore;
