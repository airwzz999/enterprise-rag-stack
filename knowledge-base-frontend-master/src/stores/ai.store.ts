import { create } from 'zustand';
import { AIConversation, AIMessage, AIModelOption } from '@/types';
import { aiService } from '@/services';

interface AIState {
  conversations: AIConversation[];
  currentConversation: AIConversation | null;
  isLoading: boolean;
  isStreaming: boolean;
  currentResponse: string;
  selectedModel: string;
  availableModels: AIModelOption[];
  ragEnabled: boolean;
  kagEnabled: boolean;

  // Actions
  fetchConversations: () => Promise<void>;
  createConversation: (title: string) => Promise<AIConversation>;
  deleteConversation: (id: string) => Promise<void>;
  setCurrentConversation: (conversation: AIConversation | null) => void;
  loadConversationMessages: (conversationId: string) => Promise<void>;
  sendMessage: (message: string, _conversationId?: string) => Promise<void>;
  clearConversation: (id: string) => Promise<void>;
  fetchModels: () => Promise<void>;
  setSelectedModel: (model: string) => void;
  toggleRag: (enabled: boolean) => void;
  toggleKag: (enabled: boolean) => void;
  reset: () => void;
}

export const useAIStore = create<AIState>((set, get) => ({
  conversations: [],
  currentConversation: null,
  isLoading: false,
  isStreaming: false,
  currentResponse: '',
  selectedModel: 'qwen',
  availableModels: [],
  ragEnabled: false,
  kagEnabled: false,

  fetchConversations: async () => {
    set({ isLoading: true });
    try {
      const apiConversations = await aiService.getConversations();
      // Merge with in-memory conversations: keep conversation data with existing messages to avoid overwriting the current active conversation
      const { currentConversation, conversations: existingConvs } = get();
      const existingMap = new Map(
        existingConvs.map((c) => [String(c.id), c])
      );
      if (currentConversation) {
        existingMap.set(String(currentConversation.id), currentConversation);
      }
      const merged = apiConversations.map((api) => {
        const existing = existingMap.get(String(api.id));
        return existing && existing.messages ? { ...api, messages: existing.messages } : api;
      });
      set({ conversations: merged, isLoading: false });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  createConversation: async (title: string) => {
    const conversation = await aiService.createConversation(title);
    set((state) => ({
      conversations: [conversation, ...state.conversations],
      currentConversation: conversation,
    }));
    return conversation;
  },

  deleteConversation: async (id: string) => {
    await aiService.deleteConversation(id);
    set((state) => ({
      conversations: state.conversations.filter((c) => String(c.id) !== String(id)),
      currentConversation:
        String(state.currentConversation?.id) === String(id) ? null : state.currentConversation,
    }));
  },

  setCurrentConversation: (conversation: AIConversation | null) => {
    set({ currentConversation: conversation });
    // If the conversation has not had its messages loaded yet (e.g. clicked from the sidebar), automatically load its history
    if (conversation && conversation.id && (!conversation.messages || conversation.messages.length === 0)) {
      get().loadConversationMessages(String(conversation.id));
    }
  },

  loadConversationMessages: async (conversationId: string) => {
    try {
      const conv = await aiService.getConversation(conversationId);
      // Mapping of the messages field returned by the backend: id→Long/String, role→string, content→string, createdAt→timestamp
      const messages: AIMessage[] = ((conv as any).messages || []).map((m: any) => ({
        id: String(m.id),
        role: m.role,
        content: m.content,
        timestamp: m.createdAt || m.timestamp,
      }));
      set((state) => {
        const updatedConv = {
          ...state.currentConversation,
          id: String(conv.id),
          title: (conv as any).title || state.currentConversation?.title || '',
          messages,
          messageCount: (conv as any).messageCount,
          model: (conv as any).model,
        };
        return {
          currentConversation: updatedConv,
          isLoading: false,
          conversations: state.conversations.map((c) =>
            String(c.id) === String(conversationId)
              ? { ...c, messages, messageCount: (conv as any).messageCount }
              : c
          ),
        };
      });
    } catch (error) {
      console.error('Failed to load conversation messages:', error);
      set({ isLoading: false });
    }
  },

  sendMessage: async (message: string, _conversationId?: string) => {
    const { selectedModel, ragEnabled, kagEnabled } = get();
    set({ isLoading: true, isStreaming: true, currentResponse: '' });

    const userMessage: AIMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: message,
      timestamp: new Date().toISOString(),
    };

    // Update the current conversation or create a local placeholder (no API call — the backend chatStream will create it automatically)
    let currentConv = get().currentConversation;
    if (!currentConv) {
      // Create a local placeholder conversation; id is empty for now, until the backend done event returns the real ID
      currentConv = {
        id: '',
        title: message.slice(0, 30),
        messages: [userMessage],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      set({ currentConversation: currentConv });
    } else {
      // Existing conversation: append the user message (also handles conversations selected from the sidebar with no messages yet)
      const existingMessages = currentConv.messages || [];
      // If this is the first message of an empty conversation, update the title with the message content
      const title = existingMessages.length === 0
        ? message.slice(0, 30)
        : currentConv.title;
      currentConv = {
        ...currentConv,
        title,
        messages: [...existingMessages, userMessage],
        updatedAt: new Date().toISOString(),
      };
      set({ currentConversation: currentConv });
    }

    try {
      // Only pass this when a real backend ID already exists; an empty placeholder is not passed (letting the backend create one)
      const realId = currentConv.id && String(currentConv.id) !== '' ? String(currentConv.id) : undefined;

      // Used to capture the conversationId returned by the backend in the done callback
      let resolvedConversationId: string | undefined = realId;
      let resolvedMessageId: string | undefined;
      let resolvedGraphContext: any = undefined;
      let resolvedCitations: any = undefined;

      // Use the streaming API, passing the selected model
      await aiService.askStream(
        {
          question: message,
          conversationId: realId,
          model: selectedModel,
          context: {
            knowledgeBase: ragEnabled,
            enableKag: kagEnabled,
          },
        },
        // onMessage: streamed text
        (chunk: string) => {
          set((state) => ({
            currentResponse: state.currentResponse + chunk,
          }));
        },
        // onDone: streaming complete, the backend returns a ChatResponseVO
        (result) => {
          resolvedConversationId = String(result.conversationId);
          resolvedMessageId = String(result.messageId);
          resolvedGraphContext = (result as any).graphContext;
          resolvedCitations = (result as any).citations;
        },
        // onError: streaming error
        (errorMsg: string) => {
          throw new Error(errorMsg);
        }
      );

      // The conversationId returned by the backend done event is the authoritative ID
      const finalConversationId = resolvedConversationId || (realId || '');
      const finalContent = get().currentResponse;

      const assistantMessage: AIMessage = {
        id: resolvedMessageId || (Date.now() + 1).toString(),
        role: 'assistant',
        content: finalContent,
        timestamp: new Date().toISOString(),
        citations: resolvedCitations,
        graphContext: resolvedGraphContext,
      };

      set((state) => {
        const conv = state.currentConversation;
        if (!conv) {
          return {
            isLoading: false,
            isStreaming: false,
            currentResponse: '',
          };
        }
        const updatedConv = {
          ...conv,
          id: finalConversationId,
          messages: [...conv.messages, assistantMessage],
          updatedAt: new Date().toISOString(),
        };
        // If this is a new conversation (no backend ID before), add it to the sidebar list
        const isNewConversation = !realId && finalConversationId;
        const conversations = isNewConversation
          ? [updatedConv, ...state.conversations.filter((c) => String(c.id) !== finalConversationId)]
          : state.conversations.map((c) => (String(c.id) === finalConversationId ? updatedConv : c));
        return {
          currentConversation: updatedConv,
          conversations,
          isLoading: false,
          isStreaming: false,
          currentResponse: '',
        };
      });
    } catch (error) {
      set({
        isLoading: false,
        isStreaming: false,
        currentResponse: '',
      });
      throw error;
    }
  },

  clearConversation: async (id: string) => {
    await aiService.clearConversation(id);
    set((state) => ({
      currentConversation:
        state.currentConversation?.id === id
          ? { ...state.currentConversation, messages: [] }
          : state.currentConversation,
    }));
  },

  fetchModels: async () => {
    try {
      const models = await aiService.getModels();
      set({ availableModels: models });
      // If no model is selected, use the default model
      const { selectedModel } = get();
      if (!selectedModel && models.length > 0) {
        const defaultModel = models.find((m) => m.isDefault);
        set({ selectedModel: defaultModel ? defaultModel.key : models[0].key });
      }
    } catch (error) {
      console.error('Failed to fetch AI model list:', error);
      // Fall back to local configuration
      const { AI_CONFIG } = await import('@/constants');
      const fallbackModels: AIModelOption[] = Object.values(AI_CONFIG.MODELS).map((m) => ({
        key: m.key,
        displayName: m.displayName,
        description: m.description,
        isDefault: m.key === AI_CONFIG.DEFAULT_MODEL,
      }));
      set({ availableModels: fallbackModels });
    }
  },

  setSelectedModel: (model: string) => {
    set({ selectedModel: model });
  },

  toggleRag: (enabled: boolean) => {
    set({ ragEnabled: enabled });
  },

  toggleKag: (enabled: boolean) => {
    set({ kagEnabled: enabled });
  },

  reset: () => {
    set({
      conversations: [],
      currentConversation: null,
      currentResponse: '',
      availableModels: [],
    });
  },
}));
