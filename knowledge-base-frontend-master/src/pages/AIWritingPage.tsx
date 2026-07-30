import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Input,
  Select,
  Button,
  Space,
  Tag,
  Typography,
  Spin,
  InputNumber,
  Tooltip,
} from 'antd';
import { App } from 'antd';
import {
  RobotOutlined,
  ThunderboltOutlined,
  EditOutlined,
  CopyOutlined,
  CheckOutlined,
  ExpandOutlined,
  FormatPainterOutlined,
  ForwardOutlined,
  FileAddOutlined,
  ClearOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { useAIWritingStore, useAppStore } from '@/stores';
import type { WritingRequest } from '@/types';
import type { Components } from 'react-markdown';

const { Text } = Typography;
const { TextArea } = Input;

// ==================== Design System: Colors & Style Constants ====================

const COLORS = {
  pageBg: '#f7f8fa',
  cardBg: '#ffffff',
  cardBorder: '#e9ebf0',
  sidebarBg: '#fafbfc',
  sidebarBorder: '#edf0f4',
  sidebarHover: '#f1f4f9',
  sidebarActive: '#e8edf4',
  textPrimary: '#111827',
  textSecondary: '#4b5563',
  textMuted: '#9ca3af',
  accent: '#2563eb',
  accentLight: '#3b82f6',
  accentBg: 'rgba(37, 99, 235, 0.06)',
  accentBorder: 'rgba(37, 99, 235, 0.15)',
  success: '#10b981',
  warning: '#f59e0b',
  danger: '#ef4444',
  inputBg: '#f9fafb',
  inputBorder: '#e5e7eb',
  inputFocusBorder: '#2563eb',
  tagBg: '#f1f4f9',
  tagHover: '#e4e9f2',
  tagText: '#4b5563',
  quickTagBg: '#f1f4f9',
  sendBtnStart: '#2563eb',
  sendBtnEnd: '#1d4ed8',
};

const SHADOWS = {
  card: '0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02)',
  panel: '0 1px 3px rgba(0, 0, 0, 0.04)',
  button: '0 2px 8px rgba(37, 99, 235, 0.25)',
  icon: '0 4px 12px rgba(37, 99, 235, 0.18)',
};

// ==================== Markdown Styles (consistent with AIAssistantPage) ====================

const MARKDOWN_STYLES = `
.writing-content {
  color: #1e293b;
  font-size: 15px;
  line-height: 1.85;
  word-break: break-word;
}
.writing-content > *:first-child { margin-top: 0 !important; }
.writing-content > *:last-child { margin-bottom: 0 !important; }

.writing-content h1 { font-size: 1.5em; font-weight: 700; margin: 1.2em 0 0.6em; color: #0f172a; letter-spacing: -0.01em; padding-bottom: 0.3em; border-bottom: 1px solid #e9ebf0; }
.writing-content h2 { font-size: 1.3em; font-weight: 700; margin: 1em 0 0.5em; color: #111827; letter-spacing: -0.01em; }
.writing-content h3 { font-size: 1.15em; font-weight: 600; margin: 0.9em 0 0.45em; color: #1f2937; }
.writing-content h4 { font-size: 1.05em; font-weight: 600; margin: 0.8em 0 0.4em; color: #374151; }

.writing-content p { margin: 0.65em 0; line-height: 1.85; }

.writing-content strong { font-weight: 650; color: #0f172a; }
.writing-content em { font-style: italic; color: #374151; }

.writing-content code:not(pre code) {
  background: #f1f5f9;
  color: #e11d48;
  padding: 0.15em 0.45em;
  border-radius: 4px;
  font-size: 0.88em;
  font-family: 'SF Mono', 'Fira Code', 'JetBrains Mono', Menlo, Consolas, monospace;
  font-weight: 500;
  border: 1px solid #e2e8f0;
}

.writing-content ul, .writing-content ol {
  padding-left: 1.6em;
  margin: 0.6em 0;
}
.writing-content li { margin: 0.3em 0; line-height: 1.75; padding-left: 0.15em; }
.writing-content li > p { margin: 0.15em 0; }
.writing-content ul > li::marker { color: #94a3b8; }
.writing-content ol > li::marker { color: #64748b; font-weight: 500; font-size: 0.9em; }

.writing-content blockquote {
  border-left: 3px solid #2563eb;
  padding: 0.6em 1em;
  margin: 0.8em 0;
  background: linear-gradient(90deg, #eff6ff 0%, #f6f9ff 100%);
  color: #4b5563;
  border-radius: 0 8px 8px 0;
}
.writing-content blockquote p { margin: 0.3em 0; }

.writing-content a {
  color: #2563eb;
  text-decoration: none;
  border-bottom: 1px solid #bfdbfe;
  transition: border-color 0.15s ease, color 0.15s ease;
}
.writing-content a:hover {
  color: #1d4ed8;
  border-bottom-color: #8b9cf7;
}

.writing-content hr {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #e5e7eb 20%, #e5e7eb 80%, transparent 100%);
  margin: 1.2em 0;
}

.writing-content .md-table-wrapper {
  overflow-x: auto;
  margin: 1em 0;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.writing-content table {
  border-collapse: collapse;
  width: 100%;
  font-size: 0.9em;
  min-width: 400px;
}
.writing-content thead { background: #f8fafc; }
.writing-content thead th { font-weight: 600; color: #1f2937; text-align: left; font-size: 0.85em; text-transform: uppercase; letter-spacing: 0.03em; }
.writing-content th, .writing-content td { border-bottom: 1px solid #f1f5f9; padding: 0.65em 0.9em; }
.writing-content tbody tr:nth-child(even) { background: #fafbfc; }
.writing-content tbody tr:hover { background: #f1f5f9; }
.writing-content tbody tr:last-child td { border-bottom: none; }

.writing-content .code-block-wrapper {
  margin: 1em 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #2d2d3f;
  box-shadow: 0 2px 10px rgba(0,0,0,0.08);
}
.writing-content .code-block-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.45em 1em;
  background: #252536;
  border-bottom: 1px solid #3a3a55;
}
.writing-content .code-block-lang {
  font-size: 11px;
  color: #8b949e;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 500;
  font-family: 'SF Mono', 'Fira Code', Menlo, monospace;
}
.writing-content .code-block-copy {
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
.writing-content .code-block-copy:hover { color: #e6edf3; background: #3a3a55; }
.writing-content .code-block-wrapper pre { margin: 0 !important; border-radius: 0 !important; }
.writing-content .code-block-wrapper code {
  font-size: 0.82em !important;
  line-height: 1.6 !important;
  font-family: 'SF Mono', 'Fira Code', 'JetBrains Mono', Menlo, Consolas, monospace !important;
}

.writing-content pre:not(.code-block-wrapper pre) {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 1em 1.2em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.8em 0;
  line-height: 1.55;
  font-size: 0.85em;
}
.writing-content pre:not(.code-block-wrapper pre) code {
  background: none !important;
  padding: 0 !important;
  color: inherit;
  font-size: inherit;
  border: none !important;
}

.writing-content pre::-webkit-scrollbar,
.writing-content .code-block-wrapper pre::-webkit-scrollbar { height: 6px; }
.writing-content pre::-webkit-scrollbar-track,
.writing-content .code-block-wrapper pre::-webkit-scrollbar-track { background: transparent; }
.writing-content pre::-webkit-scrollbar-thumb,
.writing-content .code-block-wrapper pre::-webkit-scrollbar-thumb { background: #4a4a65; border-radius: 3px; }

.writing-content img { max-width: 100%; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); margin: 0.4em 0; }
.writing-content input[type="checkbox"] { margin-right: 0.4em; accent-color: #2563eb; }
`;

// ==================== Code Block Component ====================

const CodeBlock: React.FC<{ language: string | undefined; value: string }> = ({ language, value }) => {
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
            <><span style={{ fontSize: 12 }}>&#10003;</span> Copied</>
          ) : (
            <><CopyOutlined style={{ fontSize: 12 }} /> Copy code</>
          )}
        </button>
      </div>
      <SyntaxHighlighter
        language={lang}
        style={oneDark}
        customStyle={{ margin: 0, borderRadius: 0, fontSize: '0.82em', lineHeight: 1.6 }}
        codeTagProps={{
          style: { fontFamily: "'SF Mono', 'Fira Code', 'JetBrains Mono', Menlo, Consolas, monospace" },
        }}
      >
        {value}
      </SyntaxHighlighter>
    </div>
  );
};

// ==================== Markdown Component Mapping ====================

const markdownComponents: Components = {
  code({ className, children, ...props }: any) {
    const match = /language-(\w+)/.exec(className || '');
    const value = String(children).replace(/\n$/, '');
    if (match) {
      return <CodeBlock language={match[1]} value={value} />;
    }
    return <code className={className} {...props}>{children}</code>;
  },
  pre({ children }: any) {
    return <>{children}</>;
  },
  table({ children }: any) {
    return <div className="md-table-wrapper"><table>{children}</table></div>;
  },
  img({ src, alt }: any) {
    return <img src={src} alt={alt || ''} style={{ maxWidth: '100%', borderRadius: 8, boxShadow: '0 1px 4px rgba(0,0,0,0.06)' }} loading="lazy" />;
  },
  a({ href, children }: any) {
    return <a href={href} target="_blank" rel="noopener noreferrer">{children}</a>;
  },
};

// ==================== Option Data ====================

const contentTypes = [
  { value: 'article', label: 'Article' },
  { value: 'report', label: 'Report' },
  { value: 'documentation', label: 'Technical Documentation' },
  { value: 'email', label: 'Email' },
  { value: 'announcement', label: 'Announcement' },
];

const styleOptions = [
  { value: 'formal', label: 'Formal' },
  { value: 'casual', label: 'Casual' },
  { value: 'technical', label: 'Technical' },
  { value: 'creative', label: 'Creative' },
  { value: 'academic', label: 'Academic' },
];

const toneOptions = [
  { value: 'neutral', label: 'Neutral' },
  { value: 'enthusiastic', label: 'Enthusiastic' },
  { value: 'serious', label: 'Serious' },
  { value: 'friendly', label: 'Friendly' },
  { value: 'authoritative', label: 'Authoritative' },
];

// ==================== Main Page Component ====================

const AIWritingPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { message } = App.useApp();
  const { enableAIWriting } = useAppStore();

  // AI Writing feature disabled by admin
  if (!enableAIWriting) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <div style={{ textAlign: 'center', maxWidth: 400 }}>
          <RobotOutlined style={{ fontSize: 48, color: '#94a3b8', marginBottom: 16 }} />
          <Typography.Title level={4}>AI Writing Feature Disabled</Typography.Title>
          <Typography.Text type="secondary">The administrator has disabled the AI feature in system settings. Please contact your administrator if you need to use it.</Typography.Text>
        </div>
      </div>
    );
  }
  const store = useAIWritingStore();
  const contentRef = useRef<HTMLDivElement>(null);

  // Form state
  const [topic, setTopic] = useState('');
  const [requirements, setRequirements] = useState('');
  const [contentType, setContentType] = useState<string>('article');
  const [style, setStyle] = useState<string>('formal');
  const [tone, setTone] = useState<string>('neutral');
  const [length, setLength] = useState<number | null>(800);
  const [useStream, setUseStream] = useState(true);

  // Read URL params to pre-fill the form
  useEffect(() => {
    const titleParam = searchParams.get('title');
    const contentParam = searchParams.get('content');
    if (titleParam) setTopic(decodeURIComponent(titleParam));
    if (contentParam) setRequirements(decodeURIComponent(contentParam));
  }, [searchParams]);

  // Load templates
  useEffect(() => {
    store.fetchTemplates();
  }, []);

  // Apply template
  const applyTemplate = useCallback((tpl: typeof store.templates[0]) => {
    setTopic(tpl.prompt || '');
    if (tpl.suggestedContentType) setContentType(tpl.suggestedContentType);
    if (tpl.suggestedStyle) setStyle(tpl.suggestedStyle);
  }, []);

  // Build request params
  const buildRequest = (actionType: WritingRequest['actionType']): WritingRequest => ({
    topic: topic.trim(),
    requirements: requirements.trim() || undefined,
    existingContent: requirements.trim() || undefined,
    contentType: contentType as WritingRequest['contentType'],
    style: style as WritingRequest['style'],
    tone: tone as WritingRequest['tone'],
    length: length || undefined,
    actionType,
  });

  // Generate content
  const handleGenerate = async () => {
    if (!topic.trim()) {
      message.warning('Please enter a writing topic');
      return;
    }
    try {
      if (useStream) {
        await store.generateContentStream(buildRequest('generate'));
      } else {
        await store.generateContent(buildRequest('generate'));
      }
    } catch {
      message.error('Generation failed, please try again later');
    }
  };

  // Expand
  const handleExpand = async () => {
    if (!topic.trim()) { message.warning('Please enter a writing topic'); return; }
    if (!requirements.trim()) { message.warning('Please enter the content to expand in the writing requirements'); return; }
    try {
      await store.expandContent(buildRequest('expand'));
    } catch {
      message.error('Expansion failed, please try again later');
    }
  };

  // Improve
  const handleOptimize = async () => {
    if (!topic.trim()) { message.warning('Please enter a writing topic'); return; }
    if (!requirements.trim()) { message.warning('Please enter the content to improve in the writing requirements'); return; }
    try {
      await store.optimizeContent(buildRequest('optimize'));
    } catch {
      message.error('Improvement failed, please try again later');
    }
  };

  // Continue writing
  const handleContinueWriting = async () => {
    if (!topic.trim()) { message.warning('Please enter a writing topic'); return; }
    if (!requirements.trim()) { message.warning('Please enter the content to continue in the writing requirements'); return; }
    try {
      await store.continueWriting(buildRequest('continue'));
    } catch {
      message.error('Continuation failed, please try again later');
    }
  };

  // Copy
  const [copied, setCopied] = useState(false);
  const handleCopyContent = () => {
    if (!store.generatedContent) return;
    navigator.clipboard.writeText(store.generatedContent).then(() => {
      setCopied(true);
      message.success('Copied to clipboard');
      setTimeout(() => setCopied(false), 2000);
    });
  };

  // Extract a title from the generated content: take the first meaningful line, stripping Markdown markup
  const extractTitleFromContent = (content: string): { title: string; body: string } => {
    const lines = content.split('\n');
    // Skip leading empty lines
    let titleLineIdx = 0;
    while (titleLineIdx < lines.length && lines[titleLineIdx].trim() === '') {
      titleLineIdx++;
    }
    if (titleLineIdx >= lines.length) {
      return { title: topic || 'AI Generated Document', body: content };
    }
    const firstLine = lines[titleLineIdx].trim();
    // Strip Markdown heading markers (#, ## etc.) and bold markers
    const title = firstLine
      .replace(/^#{1,6}\s+/, '')   // # Heading → Heading
      .replace(/\*\*(.+?)\*\*/g, '$1') // **bold** → bold
      .replace(/^>\s*/, '')         // > Blockquote
      .trim();
    // If the title is too long, skip extraction and fall back to topic
    if (title.length > 80 || title.length < 2) {
      return { title: topic || 'AI Generated Document', body: content };
    }
    // Remove the first line already used as the title from the content (avoids duplicating the title in the body)
    const bodyLines = [...lines];
    bodyLines.splice(titleLineIdx, 1);
    // Also remove the blank line between the title line and the body
    while (bodyLines.length > 0 && bodyLines[0] !== undefined && bodyLines[0].trim() === '') {
      bodyLines.shift();
    }
    const body = bodyLines.join('\n').trim();
    return { title, body };
  };

  // Insert into a new document (opens in a new window)
  const handleInsertToDocument = () => {
    if (!store.generatedContent) return;
    const { title, body } = extractTitleFromContent(store.generatedContent);
    const encodedContent = encodeURIComponent(body);
    const encodedTitle = encodeURIComponent(title);
    window.open(`/documents/new?content=${encodedContent}&title=${encodedTitle}`, '_blank');
  };

  // Clear
  const handleClear = () => {
    setTopic('');
    setRequirements('');
    setContentType('article');
    setStyle('formal');
    setTone('neutral');
    setLength(800);
    store.clearResult();
  };

  // ==================== Render: Empty State ====================

  const renderEmptyState = () => (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        minHeight: 500,
        padding: '40px 20px',
      }}
    >
      {/* Icon */}
      <div
        style={{
          width: 80,
          height: 80,
          borderRadius: 20,
          background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(59, 130, 246, 0.08))',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: 24,
        }}
      >
        <EditOutlined style={{ fontSize: 34, color: '#3b82f6' }} />
      </div>

      {/* Title */}
      <Text
        style={{
          fontSize: 20,
          fontWeight: 700,
          color: COLORS.textPrimary,
          marginBottom: 8,
          letterSpacing: '-0.01em',
        }}
      >
        Start AI Writing
      </Text>
      <Text
        style={{
          fontSize: 14,
          color: COLORS.textMuted,
          textAlign: 'center',
          lineHeight: 1.7,
          maxWidth: 360,
        }}
      >
        Enter your writing topic and requirements on the left, choose the content type, style, and tone,
        then click Generate and let AI write professional content for you
      </Text>

      {/* Quick tips */}
      <div style={{ marginTop: 32, display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'center', maxWidth: 480 }}>
        {['Write a technical proposal', 'Draft a weekly project report', 'Improve an existing document', 'Continue unfinished content'].map((tip) => (
          <Tag
            key={tip}
            onClick={() => setTopic(tip)}
            style={{
              padding: '6px 14px',
              fontSize: 13,
              cursor: 'pointer',
              borderRadius: 20,
              background: COLORS.quickTagBg,
              border: '1px solid transparent',
              color: COLORS.tagText,
              transition: 'all 0.15s ease',
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
            {tip}
          </Tag>
        ))}
      </div>
    </div>
  );

  // ==================== Render: Loading State ====================

  const renderLoadingState = () => (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        minHeight: 400,
        gap: 16,
      }}
    >
      <Spin size="large" />
      <div style={{ textAlign: 'center' }}>
        <Text strong style={{ fontSize: 16, color: COLORS.textPrimary, display: 'block', marginBottom: 6 }}>
          {store.isStreaming ? 'AI is writing...' : 'Generating content...'}
        </Text>
        <Text style={{ fontSize: 13, color: COLORS.textMuted }}>
          Please wait, content is being generated
        </Text>
      </div>
    </div>
  );

  // ==================== Render: Error State ====================

  const renderErrorState = () => (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        minHeight: 400,
        gap: 16,
      }}
    >
      <div
        style={{
          width: 64,
          height: 64,
          borderRadius: 16,
          background: 'rgba(239, 68, 68, 0.08)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Text style={{ fontSize: 28, color: COLORS.danger }}>!</Text>
      </div>
      <div style={{ textAlign: 'center' }}>
        <Text strong style={{ fontSize: 16, color: COLORS.textPrimary, display: 'block', marginBottom: 6 }}>
          Generation Failed
        </Text>
        <Text style={{ fontSize: 13, color: COLORS.textMuted, display: 'block', marginBottom: 16 }}>
          {store.error || 'Please try again later'}
        </Text>
        <Button type="primary" onClick={handleGenerate}>Regenerate</Button>
      </div>
    </div>
  );

  // ==================== Main Render ====================

  return (
    <>
      <style>{MARKDOWN_STYLES}</style>

      <div
        style={{
          height: 'calc(100vh - 80px)',
          display: 'flex',
          gap: 0,
          background: COLORS.pageBg,
          overflow: 'hidden',
        }}
      >
        {/* ==================== Left Panel ==================== */}
        <div
          style={{
            width: 380,
            minWidth: 380,
            height: '100%',
            overflow: 'hidden auto',
            borderRight: `1px solid ${COLORS.sidebarBorder}`,
            background: COLORS.sidebarBg,
            padding: '20px 20px 24px',
            display: 'flex',
            flexDirection: 'column',
            gap: 18,
          }}
        >
          {/* ---- Header ---- */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 42,
                height: 42,
                borderRadius: 12,
                background: `linear-gradient(135deg, ${COLORS.sendBtnStart}, ${COLORS.sendBtnEnd})`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: SHADOWS.icon,
                flexShrink: 0,
              }}
            >
              <EditOutlined style={{ fontSize: 20, color: '#fff' }} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <Text
                strong
                style={{
                  fontSize: 17,
                  color: COLORS.textPrimary,
                  display: 'block',
                  lineHeight: 1.3,
                  letterSpacing: '-0.01em',
                }}
              >
                AI Writing
              </Text>
              <Text style={{ fontSize: 12, color: COLORS.textMuted }}>
                Intelligent Document Writing Assistant
              </Text>
            </div>
            <Tooltip title="Go back">
              <Button
                type="text"
                size="small"
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate(-1)}
                style={{ color: COLORS.textMuted }}
              />
            </Tooltip>
          </div>

          {/* Divider */}
          <div style={{ height: 1, background: COLORS.sidebarBorder, margin: '0 -20px' }} />

          {/* ---- Writing Topic ---- */}
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
              <Text strong style={{ fontSize: 13, color: COLORS.textSecondary }}>
                Writing Topic
              </Text>
              <Text type="danger" style={{ fontSize: 11 }}>* Required</Text>
            </div>
            <Input
              placeholder="e.g. How to write a great technical proposal"
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              style={{
                height: 40,
                borderRadius: 8,
                borderColor: COLORS.inputBorder,
                background: COLORS.cardBg,
                fontSize: 13,
              }}
            />
          </div>

          {/* ---- Writing Requirements ---- */}
          <div>
            <div style={{ marginBottom: 6 }}>
              <Text strong style={{ fontSize: 13, color: COLORS.textSecondary }}>
                Writing Requirements / Reference Content
              </Text>
              <Text style={{ fontSize: 11, color: COLORS.textMuted, marginLeft: 4 }}>
                (Enter existing content here when expanding/improving/continuing)
              </Text>
            </div>
            <TextArea
              placeholder="Add writing requirements, key points, or paste existing content..."
              value={requirements}
              onChange={(e) => setRequirements(e.target.value)}
              autoSize={{ minRows: 3, maxRows: 6 }}
              style={{
                borderRadius: 8,
                borderColor: COLORS.inputBorder,
                background: COLORS.cardBg,
                fontSize: 13,
              }}
            />
          </div>

          {/* ---- Content Type + Writing Style ---- */}
          <div style={{ display: 'flex', gap: 10 }}>
            <div style={{ flex: 1 }}>
              <Text strong style={{ fontSize: 13, color: COLORS.textSecondary, display: 'block', marginBottom: 6 }}>
                Content Type
              </Text>
              <Select
                value={contentType}
                onChange={setContentType}
                style={{ width: '100%' }}
                options={contentTypes}
                popupMatchSelectWidth={false}
              />
            </div>
            <div style={{ flex: 1 }}>
              <Text strong style={{ fontSize: 13, color: COLORS.textSecondary, display: 'block', marginBottom: 6 }}>
                Writing Style
              </Text>
              <Select
                value={style}
                onChange={setStyle}
                style={{ width: '100%' }}
                options={styleOptions}
                popupMatchSelectWidth={false}
              />
            </div>
          </div>

          {/* ---- Tone + Target Word Count ---- */}
          <div style={{ display: 'flex', gap: 10 }}>
            <div style={{ flex: 1 }}>
              <Text strong style={{ fontSize: 13, color: COLORS.textSecondary, display: 'block', marginBottom: 6 }}>
                Tone
              </Text>
              <Select
                value={tone}
                onChange={setTone}
                style={{ width: '100%' }}
                options={toneOptions}
                popupMatchSelectWidth={false}
              />
            </div>
            <div style={{ flex: 1 }}>
              <Text strong style={{ fontSize: 13, color: COLORS.textSecondary, display: 'block', marginBottom: 6 }}>
                Target Word Count
              </Text>
              <InputNumber
                value={length}
                onChange={(v) => setLength(v)}
                min={100}
                max={10000}
                step={100}
                style={{ width: '100%' }}
                placeholder="800"
              />
            </div>
          </div>

          {/* ---- Generation Method ---- */}
          <div>
            <Text strong style={{ fontSize: 13, color: COLORS.textSecondary, display: 'block', marginBottom: 8 }}>
              Generation Method
            </Text>
            <div style={{ display: 'flex', gap: 8 }}>
              <div
                onClick={() => setUseStream(true)}
                style={{
                  flex: 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6,
                  padding: '8px 12px',
                  borderRadius: 8,
                  cursor: 'pointer',
                  fontSize: 13,
                  fontWeight: 500,
                  border: useStream ? `1px solid ${COLORS.accent}` : `1px solid ${COLORS.inputBorder}`,
                  background: useStream ? COLORS.accentBg : COLORS.cardBg,
                  color: useStream ? COLORS.accent : COLORS.textSecondary,
                  transition: 'all 0.15s ease',
                }}
              >
                <ThunderboltOutlined style={{ fontSize: 14 }} />
                Streaming Generation
              </div>
              <div
                onClick={() => setUseStream(false)}
                style={{
                  flex: 1,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6,
                  padding: '8px 12px',
                  borderRadius: 8,
                  cursor: 'pointer',
                  fontSize: 13,
                  fontWeight: 500,
                  border: !useStream ? `1px solid ${COLORS.accent}` : `1px solid ${COLORS.inputBorder}`,
                  background: !useStream ? COLORS.accentBg : COLORS.cardBg,
                  color: !useStream ? COLORS.accent : COLORS.textSecondary,
                  transition: 'all 0.15s ease',
                }}
              >
                <RobotOutlined style={{ fontSize: 14 }} />
                Standard Generation
              </div>
            </div>
          </div>

          {/* ---- Action Buttons ---- */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={handleGenerate}
              loading={store.isGenerating && !store.isStreaming}
              disabled={store.isGenerating}
              style={{
                background: `linear-gradient(135deg, ${COLORS.sendBtnStart}, ${COLORS.sendBtnEnd})`,
                border: 'none',
                fontWeight: 600,
                borderRadius: 8,
                boxShadow: SHADOWS.button,
                flex: '1 1 auto',
                minWidth: 100,
              }}
            >
              Generate Content
            </Button>
            <Button
              icon={<ExpandOutlined />}
              onClick={handleExpand}
              disabled={store.isGenerating}
              style={{ borderRadius: 8, fontWeight: 500 }}
            >
              Expand
            </Button>
            <Button
              icon={<FormatPainterOutlined />}
              onClick={handleOptimize}
              disabled={store.isGenerating}
              style={{ borderRadius: 8, fontWeight: 500 }}
            >
              Improve
            </Button>
            <Button
              icon={<ForwardOutlined />}
              onClick={handleContinueWriting}
              disabled={store.isGenerating}
              style={{ borderRadius: 8, fontWeight: 500 }}
            >
              Continue
            </Button>
          </div>

          {/* Divider */}
          <div style={{ height: 1, background: COLORS.sidebarBorder, margin: '0 -20px' }} />

          {/* ---- Writing Templates ---- */}
          <div>
            <Text strong style={{ fontSize: 14, color: COLORS.textPrimary, display: 'block', marginBottom: 10 }}>
              Writing Templates
            </Text>
            {store.templates.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <Spin size="small" />
                <Text style={{ fontSize: 12, color: COLORS.textMuted, display: 'block', marginTop: 8 }}>Loading templates...</Text>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {store.templates.slice(0, 6).map((tpl) => (
                  <div
                    key={tpl.id}
                    onClick={() => applyTemplate(tpl)}
                    style={{
                      padding: '11px 14px',
                      borderRadius: 10,
                      cursor: 'pointer',
                      background: COLORS.cardBg,
                      border: `1px solid ${COLORS.cardBorder}`,
                      transition: 'all 0.15s ease',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = '#bfdbfe';
                      e.currentTarget.style.background = '#f0f6ff';
                      e.currentTarget.style.boxShadow = '0 2px 8px rgba(37, 99, 235, 0.06)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = COLORS.cardBorder;
                      e.currentTarget.style.background = COLORS.cardBg;
                      e.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.02)';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Text strong style={{ fontSize: 13, color: COLORS.textPrimary }}>
                        {tpl.name}
                      </Text>
                      <Tag
                        style={{
                          fontSize: 10,
                          borderRadius: 4,
                          background: COLORS.accentBg,
                          border: `1px solid ${COLORS.accentBorder}`,
                          color: COLORS.accent,
                          lineHeight: '18px',
                          margin: 0,
                        }}
                      >
                        {tpl.category}
                      </Tag>
                    </div>
                    <Text
                      style={{
                        fontSize: 12,
                        color: COLORS.textMuted,
                        marginTop: 4,
                        display: 'block',
                        lineHeight: 1.5,
                      }}
                    >
                      {tpl.description}
                    </Text>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* ---- Clear Button ---- */}
          <Button
            icon={<ClearOutlined />}
            onClick={handleClear}
            block
            style={{
              borderRadius: 8,
              fontWeight: 500,
              color: COLORS.textMuted,
              borderColor: COLORS.inputBorder,
              marginTop: 'auto',
            }}
          >
            Clear All Content
          </Button>
        </div>

        {/* ==================== Right Panel ==================== */}
        <div
          style={{
            flex: 1,
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            background: COLORS.pageBg,
            overflow: 'hidden',
          }}
        >
          {/* ---- Toolbar ---- */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 24px',
              borderBottom: `1px solid ${COLORS.cardBorder}`,
              background: COLORS.cardBg,
              minHeight: 52,
              boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
              flexShrink: 0,
            }}
          >
            {/* Left-side stats */}
            <Space size={14}>
              {store.lastResult && (
                <>
                  <Tag
                    style={{
                      fontSize: 11,
                      borderRadius: 5,
                      background: COLORS.accentBg,
                      border: `1px solid ${COLORS.accentBorder}`,
                      color: COLORS.accent,
                      padding: '0 10px',
                      lineHeight: '22px',
                      margin: 0,
                    }}
                  >
                    <RobotOutlined style={{ marginRight: 4 }} />
                    {store.lastResult.model || 'AI'}
                  </Tag>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 14,
                      paddingLeft: 14,
                      borderLeft: `1px solid ${COLORS.cardBorder}`,
                    }}
                  >
                    <Text style={{ fontSize: 12, color: COLORS.textSecondary }}>
                      <span style={{ fontWeight: 600, color: COLORS.textPrimary }}>{store.wordCount}</span> words
                    </Text>
                    {store.tokens > 0 && (
                      <Text style={{ fontSize: 12, color: COLORS.textMuted }}>
                        {store.tokens} tokens
                      </Text>
                    )}
                  </div>
                </>
              )}
              {store.isStreaming && (
                <Tag
                  style={{
                    fontSize: 11,
                    borderRadius: 5,
                    background: '#fef3c7',
                    border: '1px solid #fcd34d',
                    color: '#92400e',
                    padding: '0 10px',
                    lineHeight: '22px',
                    margin: 0,
                  }}
                >
                  <Spin size="small" style={{ marginRight: 4 }} />
                  Generating in real time...
                </Tag>
              )}
            </Space>

            {/* Right-side action buttons */}
            <Space size={8}>
              <Tooltip title="Copy all content">
                <Button
                  type="text"
                  size="small"
                  icon={copied ? <CheckOutlined /> : <CopyOutlined />}
                  onClick={handleCopyContent}
                  disabled={!store.generatedContent}
                  style={{
                    color: copied ? COLORS.success : COLORS.textMuted,
                    fontSize: 13,
                    borderRadius: 6,
                  }}
                >
                  Copy
                </Button>
              </Tooltip>
              <Button
                type="primary"
                size="small"
                icon={<FileAddOutlined />}
                onClick={handleInsertToDocument}
                disabled={!store.generatedContent}
                style={{
                  background: `linear-gradient(135deg, ${COLORS.sendBtnStart}, ${COLORS.sendBtnEnd})`,
                  border: 'none',
                  fontWeight: 600,
                  borderRadius: 8,
                  fontSize: 12,
                  boxShadow: SHADOWS.button,
                  padding: '0 16px',
                  height: 32,
                  color: '#fff',
                }}
              >
                Insert into Document
              </Button>
            </Space>
          </div>

          {/* ---- Content Area ---- */}
          <div
            ref={contentRef}
            style={{
              flex: 1,
              overflow: 'hidden auto',
              padding: '28px 32px',
            }}
          >
            {/* Has content -> Markdown rendering */}
            {store.generatedContent ? (
              <div className="writing-content">
                <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]} components={markdownComponents}>
                  {store.generatedContent}
                </ReactMarkdown>
                {store.isStreaming && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      marginTop: 20,
                      padding: 10,
                    }}
                  >
                    <Spin size="small" />
                    <Text style={{ color: COLORS.textMuted, fontSize: 13 }}>AI is writing...</Text>
                  </div>
                )}
              </div>
            ) : store.isGenerating ? (
              renderLoadingState()
            ) : store.error ? (
              renderErrorState()
            ) : (
              renderEmptyState()
            )}
          </div>

          {/* ---- Footer Disclaimer ---- */}
          <div
            style={{
              padding: '10px 24px',
              borderTop: `1px solid ${COLORS.cardBorder}`,
              background: COLORS.cardBg,
              textAlign: 'center',
              flexShrink: 0,
            }}
          >
            <Text style={{ fontSize: 11, color: COLORS.textMuted }}>
              Content generated by AI, for reference and writing assistance only
            </Text>
          </div>
        </div>
      </div>
    </>
  );
};

export default AIWritingPage;
