import { useState, useRef, useEffect } from 'react';
import { streamChat, type ChatMessage } from '../api/ai';

interface AIChatPanelProps {
  templateContext?: string;
}

const SUGGESTIONS = [
  'Help me design a template for invoice numbers',
  'What format should I use for lot numbers?',
  'How do I set up a yearly resetting counter?',
  'Explain the FINANCIAL_YEAR date format',
];

export default function AIChatPanel({ templateContext }: AIChatPanelProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [streamingContent, setStreamingContent] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  const send = async (text: string) => {
    if (!text.trim() || streaming) return;

    const userMsg: ChatMessage = { role: 'user', content: text };
    const history = [...messages, userMsg];
    setMessages(history);
    setInput('');
    setStreaming(true);
    setStreamingContent('');

    // Prepend template context as first user message if present
    const contextualHistory: ChatMessage[] = templateContext
      ? [{ role: 'user', content: `I am building a template: ${templateContext}` }, ...history]
      : history;

    let accumulated = '';

    await streamChat(
      contextualHistory,
      (token) => {
        accumulated += token;
        setStreamingContent(accumulated);
      },
      () => {
        setMessages((prev) => [...prev, { role: 'assistant', content: accumulated }]);
        setStreamingContent('');
        setStreaming(false);
      },
      (err) => {
        setMessages((prev) => [
          ...prev,
          { role: 'assistant', content: `Error: ${err}. Is ANTHROPIC_API_KEY set?` },
        ]);
        setStreamingContent('');
        setStreaming(false);
      },
    );
  };

  return (
    <div className="flex flex-col h-full border border-gray-200 rounded-xl bg-white overflow-hidden">
      <div className="px-4 py-3 border-b border-gray-100 flex items-center gap-2">
        <span className="text-sm font-semibold text-gray-700">AI Assistant</span>
        <span className="text-xs bg-brand-50 text-brand-600 px-2 py-0.5 rounded-full">claude-opus-4-8</span>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3 min-h-0">
        {messages.length === 0 && !streaming && (
          <div className="space-y-2">
            <p className="text-xs text-gray-400 text-center">Ask anything about template design</p>
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                onClick={() => send(s)}
                className="w-full text-left text-xs text-gray-600 bg-gray-50 hover:bg-brand-50 hover:text-brand-700 px-3 py-2 rounded-lg transition-colors"
              >
                {s}
              </button>
            ))}
          </div>
        )}

        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[85%] text-xs rounded-xl px-3 py-2 whitespace-pre-wrap leading-relaxed ${
                msg.role === 'user'
                  ? 'bg-brand-600 text-white'
                  : 'bg-gray-100 text-gray-800'
              }`}
            >
              {msg.content}
            </div>
          </div>
        ))}

        {streaming && streamingContent && (
          <div className="flex justify-start">
            <div className="max-w-[85%] text-xs rounded-xl px-3 py-2 bg-gray-100 text-gray-800 whitespace-pre-wrap leading-relaxed">
              {streamingContent}
              <span className="inline-block w-1.5 h-3 bg-gray-400 ml-0.5 animate-pulse" />
            </div>
          </div>
        )}

        {streaming && !streamingContent && (
          <div className="flex justify-start">
            <div className="text-xs bg-gray-100 rounded-xl px-3 py-2 text-gray-400">
              Thinking…
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>

      <div className="p-3 border-t border-gray-100">
        <form
          onSubmit={(e) => { e.preventDefault(); send(input); }}
          className="flex gap-2"
        >
          <input
            className="input flex-1 text-xs py-2"
            placeholder="Ask about templates…"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={streaming}
          />
          <button
            type="submit"
            disabled={streaming || !input.trim()}
            className="btn-primary px-3 py-2 text-xs"
          >
            ↑
          </button>
        </form>
      </div>
    </div>
  );
}
