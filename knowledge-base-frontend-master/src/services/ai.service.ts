import { http } from './request';
import { AIRequest, AIConversation, AIQuickQuestion, AIModelOption, WritingRequest, WritingResult, WritingTemplate } from '@/types';
import { tokenStorage } from '@/utils/token-storage';

/**
 * Batched callback for streamed output
 *
 * <p>Every flushInterval ms, the tokens buffered so far are sent to onMessage as a batch,
 * rather than triggering a callback per token. This reduces the state update frequency
 * and avoids layout jitter on the page.</p>
 */
function createBatchedCallback(
  onMessage: (chunk: string) => void,
  flushInterval: number = 80,
): { addToken: (token: string) => void; flush: () => void } {
  let buffer: string[] = [];
  let timer: ReturnType<typeof setInterval> | null = null;

  const startTimer = () => {
    if (timer) return;
    timer = setInterval(() => {
      if (buffer.length > 0) {
        const batch = buffer.join('');
        buffer = [];
        onMessage(batch);
      }
    }, flushInterval);
  };

  return {
    addToken: (token: string) => {
      buffer.push(token);
      startTimer();
    },
    flush: () => {
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
      if (buffer.length > 0) {
        const batch = buffer.join('');
        buffer = [];
        onMessage(batch);
      }
    },
  };
}

export const aiService = {
  // Get list of available AI models
  getModels: () => {
    return http.get<AIModelOption[]>('/ai/chat/models');
  },

  // AI Q&A
  ask: (data: AIRequest) => {
    return http.post<{ content: string; conversationId: string; messageId: string; tokens: number }>('/ai/chat', {
      content: data.question,
      conversationId: data.conversationId,
      model: data.model,
    });
  },

  // Get conversation history
  getConversations: async (): Promise<AIConversation[]> => {
    const pageData = await http.get<any>('/ai/conversation/list');
    return pageData?.records || pageData || [];
  },

  // Get conversation details
  getConversation: (id: string) => {
    return http.get<AIConversation>(`/ai/conversation/${id}`);
  },

  // Create new conversation
  createConversation: (title: string) => {
    return http.post<AIConversation>('/ai/conversation', { title });
  },

  // Delete conversation
  deleteConversation: (id: string) => {
    return http.delete(`/ai/conversation/${id}`);
  },

  // Clear conversation history
  clearConversation: (id: string) => {
    return http.delete(`/ai/conversation/${id}/messages`);
  },

  // Streaming Q&A (with token batching to reduce refresh frequency)
  askStream: (
    data: AIRequest,
    onMessage: (message: string) => void,
    onDone?: (result: { conversationId: number | string; messageId: number | string; content: string; tokens: number }) => void,
    onError?: (error: string) => void,
  ) => {
    // Create a batched callback: flush every 80ms to avoid per-token refreshes causing layout jitter
    const { addToken, flush } = createBatchedCallback(onMessage, 80);

    return fetch(`${import.meta.env.VITE_API_BASE_URL}/ai/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': tokenStorage.getAuthorizationHeader(),
      },
      body: JSON.stringify({
        content: data.question,
        conversationId: data.conversationId,
        model: data.model,
        enableRag: data.context?.knowledgeBase === true,
        enableKag: data.context?.enableKag === true,
      }),
    }).then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Unable to read response stream');
      }

      let buffer = '';
      let currentEvent = '';
      let messageLines: string[] = [];  // Accumulates multiple data lines for a message event

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        // Keep the last incomplete line in the buffer
        buffer = lines.pop() || '';

        for (const line of lines) {
          // Parse an event: line
          if (line.startsWith('event:')) {
            // Flush accumulated message data before switching events
            if (messageLines.length > 0) {
              addToken(messageLines.join('\n'));
              messageLines = [];
            }
            currentEvent = line.slice(6).trim();
            continue;
          }

          // Parse a data: line (strip the 'data:' prefix, keeping one optional leading space)
          if (line.startsWith('data:')) {
            const data = line.startsWith('data: ') ? line.slice(6) : line.slice(5);

            if (currentEvent === 'error') {
              flush(); // Flush accumulated content before the error
              onError?.(data);
              continue;
            }

            if (currentEvent === 'done') {
              flush(); // Flush the last batch before completing
              try {
                const parsed = JSON.parse(data);
                onDone?.(parsed);
              } catch {
                // done event data is not JSON, ignore
              }
              continue;
            }

            // message event (or no event type): accumulate data lines, preserving blank lines
            messageLines.push(data);
            continue;
          }

          // Blank line = SSE event boundary, flush accumulated message data
          if (line === '') {
            if (messageLines.length > 0) {
              addToken(messageLines.join('\n'));
              messageLines = [];
            }
            currentEvent = '';
          }
        }
      }

      // Flush any remaining message data when the stream ends
      if (messageLines.length > 0) {
        addToken(messageLines.join('\n'));
      }
      // Ensure the last batch is flushed
      flush();
    });
  },

  // Get AI suggestions
  getSuggestions: (documentId?: string) => {
    return http.get<string[]>('/ai/suggestions', {
      params: { documentId },
    });
  },

  // Submit feedback
  submitFeedback: (data: { messageId: string; conversationId: string; type: 'like' | 'dislike' }) => {
    return http.post('/ai/feedback', data);
  },

  // Get quick questions
  getQuickQuestions: () => {
    return http.get<AIQuickQuestion[]>('/ai/quick-questions');
  },

  // ==================== AI Writing APIs ====================

  // Get writing templates
  getWritingTemplates: () => {
    return http.get<WritingTemplate[]>('/ai/writing/templates');
  },

  // Generate writing content
  generateWriting: (data: WritingRequest) => {
    return http.post<WritingResult>('/ai/writing/generate', { ...data, actionType: 'generate' });
  },

  // Generate writing content (streaming)
  generateWritingStream: (
    data: WritingRequest,
    onMessage: (chunk: string) => void,
    onDone?: (result: WritingResult) => void,
    onError?: (error: string) => void,
  ) => {
    const apiBase = import.meta.env.VITE_API_BASE_URL;
    return fetch(`${apiBase}/ai/writing/generate/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': tokenStorage.getAuthorizationHeader(),
      },
      body: JSON.stringify(data),
    }).then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      if (!reader) throw new Error('Unable to read response stream');

      let buffer = '';
      let currentEvent = '';
      let messageLines: string[] = [];

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('event:')) {
            if (messageLines.length > 0) {
              onMessage(messageLines.join('\n'));
              messageLines = [];
            }
            currentEvent = line.slice(6).trim();
            continue;
          }
          if (line.startsWith('data:')) {
            const data = line.startsWith('data: ') ? line.slice(6) : line.slice(5);
            if (currentEvent === 'error') {
              onError?.(data);
              continue;
            }
            if (currentEvent === 'done') {
              try {
                const parsed = JSON.parse(data);
                onDone?.(parsed);
              } catch { /* ignore */ }
              continue;
            }
            messageLines.push(data);
            continue;
          }
          if (line === '') {
            if (messageLines.length > 0) {
              onMessage(messageLines.join('\n'));
              messageLines = [];
            }
            currentEvent = '';
          }
        }
      }
      if (messageLines.length > 0) {
        onMessage(messageLines.join('\n'));
      }
    });
  },

  // Expand content
  expandWriting: (data: WritingRequest) => {
    return http.post<WritingResult>('/ai/writing/expand', { ...data, actionType: 'expand' });
  },

  // Optimize content
  optimizeWriting: (data: WritingRequest) => {
    return http.post<WritingResult>('/ai/writing/optimize', { ...data, actionType: 'optimize' });
  },

  // Continue writing content
  continueWriting: (data: WritingRequest) => {
    return http.post<WritingResult>('/ai/writing/continue', { ...data, actionType: 'continue' });
  },

  // ==================== Document Summary APIs ====================

  /**
   * Generate an AI summary based on document content (non-streaming)
   */
  generateDocSummary: (params: { content: string; title?: string; length?: number }) => {
    return http.post<{
      processType: string;
      processedContent: string;
      success: boolean;
      message: string;
      tokens?: number;
    }>('/ai/document/summary/content', {
      content: params.content,
      title: params.title || '',
      processType: 'summary',
      processParams: {
        summaryLength: params.length || 200,
      },
    });
  },

  /**
   * Generate an AI summary based on document content (streaming SSE)
   */
  generateDocSummaryStream: (
    params: { content: string; title?: string; length?: number },
    onChunk: (chunk: string) => void,
    onDone?: (result: { processedContent: string; success: boolean; message: string }) => void,
    onError?: (error: string) => void,
  ) => {
    const apiBase = import.meta.env.VITE_API_BASE_URL;
    return fetch(`${apiBase}/ai/document/summary/content/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': tokenStorage.getAuthorizationHeader(),
      },
      body: JSON.stringify({
        content: params.content,
        title: params.title || '',
        processType: 'summary',
        processParams: {
          summaryLength: params.length || 200,
        },
      }),
    }).then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      if (!reader) throw new Error('Unable to read response stream');

      let buffer = '';
      let currentEvent = '';
      let messageLines: string[] = [];

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('event:')) {
            if (messageLines.length > 0) {
              onChunk(messageLines.join('\n'));
              messageLines = [];
            }
            currentEvent = line.slice(6).trim();
            continue;
          }
          if (line.startsWith('data:')) {
            const data = line.startsWith('data: ') ? line.slice(6) : line.slice(5);
            if (currentEvent === 'error') {
              onError?.(data);
              continue;
            }
            if (currentEvent === 'done') {
              try {
                const parsed = JSON.parse(data);
                onDone?.(parsed);
              } catch { /* ignore */ }
              continue;
            }
            messageLines.push(data);
            continue;
          }
          if (line === '') {
            if (messageLines.length > 0) {
              onChunk(messageLines.join('\n'));
              messageLines = [];
            }
            currentEvent = '';
          }
        }
      }
      if (messageLines.length > 0) {
        onChunk(messageLines.join('\n'));
      }
    });
  },
};

export default aiService;
