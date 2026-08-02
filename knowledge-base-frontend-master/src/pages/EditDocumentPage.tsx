import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { Spin } from 'antd';
import { App } from 'antd';
import { useAppStore } from '@/stores';
import { categoryService, fileService, documentService, reviewService } from '@/services';
import './CreateDocumentPage.css'; // Reuses CreateDocumentPage's styles
import { useAutoSave } from '@/hooks/useAutoSave';
import { SaveStatusIndicator } from '@/components/SaveStatusIndicator';
import { DraftRecoveryDialog } from '@/components/DraftRecoveryDialog';

// CSS variable definitions - 100% consistent with the prototype
const styles = {
  // Color variables
  '--primary-color': '#2563eb',
  '--success-color': '#10b981',
  '--danger-color': '#ef4444',
  '--warning-color': '#f59e0b',
  '--text-primary': '#1e293b',
  '--text-secondary': '#64748b',
  '--text-muted': '#94a3b8',
  '--bg-primary': '#ffffff',
  '--bg-secondary': '#f8fafc',
  '--bg-tertiary': '#f1f5f9',
  '--border-color': '#e2e8f0',
  '--radius-sm': '6px',
  '--radius-md': '8px',
  '--radius-lg': '12px',
  '--shadow-sm': '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  '--shadow-md': '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
} as React.CSSProperties;

// Add animation styles and text selection styles
if (typeof document !== 'undefined' && document.head) {
  const styleElement = document.createElement('style');
  styleElement.textContent = `
    @keyframes slideDown {
      from {
        opacity: 0,
        transform: translateX(-50%) translateY(-10px);
      }
      to {
        opacity: 1,
        transform: translateX(-50%) translateY(0);
      }
    }
    @keyframes fadeIn {
      from {
        opacity: 0;
      }
      to {
        opacity: 1;
      }
    }
    /* Text selection styles */
    #documentContent::selection {
      background-color: rgba(105, 89, 205, 0.3);
      color: inherit;
    }
    #documentContent::-moz-selection {
      background-color: rgba(105, 89, 205, 0.3);
      color: inherit;
    }
    /* Improve text selection experience */
    #documentContent {
      -webkit-user-select: text;
      -moz-user-select: text;
      -ms-user-select: text;
      user-select: text;
    }
  `;
  styleElement.setAttribute('data-text-selection', 'true');
  if (!document.head.querySelector('style[data-text-selection="true"]')) {
    document.head.appendChild(styleElement);
  }
}

const EditDocumentPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const { requireApproval, enableAIWriting, maxFileSize } = useAppStore();
  const isDirectPublish = !requireApproval;

  // Get the source page
  const from = searchParams.get('from') || 'documents';

  // Form state
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [summary, setSummary] = useState('');
  const [author, setAuthor] = useState('Technical Committee');
  const [categoryId, setCategoryId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [visibility, setVisibility] = useState('private');
  const [tags, setTags] = useState<string[]>([]);
  const [inputTag, setInputTag] = useState('');
  const [loading, setLoading] = useState(true); // Initially true on the edit page, since the document needs to be loaded
  const [documentId, setDocumentId] = useState<string>(''); // Stores the document ID

  // Image processing state
  const [uploadingImages, setUploadingImages] = useState<Set<string>>(new Set());
  const [processedImages, setProcessedImages] = useState<Map<string, string>>(new Map());
  const isProcessingRef = useRef(false);
  const [showPasteHint, setShowPasteHint] = useState(true);

  // Drag-and-drop state
  const [isDragging, setIsDragging] = useState(false);

  // Text selection state
  const [showSelectionToolbar, setShowSelectionToolbar] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Context menu state
  const [contextMenu, setContextMenu] = useState<{
    visible: boolean;
    x: number;
    y: number;
  }>({ visible: false, x: 0, y: 0 });

  // History (for undo/redo)
  const [history, setHistory] = useState<string[]>([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const isUndoRedoRef = useRef(false); // Flags whether an undo/redo operation is in progress
  const lastSavedContentRef = useRef(''); // Last saved content, to avoid duplicate saves

  // Publish options
  const [saveOption, setSaveOption] = useState<'submit_review' | 'draft'>('submit_review');

  // Toggle state
  const [allowComments, setAllowComments] = useState(true);
  const [allowEdit, setAllowEdit] = useState(false);
  const [aiIndex, setAiIndex] = useState(true);

  // Category data
  const [categories, setCategories] = useState<any[]>([]);

  // ========== Auto-save ==========
  // Build the form data object required for auto-save
  const autoSaveFormData = {
    title,
    content,
    summary,
    categoryId,
    teamId,
    tags,
    visibility,
    allowComments,
    allowEdit,
    aiIndex,
    saveOption,
  };

  const {
    saveStatus,
    lastSavedAt,
    recoveryDraft,
    isRecoveryDialogOpen,
    acceptRecovery,
    dismissRecovery,
    clearDraft,
  } = useAutoSave({
    documentId: id, // Editing an existing document, use the document ID from the URL param
    formData: autoSaveFormData,
  });

  // Restore draft: populate the form with draft data from localStorage
  useEffect(() => {
    // Only process this after the remote document has loaded and there is a recoverable draft
    // Note: acceptRecovery is triggered by DraftRecoveryDialog
  }, [recoveryDraft]);

  // Handle draft recovery confirmation
  const handleAcceptRecovery = () => {
    const draft = acceptRecovery();
    if (draft) {
      // Fill the draft data back into the form (overwriting the current editing content)
      setTitle(draft.title || '');
      setContent(draft.content || '');
      setSummary(draft.summary || '');
      if (draft.categoryId) setCategoryId(String(draft.categoryId));
      if (draft.teamId) setTeamId(String(draft.teamId));
      if (draft.tags && draft.tags.length > 0) setTags(draft.tags);
      if (draft.visibility) setVisibility(draft.visibility);
      setAllowComments(draft.allowComments);
      setAllowEdit(draft.allowEdit);
      setAiIndex(draft.aiIndex);
      if (draft.saveOption) setSaveOption(draft.saveOption);
    }
  };

  useEffect(() => {
    fetchCategories();
    if (id) {
      setDocumentId(id);
      fetchDocument(id);
    }
  }, [id]);

  // Load existing document data
  const fetchDocument = async (docId: string) => {
    try {
      setLoading(true);
      console.log('🔄 Starting to load document, ID:', docId);
      console.log('🔗 API path: `/document/documents/${docId}`');

      const data = await documentService.getDocument(docId);
      // The backend response includes fields (allowComment, visibility, numeric isPublic)
      // beyond the declared Document type; read those through rawData.
      const rawData = data as unknown as Record<string, unknown>;
      console.log('✅ Document loaded successfully, returned data:', data);
      console.log('📋 Data type check:', {
        titleType: typeof data.title,
        contentType: typeof data.content,
        summaryType: typeof data.summary,
        tagsType: typeof data.tags,
        categoryIdType: typeof data.categoryId,
        statusType: typeof data.status,
        allowCommentType: typeof rawData.allowComment,
        isPublicType: typeof data.isPublic,
        author: data.author,
        fullData: JSON.stringify(data, null, 2)
      });

      // Process the tags field - the backend returns a comma-separated string
      let processedTags: string[] = [];
      if (data.tags) {
        if (typeof data.tags === 'string') {
          processedTags = (data.tags as string).split(',').filter((tag: string) => tag.trim()).map(tag => tag.trim());
        } else if (Array.isArray(data.tags)) {
          processedTags = data.tags;
        }
      }
      console.log('🏷️ Processed tags:', processedTags);

      // Process categoryId - the backend returns a number, needs to be converted to a string to match the select value
      const processedCategoryId = data.categoryId ? String(data.categoryId) : '';
      console.log('📁 Processed categoryId:', processedCategoryId, 'type:', typeof processedCategoryId);

      // Process the allowComment field - the backend returns a number (0 or 1)
      const processedAllowComments = rawData.allowComment !== undefined ? rawData.allowComment === 1 : true;
      console.log('💬 Processed allowComments:', processedAllowComments);

      // Process the author info - the backend returns an AuthorVO object
      let processedAuthor = 'Technical Committee';
      if (data.author) {
        if (typeof data.author === 'object' && data.author.username) {
          processedAuthor = data.author.username;
        } else if (typeof data.author === 'string') {
          processedAuthor = data.author;
        }
      } else if (data.authorName) {
        processedAuthor = data.authorName;
      }
      console.log('👤 Processed author:', processedAuthor);

      // Set the form data - note the field name mapping
      console.log('🎯 Starting to set form data...');
      console.log('📊 Full API response data:', JSON.stringify(data, null, 2));

      setTitle(data.title || '');
      console.log('✅ Title set:', data.title);

      setContent(data.content || '');
      console.log('✅ Content set, length:', data.content?.length || 0);

      setSummary(data.summary || '');
      console.log('✅ Summary set:', data.summary);

      setAuthor(processedAuthor);
      setCategoryId(processedCategoryId);

      // Process visibility - maps to the isPublic field
      // is_public: 1=visible to everyone (public), 0=visible to team (team)
      const visibilityValue = rawData.isPublic !== undefined ?
        (rawData.isPublic === 1 ? 'public' : 'team') :
        ((rawData.visibility as string) || 'team');
      setVisibility(visibilityValue);

      // The teamId field does not exist in the database, set it to empty
      setTeamId('');

      setTags(processedTags);
      console.log('✅ Tags set:', processedTags);
      console.log('✅ Visibility set:', visibilityValue, '(isPublic:', data.isPublic, ')');

      setAllowComments(processedAllowComments);
      setAllowEdit(false); // The backend does not have this field, use the default value
      setAiIndex(true); // The backend does not have this field, use the default value
      setSaveOption(data.status === 1 || data.status === 3 ? 'submit_review' : 'draft');
      console.log('✅ Other fields set');

      // Initialize history
      if (data.content) {
        setHistory([data.content]);
        setHistoryIndex(0);
        lastSavedContentRef.current = data.content;
        console.log('✅ History initialized');
      }

      console.log('🎉 Form data set successfully');
    } catch (error) {
      console.error('Failed to load document:', error);
      message.error('Failed to load document: ' + (error as any)?.message || 'Unknown error');
      // Navigation fallback logic could be added here
      // navigate('/documents');
    } finally {
      setLoading(false);
    }
  };

  // Global click listener to close the context menu
  useEffect(() => {
    const handleClickOutside = () => {
      if (contextMenu.visible) {
        closeContextMenu();
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [contextMenu.visible]);

  // Initialize history
  useEffect(() => {
    if (content && historyIndex === -1) {
      setHistory([content]);
      setHistoryIndex(0);
      lastSavedContentRef.current = content;
    }
  }, []);

  // Watch content changes and auto-save history
  useEffect(() => {
    if (!isUndoRedoRef.current && content !== undefined) {
      // Delay saving history to avoid saving too frequently
      const timer = setTimeout(() => {
        if (content !== lastSavedContentRef.current) {
          const newHistory = history.slice(0, historyIndex + 1);
          // Only save when the content has actually changed
          if (newHistory.length === 0 || newHistory[newHistory.length - 1] !== content) {
            newHistory.push(content);
            if (newHistory.length > 50) { // Limit the history size
              newHistory.shift();
            }
            setHistory(newHistory);
            setHistoryIndex(newHistory.length - 1);
            lastSavedContentRef.current = content;
          }
        }
      }, 1000); // 1 second delay

      return () => clearTimeout(timer);
    }
  }, [content]);

  const fetchCategories = async () => {
    try {
      const tree = await categoryService.getCategoryTree();
      setCategories(flattenCategories(tree));
    } catch (error) {
      console.error('Failed to fetch categories:', error);
    }
  };

  const flattenCategories = (tree: any[]): any[] => {
    const result: any[] = [];
    let uniqueCounter = 0; // Used to generate a unique key

    const traverse = (nodes: any[], level: number = 0) => {
      nodes.forEach((node, index) => {
        // Generate a unique key for each category to avoid conflicts even if IDs repeat
        const uniqueKey = `${node.id}_${level}_${index}_${uniqueCounter++}`;
        result.push({
          id: String(node.id), // Convert to a string to match the document data's categoryId
          name: node.name,
          uniqueKey: uniqueKey // Add a unique key for React
        });
        if (node.children) {
          traverse(node.children, level + 1);
        }
      });
    };
    traverse(tree);
    return result;
  };

  /**
   * Determine whether this is an external image URL (needs conversion)
   */
  const isExternalImageUrl = (url: string): boolean => {
    if (!url || typeof url !== 'string') {
      return false;
    }

    // Exclude relative paths
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return false;
    }

    // Exclude file-system image addresses
    // 1. Exclude direct RustFS access addresses (based on endpoint and port)
    const rustfsPatterns = [
      /117\.72\.88\.11:9091/,  // RustFS server address
      /:9091/,                 // RustFS port
      /knowledge-dev/,         // File-system bucket name
      /mall-dev/,              // Other possible bucket names
    ];

    if (rustfsPatterns.some(pattern => pattern.test(url))) {
      console.log('This is a file-system image, no conversion needed:', url);
      return false; // This is a file-system image, no conversion needed
    }

    // 2. Exclude internal domains and local addresses
    const internalDomains = [
      'localhost',
      '127.0.0.1',
      'rustfs',
      window.location.hostname,
    ];

    if (internalDomains.some(domain => url.includes(domain))) {
      console.log('This is an internal address, no conversion needed:', url);
      return false; // This is an internal address, no conversion needed
    }

    // All other HTTP(S) addresses need conversion
    console.log('This is an external image, conversion needed:', url);
    return true;
  };

  /**
   * Extract all image URLs from Markdown content
   */
  const extractImageUrls = (markdown: string): string[] => {
    const urls: string[] = [];

    // Match Markdown-format images: ![alt](url) or ![alt](url "title")
    const markdownImageRegex = /!\[([^\]]*)\]\(([^)]+)\)/g;
    let match;
    while ((match = markdownImageRegex.exec(markdown)) !== null) {
      const url = match[2].trim().split(' ')[0]; // Remove the title part
      urls.push(url);
    }

    // Match HTML-format images: <img src="url" />
    const htmlImageRegex = /<img[^>]+src=["']([^"']+)["'][^>]*>/gi;
    while ((match = htmlImageRegex.exec(markdown)) !== null) {
      urls.push(match[1]);
    }

    return [...new Set(urls)]; // Deduplicate
  };

  /**
   * Handle double-click to select the whole line
   */
  const handleDoubleClick = (e: React.MouseEvent<HTMLTextAreaElement>) => {
    console.log('🖱️ Double-click event triggered'); // Debug log
    const textarea = e.target as HTMLTextAreaElement;
    if (!textarea) {
      console.log('❌ textarea element does not exist'); // Debug log
      return;
    }

    // Do not prevent the default behavior, let the browser select the word first
    // e.preventDefault();

    // Expand the selection to the whole line on the next event loop tick
    setTimeout(() => {
      const cursorPosition = textarea.selectionStart;
      const text = textarea.value;

      console.log('📍 Current cursor position:', cursorPosition, 'Text length:', text.length); // Debug log

      // Find the start position of the current line
      let lineStart = cursorPosition;
      while (lineStart > 0 && text.charAt(lineStart - 1) !== '\n') {
        lineStart--;
      }

      // Find the end position of the current line
      let lineEnd = cursorPosition;
      while (lineEnd < text.length && text.charAt(lineEnd) !== '\n') {
        lineEnd++;
      }

      console.log('✅ Computed line range:', lineStart, '-', lineEnd); // Debug log
      console.log('📝 Selected text:', text.substring(lineStart, lineEnd)); // Debug log

      // Select the whole line
      textarea.setSelectionRange(lineStart, lineEnd);

      // Show the formatting toolbar
      setShowSelectionToolbar(true);
    }, 10);

    console.log('⏰ setTimeout set, expanding selection after 10ms'); // Debug log
  };

  /**
   * Handle mouse down event
   */
  const handleMouseDown = (_e: React.MouseEvent<HTMLTextAreaElement>) => {
    console.log('🖱️ Mouse down'); // Debug log
    // Only hide the toolbar, without interfering with any default behavior
    setShowSelectionToolbar(false);
  };

  /**
   * Handle mouse up event
   */
  const handleMouseUp = (e: React.MouseEvent<HTMLTextAreaElement>) => {
    console.log('🖱️ Mouse up'); // Debug log
    const textarea = e.target as HTMLTextAreaElement;
    if (!textarea) {
      console.log('❌ textarea element does not exist'); // Debug log
      return;
    }

    // Check whether any text is selected
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;

    console.log('📍 Current selection range:', start, '-', end, 'Length:', end - start); // Debug log

    if (start !== end) {
      console.log('✅ Show toolbar'); // Debug log
      setShowSelectionToolbar(true);
    } else {
      console.log('❌ No text selected'); // Debug log
    }
  };

  /**
   * Handle text selection changes
   */
  const handleSelect = (e: React.SyntheticEvent<HTMLTextAreaElement>) => {
    console.log('📋 onSelect event triggered'); // Debug log
    const textarea = e.target as HTMLTextAreaElement;
    if (!textarea) {
      console.log('❌ textarea element does not exist'); // Debug log
      return;
    }

    // Check whether any text is selected
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;

    console.log('📍 onSelect selection range:', start, '-', end); // Debug log

    if (start !== end) {
      console.log('✅ Show toolbar'); // Debug log
      setShowSelectionToolbar(true);
    } else {
      console.log('❌ Hide toolbar'); // Debug log
      setShowSelectionToolbar(false);
    }
  };

  /**
   * Handle context menu
   */
  const handleContextMenu = (e: React.MouseEvent<HTMLTextAreaElement>) => {
    e.preventDefault();
    const textarea = e.target as HTMLTextAreaElement;
    if (!textarea) return;

    setContextMenu({
      visible: true,
      x: e.clientX,
      y: e.clientY,
    });
  };

  /**
   * Close context menu
   */
  const closeContextMenu = () => {
    setContextMenu({ visible: false, x: 0, y: 0 });
  };

  /**
   * Execute an edit operation
   */
  const executeEdit = (operation: 'copy' | 'cut' | 'paste' | 'delete' | 'selectAll') => {
    const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = content.substring(start, end);

    switch (operation) {
      case 'copy':
        if (selectedText) {
          navigator.clipboard.writeText(selectedText);
          message.success('Copied to clipboard');
        }
        break;
      case 'cut':
        if (selectedText) {
          navigator.clipboard.writeText(selectedText);
          const newContent = content.substring(0, start) + content.substring(end);
          setContent(newContent);
          textarea.setSelectionRange(start, start);
          message.success('Cut');
        }
        break;
      case 'paste':
        navigator.clipboard.readText().then(text => {
          const newContent = content.substring(0, start) + text + content.substring(end);
          setContent(newContent);
          const newPosition = start + text.length;
          textarea.setSelectionRange(newPosition, newPosition);
          message.success('Pasted');
        }).catch(() => {
          message.error('Unable to access clipboard');
        });
        break;
      case 'delete':
        if (selectedText) {
          const newContent = content.substring(0, start) + content.substring(end);
          setContent(newContent);
          textarea.setSelectionRange(start, start);
          message.success('Deleted');
        } else {
          // Delete the current line
          const lineStart = content.lastIndexOf('\n', start - 1) + 1;
          const lineEnd = content.indexOf('\n', end);
          const newContent = content.substring(0, lineStart) +
            (lineEnd !== -1 ? content.substring(lineEnd + 1) : '');
          setContent(newContent);
          textarea.setSelectionRange(lineStart, lineStart);
          message.success('Current line deleted');
        }
        break;
      case 'selectAll':
        textarea.setSelectionRange(0, content.length);
        setShowSelectionToolbar(true);
        break;
    }
    closeContextMenu();
  };

  /**
   * Quickly delete the current line
   */
  const deleteCurrentLine = () => {
    const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
    if (!textarea) return;

    const cursorPosition = textarea.selectionStart;
    const text = textarea.value;

    // Find the start position of the current line
    let lineStart = cursorPosition;
    while (lineStart > 0 && text.charAt(lineStart - 1) !== '\n') {
      lineStart--;
    }

    // Find the end position of the current line
    let lineEnd = cursorPosition;
    while (lineEnd < text.length && text.charAt(lineEnd) !== '\n') {
      lineEnd++;
    }

    // Delete the whole line
    const newContent = text.substring(0, lineStart) +
      (lineEnd < text.length ? text.substring(lineEnd + 1) : '');
    setContent(newContent);

    // Set the cursor position
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(lineStart, lineStart);
    }, 0);
  };

  /**
   * Copy the current line
   */
  const duplicateCurrentLine = () => {
    const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
    if (!textarea) return;

    const cursorPosition = textarea.selectionStart;
    const text = textarea.value;

    // Find the start position of the current line
    let lineStart = cursorPosition;
    while (lineStart > 0 && text.charAt(lineStart - 1) !== '\n') {
      lineStart--;
    }

    // Find the end position of the current line
    let lineEnd = cursorPosition;
    while (lineEnd < text.length && text.charAt(lineEnd) !== '\n') {
      lineEnd++;
    }

    const currentLine = text.substring(lineStart, lineEnd);
    const newContent = text.substring(0, lineEnd) + '\n' + currentLine + text.substring(lineEnd);
    setContent(newContent);

    message.success('Current line copied');
  };

  /**
   * Quickly comment/uncomment the current line
   */
  const toggleComment = () => {
    const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
    if (!textarea) return;

    const cursorPosition = textarea.selectionStart;
    const text = textarea.value;

    // Find the start position of the current line
    let lineStart = cursorPosition;
    while (lineStart > 0 && text.charAt(lineStart - 1) !== '\n') {
      lineStart--;
    }

    // Find the end position of the current line
    let lineEnd = cursorPosition;
    while (lineEnd < text.length && text.charAt(lineEnd) !== '\n') {
      lineEnd++;
    }

    const currentLine = text.substring(lineStart, lineEnd);
    const trimmedLine = currentLine.trimStart();

    // HTML comment syntax
    if (trimmedLine.startsWith('<!--')) {
      // Uncomment
      const uncommented = currentLine.replace(/<!--\s*/, '').replace(/\s*-->/, '');
      const newContent = text.substring(0, lineStart) + uncommented + text.substring(lineEnd);
      setContent(newContent);
    } else {
      // Add comment
      const commented = '<!-- ' + currentLine + ' -->';
      const newContent = text.substring(0, lineStart) + commented + text.substring(lineEnd);
      setContent(newContent);
    }
  };


  /**
   * Undo operation
   */
  const handleUndo = () => {
    if (historyIndex > 0) {
      isUndoRedoRef.current = true; // Flag that an undo operation has started
      const previousContent = history[historyIndex - 1];
      setContent(previousContent);
      setHistoryIndex(historyIndex - 1);
      lastSavedContentRef.current = previousContent;
      message.success('Undone');

      // Delay resetting the flag to ensure setContent completes
      setTimeout(() => {
        isUndoRedoRef.current = false;
      }, 100);
    } else {
      message.warning('No more actions to undo');
    }
  };

  /**
   * Redo operation
   */
  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      isUndoRedoRef.current = true; // Flag that a redo operation has started
      const nextContent = history[historyIndex + 1];
      setContent(nextContent);
      setHistoryIndex(historyIndex + 1);
      lastSavedContentRef.current = nextContent;
      message.success('Redone');

      // Delay resetting the flag to ensure setContent completes
      setTimeout(() => {
        isUndoRedoRef.current = false;
      }, 100);
    } else {
      message.warning('No more actions to redo');
    }
  };

  /**
   * Handle keyboard shortcuts
   */
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    const textarea = e.target as HTMLTextAreaElement;
    const ctrlOrCmd = e.ctrlKey || e.metaKey;

    // Ctrl/Cmd + Enter: save document
    if (ctrlOrCmd && e.key === 'Enter') {
      e.preventDefault();
      handleSaveDocument();
    }

    // Ctrl/Cmd + S: save document
    if (ctrlOrCmd && e.key === 's') {
      e.preventDefault();
      handleSaveDocument();
    }

    // Ctrl/Cmd + Z: undo
    if (ctrlOrCmd && e.key === 'z' && !e.shiftKey) {
      e.preventDefault();
      handleUndo();
    }

    // Ctrl/Cmd + Shift + Z or Ctrl/Cmd + Y: redo
    if ((ctrlOrCmd && e.shiftKey && e.key === 'z') || (ctrlOrCmd && e.key === 'y')) {
      e.preventDefault();
      handleRedo();
    }

    // Ctrl/Cmd + D: copy the current line
    if (ctrlOrCmd && e.key === 'd') {
      e.preventDefault();
      duplicateCurrentLine();
    }

    // Ctrl/Cmd + /: comment/uncomment
    if (ctrlOrCmd && e.key === '/') {
      e.preventDefault();
      toggleComment();
    }

    // Ctrl/Cmd + Backspace: delete the current line
    if (ctrlOrCmd && e.key === 'Backspace') {
      e.preventDefault();
      deleteCurrentLine();
    }

    // Ctrl/Cmd + A: select all
    if (ctrlOrCmd && e.key === 'a') {
      e.preventDefault();
      executeEdit('selectAll');
    }

    // Ctrl/Cmd + C: copy
    if (ctrlOrCmd && e.key === 'c') {
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      if (start !== end) {
        // Let the default behavior handle copying
        return;
      } else {
        // Copy the current line when no text is selected
        e.preventDefault();
        const cursorPosition = textarea.selectionStart;
        const text = textarea.value;
        let lineStart = cursorPosition;
        while (lineStart > 0 && text.charAt(lineStart - 1) !== '\n') {
          lineStart--;
        }
        let lineEnd = cursorPosition;
        while (lineEnd < text.length && text.charAt(lineEnd) !== '\n') {
          lineEnd++;
        }
        const currentLine = text.substring(lineStart, lineEnd);
        navigator.clipboard.writeText(currentLine);
        message.success('Current line copied');
      }
    }

    // Ctrl/Cmd + X: cut
    if (ctrlOrCmd && e.key === 'x') {
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      if (start !== end) {
        // Let the default behavior handle cutting
        return;
      } else {
        // Cut the current line when no text is selected
        e.preventDefault();
        deleteCurrentLine();
        message.success('Current line cut');
      }
    }

    // Tab: indent
    if (e.key === 'Tab' && !e.shiftKey) {
      e.preventDefault();
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const newContent = content.substring(0, start) + '  ' + content.substring(end);
      setContent(newContent);
      setTimeout(() => {
        textarea.focus();
        textarea.setSelectionRange(start + 2, start + 2);
      }, 0);
    }

    // Shift + Tab: outdent
    if (e.key === 'Tab' && e.shiftKey) {
      e.preventDefault();
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const beforeText = content.substring(0, start);
      const afterText = content.substring(end);

      // Remove two spaces or one tab
      let newStart = start;
      let newBeforeText = beforeText;
      if (beforeText.endsWith('  ')) {
        newBeforeText = beforeText.slice(0, -2);
        newStart = start - 2;
      } else if (beforeText.endsWith('\t')) {
        newBeforeText = beforeText.slice(0, -1);
        newStart = start - 1;
      }

      const newContent = newBeforeText + afterText;
      setContent(newContent);
      setTimeout(() => {
        textarea.focus();
        textarea.setSelectionRange(newStart, newStart);
      }, 0);
    }
  };

  /**
   * Insert formatted text
   */
  const insertFormattedText = (before: string, after: string, placeholder: string = '') => {
    const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = content.substring(start, end) || placeholder;

    // Build the new text
    const newText = before + selectedText + after;
    const beforeText = content.substring(0, start);
    const afterText = content.substring(end);
    const newContent = beforeText + newText + afterText;

    setContent(newContent);

    // Set the cursor position
    setTimeout(() => {
      textarea.focus();
      const newPosition = start + newText.length;
      textarea.setSelectionRange(newPosition, newPosition);
      setShowSelectionToolbar(false);
    }, 0);
  };

  /**
   * Format the selected text
   */
  const formatSelectedText = (type: 'bold' | 'italic' | 'strikethrough' | 'code' | 'link' | 'image' | 'quote' | 'list') => {
    switch (type) {
      case 'bold':
        insertFormattedText('**', '**', 'bold text');
        break;
      case 'italic':
        insertFormattedText('*', '*', 'italic text');
        break;
      case 'strikethrough':
        insertFormattedText('~~', '~~', 'strikethrough text');
        break;
      case 'code':
        insertFormattedText('`', '`', 'code');
        break;
      case 'link':
        insertFormattedText('[', '](https://)', 'link text');
        break;
      case 'image':
        insertFormattedText('![', '](https://)', 'image description');
        break;
      case 'quote':
        insertFormattedText('> ', '', 'quoted text');
        break;
      case 'list':
        insertFormattedText('- ', '', 'list item');
        break;
    }
  };

  /**
   * Process external images in the content
   */
  const processExternalImages = async (markdown: string): Promise<string> => {
    const imageUrls = extractImageUrls(markdown);
    console.log('📸 Extracted all image URLs from content:', imageUrls);

    const externalUrls = imageUrls.filter(isExternalImageUrl);
    console.log('🌍 External image URLs that need conversion:', externalUrls);

    if (externalUrls.length === 0) {
      console.log('✅ No external images need conversion');
      return markdown;
    }

    // Filter out unprocessed image URLs
    const unprocessedUrls = externalUrls.filter(url => !processedImages.has(url));

    if (unprocessedUrls.length === 0) {
      console.log('✅ All external images have already been processed');
      return markdown;
    }

    console.log('External image URLs to upload:', unprocessedUrls);

    // Mark all images as uploading
    setUploadingImages(prev => {
      const newSet = new Set(prev);
      unprocessedUrls.forEach(url => newSet.add(url));
      return newSet;
    });

    try {
      // Use the batch conversion API
      const response = await fileService.batchConvertUrls(unprocessedUrls);

      console.log('Batch image conversion result:', response);

      // Save the successful mapping
      setProcessedImages(prev => {
        const newMap = new Map(prev);
        Object.entries(response.urlMappings).forEach(([oldUrl, newUrl]) => {
          newMap.set(oldUrl, newUrl);
        });
        return newMap;
      });

      // Replace image URLs
      let processedContent = markdown;

      Object.entries(response.urlMappings).forEach(([oldUrl, newUrl]) => {
        if (oldUrl !== newUrl) {
          // Replace Markdown format
          processedContent = processedContent.replace(
            new RegExp(`!\\[([^\\]]*)\\]\\(${escapeRegExp(oldUrl)}(\\s+"[^"]*"|\\s*'[^']*'|\\s*)\\)`, 'g'),
            `![$1](${newUrl}$2)`
          );

          // Replace HTML format
          processedContent = processedContent.replace(
            new RegExp(`(<img[^>]+src=["'])${escapeRegExp(oldUrl)}(["'][^>]*>)`, 'gi'),
            `$1${newUrl}$2`
          );
        }
      });

      console.log('Image processing complete: {} succeeded, {} failed',
        response.successCount, response.failureCount);

      if (response.failureCount > 0) {
        message.warning(`${response.failureCount} image(s) failed to upload, original URLs kept`);
      }
      // Removed the success toast to avoid unnecessary notifications

      return processedContent;

    } catch (error) {
      console.error('Batch image conversion failed:', error);
      throw error;
    } finally {
      // Clear all upload flags
      setUploadingImages(prev => {
        const newSet = new Set(prev);
        unprocessedUrls.forEach(url => newSet.delete(url));
        return newSet;
      });
    }
  };

  /**
   * Escape special regex characters
   */
  const escapeRegExp = (string: string): string => {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  };

  /**
   * Debounce content changes
   */
  const processContentChange = useRef(
    (() => {
      let timeoutId: ReturnType<typeof setTimeout> | null = null;
      return (newContent: string) => {
        if (timeoutId) {
          clearTimeout(timeoutId);
        }

        timeoutId = setTimeout(async () => {
          if (isProcessingRef.current) {
            return;
          }

          isProcessingRef.current = true;

          try {
            const processed = await processExternalImages(newContent);
            if (processed !== newContent) {
              setContent(processed);
              // Removed the success toast to avoid unnecessary notifications
            }
          } catch (error) {
            console.error('Failed to process image:', error);
          } finally {
            isProcessingRef.current = false;
          }
        }, 1000); // 1 second debounce
      };
    })()
  ).current;

  // Tag management
  const handleAddTag = () => {
    const trimmed = inputTag.trim();
    if (trimmed && !tags.includes(trimmed)) {
      setTags([...tags, trimmed]);
      setInputTag('');
    }
  };

  const handleRemoveTag = (index: number) => {
    setTags(tags.filter((_, i) => i !== index));
  };

  // Text formatting
  const formatText = (format: string) => {
    const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = content.substring(start, end);
    const beforeText = content.substring(0, start);
    const afterText = content.substring(end);

    let formatChars = '';
    let shouldClose = false;

    switch (format) {
      case 'bold': formatChars = '**'; shouldClose = true; break;
      case 'italic': formatChars = '*'; shouldClose = true; break;
      case 'underline': formatChars = '<u></u>'; break;
      case 'strike': formatChars = '~~'; shouldClose = true; break;
      case 'h1': formatChars = '# '; break;
      case 'h2': formatChars = '## '; break;
      case 'h3': formatChars = '### '; break;
      case 'ul': formatChars = '- '; break;
      case 'ol': formatChars = '1. '; break;
      case 'code': formatChars = '`'; shouldClose = true; break;
      case 'quote': formatChars = '> '; break;
    }

    const newText = beforeText + formatChars + selectedText + (shouldClose ? formatChars : '') + afterText;
    setContent(newText);
    textarea.focus();

    setTimeout(() => {
      textarea.setSelectionRange(
        start + formatChars.length,
        start + formatChars.length + selectedText.length
      );
    }, 0);
  };

  const insertLink = () => {
    const url = prompt('Please enter the link URL:');
    if (url) {
      const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
      if (!textarea) return;

      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const selectedText = content.substring(start, end) || 'link text';
      const linkText = `[${selectedText}](${url})`;
      setContent(content.substring(0, start) + linkText + content.substring(end));
      textarea.focus();
    }
  };

  const insertImage = () => {
    // Create a file picker
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';

    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;

      // Client-side file size validation (images use the system-configured limit)
      if (file.size > maxFileSize) {
        const maxMB = Math.round(maxFileSize / 1048576 * 10) / 10;
        message.error({ content: `Image size cannot exceed ${maxMB}MB`, key: 'uploadImage' });
        return;
      }

      try {
        message.loading({ content: 'Uploading image...', key: 'uploadImage' });

        const response = await fileService.upload(file);

        message.success({ content: 'Image uploaded successfully', key: 'uploadImage' });

        // Use previewUrl for image display (optimized specifically for preview)
        const imageUrl = response.previewUrl || response.fileUrl;
        const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
        if (!textarea) return;

        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;

        // Ensure the image is on its own line, with line breaks before and after
        const beforeText = content.substring(0, start);
        const afterText = content.substring(end);

        // Check whether a line break needs to be added before
        let prefix = '\n\n';
        if (beforeText === '' || beforeText.endsWith('\n')) {
          prefix = beforeText.endsWith('\n\n') ? '' : '\n';
        } else if (!beforeText.endsWith('\n')) {
          prefix = '\n\n';
        } else {
          prefix = '';
        }

        // Check whether a line break needs to be added after
        let suffix = '\n\n';
        if (afterText === '' || afterText.startsWith('\n')) {
          suffix = afterText.startsWith('\n\n') ? '' : '\n';
        } else if (!afterText.startsWith('\n')) {
          suffix = '\n\n';
        } else {
          suffix = '';
        }

        const imageText = `${prefix}![${response.originalName || file.name}](${imageUrl})${suffix}`;
        const newContentText = beforeText + imageText + afterText;

        setContent(newContentText);

        textarea.focus();
        // Set the cursor position after the image
        const newPosition = start + prefix.length + imageText.trim().length;
        textarea.setSelectionRange(newPosition, newPosition);
      } catch (error) {
        message.error({ content: 'Image upload failed', key: 'uploadImage' });
        console.error('Image upload failed:', error);
      }
    };

    input.click();
  };

  // Validate document
  const validateDocument = (): boolean => {
    if (!title.trim()) {
      message.error('Please enter a document title');
      return false;
    }

    if (!categoryId) {
      message.error('Please select a document category');
      return false;
    }

    if (!content.trim()) {
      message.error('Please enter document content');
      return false;
    }

    return true;
  };

  // Unified document save function
  const handleSaveDocument = async () => {
    if (saveOption === 'submit_review') {
      if (isDirectPublish) {
        await handleDirectPublish();
      } else {
        await handleSubmitForReview();
      }
    } else {
      await handleSaveDraft();
    }
  };

  // Publish the document directly (no review needed)
  const handleDirectPublish = async () => {
    if (!validateDocument()) return;

    // Check whether the document is already under review
    try {
      const doc = await documentService.getDocument(String(documentId));
      if (doc.status === 'pending_review' || doc.status === 3) {
        message.warning('This document is currently under review and cannot be modified');
        return;
      }
    } catch (e) {
      // Failed to get document status, continue anyway (the backend will validate as a fallback)
    }

    setLoading(true);
    try {
      const documentData = {
        id: documentId,
        title,
        content,
        summary,
        categoryId: categoryId || undefined,
        tags: Array.isArray(tags) ? tags.join(',') : tags,
        status: 0,
        documentType: 1,
        allowComment: allowComments ? 1 : 0,
        source: 1,
        isTop: 0,
        isRecommend: 0,
        sort: 0,
        isPublic: visibility === 'public' ? 1 : 0,
      };

      await documentService.updateDocument(String(documentId), documentData as any);
      await documentService.publishDocument(String(documentId));
      clearDraft();
      message.success('Document published!');
      window.open(`/documents/${documentId}`, '_blank');
    } catch (error) {
      console.error('Failed to publish document:', error);
    } finally {
      setLoading(false);
    }
  };

  // Save draft
  const handleSaveDraft = async () => {
    if (!validateDocument()) return;

    setLoading(true);
    try {
      // Convert the data format to match the backend DocumentDTO
      const documentData = {
        id: documentId,
        title,
        content,
        summary,
        categoryId: categoryId || undefined,
        tags: Array.isArray(tags) ? tags.join(',') : tags, // Convert to a comma-separated string
        status: 0, // 0-draft
        documentType: 1, // 1-article
        allowComment: allowComments ? 1 : 0,
        source: 1, // 1-original
        isTop: 0,
        isRecommend: 0,
        sort: 0,
        isPublic: visibility === 'public' ? 1 : 0, // Map visibility to isPublic
      };

      console.log('Updating document draft, request data:', documentData);
      const result = await documentService.updateDocument(String(documentId), documentData as any);
      console.log('Document draft updated successfully, result:', result);
      clearDraft();
      message.success('Draft saved successfully!');
      window.open(`/documents/${documentId}`, '_blank');
    } catch (error) {
      console.error('Failed to update document draft:', error);
      // Error handled by request interceptor
    } finally {
      setLoading(false);
    }
  };

  // Submit document for review
  const handleSubmitForReview = async () => {
    if (!validateDocument()) return;

    // Check whether the document is already under review
    try {
      const doc = await documentService.getDocument(String(documentId));
      if (doc.status === 'pending_review' || doc.status === 3) {
        message.warning('This document is currently under review and cannot be modified');
        return;
      }
    } catch (e) {
      // Failed to get document status, continue anyway (the backend will validate as a fallback)
    }

    setLoading(true);
    try {
      // Convert the data format to match the backend DocumentDTO
      const documentData = {
        id: documentId,
        title,
        content,
        summary,
        categoryId: categoryId || undefined,
        tags: Array.isArray(tags) ? tags.join(',') : tags, // Convert to a comma-separated string
        // Do not set status; the backend's submitForReview manages status changes uniformly
        documentType: 1, // 1-article
        allowComment: allowComments ? 1 : 0,
        source: 1, // 1-original
        isTop: 0,
        isRecommend: 0,
        sort: 0,
        isPublic: visibility === 'public' ? 1 : 0, // Map visibility to isPublic
      };

      console.log('Submitting for review, request data:', documentData);
      await documentService.updateDocument(String(documentId), documentData as any);
      // Create a review record
      await reviewService.submitForReview(String(documentId));
      console.log('Review submission successful');
      clearDraft();
      message.success('Document submitted for review!');
      window.open(`/documents/${documentId}`, '_blank');
    } catch (error) {
      console.error('Failed to submit for review:', error);
      // Error handled by request interceptor
    } finally {
      setLoading(false);
    }
  };

  // AI features
  const aiGenerateOutline = () => {
    if (!title.trim()) {
      message.warning('Please enter a document title first; AI will generate an outline based on the title.');
      return;
    }

    const outline = `# ${title}

## Overview
Describe the background, purpose, and scope of the document here...

## Main Content

### Part One
- Key point 1
- Key point 2
- Key point 3

### Part Two
- Key point 1
- Key point 2
- Key point 3

## Conclusion
Summarize the core points of the document and action recommendations...

## References
- Related document 1
- Related document 2`;

    setContent(outline);
    message.success('Document outline generated');
  };

  const aiExpandContent = () => {
    if (!content.trim()) {
      message.warning('Please enter some content first; AI will help you expand it.');
      return;
    }

    setContent(content + '\n\n## Detailed Explanation\n\n[AI is generating more detailed content for you...]\n\n- Additional note 1\n- Additional note 2\n- Additional note 3');
    message.success('Content expanded');
  };

  const aiImproveWriting = () => {
    message.info('The AI writing assistant is analyzing your content...\n\nSuggestions:\n1. Use more concise language\n2. Add specific data and examples\n3. Improve paragraph structure\n4. Add necessary charts and diagrams');
  };

  const aiAddExamples = () => {
    setContent(content + '\n\n## Example Explanation\n\n### Example 1:\n[Specific example description...]\n\n### Example 2:\n[Specific example description...]\n\nThese examples can help readers better understand the content.');
    message.success('Example framework added');
  };

  return (
    <>
    <div style={{
      ...styles,
      width: '100%',
      maxWidth: '100%',
      boxSizing: 'border-box',
      paddingTop: '0',
      marginTop: '-64px',
    }}>
      {/* Loading state display */}
      {loading && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(255, 255, 255, 0.9)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
        }}>
          <div style={{ textAlign: 'center' }}>
            <Spin size="large" />
            <div style={{ marginTop: '16px', color: 'var(--text-secondary)' }}>
              Loading document...
            </div>
          </div>
        </div>
      )}
      {/* Top navigation bar */}
      <nav style={{
        background: 'var(--bg-primary)',
        borderBottom: '1px solid var(--border-color)',
        padding: '0 24px',
        height: '64px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 1000,
        boxShadow: 'var(--shadow-sm)',
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '24px',
        }}>
          <div
            onClick={() => navigate('/')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              fontSize: '20px',
              fontWeight: 700,
              color: 'var(--primary-color)',
              textDecoration: 'none',
              cursor: 'pointer',
            }}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            </svg>
            <span>{useAppStore.getState().systemName}</span>
          </div>
          <div
            onClick={() => navigate('/documents')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: 'var(--radius-md)',
              color: 'var(--text-secondary)',
              textDecoration: 'none',
              fontSize: '14px',
              fontWeight: 500,
              transition: 'all 0.2s',
              cursor: 'pointer',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = 'var(--bg-tertiary)';
              e.currentTarget.style.color = 'var(--text-primary)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent';
              e.currentTarget.style.color = 'var(--text-secondary)';
            }}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            Back to Document Center
          </div>
        </div>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '6px 12px',
          borderRadius: 'var(--radius-lg)',
          cursor: 'pointer',
        }}>
          <div style={{
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #2563eb, #8b5cf6)',
            color: 'white',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '14px',
            fontWeight: 600,
          }}>
            {localStorage.getItem('username')?.substring(0, 2).toUpperCase() || 'JD'}
          </div>
        </div>
      </nav>

      {/* Main container */}
      <div style={{
        paddingTop: '80px',
        paddingLeft: '16px',
        paddingRight: '16px',
        paddingBottom: '16px',
        backgroundColor: 'var(--bg-secondary)',
        minHeight: '100vh',
        width: '100%',
        maxWidth: '100%',
        boxSizing: 'border-box',
      }}>
        {/* Page header */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
        }}>
          <div>
            <div style={{
              fontSize: '14px',
              color: 'var(--text-muted)',
              marginBottom: '4px',
            }}>
              {from === 'drafts' ? 'Drafts / Edit Document' : 'Document Center / Edit Document'}
            </div>
            <h1 style={{
              fontSize: '24px',
              fontWeight: '700',
              color: 'var(--text-primary)',
              margin: 0,
            }}>
              Edit Document
            </h1>
          </div>
          <div style={{
            display: 'flex',
            gap: '12px',
          }}>
            <button
              onClick={() => navigate('/documents')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 20px',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-primary)',
                color: 'var(--text-secondary)',
                fontSize: '14px',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = 'var(--bg-tertiary)';
                e.currentTarget.style.color = 'var(--text-primary)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'var(--bg-primary)';
                e.currentTarget.style.color = 'var(--text-secondary)';
              }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
              Cancel
            </button>
            <SaveStatusIndicator status={saveStatus} lastSavedAt={lastSavedAt} />
            <button
              onClick={handleSaveDraft}
              disabled={loading}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 20px',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--bg-primary)',
                color: 'var(--text-secondary)',
                fontSize: '14px',
                fontWeight: '600',
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s',
                opacity: loading ? 0.6 : 1,
              }}
              onMouseEnter={(e) => {
                if (!loading) {
                  e.currentTarget.style.background = 'var(--bg-tertiary)';
                  e.currentTarget.style.color = 'var(--text-primary)';
                }
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'var(--bg-primary)';
                e.currentTarget.style.color = 'var(--text-secondary)';
              }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                <polyline points="17 21 17 13 7 13 7 21"></polyline>
                <polyline points="7 3 7 8 15 8"></polyline>
              </svg>
              Save Draft
            </button>
            <button
              onClick={handleSubmitForReview}
              disabled={loading}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 20px',
                border: 'none',
                borderRadius: 'var(--radius-md)',
                background: 'var(--primary-color)',
                color: 'white',
                fontSize: '14px',
                fontWeight: '600',
                cursor: loading ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s',
                opacity: loading ? 0.6 : 1,
              }}
              onMouseEnter={(e) => {
                if (!loading) {
                  e.currentTarget.style.background = '#1d4ed8';
                }
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'var(--primary-color)';
              }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              Submit Document
            </button>
          </div>
        </div>

        {/* Editor container */}
        <div style={{
          background: 'var(--bg-primary)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-md)',
          overflow: 'hidden',
          width: '100%',
          maxWidth: '100%',
          boxSizing: 'border-box',
        }}>
          {/* Editor header */}
          <div style={{
            padding: '24px',
            borderBottom: '1px solid var(--border-color)',
          }}>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Enter document title..."
              style={{
                width: '100%',
                padding: '10px 16px',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                fontSize: '18px',
                fontWeight: '600',
                color: '#1e293b',
                outline: 'none',
                background: '#ffffff',
                transition: 'all 0.2s',
                marginBottom: '16px',
              }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = '#2563eb';
                e.currentTarget.style.boxShadow = '0 0 0 3px rgba(37, 99, 235, 0.1)';
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = '#e2e8f0';
                e.currentTarget.style.boxShadow = 'none';
              }}
            />
            <div style={{
              display: 'flex',
              gap: '12px',
              flexWrap: 'wrap',
            }}>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                style={{
                  padding: '8px 14px',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '14px',
                  color: 'var(--text-primary)',
                  background: 'var(--bg-primary)',
                  cursor: 'pointer',
                  outline: 'none',
                }}
              >
                <option value="">Select category</option>
                {categories.map((cat, index) => (
                  <option key={cat.uniqueKey || `cat_${index}`} value={cat.id}>{cat.name}</option>
                ))}
              </select>
              {/* Hide the team space field since it does not exist in the database table */}
              {false && <select
                value={teamId}
                onChange={(e) => setTeamId(e.target.value)}
                style={{
                  padding: '8px 14px',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '14px',
                  color: 'var(--text-primary)',
                  background: 'var(--bg-primary)',
                  cursor: 'pointer',
                  outline: 'none',
                }}
              >
                <option value="">Select team space</option>
                <option value="rd">R&D Center</option>
                <option value="marketing">Marketing</option>
                <option value="support">Customer Service</option>
              </select>}
              <select
                value={visibility}
                onChange={(e) => setVisibility(e.target.value)}
                style={{
                  padding: '8px 14px',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '14px',
                  color: 'var(--text-primary)',
                  background: 'var(--bg-primary)',
                  cursor: 'pointer',
                  outline: 'none',
                }}
              >
                <option value="private">Private</option>
                <option value="team">Team Visible</option>
                <option value="public">Visible to Everyone</option>
              </select>
            </div>
          </div>

          {/* Editor body */}
          <div style={{
            display: 'flex',
            width: '100%',
            maxWidth: '100%',
            boxSizing: 'border-box',
          }}>
            {/* Main editing area - split left/right */}
            <div style={{
              flex: 1,
              display: 'flex',
              borderRight: '1px solid var(--border-color)',
              minWidth: 0,
              boxSizing: 'border-box',
            }}>
              {/* Left: Markdown input area */}
              <div style={{
                flex: '1 1 0%',
                display: 'flex',
                flexDirection: 'column',
                minWidth: 0,
                width: '100%',
                boxSizing: 'border-box',
              }}>
                {/* Toolbar */}
                <div style={{
                  display: 'flex',
                  gap: '8px',
                  padding: '16px 24px',
                  borderBottom: '1px solid var(--border-color)',
                  background: 'var(--bg-primary)',
                  flexWrap: 'wrap',
                }}>
                  <button
                    onClick={() => formatText('bold')}
                    title="Bold"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"></path>
                      <path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z"></path>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('italic')}
                    title="Italic"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <line x1="19" y1="4" x2="10" y2="4"></line>
                      <line x1="14" y1="20" x2="5" y2="20"></line>
                      <line x1="15" y1="4" x2="9" y2="20"></line>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('underline')}
                    title="Underline"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M6 3v7a6 6 0 0 0 6 6 6 6 0 0 0 6-6V3"></path>
                      <line x1="4" y1="21" x2="20" y2="21"></line>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('strike')}
                    title="Strikethrough"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M16 4H9a3 3 0 0 0-3 3v0a3 3 0 0 0 3 3h6"></path>
                      <path d="M7 20h10a3 3 0 0 0 3-3v0a3 3 0 0 0-3-3H7"></path>
                      <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                  </button>
                  <div style={{
                    width: '1px',
                    background: 'var(--border-color)',
                    margin: '0 8px',
                  }}></div>
                  <button
                    onClick={() => formatText('h1')}
                    title="Heading 1"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M4 12h8"></path>
                      <path d="M4 18V6"></path>
                      <path d="M12 18V6"></path>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('h2')}
                    title="Heading 2"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M4 12h8"></path>
                      <path d="M4 18V6"></path>
                      <path d="M12 18V6"></path>
                      <path d="M17 12h4"></path>
                      <path d="M19 18v-6"></path>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('h3')}
                    title="Heading 3"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M4 12h8"></path>
                      <path d="M4 18V6"></path>
                      <path d="M12 18V6"></path>
                      <path d="M17 12h4"></path>
                      <path d="M17 16h4"></path>
                      <path d="M19 9V6"></path>
                    </svg>
                  </button>
                  <div style={{
                    width: '1px',
                    background: 'var(--border-color)',
                    margin: '0 8px',
                  }}></div>
                  <button
                    onClick={() => formatText('ul')}
                    title="Bulleted List"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <line x1="8" y1="6" x2="21" y2="6"></line>
                      <line x1="8" y1="12" x2="21" y2="12"></line>
                      <line x1="8" y1="18" x2="21" y2="18"></line>
                      <line x1="3" y1="6" x2="3.01" y2="6"></line>
                      <line x1="3" y1="12" x2="3.01" y2="12"></line>
                      <line x1="3" y1="18" x2="3.01" y2="18"></line>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('ol')}
                    title="Numbered List"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <line x1="10" y1="6" x2="21" y2="6"></line>
                      <line x1="10" y1="12" x2="21" y2="12"></line>
                      <line x1="10" y1="18" x2="21" y2="18"></line>
                      <path d="M4 6h1v4"></path>
                      <path d="M4 10h2"></path>
                      <path d="M6 18H4c0-1 2-2 2-3s-1-1.5-2-1"></path>
                    </svg>
                  </button>
                  <div style={{
                    width: '1px',
                    background: 'var(--border-color)',
                    margin: '0 8px',
                  }}></div>
                  <button
                    onClick={() => formatText('code')}
                    title="Code Block"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <polyline points="16 18 22 12 16 6"></polyline>
                      <polyline points="8 6 2 12 8 18"></polyline>
                    </svg>
                  </button>
                  <button
                    onClick={() => formatText('quote')}
                    title="Quote"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c0 3 2 7 6 7"></path>
                      <path d="M15 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2h-4c0 3 2 7 6 7"></path>
                    </svg>
                  </button>
                  <button
                    onClick={insertLink}
                    title="Insert Link"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                      <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                    </svg>
                  </button>
                  <button
                    onClick={insertImage}
                    title="Insert Image"
                    style={{
                      width: '36px',
                      height: '36px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--bg-primary)',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = 'var(--bg-tertiary)';
                      e.currentTarget.style.color = 'var(--text-primary)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'var(--bg-primary)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                      <circle cx="8.5" cy="8.5" r="1.5"></circle>
                      <polyline points="21 15 16 10 5 21"></polyline>
                    </svg>
                  </button>

                  {/* Upload status indicator */}
                  {uploadingImages.size > 0 && (
                    <div style={{
                      padding: '8px 12px',
                      background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(139, 92, 246, 0.1))',
                      border: '1px solid rgba(37, 99, 235, 0.3)',
                      borderRadius: 'var(--radius-md)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      fontSize: '12px',
                      color: 'var(--primary-color)',
                    }}>
                      <Spin size="small" />
                      <span>Uploading images ({uploadingImages.size})</span>
                    </div>
                  )}
                </div>

                {/* Editor content */}
                <div
                  style={{
                    flex: 1,
                    padding: '24px',
                    display: 'flex',
                    flexDirection: 'column',
                    position: 'relative',
                    border: isDragging ? '2px dashed var(--primary-color)' : '2px dashed transparent',
                    borderRadius: 'var(--radius-md)',
                    background: isDragging ? 'rgba(37, 99, 235, 0.05)' : 'transparent',
                    transition: 'all 0.2s ease',
                  }}
                  onDragOver={(e) => {
                    // Only prevent default behavior when dropping files, without interfering with text selection
                    if (e.dataTransfer?.types.includes('Files')) {
                      e.preventDefault();
                      setIsDragging(true);
                    }
                  }}
                  onDragLeave={(e) => {
                    // Only handle this when dropping files
                    if (e.dataTransfer?.types.includes('Files')) {
                      e.preventDefault();
                      // Only hide the hint when leaving the entire container
                      const rect = e.currentTarget.getBoundingClientRect();
                      if (
                        e.clientX < rect.left ||
                        e.clientX > rect.right ||
                        e.clientY < rect.top ||
                        e.clientY > rect.bottom
                      ) {
                        setIsDragging(false);
                      }
                    }
                  }}
                  onDrop={async (e) => {
                    // Only prevent default behavior when dropping files
                    const files = e.dataTransfer?.files;
                    if (files && files.length > 0) {
                      e.preventDefault();
                      setIsDragging(false);

                      // Process the dropped file
                      for (const file of Array.from(files)) {
                        if (file.type.startsWith('image/')) {
                          try {
                            message.loading({ content: 'Uploading image...', key: 'dropImage' });

                            const response = await fileService.upload(file);
                            message.success({ content: 'Image uploaded successfully', key: 'dropImage' });

                            // Use previewUrl for image display (optimized specifically for preview)
                            const imageUrl = response.previewUrl || response.fileUrl;

                            // Insert the image at the cursor position or at the end of the content
                            const textarea = document.getElementById('documentContent') as HTMLTextAreaElement;
                            if (textarea) {
                              const start = textarea.selectionStart;
                              const end = textarea.selectionEnd;
                              const beforeText = content.substring(0, start);
                              const afterText = content.substring(end);

                              // Ensure the image is on its own line, with line breaks before and after
                              let prefix = '\n\n';
                              if (beforeText === '' || beforeText.endsWith('\n')) {
                                prefix = beforeText.endsWith('\n\n') ? '' : '\n';
                              } else if (!beforeText.endsWith('\n')) {
                                prefix = '\n\n';
                              } else {
                                prefix = '';
                              }

                              let suffix = '\n\n';
                              if (afterText === '' || afterText.startsWith('\n')) {
                                suffix = afterText.startsWith('\n\n') ? '' : '\n';
                              } else if (!afterText.startsWith('\n')) {
                                suffix = '\n\n';
                              } else {
                                suffix = '';
                              }

                              const imageText = `${prefix}![${response.originalName || file.name}](${imageUrl})${suffix}`;
                              const newContentText = beforeText + imageText + afterText;

                              setContent(newContentText);

                              // Set the cursor position
                              setTimeout(() => {
                                textarea.focus();
                                const newPosition = start + prefix.length + imageText.trim().length;
                                textarea.setSelectionRange(newPosition, newPosition);
                              }, 0);
                            }
                          } catch (error) {
                            message.error({ content: 'Image upload failed', key: 'dropImage' });
                            console.error('Drag-and-drop image upload failed:', error);
                          }
                          break; // Only handle the first image
                        }
                      }
                    } else {
                      // When there is no file, do not prevent default behavior, allow text-selection dragging
                      setIsDragging(false);
                    }
                  }}
                >
                  {/* Editor hint */}
                  {isDragging ? (
                    <div style={{
                      padding: '12px 16px',
                      background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(139, 92, 246, 0.1))',
                      border: '1px solid rgba(37, 99, 235, 0.4)',
                      borderRadius: 'var(--radius-md)',
                      marginBottom: '16px',
                      fontSize: '13px',
                      color: 'var(--primary-color)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '8px',
                      fontWeight: 500,
                      transition: 'all 0.2s ease',
                    }}>
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                        <polyline points="17 8 12 3 7 8"></polyline>
                        <line x1="12" y1="3" x2="12" y2="15"></line>
                      </svg>
                      <span>Release to upload the image</span>
                    </div>
                  ) : showPasteHint && (
                    <div style={{
                      padding: '12px 16px',
                      background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.05), rgba(139, 92, 246, 0.05))',
                      border: '1px solid rgba(37, 99, 235, 0.2)',
                      borderRadius: 'var(--radius-md)',
                      marginBottom: '16px',
                      fontSize: '13px',
                      color: 'var(--text-secondary)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      transition: 'all 0.3s ease',
                      animation: 'fadeIn 0.3s ease',
                    }}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="16" x2="12" y2="12"></line>
                        <line x1="12" y1="8" x2="12.01" y2="8"></line>
                      </svg>
                      <span>When pasting Markdown content containing external images, the images will automatically be uploaded to the server and replaced with new addresses</span>
                    </div>
                  )}

                  {/* Selection toolbar */}
                  {showSelectionToolbar && (
                    <div style={{
                      position: 'absolute',
                      top: '60px',
                      left: '50%',
                      transform: 'translateX(-50%)',
                      background: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-lg)',
                      padding: '8px 16px',
                      display: 'flex',
                      gap: '8px',
                      alignItems: 'center',
                      boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                      zIndex: 10,
                      animation: 'slideDown 0.2s ease',
                    }}>
                      <button
                        onClick={() => formatSelectedText('bold')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '16px',
                          fontWeight: 'bold',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Bold (Ctrl+B)"
                      >
                        B
                      </button>
                      <button
                        onClick={() => formatSelectedText('italic')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '16px',
                          fontStyle: 'italic',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Italic (Ctrl+I)"
                      >
                        I
                      </button>
                      <button
                        onClick={() => formatSelectedText('strikethrough')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '16px',
                          textDecoration: 'line-through',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Strikethrough"
                      >
                        S
                      </button>
                      <div style={{ width: '1px', height: '20px', background: 'var(--border-color)' }} />
                      <button
                        onClick={() => formatSelectedText('code')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '14px',
                          fontFamily: 'Monaco, Menlo, monospace',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Code"
                      >
                        &lt;/&gt;
                      </button>
                      <button
                        onClick={() => formatSelectedText('link')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '14px',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Link"
                      >
                        🔗
                      </button>
                      <button
                        onClick={() => formatSelectedText('image')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '14px',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Image"
                      >
                        🖼️
                      </button>
                      <div style={{ width: '1px', height: '20px', background: 'var(--border-color)' }} />
                      <button
                        onClick={() => formatSelectedText('quote')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '14px',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="Quote"
                      >
                        💬
                      </button>
                      <button
                        onClick={() => formatSelectedText('list')}
                        style={{
                          padding: '6px 12px',
                          background: 'none',
                          border: '1px solid var(--border-color)',
                          borderRadius: 'var(--radius-sm)',
                          cursor: 'pointer',
                          fontSize: '14px',
                          transition: 'all 0.2s',
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.background = 'var(--primary-color)';
                          e.currentTarget.style.color = 'white';
                          e.currentTarget.style.borderColor = 'var(--primary-color)';
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.background = 'none';
                          e.currentTarget.style.color = 'inherit';
                          e.currentTarget.style.borderColor = 'var(--border-color)';
                        }}
                        title="List"
                      >
                        ☰
                      </button>
                    </div>
                  )}

                  <textarea
                    id="documentContent"
                    ref={textareaRef}
                    value={content}
                    onChange={(e) => {
                      const newContent = e.target.value;
                      setContent(newContent);
                      // Hide the hint once the user starts typing
                      if (showPasteHint && newContent.length > 0) {
                        setShowPasteHint(false);
                      }
                      // Hide the selection toolbar
                      if (showSelectionToolbar) {
                        setShowSelectionToolbar(false);
                      }
                      // Automatically process external images
                      processContentChange(newContent);
                    }}
                    onMouseDown={handleMouseDown}
                    onMouseUp={handleMouseUp}
                    onDoubleClick={handleDoubleClick}
                    onContextMenu={handleContextMenu}
                    onKeyDown={handleKeyDown}
                    onSelect={handleSelect}
                    onPaste={async (e) => {
                      const items = e.clipboardData?.items;
                      if (!items) return;

                      // Check whether there is an image file
                      for (const item of Array.from(items)) {
                        if (item.type.startsWith('image/')) {
                          e.preventDefault();
                          const file = item.getAsFile();
                          if (file) {
                            try {
                              message.loading({ content: 'Pasting and uploading image...', key: 'pasteImage' });
                              const response = await fileService.upload(file);
                              message.success({ content: 'Image uploaded successfully', key: 'pasteImage' });

                              const textarea = e.target as HTMLTextAreaElement;
                              const start = textarea.selectionStart;
                              const end = textarea.selectionEnd;

                              // Use previewUrl for image display (optimized specifically for preview)
                              const imageUrl = response.previewUrl || response.fileUrl;

                              // Insert the image at the cursor position
                              const beforeText = content.substring(0, start);
                              const afterText = content.substring(end);

                              // Ensure the image is on its own line, with line breaks before and after
                              let prefix = '\n\n';
                              if (beforeText === '' || beforeText.endsWith('\n')) {
                                prefix = beforeText.endsWith('\n\n') ? '' : '\n';
                              } else if (!beforeText.endsWith('\n')) {
                                prefix = '\n\n';
                              } else {
                                prefix = '';
                              }

                              let suffix = '\n\n';
                              if (afterText === '' || afterText.startsWith('\n')) {
                                suffix = afterText.startsWith('\n\n') ? '' : '\n';
                              } else if (!afterText.startsWith('\n')) {
                                suffix = '\n\n';
                              } else {
                                suffix = '';
                              }

                              const imageText = `${prefix}![${response.originalName || file.name}](${imageUrl})${suffix}`;
                              const newContentText = beforeText + imageText + afterText;

                              setContent(newContentText);

                              // Set the cursor position
                              setTimeout(() => {
                                textarea.focus();
                                const newPosition = start + prefix.length + imageText.trim().length;
                                textarea.setSelectionRange(newPosition, newPosition);
                              }, 0);
                            } catch (error) {
                              message.error({ content: 'Image upload failed', key: 'pasteImage' });
                              console.error('Paste image upload failed:', error);
                            }
                          }
                          break; // Only handle the first image
                        }
                      }
                    }}
                    placeholder={`Start writing your content...
Supports Markdown formatting:
# Heading
## Subheading
**Bold** *Italic* ~~Strikethrough~~
- Bulleted list
1. Numbered list
\`code\`
[link](url)

Or just type plain text and we will automatically format it for you.`}
                    style={{
                      width: '100%',
                      flex: 1,
                      height: '100%',
                      minHeight: '400px',
                      padding: '20px',
                      border: 'none',
                      fontSize: '16px',
                      lineHeight: '1.8',
                      fontFamily: 'inherit',
                      resize: 'none',
                      outline: 'none',
                      background: 'transparent',
                      // Remove CSS properties that might interfere with selection
                      // userSelect: 'text',
                      // WebkitUserSelect: 'text',
                      cursor: 'text',
                      // Ensure text selection works properly
                      caretColor: '#6959CD',
                    }}
                  />

                  {/* Context menu */}
                  {contextMenu.visible && (
                    <div
                      style={{
                        position: 'fixed',
                        left: contextMenu.x,
                        top: contextMenu.y,
                        background: 'var(--bg-primary)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-md)',
                        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                        zIndex: 1000,
                        minWidth: '180px',
                        animation: 'fadeIn 0.15s ease',
                      }}
                      onMouseLeave={closeContextMenu}
                    >
                      <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        padding: '4px 0',
                      }}>
                        <button
                          onClick={() => executeEdit('copy')}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>📋</span>
                          <span>Copy</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+C
                          </span>
                        </button>

                        <button
                          onClick={() => executeEdit('cut')}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>✂️</span>
                          <span>Cut</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+X
                          </span>
                        </button>

                        <button
                          onClick={() => executeEdit('paste')}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>📄</span>
                          <span>Paste</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+V
                          </span>
                        </button>

                        <div style={{
                          height: '1px',
                          background: 'var(--border-color)',
                          margin: '4px 0',
                        }} />

                        <button
                          onClick={() => executeEdit('selectAll')}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>⬚</span>
                          <span>Select All</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+A
                          </span>
                        </button>

                        <button
                          onClick={() => executeEdit('delete')}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>🗑️</span>
                          <span>Delete</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Del
                          </span>
                        </button>

                        <div style={{
                          height: '1px',
                          background: 'var(--border-color)',
                          margin: '4px 0',
                        }} />

                        <button
                          onClick={duplicateCurrentLine}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>📋</span>
                          <span>Copy Current Line</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+D
                          </span>
                        </button>

                        <button
                          onClick={deleteCurrentLine}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>🗑️</span>
                          <span>Delete Current Line</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+⌫
                          </span>
                        </button>

                        <div style={{
                          height: '1px',
                          background: 'var(--border-color)',
                          margin: '4px 0',
                        }} />

                        <button
                          onClick={handleUndo}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>↩️</span>
                          <span>Undo</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+Z
                          </span>
                        </button>

                        <button
                          onClick={handleRedo}
                          style={{
                            padding: '8px 16px',
                            background: 'none',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px',
                            fontSize: '14px',
                            color: 'var(--text-primary)',
                            transition: 'background 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.background = 'var(--bg-secondary)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.background = 'none';
                          }}
                        >
                          <span>↪️</span>
                          <span>Redo</span>
                          <span style={{ marginLeft: 'auto', fontSize: '12px', color: 'var(--text-secondary)' }}>
                            Ctrl+Y
                          </span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Right: live preview area */}
              <div style={{
                flex: '1 1 0%',
                display: 'flex',
                flexDirection: 'column',
                borderLeft: '1px solid var(--border-color)',
                background: 'var(--bg-secondary)',
                minWidth: 0,
                width: '100%',
                boxSizing: 'border-box',
              }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '12px 16px',
                  borderBottom: '1px solid var(--border-color)',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: 'var(--text-primary)',
                }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                  Live Preview
                  {uploadingImages.size > 0 && (
                    <span style={{
                      marginLeft: '12px',
                      padding: '4px 10px',
                      background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(139, 92, 246, 0.1))',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '12px',
                      color: 'var(--primary-color)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px',
                    }}>
                      <Spin size="small" />
                      {uploadingImages.size} image(s) uploading
                    </span>
                  )}
                </div>
                <div style={{
                  flex: 1,
                  padding: '0',
                  overflow: 'auto',
                }}>
                  {content.trim() ? (
                    <div style={{
                      background: 'var(--bg-primary)',
                      borderRadius: 'var(--radius-xl)',
                      padding: '40px',
                      border: '1px solid var(--border-color)',
                      minHeight: '100%',
                      fontSize: '16px',
                      lineHeight: '1.8',
                      color: 'var(--text-secondary)',
                    }}>
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        rehypePlugins={[rehypeRaw]}
                        components={{
                          h1: ({ children }) => (
                            <h1 style={{
                              fontSize: '32px',
                              fontWeight: '700',
                              margin: '0 0 16px',
                              color: 'var(--text-primary)',
                              lineHeight: '1.3',
                            }}>{children}</h1>
                          ),
                          h2: ({ children }) => (
                            <h2 style={{
                              fontSize: '24px',
                              fontWeight: '700',
                              color: 'var(--text-primary)',
                              marginBottom: '16px',
                              paddingBottom: '12px',
                              borderBottom: '2px solid var(--bg-tertiary)',
                              marginTop: '0',
                            }}>{children}</h2>
                          ),
                          h3: ({ children }) => (
                            <h3 style={{
                              fontSize: '20px',
                              fontWeight: '600',
                              color: 'var(--text-primary)',
                              margin: '24px 0 12px',
                              lineHeight: '1.4',
                            }}>{children}</h3>
                          ),
                          h4: ({ children }) => (
                            <h4 style={{
                              fontSize: '18px',
                              fontWeight: '600',
                              color: 'var(--text-primary)',
                              margin: '20px 0 12px',
                              lineHeight: '1.4',
                            }}>{children}</h4>
                          ),
                          p: ({ children }) => (
                            <p style={{
                              fontSize: '16px',
                              lineHeight: '1.8',
                              color: 'var(--text-secondary)',
                              marginBottom: '16px',
                              margin: '0 0 16px 0',
                            }}>{children}</p>
                          ),
                          ul: ({ children }) => (
                            <ul style={{
                              marginLeft: '24px',
                              marginBottom: '16px',
                            }}>{children}</ul>
                          ),
                          ol: ({ children }) => (
                            <ol style={{
                              marginLeft: '24px',
                              marginBottom: '16px',
                            }}>{children}</ol>
                          ),
                          li: ({ children }) => (
                            <li style={{
                              fontSize: '16px',
                              lineHeight: '1.8',
                              color: 'var(--text-secondary)',
                              marginBottom: '8px',
                            }}>{children}</li>
                          ),
                          code: ({ children }) => (
                            <code style={{
                              background: 'var(--bg-tertiary)',
                              padding: '2px 6px',
                              borderRadius: '4px',
                              fontSize: '14px',
                              fontFamily: 'Monaco, Menlo, monospace',
                              color: '#e83e8c',
                            }}>{children}</code>
                          ),
                          pre: ({ children }) => (
                            <pre style={{
                              background: 'var(--bg-tertiary)',
                              borderRadius: 'var(--radius-lg)',
                              padding: '20px',
                              margin: '16px 0',
                              overflowX: 'auto',
                              border: '1px solid var(--border-color)',
                              fontSize: '14px',
                              lineHeight: '1.6',
                              fontFamily: 'Monaco, Menlo, Ubuntu Mono, monospace',
                              color: 'var(--text-primary)',
                            }}>{children}</pre>
                          ),
                          blockquote: ({ children }) => (
                            <blockquote style={{
                              background: 'rgba(37, 99, 235, 0.05)',
                              borderLeft: '4px solid var(--primary-color)',
                              padding: '16px 20px',
                              margin: '16px 0',
                              color: 'var(--text-secondary)',
                            }}>{children}</blockquote>
                          ),
                          a: ({ href, children }) => (
                            <a href={href} style={{
                              color: 'var(--primary-color)',
                              textDecoration: 'none',
                              transition: 'color 0.2s',
                            }}>{children}</a>
                          ),
                          img: ({ src, alt }) => (
                            <img
                              src={src}
                              alt={alt}
                              style={{
                                maxWidth: '100%',
                                height: 'auto',
                                borderRadius: 'var(--radius-md)',
                                margin: '16px 0',
                              }}
                            />
                          ),
                          table: ({ children }) => (
                            <table style={{
                              width: '100%',
                              borderCollapse: 'collapse',
                              margin: '16px 0',
                              border: '1px solid var(--border-color)',
                            }}>{children}</table>
                          ),
                          thead: ({ children }) => (
                            <thead style={{
                              background: 'var(--bg-tertiary)',
                            }}>{children}</thead>
                          ),
                          th: ({ children }) => (
                            <th style={{
                              border: '1px solid var(--border-color)',
                              padding: '12px 16px',
                              textAlign: 'left',
                              fontWeight: '600',
                              color: 'var(--text-primary)',
                              fontSize: '14px',
                            }}>{children}</th>
                          ),
                          td: ({ children }) => (
                            <td style={{
                              border: '1px solid var(--border-color)',
                              padding: '12px 16px',
                              textAlign: 'left',
                              fontSize: '14px',
                            }}>{children}</td>
                          ),
                          hr: () => (
                            <hr style={{
                              border: 'none',
                              borderTop: '1px solid var(--border-color)',
                              margin: '24px 0',
                            }} />
                          ),
                          strong: ({ children }) => (
                            <strong style={{ fontWeight: '700', color: 'var(--text-primary)' }}>{children}</strong>
                          ),
                          em: ({ children }) => (
                            <em style={{ fontStyle: 'italic' }}>{children}</em>
                          ),
                          del: ({ children }) => (
                            <del style={{ textDecoration: 'line-through', color: 'var(--text-muted)' }}>{children}</del>
                          ),
                        }}
                      >
                        {content}
                      </ReactMarkdown>
                    </div>
                  ) : (
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      height: '100%',
                      minHeight: '400px',
                      background: 'var(--bg-primary)',
                      borderRadius: 'var(--radius-xl)',
                      border: '1px solid var(--border-color)',
                    }}>
                      <p style={{
                        color: 'var(--text-muted)',
                        textAlign: 'center',
                        fontSize: '15px',
                      }}>
                        Type content on the left, and the preview will display here in real time...
                      </p>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Right sidebar */}
            <div style={{
              width: '320px',
              flexShrink: 0,
              borderLeft: '1px solid var(--border-color)',
              background: 'var(--bg-secondary)',
              padding: '24px',
              boxSizing: 'border-box',
            }}>
              {/* Document settings */}
              <div style={{ marginBottom: '32px' }}>
                <div style={{
                  fontSize: '14px',
                  fontWeight: '700',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '16px',
                  letterSpacing: '0.5px',
                }}>
                  Document Settings
                </div>

                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    marginBottom: '8px',
                  }}>
                    Document Description
                  </label>
                  <textarea
                    value={summary}
                    onChange={(e) => setSummary(e.target.value)}
                    placeholder="Briefly describe the document content..."
                    style={{
                      width: '100%',
                      minHeight: '80px',
                      padding: '10px 14px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '14px',
                      fontFamily: 'inherit',
                      resize: 'vertical',
                      outline: 'none',
                      transition: 'all 0.2s',
                    }}
                    onFocus={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                      e.currentTarget.style.boxShadow = '0 0 0 3px rgba(37, 99, 235, 0.1)';
                    }}
                    onBlur={(e) => {
                      e.currentTarget.style.borderColor = 'var(--border-color)';
                      e.currentTarget.style.boxShadow = 'none';
                    }}
                  />
                </div>

                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    marginBottom: '8px',
                  }}>
                    Tags
                  </label>
                  <div style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: '8px',
                    padding: '8px',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--bg-primary)',
                    minHeight: '40px',
                  }}>
                    {tags.map((tag, index) => (
                      <span
                        key={index}
                        style={{
                          padding: '4px 12px',
                          background: 'var(--primary-color)',
                          color: 'white',
                          borderRadius: 'var(--radius-lg)',
                          fontSize: '12px',
                          fontWeight: '600',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '6px',
                        }}
                      >
                        {tag}
                        <span
                          onClick={() => handleRemoveTag(index)}
                          style={{
                            cursor: 'pointer',
                            opacity: 0.7,
                            transition: 'opacity 0.2s',
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.opacity = '1';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.opacity = '0.7';
                          }}
                        >
                          ×
                        </span>
                      </span>
                    ))}
                    <input
                      type="text"
                      value={inputTag}
                      onChange={(e) => setInputTag(e.target.value)}
                      onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          handleAddTag();
                        }
                      }}
                      placeholder="Type a tag and press Enter to add..."
                      style={{
                        flex: 1,
                        minWidth: '100px',
                        border: 'none',
                        outline: 'none',
                        fontSize: '14px',
                        background: 'transparent',
                      }}
                    />
                  </div>
                </div>

                <div style={{ marginBottom: '20px' }}>
                  <label style={{
                    display: 'block',
                    fontSize: '14px',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    marginBottom: '8px',
                  }}>
                    Author Information
                  </label>
                  <input
                    type="text"
                    value={author}
                    onChange={(e) => setAuthor(e.target.value)}
                    placeholder="Author name"
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '14px',
                      fontFamily: 'inherit',
                      outline: 'none',
                      transition: 'all 0.2s',
                    }}
                    onFocus={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                      e.currentTarget.style.boxShadow = '0 0 0 3px rgba(37, 99, 235, 0.1)';
                    }}
                    onBlur={(e) => {
                      e.currentTarget.style.borderColor = 'var(--border-color)';
                      e.currentTarget.style.boxShadow = 'none';
                    }}
                  />
                </div>
              </div>

              {/* Submit options */}
              <div style={{ marginBottom: '32px' }}>
                <div style={{
                  fontSize: '14px',
                  fontWeight: '700',
                  textTransform: 'uppercase',
                  color: 'var(--text-muted)',
                  marginBottom: '16px',
                  letterSpacing: '0.5px',
                }}>
                  Publish Options
                </div>

                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 16px',
                  background: 'var(--bg-primary)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  marginBottom: '12px',
                }}>
                  <span style={{ fontSize: '14px', fontWeight: '600' }}>Allow Comments</span>
                  <div
                    onClick={() => setAllowComments(!allowComments)}
                    style={{
                      position: 'relative',
                      width: '48px',
                      height: '24px',
                      background: allowComments ? 'var(--primary-color)' : 'var(--border-color)',
                      borderRadius: '12px',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                    }}
                  >
                    <div style={{
                      position: 'absolute',
                      width: '20px',
                      height: '20px',
                      background: 'white',
                      borderRadius: '50%',
                      top: '2px',
                      left: allowComments ? '26px' : '2px',
                      transition: 'all 0.2s',
                    }} />
                  </div>
                </div>

                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 16px',
                  background: 'var(--bg-primary)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  marginBottom: '12px',
                }}>
                  <span style={{ fontSize: '14px', fontWeight: '600' }}>Allow Editing</span>
                  <div
                    onClick={() => setAllowEdit(!allowEdit)}
                    style={{
                      position: 'relative',
                      width: '48px',
                      height: '24px',
                      background: allowEdit ? 'var(--primary-color)' : 'var(--border-color)',
                      borderRadius: '12px',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                    }}
                  >
                    <div style={{
                      position: 'absolute',
                      width: '20px',
                      height: '20px',
                      background: 'white',
                      borderRadius: '50%',
                      top: '2px',
                      left: allowEdit ? '26px' : '2px',
                      transition: 'all 0.2s',
                    }} />
                  </div>
                </div>

                <div style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 16px',
                  background: 'var(--bg-primary)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  marginBottom: '16px',
                }}>
                  <span style={{ fontSize: '14px', fontWeight: '600' }}>AI Indexing</span>
                  <div
                    onClick={() => setAiIndex(!aiIndex)}
                    style={{
                      position: 'relative',
                      width: '48px',
                      height: '24px',
                      background: aiIndex ? 'var(--primary-color)' : 'var(--border-color)',
                      borderRadius: '12px',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                    }}
                  >
                    <div style={{
                      position: 'absolute',
                      width: '20px',
                      height: '20px',
                      background: 'white',
                      borderRadius: '50%',
                      top: '2px',
                      left: aiIndex ? '26px' : '2px',
                      transition: 'all 0.2s',
                    }} />
                  </div>
                </div>

                <div style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '12px',
                }}>
                  <div
                    onClick={() => setSaveOption('submit_review')}
                    style={{
                      padding: '16px',
                      background: 'var(--bg-primary)',
                      border: saveOption === 'submit_review' ? '2px solid var(--primary-color)' : '2px solid var(--border-color)',
                      borderRadius: 'var(--radius-lg)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      textAlign: 'center',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = saveOption === 'submit_review' ? 'var(--primary-color)' : 'var(--border-color)';
                    }}
                  >
                    <div style={{
                      width: '32px',
                      height: '32px',
                      margin: '0 auto 8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}>
                      <svg style={{ color: 'var(--success-color)' }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M4 11a9 9 0 0 1 9 9"/>
                        <path d="M4 4a16 16 0 0 1 16 16"/>
                        <circle cx="5" cy="19" r="1"/>
                      </svg>
                    </div>
                    <div style={{
                      fontSize: '14px',
                      fontWeight: '600',
                      color: 'var(--text-primary)',
                      marginBottom: '4px',
                    }}>
                      {isDirectPublish ? 'Publish Directly' : 'Submit for Review'}
                    </div>
                    <div style={{
                      fontSize: '12px',
                      color: 'var(--text-muted)',
                    }}>
                      {isDirectPublish ? 'The document will be published directly without review' : 'It will be published after being reviewed by a reviewer'}
                    </div>
                  </div>
                  <div
                    onClick={() => setSaveOption('draft')}
                    style={{
                      padding: '16px',
                      background: 'var(--bg-primary)',
                      border: saveOption === 'draft' ? '2px solid var(--primary-color)' : '2px solid var(--border-color)',
                      borderRadius: 'var(--radius-lg)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      textAlign: 'center',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = saveOption === 'draft' ? 'var(--primary-color)' : 'var(--border-color)';
                    }}
                  >
                    <div style={{
                      width: '32px',
                      height: '32px',
                      margin: '0 auto 8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}>
                      <svg style={{ color: 'var(--warning-color)' }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </div>
                    <div style={{
                      fontSize: '14px',
                      fontWeight: '600',
                      color: 'var(--text-primary)',
                      marginBottom: '4px',
                    }}>
                      Save Draft
                    </div>
                    <div style={{
                      fontSize: '12px',
                      color: 'var(--text-muted)',
                    }}>
                      Continue editing later
                    </div>
                  </div>
                </div>
              </div>

              {/* AI writing assistant */}
              {enableAIWriting && (
              <div style={{
                background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.05), rgba(139, 92, 246, 0.05))',
                border: '1px solid rgba(37, 99, 235, 0.2)',
                borderRadius: 'var(--radius-lg)',
                padding: '16px',
              }}>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: '14px',
                  fontWeight: '600',
                  color: 'var(--text-primary)',
                  marginBottom: '12px',
                }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 2a10 10 0 1 0 10 10H12V2z"/>
                    <path d="M12 12 2.1 12a10 10 0 0 0 10 10h-10v-20z"/>
                    <path d="M12 12 12 21.9a10 10 0 0 0 10-10h-10v10z"/>
                  </svg>
                  AI Writing Assistant
                </div>
                <div style={{
                  fontSize: '13px',
                  color: 'var(--text-secondary)',
                  lineHeight: '1.6',
                }}>
                  An intelligent writing assistant powered by Claude 3.5 Opus, helping you edit and refine professional documents.
                </div>
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                  marginTop: '12px',
                }}>
                  <button
                    onClick={aiGenerateOutline}
                    style={{
                      padding: '8px 12px',
                      background: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '13px',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      textAlign: 'left',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                      e.currentTarget.style.color = 'var(--primary-color)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = 'var(--border-color)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ verticalAlign: 'middle' }}>
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                        <line x1="16" y1="13" x2="8" y2="13"/>
                        <line x1="16" y1="17" x2="8" y2="17"/>
                        <polyline points="10 9 9 9 8 9"/>
                      </svg>
                      Generate Document Outline
                    </div>
                  </button>
                  <button
                    onClick={aiExpandContent}
                    style={{
                      padding: '8px 12px',
                      background: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '13px',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      textAlign: 'left',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                      e.currentTarget.style.color = 'var(--primary-color)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = 'var(--border-color)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ verticalAlign: 'middle' }}>
                        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4 7.8 7.8 0 0 1-2.4-4.2"/>
                        <path d="M9 9h.01"/>
                        <path d="M9 12h.01"/>
                        <path d="M9 15h.01"/>
                        <path d="M9 18h.01"/>
                        <path d="M12 15h.01"/>
                        <path d="M12 18h.01"/>
                        <path d="M15 15h.01"/>
                        <path d="M15 18h.01"/>
                      </svg>
                      Expand Content
                    </div>
                  </button>
                  <button
                    onClick={aiImproveWriting}
                    style={{
                      padding: '8px 12px',
                      background: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '13px',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      textAlign: 'left',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                      e.currentTarget.style.color = 'var(--primary-color)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = 'var(--border-color)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ verticalAlign: 'middle' }}>
                        <path d="M12 20h9"/>
                        <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
                      </svg>
                      Improve Wording
                    </div>
                  </button>
                  <button
                    onClick={aiAddExamples}
                    style={{
                      padding: '8px 12px',
                      background: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '13px',
                      color: 'var(--text-secondary)',
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      textAlign: 'left',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = 'var(--primary-color)';
                      e.currentTarget.style.color = 'var(--primary-color)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = 'var(--border-color)';
                      e.currentTarget.style.color = 'var(--text-secondary)';
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ verticalAlign: 'middle' }}>
                        <path d="M9 18h6"/>
                        <path d="M10 22h4"/>
                        <path d="M12 2a7 7 0 0 0-7 7c0 2 2 3 2 5h10c0-2 2-3 2-5a7 7 0 0 0-7-7z"/>
                      </svg>
                      Add Example
                    </div>
                  </button>
                </div>
              </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
    <DraftRecoveryDialog
      open={isRecoveryDialogOpen}
      draft={recoveryDraft}
      onAccept={handleAcceptRecovery}
      onDismiss={dismissRecovery}
    />
    </>
  );
};

export default EditDocumentPage;
