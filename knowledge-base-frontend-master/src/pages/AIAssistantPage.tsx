import React, { useCallback, useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react';
import {
  Card,
  Input,
  List,
  Typography,
  Button,
  Space,
  Tag,
  Avatar,
  Spin,
  Dropdown,
  Select,
  Switch,
  MenuProps,
  Tooltip,
} from 'antd';
import { App } from 'antd';
import {
  SendOutlined,
  RobotOutlined,
  UserOutlined,
  PlusOutlined,
  DeleteOutlined,
  LikeOutlined,
  DislikeOutlined,
  BookOutlined,
  ThunderboltOutlined,
  MoreOutlined,
  CopyOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { useAIStore, useAuthStore, useAppStore } from '@/stores';
import { aiService } from '@/services';
import { AIQuickQuestion, Citation } from '@/types';
import type { GraphContext } from '@/types';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { prepareStreamingMarkdown } from '@/utils/markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import type { Components } from 'react-markdown';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

// ==================== Design System: Colors & Style Constants ====================

const COLORS = {
  // Page background
  pageBg: '#f7f8fa',
  // Card
  cardBg: '#ffffff',
  cardBorder: '#e9ebf0',
  // Sidebar
  sidebarBg: '#fafbfc',
  sidebarBorder: '#edf0f4',
  sidebarHover: '#f1f4f9',
  sidebarActive: '#e8edf4',
  // User message bubble
  userBubbleBg: '#eff3ff',
  userBubbleBorder: '#d9e2f7',
  userBubbleText: '#1a1d23',
  userAvatarBg: '#5b7ce6',
  // AI message bubble
  aiBubbleBg: '#ffffff',
  aiBubbleBorder: '#e9ebf0',
  aiBubbleText: '#1a1d23',
  aiAvatarStart: '#6366f1',
  aiAvatarEnd: '#8b5cf6',
  // Action button
  actionColor: '#9ca3af',
  actionHoverColor: '#4b5563',
  actionActiveColor: '#5b7ce6',
  // Input area
  inputBg: '#f9fafb',
  inputBorder: '#e5e7eb',
  inputFocusBorder: '#6366f1',
  sendBtnStart: '#6366f1',
  sendBtnEnd: '#8b5cf6',
  // Quick questions
  quickTagBg: '#f1f4f9',
  quickTagHover: '#e4e9f2',
  quickTagText: '#4b5563',
  // Other
  dividerColor: '#edf0f4',
  timestampColor: '#9ca3af',
  codeBlockBg: '#1e1e2e',
  codeBlockText: '#cdd6f4',
};

const SHADOWS = {
  card: '0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02)',
  aiBubble: '0 1px 2px rgba(0, 0, 0, 0.03)',
  userBubble: 'none',
};

// ==================== Inject Markdown Styles (Top-tier Design Standards) ====================

const MARKDOWN_STYLES = `
/* ---------- Base typography ---------- */
.ai-message-content {
  color: #1e293b;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}
.ai-message-content > *:first-child { margin-top: 0 !important; }
.ai-message-content > *:last-child { margin-bottom: 0 !important; }

/* ---------- Headings ---------- */
.ai-message-content h1 { font-size: 1.35em; font-weight: 700; margin: 1.2em 0 0.6em; color: #0f172a; letter-spacing: -0.01em; padding-bottom: 0.3em; border-bottom: 1px solid #e9ebf0; }
.ai-message-content h2 { font-size: 1.2em; font-weight: 700; margin: 1em 0 0.5em; color: #111827; letter-spacing: -0.01em; }
.ai-message-content h3 { font-size: 1.08em; font-weight: 600; margin: 0.9em 0 0.45em; color: #1f2937; }
.ai-message-content h4 { font-size: 1em; font-weight: 600; margin: 0.8em 0 0.4em; color: #374151; }
.ai-message-content h5, .ai-message-content h6 { font-size: 0.95em; font-weight: 600; margin: 0.7em 0 0.35em; color: #4b5563; }

/* ---------- Paragraphs ---------- */
.ai-message-content p { margin: 0.6em 0; line-height: 1.8; }
.ai-message-content p:first-child { margin-top: 0; }
.ai-message-content p:last-child { margin-bottom: 0; }

/* ---------- Emphasis & bold ---------- */
.ai-message-content strong { font-weight: 650; color: #0f172a; }
.ai-message-content em { font-style: italic; color: #374151; }

/* ---------- Inline code ---------- */
.ai-message-content code:not(pre code) {
  background: #f1f5f9;
  color: #e11d48;
  padding: 0.15em 0.45em;
  border-radius: 4px;
  font-size: 0.88em;
  font-family: 'SF Mono', 'Fira Code', 'JetBrains Mono', Menlo, Consolas, monospace;
  font-weight: 500;
  border: 1px solid #e2e8f0;
}

/* ---------- Lists ---------- */
.ai-message-content ul, .ai-message-content ol {
  padding-left: 1.6em;
  margin: 0.6em 0;
}
.ai-message-content li {
  margin: 0.3em 0;
  line-height: 1.75;
  padding-left: 0.15em;
}
.ai-message-content li > p { margin: 0.15em 0; }
.ai-message-content ul ul, .ai-message-content ol ol, .ai-message-content ul ol, .ai-message-content ol ul {
  margin: 0.2em 0;
}
.ai-message-content ul > li::marker { color: #94a3b8; }
.ai-message-content ol > li::marker { color: #64748b; font-weight: 500; font-size: 0.9em; }

/* ---------- Blockquotes ---------- */
.ai-message-content blockquote {
  border-left: 3px solid #6366f1;
  padding: 0.6em 1em;
  margin: 0.8em 0;
  background: linear-gradient(90deg, #f5f3ff 0%, #faf9ff 100%);
  color: #4b5563;
  border-radius: 0 8px 8px 0;
  font-style: normal;
}
.ai-message-content blockquote p { margin: 0.3em 0; }
.ai-message-content blockquote strong { color: #374151; }

/* ---------- Links ---------- */
.ai-message-content a {
  color: #6366f1;
  text-decoration: none;
  border-bottom: 1px solid #c7d2fe;
  transition: border-color 0.15s ease, color 0.15s ease;
}
.ai-message-content a:hover {
  color: #4f46e5;
  border-bottom-color: #8b9cf7;
}

/* ---------- Dividers ---------- */
.ai-message-content hr {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #e5e7eb 20%, #e5e7eb 80%, transparent 100%);
  margin: 1.2em 0;
}

/* ---------- Tables ---------- */
.ai-message-content .md-table-wrapper {
  overflow-x: auto;
  margin: 1em 0;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.ai-message-content table {
  border-collapse: collapse;
  width: 100%;
  font-size: 0.9em;
  min-width: 400px;
}
.ai-message-content thead {
  background: #f8fafc;
}
.ai-message-content thead th {
  font-weight: 600;
  color: #1f2937;
  text-align: left;
}
.ai-message-content th, .ai-message-content td {
  border-bottom: 1px solid #f1f5f9;
  padding: 0.65em 0.9em;
}
.ai-message-content th {
  font-size: 0.85em;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.ai-message-content tbody tr:nth-child(even) { background: #fafbfc; }
.ai-message-content tbody tr:hover { background: #f1f5f9; }
.ai-message-content tbody tr:last-child td { border-bottom: none; }

/* ---------- Code blocks (custom CodeBlock component + generic pre fallback) ---------- */
.ai-message-content .code-block-wrapper {
  margin: 1em 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #2d2d3f;
  box-shadow: 0 2px 10px rgba(0,0,0,0.08);
}
.ai-message-content .code-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.45em 1em;
  background: #252536;
  border-bottom: 1px solid #3a3a55;
}
.ai-message-content .code-block-lang {
  font-size: 11px;
  color: #8b949e;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 500;
  font-family: 'SF Mono', 'Fira Code', Menlo, monospace;
}
.ai-message-content .code-block-copy {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #8b949e;
  cursor: pointer;
  background: none;
  border: none;
  padding: 3px 8px;
  border-radius: 4px;
  transition: all 0.15s ease;
  font-family: inherit;
}
.ai-message-content .code-block-copy:hover {
  color: #e6edf3;
  background: #3a3a55;
}
.ai-message-content .code-block-wrapper pre {
  margin: 0 !important;
  border-radius: 0 !important;
  white-space: pre !important;
  word-break: normal !important;
}
.ai-message-content .code-block-wrapper code {
  font-size: 0.82em !important;
  line-height: 1.6 !important;
  font-family: 'SF Mono', 'Fira Code', 'JetBrains Mono', Menlo, Consolas, monospace !important;
  white-space: pre !important; /* Prevent CSS from collapsing spaces in code */
  word-break: normal !important; /* Prevent line breaks in the middle of keywords */
}

/* Generic pre fallback (outside code-block-wrapper) */
.ai-message-content pre:not(.code-block-wrapper pre) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 1em 1.2em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.8em 0;
  line-height: 1.55;
  font-size: 0.85em;
  white-space: pre; /* Prevent CSS from collapsing spaces in code */
  word-break: normal; /* Prevent line breaks in the middle of keywords */
}
.ai-message-content pre:not(.code-block-wrapper pre) code {
  background: none !important;
  padding: 0 !important;
  color: inherit;
  font-size: inherit;
  border: none !important;
  white-space: pre !important; /* Prevent CSS from collapsing spaces in code */
  word-break: normal !important; /* Prevent line breaks in the middle of keywords */
}

/* ---------- Scrollbar inside code blocks ---------- */
.ai-message-content pre::-webkit-scrollbar,
.ai-message-content .code-block-wrapper pre::-webkit-scrollbar {
  height: 6px;
}
.ai-message-content pre::-webkit-scrollbar-track,
.ai-message-content .code-block-wrapper pre::-webkit-scrollbar-track {
  background: transparent;
}
.ai-message-content pre::-webkit-scrollbar-thumb,
.ai-message-content .code-block-wrapper pre::-webkit-scrollbar-thumb {
  background: #4a4a65;
  border-radius: 3px;
}

/* ---------- Images ---------- */
.ai-message-content img {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  margin: 0.4em 0;
}

/* ---------- Task lists ---------- */
.ai-message-content input[type="checkbox"] {
  margin-right: 0.4em;
  accent-color: #6366f1;
}

/* ---------- Footnotes ---------- */
.ai-message-content sup a { font-size: 0.8em; color: #6366f1; }
`;

// ==================== Code Block Component (Syntax Highlighting + Copy) ====================

const CodeBlock: React.FC<{
  language: string | undefined;
  value: string;
}> = React.memo(({ language, value }) => {
  const { message } = App.useApp();
  const [copied, setCopied] = useState(false);
  const lang = language || 'text';

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      message.error('Copy failed');
    }
  };

  return (
    <div className="code-block-wrapper">
      <div className="code-block-header">
        <span className="code-block-lang">{lang}</span>
        <button className="code-block-copy" onClick={handleCopy} type="button">
          {copied ? (
            <>
              <span style={{ fontSize: 12 }}>&#10003;</span> Copied
            </>
          ) : (
            <>
              <CopyOutlined style={{ fontSize: 12 }} /> Copy code
            </>
          )}
        </button>
      </div>
      <SyntaxHighlighter
        language={lang}
        style={oneDark}
        wrapLongLines={true}
        customStyle={{
          margin: 0,
          borderRadius: 0,
          fontSize: '0.82em',
          lineHeight: 1.6,
          whiteSpace: 'pre',
        }}
        codeTagProps={{
          style: {
            fontFamily: "'SF Mono', 'Fira Code', 'JetBrains Mono', Menlo, Consolas, monospace",
            whiteSpace: 'pre',
          },
        }}
      >
        {value}
      </SyntaxHighlighter>
    </div>
  );
});

// ==================== ReactMarkdown Component Mapping ====================

/**
 * Extract code text from the HAST AST node (more reliable than extracting from React children).
 * <p>ReactMarkdown v9 passes the raw HAST/MDAST node via the `node` prop;
 * reading node.children[].value directly avoids whitespace loss caused by React children reorganization.</p>
 */
const extractCodeFromNode = (node: any): string => {
  if (!node?.children) return '';
  if (typeof node.children === 'string') return node.children;
  return node.children
    .filter((c: any) => c.type === 'text')
    .map((c: any) => c.value)
    .join('');
};

/**
 * Recursively extract plain text content from React children, preserving all whitespace.
 * <p>Used as a fallback when text cannot be extracted from the AST node (e.g. inline code).</p>
 */
const extractText = (children: React.ReactNode): string => {
  if (typeof children === 'string') return children;
  if (Array.isArray(children)) return children.map(extractText).join('');
  if (React.isValidElement(children)) return extractText((children.props as any).children);
  return String(children);
};

const markdownComponents: Components = {
  code({ className, children, ...props }) {
    const match = /language-(\w+)/.exec(className || '');
    // Prefer extracting text from the HAST node (preserves original spacing), fall back to extractText
    const rawNode = (props as any).node;
    const value = (rawNode ? extractCodeFromNode(rawNode) : extractText(children)).replace(/\n$/, '');

    // Code block with a language identifier -> use the CodeBlock component
    if (match) {
      return <CodeBlock language={match[1]} value={value} />;
    }

    // Inline code
    return (
      <code className={className} {...props}>
        {children}
      </code>
    );
  },
  pre({ children }) {
    // If children is already a CodeBlock (already wrapped), return directly; otherwise use fallback pre
    return <>{children}</>;
  },
  table({ children }) {
    return <div className="md-table-wrapper"><table>{children}</table></div>;
  },
  img({ src, alt }) {
    return (
      <img
        src={src}
        alt={alt || ''}
        style={{ maxWidth: '100%', borderRadius: 8, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }}
        loading="lazy"
      />
    );
  },
  a({ href, children }) {
    return (
      <a href={href} target="_blank" rel="noopener noreferrer">
        {children}
      </a>
    );
  },
};

// ==================== Chat Input Component (isolated to avoid full-page re-renders while typing) ====================

interface ChatInputProps {
  onSend: (content: string) => void;
  isLoading: boolean;
  isTyping: boolean;
}

export interface ChatInputHandle {
  submitWithText: (text: string) => void;
}

const ChatInput = forwardRef<ChatInputHandle, ChatInputProps>(({ onSend, isLoading, isTyping }, ref) => {
  const [inputValue, setInputValue] = useState('');

  useImperativeHandle(ref, () => ({
    submitWithText: (text: string) => {
      onSend(text);
    },
  }));

  const handleSend = useCallback(() => {
    if (!inputValue.trim()) return;
    onSend(inputValue.trim());
    setInputValue('');
  }, [inputValue, onSend]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (inputValue.trim()) {
        onSend(inputValue.trim());
        setInputValue('');
      }
    }
  }, [inputValue, onSend]);

  return (
    <div
      style={{
        padding: '10px 20px 12px',
        borderTop: `1px solid ${COLORS.inputBorder}`,
        background: '#fafbfc',
        flexShrink: 0,
      }}
    >
      <div
        style={{
          maxWidth: 900,
          margin: '0 auto',
          display: 'flex',
          gap: 10,
          alignItems: 'flex-end',
        }}
      >
        <div
          style={{
            flex: 1,
            background: '#ffffff',
            borderRadius: 14,
            border: `1px solid ${COLORS.inputBorder}`,
            transition: 'border-color 0.2s ease, box-shadow 0.2s ease',
            overflow: 'hidden',
          }}
        >
          <TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Type your question, Enter to send, Shift+Enter for a new line"
            autoSize={{ minRows: 1, maxRows: 6 }}
            disabled={isLoading || isTyping}
            style={{
              border: 'none',
              background: 'transparent',
              resize: 'none',
              padding: '10px 14px',
              fontSize: 14,
              lineHeight: 1.6,
              borderRadius: 0,
              boxShadow: 'none',
            }}
            onFocus={(e) => {
              const wrapper = (e.currentTarget as HTMLElement).closest('div');
              if (wrapper) {
                wrapper.style.borderColor = COLORS.inputFocusBorder;
                wrapper.style.boxShadow = `0 0 0 2px rgba(99, 102, 241, 0.1)`;
              }
            }}
            onBlur={(e) => {
              const wrapper = (e.currentTarget as HTMLElement).closest('div');
              if (wrapper) {
                wrapper.style.borderColor = COLORS.inputBorder;
                wrapper.style.boxShadow = 'none';
              }
            }}
          />
        </div>
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          disabled={!inputValue.trim() || isLoading || isTyping}
          style={{
            height: 40,
            width: 40,
            borderRadius: 12,
            background: inputValue.trim()
              ? `linear-gradient(135deg, ${COLORS.sendBtnStart}, ${COLORS.sendBtnEnd})`
              : '#e5e7eb',
            border: 'none',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: inputValue.trim()
              ? `0 3px 12px rgba(99, 102, 241, 0.35)`
              : 'none',
            transition: 'all 0.2s ease',
            flexShrink: 0,
          }}
        />
      </div>
      <div
        style={{
          maxWidth: 900,
          margin: '6px auto 0',
          textAlign: 'center',
        }}
      >
        <Text style={{ fontSize: 11, color: COLORS.timestampColor }}>
          Content generated by AI, for reference only
        </Text>
      </div>
    </div>
  );
});

// ==================== User Message Bubble Component ====================

interface UserMessageBubbleProps {
  content: string;
  timestamp?: string;
  onRetry?: () => void;
  avatar?: string;
}

const UserMessageBubble: React.FC<UserMessageBubbleProps> = React.memo(({ content, timestamp, onRetry, avatar }) => {
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'flex-start',
        gap: 10,
        padding: '0 8px',
      }}
    >
      <div style={{ maxWidth: '72%', display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
        <div
          style={{
            display: 'inline-block',
            maxWidth: '100%',
            padding: '12px 18px',
            borderRadius: '18px 18px 4px 18px',
            background: COLORS.userBubbleBg,
            border: `1px solid ${COLORS.userBubbleBorder}`,
            color: COLORS.userBubbleText,
            fontSize: 14,
            lineHeight: 1.7,
            wordBreak: 'break-word',
            boxShadow: SHADOWS.userBubble,
          }}
        >
          <Text style={{ fontSize: 14, lineHeight: 1.7, color: COLORS.userBubbleText }}>
            {content}
          </Text>
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            marginTop: 6,
            marginRight: 4,
            minHeight: 28,
          }}
        >
          {timestamp && (
            <Text style={{ fontSize: 11, color: COLORS.timestampColor, flexShrink: 0 }}>
              {timestamp}
            </Text>
          )}
          {onRetry && (
            <Tooltip title="Regenerate response">
              <Button
                type="text"
                size="small"
                icon={<ReloadOutlined />}
                style={{ color: COLORS.actionColor, fontSize: 13 }}
                onClick={onRetry}
              />
            </Tooltip>
          )}
        </div>
      </div>
      <Avatar
        size={34}
        src={avatar}
        icon={!avatar ? <UserOutlined /> : undefined}
        style={{
          backgroundColor: avatar ? 'transparent' : COLORS.userAvatarBg,
          flexShrink: 0,
          boxShadow: '0 1px 3px rgba(91, 124, 230, 0.3)',
          border: avatar ? '2px solid #e9ebf0' : 'none',
        }}
      />
    </div>
  );
});

// ==================== AI Message Bubble Component ====================

interface AIMessageBubbleProps {
  content: string;
  messageId: string;
  timestamp?: string;
  feedback: 'like' | 'dislike' | undefined;
  showReferences: boolean;
  suggestedQuestions?: string[];
  citations?: Citation[];
  graphContext?: GraphContext;
  onFeedback: (messageId: string, type: 'like' | 'dislike') => void;
  onToggleReferences: (messageId: string) => void;
  onQuickQuestion: (question: string) => void;
}

const AIMessageBubble: React.FC<AIMessageBubbleProps> = React.memo(({
  content,
  messageId,
  timestamp,
  feedback,
  showReferences,
  suggestedQuestions,
  citations,
  graphContext,
  onFeedback,
  onToggleReferences,
  onQuickQuestion,
}) => {
  const { message } = App.useApp();
  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'flex-start',
        alignItems: 'flex-start',
        gap: 10,
        padding: '0 8px',
      }}
    >
      <Avatar
        size={34}
        icon={<RobotOutlined />}
        style={{
          background: `linear-gradient(135deg, ${COLORS.aiAvatarStart}, ${COLORS.aiAvatarEnd})`,
          flexShrink: 0,
          boxShadow: `0 1px 3px rgba(99, 102, 241, 0.3)`,
        }}
      />
      <div style={{ maxWidth: '82%', minWidth: 0 }}>
        <div
          className="ai-message-content"
          style={{
            display: 'block',
            maxWidth: '100%',
            padding: '16px 22px',
            borderRadius: '18px 18px 18px 4px',
            background: COLORS.aiBubbleBg,
            border: `1px solid ${COLORS.aiBubbleBorder}`,
            color: COLORS.aiBubbleText,
            fontSize: 14,
            lineHeight: 1.8,
            wordBreak: 'break-word',
            boxShadow: SHADOWS.aiBubble,
          }}
        >
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            rehypePlugins={[rehypeRaw]}
            components={markdownComponents}
          >
            {prepareStreamingMarkdown(content)}
          </ReactMarkdown>
        </div>

        {/* Timestamp + action bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            marginTop: 6,
            marginLeft: 4,
            minHeight: 28,
          }}
        >
          {timestamp && (
            <Text style={{ fontSize: 11, color: COLORS.timestampColor, flexShrink: 0 }}>
              {timestamp}
            </Text>
          )}
          <Tooltip title="Copy">
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              style={{ color: COLORS.actionColor, fontSize: 13 }}
              onClick={() => {
                navigator.clipboard.writeText(content);
                message.success('Copied to clipboard');
              }}
            />
          </Tooltip>
          <Tooltip title={feedback === 'like' ? 'Unlike' : 'Helpful'}>
            <Button
              type="text"
              size="small"
              icon={<LikeOutlined />}
              style={{
                color: feedback === 'like' ? COLORS.actionActiveColor : COLORS.actionColor,
                fontSize: 13,
              }}
              onClick={() => onFeedback(messageId, 'like')}
            />
          </Tooltip>
          <Tooltip title={feedback === 'dislike' ? 'Undo dislike' : 'Not helpful'}>
            <Button
              type="text"
              size="small"
              icon={<DislikeOutlined />}
              style={{
                color: feedback === 'dislike' ? '#ef4444' : COLORS.actionColor,
                fontSize: 13,
              }}
              onClick={() => onFeedback(messageId, 'dislike')}
            />
          </Tooltip>
          <Tooltip title="Knowledge references">
            <Button
              type="text"
              size="small"
              icon={<BookOutlined />}
              style={{
                color: showReferences ? COLORS.actionActiveColor : COLORS.actionColor,
                fontSize: 13,
              }}
              onClick={() => onToggleReferences(messageId)}
            />
          </Tooltip>
        </div>

        {/* Knowledge reference panel */}
        {showReferences && citations && citations.length > 0 && (
          <Card
            size="small"
            style={{
              marginTop: 8,
              marginLeft: 4,
              background: '#f8f9fb',
              border: '1px solid #e9ebf0',
              borderRadius: 10,
            }}
          >
            <Paragraph style={{ marginBottom: 10, fontWeight: 600, fontSize: 13, color: '#374151' }}>
              Knowledge Reference Sources
            </Paragraph>
            <List
              size="small"
              dataSource={citations}
              renderItem={(ref) => (
                <List.Item style={{ padding: '8px 0', borderBottom: '1px solid #f1f4f9' }}>
                  <List.Item.Meta
                    title={
                      <Space>
                        <Text style={{ fontSize: 13, fontWeight: 500 }}>{ref.documentTitle}</Text>
                        <Tag
                          color="green"
                          style={{ fontSize: 11, borderRadius: 4, lineHeight: '18px' }}
                        >
                          {Math.round(ref.relevanceScore * 100)}% relevant
                        </Tag>
                      </Space>
                    }
                    description={
                      <Text type="secondary" ellipsis style={{ fontSize: 12 }}>
                        {ref.excerpt}
                      </Text>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        )}

        {/* KAG knowledge graph context */}
        {graphContext && graphContext.hasResults && (
          <Card
            size="small"
            style={{
              marginTop: 8,
              marginLeft: 4,
              background: 'linear-gradient(135deg, #f5f3ff 0%, #faf9ff 100%)',
              border: '1px solid #d9d0f0',
              borderRadius: 10,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
              <ThunderboltOutlined style={{ color: '#6366f1', fontSize: 13 }} />
              <Paragraph style={{ margin: 0, fontWeight: 600, fontSize: 13, color: '#374151' }}>
                Knowledge Graph Reasoning
              </Paragraph>
            </div>

            {/* Graph entities */}
            {graphContext.entities && graphContext.entities.length > 0 && (
              <div style={{ marginBottom: 10 }}>
                <Text style={{ fontSize: 11, color: '#9ca3af', fontWeight: 500 }}>Related Entities</Text>
                <div style={{ marginTop: 4, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                  {graphContext.entities.map((entity, idx) => (
                    <Tag
                      key={idx}
                      style={{
                        borderRadius: 6,
                        fontSize: 11,
                        background: '#f0eeff',
                        border: '1px solid #d4cff0',
                        color: '#5b21b6',
                      }}
                    >
                      {entity.name}
                      {entity.type ? ` (${entity.type})` : ''}
                    </Tag>
                  ))}
                </div>
              </div>
            )}

            {/* Graph paths */}
            {graphContext.paths && graphContext.paths.length > 0 && (
              <div style={{ marginBottom: 8 }}>
                <Text style={{ fontSize: 11, color: '#9ca3af', fontWeight: 500 }}>Reasoning Path</Text>
                {graphContext.paths.map((path, idx) => (
                  <div
                    key={idx}
                    style={{
                      marginTop: 4,
                      padding: '6px 10px',
                      background: '#fff',
                      borderRadius: 6,
                      border: '1px solid #e5e7eb',
                      fontSize: 12,
                      color: '#4b5563',
                    }}
                  >
                    {path.nodes.join(' → ')}
                    <span style={{ fontSize: 10, color: '#9ca3af', marginLeft: 6 }}>
                      ({path.hops} hops)
                    </span>
                  </div>
                ))}
              </div>
            )}
          </Card>
        )}

        {/* Suggested questions */}
        {suggestedQuestions && suggestedQuestions.length > 0 && (
          <div style={{ marginTop: 10, marginLeft: 4 }}>
            <Text style={{ fontSize: 12, color: COLORS.timestampColor }}>Related questions:</Text>
            <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {suggestedQuestions.map((q, index) => (
                <Tag
                  key={index}
                  style={{
                    padding: '4px 12px',
                    fontSize: 12,
                    cursor: 'pointer',
                    borderRadius: 12,
                    background: COLORS.quickTagBg,
                    border: 'none',
                    color: COLORS.quickTagText,
                  }}
                  onClick={() => onQuickQuestion(q)}
                >
                  {q}
                </Tag>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
});

// ==================== Main Component ====================

export const AIAssistantPage: React.FC = () => {
  const { message } = App.useApp();
  const { enableAI } = useAppStore();

  // AI feature disabled by admin
  if (!enableAI) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <Card style={{ textAlign: 'center', maxWidth: 400 }}>
          <RobotOutlined style={{ fontSize: 48, color: '#94a3b8', marginBottom: 16 }} />
          <Typography.Title level={4}>AI Assistant Feature Disabled</Typography.Title>
          <Typography.Text type="secondary">The administrator has disabled the AI Assistant feature in system settings. Please contact your administrator if you need to use it.</Typography.Text>
        </Card>
      </div>
    );
  }
  const {
    conversations,
    currentConversation,
    isLoading,
    isStreaming,
    currentResponse,
    selectedModel,
    availableModels,
    ragEnabled,
    kagEnabled,
    fetchConversations,
    createConversation,
    deleteConversation,
    setCurrentConversation,
    sendMessage,
    fetchModels,
    setSelectedModel,
    toggleRag,
    toggleKag,
  } = useAIStore();

  const { user } = useAuthStore();

  const chatInputRef = useRef<ChatInputHandle>(null);
  const [isTyping, setIsTyping] = useState(false);
  const [quickQuestions, setQuickQuestions] = useState<AIQuickQuestion[]>([]);
  const [messageFeedbacks, setMessageFeedbacks] = useState<Record<string, 'like' | 'dislike'>>({});
  const [showReferences, setShowReferences] = useState<Record<string, boolean>>({});
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const sidebarListRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const init = async () => {
      try {
        await fetchConversations();
        // After refreshing the page, automatically select the latest conversation and load chat history
        const state = useAIStore.getState();
        if (!state.currentConversation && state.conversations.length > 0) {
          state.setCurrentConversation(state.conversations[0]);
        }
      } catch {
        // fetchConversations already handles the loading state internally
      }
    };
    init();
    fetchQuickQuestions();
    fetchModels();
  }, []);

  // When the current conversation changes, scroll the sidebar to the active item
  useEffect(() => {
    if (!sidebarListRef.current || !currentConversation?.id) return;
    const activeEl = sidebarListRef.current.querySelector('[data-conv-active="true"]');
    if (activeEl) {
      activeEl.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }, [currentConversation?.id]);

  useEffect(() => {
    scrollToBottom();
  }, [currentConversation?.messages, currentResponse]);

  const scrollToBottom = () => {
    // Use instant scrolling during streaming output to avoid the smooth animation (~300ms) being
    // repeatedly interrupted by content updates every 80ms, which causes the page to jump.
    messagesEndRef.current?.scrollIntoView({
      behavior: isStreaming ? 'auto' : 'smooth',
    });
  };

  const fetchQuickQuestions = async () => {
    try {
      const questions = await aiService.getSuggestions();
      setQuickQuestions(
        questions.map((q, index) => ({
          id: `qq-${index}`,
          title: q,
          question: q,
          icon: 'thunderbolt',
        }))
      );
    } catch {
      setQuickQuestions([
        { id: 'qq-1', title: 'How do I use the enterprise knowledge base?', question: 'How do I use the enterprise knowledge base?', icon: 'thunderbolt' },
        { id: 'qq-2', title: 'Find content about technical documentation', question: 'Find content about technical documentation', icon: 'thunderbolt' },
        { id: 'qq-3', title: 'Help me summarize recent documents', question: 'Help me summarize recent documents', icon: 'thunderbolt' },
        { id: 'qq-4', title: 'Analyze document trends', question: 'Analyze document trends', icon: 'thunderbolt' },
      ]);
    }
  };

  const handleSend = useCallback(async (content: string) => {
    if (!content.trim()) return;
    setIsTyping(true);
    try {
      await sendMessage(content, currentConversation?.id != null ? String(currentConversation.id) : undefined);
    } finally {
      setIsTyping(false);
    }
  }, [sendMessage, currentConversation?.id]);

  const handleQuickQuestion = useCallback((question: string) => {
    chatInputRef.current?.submitWithText(question);
  }, []);

  const handleNewConversation = async () => {
    try {
      await createConversation(`New Conversation ${conversations.length + 1}`);
    } catch {
      // handled by store
    }
  };

  const handleDeleteConversation = async (id: string) => {
    try {
      await deleteConversation(id);
    } catch {
      // handled by store
    }
  };

  const handleFeedback = useCallback(async (messageId: string, type: 'like' | 'dislike') => {
    if (!currentConversation) return;
    try {
      if (messageFeedbacks[messageId] === type) {
        setMessageFeedbacks((prev) => {
          const next = { ...prev };
          delete next[messageId];
          return next;
        });
        message.success('Feedback removed');
      } else {
        await aiService.submitFeedback({
          messageId,
          conversationId: String(currentConversation.id),
          type,
        });
        setMessageFeedbacks((prev) => ({ ...prev, [messageId]: type }));
        message.success(type === 'like' ? 'Thanks for your feedback!' : 'Thanks for your feedback, we will keep improving');
      }
    } catch {
      // handled
    }
  }, [currentConversation, messageFeedbacks]);

  const toggleReferences = useCallback((messageId: string) => {
    setShowReferences((prev) => ({ ...prev, [messageId]: !prev[messageId] }));
  }, []);

  const handleRetry = useCallback(async (userMessageContent: string, messageIdx: number) => {
    if (!currentConversation || isStreaming || isTyping) return;
    // Remove the current user message and all messages after it (i.e. removes the old AI reply)
    const trimmedMessages = currentConversation.messages.slice(0, messageIdx);
    setCurrentConversation({
      ...currentConversation,
      messages: trimmedMessages,
      updatedAt: new Date().toISOString(),
    });
    setIsTyping(true);
    try {
      await sendMessage(userMessageContent);
    } finally {
      setIsTyping(false);
    }
  }, [currentConversation, isStreaming, isTyping, sendMessage, setCurrentConversation]);

  const formatTime = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      const hh = String(d.getHours()).padStart(2, '0');
      const mm = String(d.getMinutes()).padStart(2, '0');
      return `${hh}:${mm}`;
    } catch {
      return '';
    }
  };

  const truncateTitle = (title: string, maxLen: number = 12) => {
    if (!title) return '';
    return title.length > maxLen ? title.slice(0, maxLen) + '...' : title;
  };

  // ==================== Empty State ====================

  const renderEmptyState = () => (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '80px 40px',
        height: '100%',
      }}
    >
      <div
        style={{
          width: 72,
          height: 72,
          borderRadius: 20,
          background: `linear-gradient(135deg, ${COLORS.aiAvatarStart}, ${COLORS.aiAvatarEnd})`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 20,
          boxShadow: `0 8px 24px rgba(99, 102, 241, 0.2)`,
        }}
      >
        <RobotOutlined style={{ fontSize: 32, color: '#fff' }} />
      </div>
      <Title level={3} style={{ margin: 0, fontWeight: 600, color: '#111827' }}>
        Hi, I'm your AI Assistant
      </Title>
      <Text style={{ color: COLORS.timestampColor, marginTop: 8, fontSize: 14 }}>
        Providing precise answers based on the knowledge base — try one of the following questions to start a conversation
      </Text>

      {quickQuestions.length > 0 && (
        <div style={{ marginTop: 36, maxWidth: 560, width: '100%' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              marginBottom: 14,
            }}
          >
            <ThunderboltOutlined style={{ color: '#6366f1', fontSize: 14 }} />
            <Text style={{ fontSize: 13, fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Quick Questions
            </Text>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {quickQuestions.map((qq) => (
              <div
                key={qq.id}
                onClick={() => handleQuickQuestion(qq.question)}
                style={{
                  padding: '12px 16px',
                  borderRadius: 10,
                  background: COLORS.quickTagBg,
                  cursor: 'pointer',
                  fontSize: 14,
                  color: '#374151',
                  border: '1px solid transparent',
                  transition: 'all 0.15s ease',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#e8edf6';
                  e.currentTarget.style.borderColor = '#d4ddf0';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = COLORS.quickTagBg;
                  e.currentTarget.style.borderColor = 'transparent';
                }}
              >
                <span
                  style={{
                    width: 20,
                    height: 20,
                    borderRadius: 6,
                    background: `linear-gradient(135deg, ${COLORS.sendBtnStart}, ${COLORS.sendBtnEnd})`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 10,
                    color: '#fff',
                    flexShrink: 0,
                  }}
                >
                  <ThunderboltOutlined />
                </span>
                {qq.title}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );

  // ==================== Main Render ====================

  return (
    <>
      {/* Markdown style injection */}
      <style>{MARKDOWN_STYLES}</style>

      <div style={{ height: 'calc(100vh - 80px)', display: 'flex', gap: 0 }}>
        {/* ======== Conversation history sidebar ======== */}
        <div
          style={{
            width: 320,
            display: 'flex',
            flexDirection: 'column',
            background: COLORS.sidebarBg,
            borderRight: `1px solid ${COLORS.sidebarBorder}`,
            borderRadius: '12px 0 0 12px',
            overflow: 'hidden',
          }}
        >
          {/* Top action bar */}
          <div
            style={{
              padding: '16px',
              borderBottom: `1px solid ${COLORS.sidebarBorder}`,
            }}
          >
            <Button
              type="default"
              icon={<PlusOutlined />}
              onClick={handleNewConversation}
              block
              style={{
                height: 40,
                borderRadius: 10,
                fontWeight: 500,
                fontSize: 14,
                border: `1px solid ${COLORS.inputBorder}`,
                color: '#374151',
                boxShadow: 'none',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 6,
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = '#6366f1';
                e.currentTarget.style.color = '#6366f1';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = COLORS.inputBorder;
                e.currentTarget.style.color = '#374151';
              }}
            >
              New Conversation
            </Button>
          </div>

          {/* Conversation list */}
          <div ref={sidebarListRef} style={{ flex: 1, overflow: 'auto', padding: '6px 8px' }}>
            {conversations.length === 0 && (
              <div style={{ textAlign: 'center', padding: '40px 16px' }}>
                <Text style={{ color: COLORS.timestampColor, fontSize: 13 }}>No conversations yet</Text>
              </div>
            )}
            {conversations.map((conversation) => {
              const isActive = String(currentConversation?.id) === String(conversation.id);
              const moreItems: MenuProps['items'] = [
                {
                  key: 'delete',
                  label: 'Delete Conversation',
                  icon: <DeleteOutlined />,
                  danger: true,
                  onClick: () => handleDeleteConversation(String(conversation.id)),
                },
              ];

              return (
                <div
                  key={conversation.id}
                  data-conv-active={isActive ? 'true' : 'false'}
                  onClick={() => setCurrentConversation(conversation)}
                  style={{
                    position: 'relative',
                    padding: '10px 12px',
                    borderRadius: 8,
                    cursor: 'pointer',
                    background: isActive ? COLORS.sidebarActive : 'transparent',
                    marginBottom: 2,
                    transition: 'all 0.15s ease',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    boxShadow: isActive ? '0 1px 3px rgba(0,0,0,0.04)' : 'none',
                  }}
                  onMouseEnter={(e) => {
                    if (!isActive) e.currentTarget.style.background = COLORS.sidebarHover;
                  }}
                  onMouseLeave={(e) => {
                    if (!isActive) e.currentTarget.style.background = 'transparent';
                  }}
                >
                  {/* Active-state left accent bar */}
                  {isActive && (
                    <div
                      style={{
                        position: 'absolute',
                        left: 0,
                        top: '50%',
                        transform: 'translateY(-50%)',
                        width: 3,
                        height: 24,
                        borderRadius: '0 3px 3px 0',
                        background: `linear-gradient(180deg, ${COLORS.aiAvatarStart}, ${COLORS.aiAvatarEnd})`,
                      }}
                    />
                  )}
                  <div style={{ flex: 1, minWidth: 0, paddingLeft: isActive ? 4 : 0 }}>
                    <Text
                      ellipsis
                      style={{
                        fontSize: 13,
                        fontWeight: isActive ? 600 : 400,
                        color: isActive ? '#111827' : '#374151',
                        display: 'block',
                      }}
                    >
                      {conversation.title}
                    </Text>
                  </div>
                  <Dropdown menu={{ items: moreItems }} trigger={['click']}>
                    <Button
                      type="text"
                      size="small"
                      icon={<MoreOutlined />}
                      onClick={(e) => e.stopPropagation()}
                      style={{ color: COLORS.timestampColor, flexShrink: 0 }}
                    />
                  </Dropdown>
                </div>
              );
            })}
          </div>
        </div>

        {/* ======== Main conversation area ======== */}
        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            background: COLORS.cardBg,
            borderRadius: '0 12px 12px 0',
            overflow: 'hidden',
            border: `1px solid ${COLORS.cardBorder}`,
            borderLeft: 'none',
            boxShadow: SHADOWS.card,
          }}
        >
          {/* Top bar */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 20px',
              borderBottom: `1px solid ${COLORS.cardBorder}`,
              background: '#fafbfc',
              flexShrink: 0,
            }}
          >
            <Space size={10}>
              <Avatar
                size={30}
                icon={<RobotOutlined />}
                style={{
                  background: `linear-gradient(135deg, ${COLORS.aiAvatarStart}, ${COLORS.aiAvatarEnd})`,
                }}
              />
              <Text style={{ fontSize: 14, fontWeight: 600, color: '#111827' }}>AI Assistant</Text>
              <Tag
                style={{
                  fontSize: 11,
                  borderRadius: 5,
                  background: '#eff3ff',
                  border: 'none',
                  color: '#5b7ce6',
                  padding: '0 8px',
                  lineHeight: '20px',
                }}
              >
                Knowledge Base Enhancement
              </Tag>
            </Space>

            <Space size={10}>
              <Tooltip title="When enabled, the AI will retrieve relevant documents from the knowledge base to assist its answers">
                <Space size={4} style={{ fontSize: 12, color: '#6b7280' }}>
                  <Switch
                    size="small"
                    checked={ragEnabled}
                    onChange={(checked) => toggleRag(checked)}
                  />
                  <span>Knowledge Base Enhancement</span>
                </Space>
              </Tooltip>
              <Tooltip title="When enabled, the AI will use the knowledge graph for multi-hop reasoning to understand relationships between pieces of knowledge">
                <Space size={4} style={{ fontSize: 12, color: '#6b7280' }}>
                  <Switch
                    size="small"
                    checked={kagEnabled}
                    onChange={(checked) => toggleKag(checked)}
                  />
                  <span>Graph Reasoning</span>
                </Space>
              </Tooltip>
              {availableModels.length > 0 && (
                <Select
                  value={selectedModel}
                  onChange={(value) => setSelectedModel(value)}
                  size="small"
                  popupMatchSelectWidth={false}
                  style={{ minWidth: 130 }}
                  options={availableModels.map((m) => ({
                    value: m.key,
                    label: (
                      <Space size={6}>
                        <span
                          style={{
                            display: 'inline-block',
                            width: 7,
                            height: 7,
                            borderRadius: '50%',
                            background: m.key === 'qwen' ? '#6366f1' : '#10b981',
                          }}
                        />
                        <span style={{ fontSize: 13 }}>{m.displayName}</span>
                        {m.isDefault && (
                          <Tag
                            style={{
                              fontSize: 10,
                              borderRadius: 3,
                              padding: '0 4px',
                              lineHeight: '16px',
                              margin: 0,
                              border: 'none',
                              background: '#e8edf4',
                              color: '#6b7280',
                            }}
                          >
                            Default
                          </Tag>
                        )}
                      </Space>
                    ),
                  }))}
                />
              )}
            </Space>
          </div>

          {/* Message container */}
          <div
            ref={messagesContainerRef}
            style={{
              flex: 1,
              overflow: 'auto',
              padding: '16px 20px',
              background: COLORS.pageBg,
            }}
          >
            {!currentConversation || !currentConversation.messages || currentConversation.messages.length === 0 ? (
              renderEmptyState()
            ) : (
              <div style={{ maxWidth: 900, margin: '0 auto' }}>
                {currentConversation.messages.map((message, idx) => {
                  const messageId = String(message.id);
                  const ts = message.timestamp || '';

                  return message.role === 'user' ? (
                    <div key={messageId} style={{ marginBottom: 28 }}>
                      <UserMessageBubble
                        content={message.content}
                        timestamp={formatTime(ts)}
                        avatar={user?.avatar}
                        onRetry={() => handleRetry(message.content, idx)}
                      />
                    </div>
                  ) : (
                    <div key={messageId} style={{ marginBottom: 28 }}>
                      <AIMessageBubble
                        content={message.content}
                        messageId={messageId}
                        timestamp={formatTime(ts)}
                        feedback={messageFeedbacks[messageId]}
                        showReferences={showReferences[messageId] || false}
                        suggestedQuestions={(message as any).suggestedQuestions}
                        citations={message.citations}
                        graphContext={message.graphContext}
                        onFeedback={handleFeedback}
                        onToggleReferences={toggleReferences}
                        onQuickQuestion={handleQuickQuestion}
                      />
                      {message.fromKnowledgeBase && (
                        <div style={{ padding: '0 8px', marginTop: 4 }}>
                          <Tag color="green" style={{ fontSize: 11, borderRadius: 4 }}>
                            Knowledge Base Enhancement
                          </Tag>
                        </div>
                      )}
                      {message.citations && message.citations.length > 0 && (
                        <div
                          style={{
                            padding: '8px 8px 0',
                            marginTop: 6,
                            borderTop: '1px solid #f0f0f0',
                          }}
                        >
                          <Text
                            type="secondary"
                            style={{ fontSize: 11, fontWeight: 500, marginBottom: 4, display: 'block' }}
                          >
                            Reference Sources
                          </Text>
                          <Space wrap size={[4, 4]}>
                            {message.citations.map((citation) => (
                              <Tooltip
                                key={citation.index}
                                title={
                                  <div style={{ maxWidth: 300 }}>
                                    <div style={{ fontWeight: 600, marginBottom: 4 }}>
                                      {citation.documentTitle}
                                    </div>
                                    <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.75)', lineHeight: 1.5 }}>
                                      {citation.excerpt.length > 150
                                        ? citation.excerpt.slice(0, 150) + '...'
                                        : citation.excerpt}
                                    </div>
                                    <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', marginTop: 4 }}>
                                      Relevance: {(citation.relevanceScore * 100).toFixed(0)}%
                                    </div>
                                  </div>
                                }
                              >
                                <Tag
                                  color="blue"
                                  style={{
                                    cursor: 'pointer',
                                    borderRadius: 4,
                                    fontSize: 11,
                                    padding: '0 8px',
                                  }}
                                >
                                  [{citation.index}] {truncateTitle(citation.documentTitle, 12)}
                                </Tag>
                              </Tooltip>
                            ))}
                          </Space>
                        </div>
                      )}
                    </div>
                  );
                })}

                {/* Streaming response - typing */}
                {isStreaming && currentResponse && (
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'flex-start',
                      alignItems: 'flex-start',
                      gap: 10,
                      padding: '0 8px',
                      marginBottom: 28,
                    }}
                  >
                    <Avatar
                      size={34}
                      icon={<RobotOutlined />}
                      style={{
                        background: `linear-gradient(135deg, ${COLORS.aiAvatarStart}, ${COLORS.aiAvatarEnd})`,
                        flexShrink: 0,
                        boxShadow: `0 1px 3px rgba(99, 102, 241, 0.3)`,
                      }}
                    />
                    <div style={{ flex: 1, maxWidth: '78%' }}>
                      <div
                        className="ai-message-content"
                        style={{
                          display: 'inline-block',
                          maxWidth: '100%',
                          padding: '14px 20px',
                          borderRadius: '18px 18px 18px 4px',
                          background: COLORS.aiBubbleBg,
                          border: `1px solid ${COLORS.aiBubbleBorder}`,
                          color: COLORS.aiBubbleText,
                          fontSize: 14,
                          lineHeight: 1.75,
                          wordBreak: 'break-word',
                          boxShadow: SHADOWS.aiBubble,
                        }}
                      >
                        <ReactMarkdown
                          remarkPlugins={[remarkGfm]}
                          rehypePlugins={[rehypeRaw]}
                          components={markdownComponents}
                        >
                          {prepareStreamingMarkdown(currentResponse)}
                        </ReactMarkdown>
                        <Spin
                          size="small"
                          style={{
                            marginLeft: 6,
                            display: 'inline-flex',
                            alignItems: 'center',
                          }}
                        />
                      </div>
                    </div>
                  </div>
                )}

                {isStreaming && !currentResponse && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      padding: '0 8px',
                      marginBottom: 28,
                    }}
                  >
                    <Avatar
                      size={34}
                      icon={<RobotOutlined />}
                      style={{
                        background: `linear-gradient(135deg, ${COLORS.aiAvatarStart}, ${COLORS.aiAvatarEnd})`,
                        flexShrink: 0,
                      }}
                    />
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        padding: '14px 20px',
                        borderRadius: '18px 18px 18px 4px',
                        background: COLORS.aiBubbleBg,
                        border: `1px solid ${COLORS.aiBubbleBorder}`,
                      }}
                    >
                      <Spin size="small" />
                      <Text style={{ color: COLORS.timestampColor, fontSize: 13 }}>
                        Thinking...
                      </Text>
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* ======== Input area ======== */}
          <ChatInput
            ref={chatInputRef}
            onSend={handleSend}
            isLoading={isLoading}
            isTyping={isTyping}
          />
        </div>
      </div>
    </>
  );
};

export default AIAssistantPage;
