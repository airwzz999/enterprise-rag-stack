import { create } from 'zustand';
import { foundationService } from '@/services';

interface AppState {
  systemName: string;
  systemDescription: string;
  requireApproval: boolean;
  enableComments: boolean;
  enableAI: boolean;
  enableAIWriting: boolean;
  enableFullTextSearch: boolean;
  enableEmail: boolean;
  enableWebSocket: boolean;
  maxFileSize: number;
  allowedFileTypes: string;
  loaded: boolean;

  fetchAppConfig: () => Promise<void>;
}

/** System config → store field mapping */
const CONFIG_MAP: Record<string, [keyof AppState, string]> = {
  'system.name':        ['systemName', 'Enterprise Knowledge Base'],
  'system.description': ['systemDescription', 'Intelligent Enterprise Knowledge Management Platform'],
};

/** Boolean config mapping: configKey → [storeKey, defaultValue] */
const BOOLEAN_CONFIG_MAP: Record<string, [keyof AppState, boolean]> = {
  'system.requireApproval':    ['requireApproval', true],
  'system.enableComments':      ['enableComments', true],
  'system.enableAI':            ['enableAI', true],
  'system.enableAIWriting':     ['enableAIWriting', true],
  'system.enableFullTextSearch': ['enableFullTextSearch', true],
  'email.enabled':                ['enableEmail', true],
  'websocket.enabled':            ['enableWebSocket', true],
};

export const useAppStore = create<AppState>((set, get) => ({
  systemName: 'Enterprise Knowledge Base',
  systemDescription: 'Intelligent Enterprise Knowledge Management Platform',
  requireApproval: true,
  enableComments: true,
  enableAI: true,
  enableAIWriting: true,
  enableFullTextSearch: true,
  enableEmail: true,
  enableWebSocket: true,
  maxFileSize: 104857600, // 100MB
  allowedFileTypes: 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,jpg,jpeg,png,gif,bmp,webp,svg,ico,mp4,avi,mov,wmv,flv,mkv,webm,mp3,wav,flac,aac,ogg,m4a,wma',
  loaded: false,

  fetchAppConfig: async () => {
    if (get().loaded) return;
    try {
      const configs: Record<string, string> = await foundationService.config.getPublic();
      const updates: Partial<Pick<AppState, 'systemName' | 'systemDescription'>> = {};

      for (const [configKey, [storeKey, fallback]] of Object.entries(CONFIG_MAP)) {
        const raw = configs[configKey];
        (updates as Record<string, string>)[storeKey] = raw && raw.trim() ? raw : fallback;
      }

      // Read boolean config values
      const booleanUpdates: Record<string, boolean> = {};
      for (const [configKey, [storeKey, defaultVal]] of Object.entries(BOOLEAN_CONFIG_MAP)) {
        const raw = configs[configKey];
        booleanUpdates[storeKey] = raw !== undefined ? raw === 'true' : defaultVal;
      }

      // Read file upload config
      const maxFileSizeRaw = configs['file.upload.max.size'];
      const allowedFileTypesRaw = configs['file.upload.allowed.types'];

      // Ensure pdf is always in the allow list (the database config may omit it)
      const normalizedTypes = allowedFileTypesRaw || 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,jpg,jpeg,png,gif,bmp,webp,svg,ico,mp4,avi,mov,wmv,flv,mkv,webm,mp3,wav,flac,aac,ogg,m4a,wma';
      const typeSet = new Set(normalizedTypes.split(',').map((t: string) => t.trim().toLowerCase()));
      typeSet.add('pdf'); // Hard-guarantee that PDF is always uploadable

      set({
        ...updates,
        ...booleanUpdates,
        maxFileSize: maxFileSizeRaw ? parseInt(maxFileSizeRaw, 10) || 104857600 : 104857600,
        allowedFileTypes: Array.from(typeSet).join(','),
        loaded: true,
      });

      // Update the browser title accordingly
      document.title = (updates.systemName || get().systemName) + ' | Enterprise Knowledge Base';
    } catch {
      // Fall back to default values on failure, and mark as loaded to avoid infinite retries
      set({ loaded: true });
      document.title = get().systemName + ' | Enterprise Knowledge Base';
    }
  },
}));
