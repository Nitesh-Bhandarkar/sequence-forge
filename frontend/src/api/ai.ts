import { apiClient } from '../lib/api';
import { getToken } from '../lib/auth';
import type { ApiResponse } from '../types';

export interface ClassifyPlaceholderResponse {
  placeholderType: 'STATIC' | 'DATE' | 'COUNTER';
  dateFormat?: string;
  description: string;
  reasoning: string;
}

export const classifyPlaceholder = async (
  placeholderName: string,
  context?: string,
): Promise<ClassifyPlaceholderResponse> => {
  const res = await apiClient.post<ApiResponse<ClassifyPlaceholderResponse>>(
    '/ai/classify-placeholder',
    { placeholderName, context },
  );
  return res.data.data!;
};

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

// Streams chat tokens via SSE. Calls onToken for each chunk, onDone when complete.
export const streamChat = async (
  messages: ChatMessage[],
  onToken: (token: string) => void,
  onDone: () => void,
  onError: (err: string) => void,
): Promise<void> => {
  const token = getToken();
  const response = await fetch('/api/v1/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ messages }),
  });

  if (!response.ok) {
    onError(`HTTP ${response.status}`);
    return;
  }

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const text = decoder.decode(value, { stream: true });
      for (const line of text.split('\n')) {
        if (line.startsWith('event:done')) {
          onDone();
          return;
        }
        if (line.startsWith('data:')) {
          onToken(line.slice(5));
        }
      }
    }
    onDone();
  } catch (e) {
    onError(String(e));
  } finally {
    reader.releaseLock();
  }
};
