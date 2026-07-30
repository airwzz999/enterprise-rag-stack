import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Input,
  Space,
  Tag,
  Tooltip,
  Modal,
  Upload,
  Image,
  Row,
  Col,
  Statistic,
  Progress,
  Select,
  Popconfirm,
  Spin,
} from 'antd';
import { App } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import type { ColumnsType } from 'antd/es/table';
import {
  FileOutlined,
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
  DeleteOutlined,
  CloudUploadOutlined,
  FilePdfOutlined,
  FileImageOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  FileTextOutlined,
  FileMarkdownOutlined,
  VideoCameraOutlined,
  AudioOutlined,
  FileZipOutlined,
  EditOutlined,
  CopyOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  AppstoreOutlined,
  DatabaseOutlined,
  ClockCircleOutlined,
  CloudServerOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import { useFileManagementStore } from '@/stores/file-management.store';
import { useAppStore } from '@/stores';
import { fileManagementService, FileMetadata } from '@/services/file-management.service';
import mammoth from 'mammoth';
import * as XLSX from 'xlsx';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import { pdfjs, Document, Page } from 'react-pdf';
import 'react-pdf/dist/Page/TextLayer.css';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import './FileManagementPage.css';

// Configure the PDF.js worker
pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/en';

dayjs.extend(relativeTime);
dayjs.locale('en');

/**
 * Normalize Markdown content to prevent headings from being misparsed as code blocks
 * due to indentation introduced by pasting or file formatting.
 * Finds the minimum common indentation across all non-empty lines and strips it from each line.
 */
const normalizeMarkdown = (text: string): string => {
  if (!text) return text;
  const lines = text.split('\n');

  let minIndent = Infinity;
  for (const line of lines) {
    if (line.trim().length === 0) continue;
    const match = line.match(/^[ \t]*/);
    if (match) minIndent = Math.min(minIndent, match[0].length);
  }

  if (minIndent === Infinity || minIndent === 0) return text;

  return lines
    .map(line => {
      if (line.trim().length === 0) return line;
      return line.slice(Math.min(minIndent, line.length));
    })
    .join('\n');
};

export const FileManagementPage: React.FC = () => {
  const { message } = App.useApp();
  // Use the new state management
  const {
    files,
    statistics,
    isLoading,
    error,
    currentCategory,
    selectedFiles,
    loadFileList,
    loadStatistics,
    uploadFile,
    deleteFile,
    batchDeleteFiles,
    renameFile,
    updateFilePermission,
    copyFile,
    searchFiles,
    setSelectedFiles,
    setCurrentCategory,
    clearError,
  } = useFileManagementStore();

  const maxFileSize = useAppStore((s) => s.maxFileSize);
  const allowedFileTypes = useAppStore((s) => s.allowedFileTypes);

  // Local state
  const [uploadModalVisible, setUploadModalVisible] = useState(false);
  const [renameModalVisible, setRenameModalVisible] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const previewContentRef = React.useRef<HTMLDivElement>(null);

  // Pause all media playback when closing the preview
  const closePreview = () => {
    // Pause all video / audio elements within the preview container
    if (previewContentRef.current) {
      previewContentRef.current.querySelectorAll('video, audio').forEach((el) => {
        (el as HTMLMediaElement).pause();
        (el as HTMLMediaElement).removeAttribute('src');
      });
    }
    setMediaLoading(false);
    // Reset document preview state
    setTxtContent(null); setTxtLoading(false); setTxtError(null);
    setMdContent(null); setMdLoading(false); setMdError(null);
    setDocxHtml(null); setDocxLoading(false); setDocxError(null);
    setXlsxWb(null); setXlsxLoading(false); setXlsxError(null);
    setPptSlideImages(null); setPptCurrentSlide(0); setPptLoading(false); setPptError(null);
    setPdfNumPages(null); setPdfScale(1.2); setPdfLoadError(null);
    setPreviewVisible(false);
    setCurrentFile(null);
  };
  const [currentFile, setCurrentFile] = useState<FileMetadata | null>(null);
  const [newFileName, setNewFileName] = useState('');
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([]);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [mediaLoading, setMediaLoading] = useState(false);

  // Document preview content state
  const [txtContent, setTxtContent] = useState<string | null>(null);
  const [txtLoading, setTxtLoading] = useState(false);
  const [txtError, setTxtError] = useState<string | null>(null);
  const [mdContent, setMdContent] = useState<string | null>(null);
  const [mdLoading, setMdLoading] = useState(false);
  const [mdError, setMdError] = useState<string | null>(null);
  const [docxHtml, setDocxHtml] = useState<string | null>(null);
  const [docxLoading, setDocxLoading] = useState(false);
  const [docxError, setDocxError] = useState<string | null>(null);
  const [xlsxWb, setXlsxWb] = useState<XLSX.WorkBook | null>(null);
  const [xlsxLoading, setXlsxLoading] = useState(false);
  const [xlsxError, setXlsxError] = useState<string | null>(null);

  // PPT preview state
  const [pptSlideImages, setPptSlideImages] = useState<string[] | null>(null);
  const [pptCurrentSlide, setPptCurrentSlide] = useState(0);
  const [pptLoading, setPptLoading] = useState(false);
  const [pptError, setPptError] = useState<string | null>(null);
  const pptMainRef = React.useRef<HTMLDivElement>(null);

  // PDF preview state
  const [pdfNumPages, setPdfNumPages] = useState<number | null>(null);
  const [pdfScale, setPdfScale] = useState(1.2);
  const [pdfLoadError, setPdfLoadError] = useState<string | null>(null);

  // File category options
  const categoryOptions = [
    { label: 'All Files', value: 'all' },
    { label: 'Images', value: 'image' },
    { label: 'Documents', value: 'document' },
    { label: 'Videos', value: 'video' },
    { label: 'Audio', value: 'audio' },
    { label: 'Archives', value: 'archive' },
    { label: 'Other', value: 'other' },
  ];

  // Load initial data (batched to avoid duplicate requests)
  useEffect(() => {
    const initFileManagement = async () => {
      try {
        // Load data in parallel, issuing only a single request
        await Promise.all([
          loadFileList(),
          loadStatistics()
        ]);
      } catch (error) {
        console.error('Failed to initialize file management data:', error);
      }
    };

    initFileManagement();
  }, []); // Run only once on mount

  // Error handling
  useEffect(() => {
    if (error) {
      message.error(error);
      clearError();
    }
  }, [error]);

  // Document preview: fetch file content asynchronously when currentFile changes
  useEffect(() => {
    if (!currentFile || !isDocumentPreviewable(currentFile)) return;

    const streamUrl = fileManagementService.getMediaStreamUrl(currentFile.id);
    const ext = currentFile.fileExtension?.toLowerCase();

    // PDF doesn't need to be fetched here (rendered via iframe)
    if (ext === 'pdf') return;

    let cancelled = false;

    const fetchContent = async () => {
      if (ext === 'txt') {
        setTxtError(null); setTxtLoading(true);
        try {
          const res = await fetch(streamUrl);
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const text = await res.text();
          if (!cancelled) { setTxtContent(text); setTxtLoading(false); }
        } catch (err: any) {
          if (!cancelled) { setTxtError(err.message); setTxtLoading(false); }
        }
      } else if (ext === 'md') {
        setMdError(null); setMdLoading(true);
        try {
          const res = await fetch(streamUrl);
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const text = await res.text();
          if (!cancelled) { setMdContent(text); setMdLoading(false); }
        } catch (err: any) {
          if (!cancelled) { setMdError(err.message); setMdLoading(false); }
        }
      } else if (ext === 'docx') {
        setDocxError(null); setDocxLoading(true);
        try {
          const res = await fetch(streamUrl);
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const buf = await res.arrayBuffer();
          const result = await mammoth.convertToHtml({ arrayBuffer: buf }, {
            styleMap: [
              "p[style-name='Heading 1'] => h1:fresh",
              "p[style-name='Heading 2'] => h2:fresh",
              "p[style-name='Heading 3'] => h3:fresh",
              "p[style-name='Title'] => h1.title:fresh",
              "p[style-name='Subtitle'] => h2.subtitle:fresh",
              "r[style-name='Strong'] => strong",
              "r[style-name='Emphasis'] => em",
            ],
            convertImage: mammoth.images.imgElement((image: any) =>
              image.read().then((imageBuffer: ArrayBuffer) => ({
                src: `data:${image.contentType};base64,${btoa(String.fromCharCode(...new Uint8Array(imageBuffer)))}`,
              }))
            ),
          });
          if (!cancelled) { setDocxHtml(result.value); setDocxLoading(false); }
        } catch (err: any) {
          if (!cancelled) { setDocxError(err.message); setDocxLoading(false); }
        }
      } else if (ext === 'xlsx' || ext === 'xls') {
        setXlsxError(null); setXlsxLoading(true);
        try {
          const res = await fetch(streamUrl);
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const buf = await res.arrayBuffer();
          const wb = XLSX.read(buf, { type: 'array', cellStyles: true, cellNF: true, cellDates: true });
          if (!cancelled) { setXlsxWb(wb); setXlsxLoading(false); }
        } catch (err: any) {
          if (!cancelled) { setXlsxError(err.message); setXlsxLoading(false); }
        }
      } else if (ext === 'pptx' || ext === 'ppt') {
        setPptError(null); setPptLoading(true);
        try {
          const images = await fileManagementService.getPptxSlideImages(currentFile.id);
          if (!cancelled) { setPptSlideImages(images); setPptCurrentSlide(0); setPptLoading(false); }
        } catch (err: any) {
          if (!cancelled) { setPptError(err.message || 'Failed to load'); setPptLoading(false); }
        }
      }
    };

    fetchContent();
    return () => { cancelled = true; };
  }, [currentFile]);

  // PPT image auto-fit: measure the container width with JS and compute exact pixel dimensions for the img style, completely avoiding CSS layout pitfalls
  React.useLayoutEffect(() => {
    const container = pptMainRef.current;
    if (!container || !pptSlideImages) return;
    const applySize = () => {
      const img = container.querySelector<HTMLImageElement>('img');
      if (!img) return;
      const pad = 16 * 2; // .ppt-viewer-main padding
      const maxW = container.clientWidth - pad;
      if (maxW <= 0) return;
      const w = Math.round(maxW);
      const h = Math.round(maxW * 9 / 16);
      img.style.width = w + 'px';
      img.style.height = h + 'px';
      img.style.display = 'block';
      img.style.borderRadius = '4px';
      img.style.boxShadow = '0 2px 8px rgba(0,0,0,0.1)';
      img.style.background = '#fff';
    };
    // Wait a frame for DOM layout to complete
    requestAnimationFrame(() => {
      applySize();
      // Run a second time to ensure correctness after the image has loaded
      requestAnimationFrame(applySize);
    });
    const ro = new ResizeObserver(() => requestAnimationFrame(applySize));
    ro.observe(container);
    return () => ro.disconnect();
  }, [pptCurrentSlide, pptSlideImages]);

  // ========== Document preview utility functions ==========

  // XLSX → styled HTML table
  const xlsxToStyledHtml = (wb: XLSX.WorkBook): string => {
    const sheetName = wb.SheetNames[0];
    const ws = wb.Sheets[sheetName];
    const ref = ws['!ref'];
    if (!ref) return '<p>Empty table</p>';

    const range = XLSX.utils.decode_range(ref);
    const merges: XLSX.Range[] = ws['!merges'] || [];
    // Build a fast lookup table for merged cells
    const mergeMap = new Map<string, XLSX.Range>();
    merges.forEach(m => {
      for (let R = m.s.r; R <= m.e.r; R++) {
        for (let C = m.s.c; C <= m.e.c; C++) {
          mergeMap.set(XLSX.utils.encode_cell({ r: R, c: C }), m);
        }
      }
    });

    const escapeHtml = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

    // Compute column widths
    const colWidths: number[] = [];
    if (ws['!cols']) {
      ws['!cols'].forEach((col: any, i: number) => {
        colWidths[i] = col.wch ? Math.min(col.wch * 7, 300) : 80;
      });
    }

    let html = '<table style="border-collapse:collapse;font-size:13px;">';
    // Column width styles
    if (colWidths.length > 0) {
      html += '<colgroup>';
      colWidths.forEach(w => { html += `<col style="width:${w}px;">`; });
      html += '</colgroup>';
    }

    for (let R = range.s.r; R <= range.e.r; R++) {
      html += '<tr>';
      for (let C = range.s.c; C <= range.e.c; C++) {
        const addr = XLSX.utils.encode_cell({ r: R, c: C });
        const cell: any = ws[addr];
        const merge = mergeMap.get(addr);

        // Skip non-origin cells within a merged range
        if (merge && (merge.s.r !== R || merge.s.c !== C)) continue;

        // Collect styles
        const css: string[] = [];
        css.push('border:1px solid #d1d5db;padding:6px 10px;');
        if (R === 0) css.push('font-weight:600;');

        // Extract fill color from cell.s
        if (cell?.s?.patternType === 'solid' && cell.s.fgColor?.rgb) {
          css.push(`background-color:#${cell.s.fgColor.rgb};`);
        }

        // Set alignment based on the value type
        if (cell?.t === 'n') css.push('text-align:right;');

        // Build rowspan/colspan
        const attrs: string[] = [];
        if (merge) {
          if (merge.e.r - merge.s.r + 1 > 1) attrs.push(`rowspan="${merge.e.r - merge.s.r + 1}"`);
          if (merge.e.c - merge.s.c + 1 > 1) attrs.push(`colspan="${merge.e.c - merge.s.c + 1}"`);
        }
        attrs.push(`style="${css.join(' ')}"`);

        // Get the cell's display content (prefer rich text HTML, fall back to formatted text)
        let content = '';
        if (cell) {
          if (cell.h) {
            content = cell.h; // rich text HTML
          } else {
            content = escapeHtml(cell.w ?? cell.v?.toString() ?? '');
          }
        }

        html += `<td ${attrs.join(' ')}>${content}</td>`;
      }
      html += '</tr>';
    }
    html += '</table>';
    return html;
  };

  // Determine whether the file has previewable document content (category=document or extension fallback, for backward compatibility with legacy data)
  const DOCUMENT_EXTENSIONS = new Set(['pdf', 'md', 'markdown', 'txt', 'docx', 'xlsx', 'xls', 'pptx', 'ppt', 'doc']);
  const isDocumentPreviewable = (file: FileMetadata): boolean => {
    if (file.fileCategory === 'document') return true;
    // Extension fallback: handles files uploaded before the backend category fix (fileCategory may be "other")
    return DOCUMENT_EXTENSIONS.has(file.fileExtension?.toLowerCase() || '');
  };

  // Get the file icon
  const getFileIcon = (file: FileMetadata) => {
    const iconStyle = { fontSize: '24px', color: '#1890ff' };

    switch (file.fileCategory) {
      case 'image':
        return <FileImageOutlined style={{ ...iconStyle, color: '#52c41a' }} />;
      case 'document':
        return getDocumentIcon(file.fileExtension?.toLowerCase() || '', iconStyle);
      case 'video':
        return <VideoCameraOutlined style={{ ...iconStyle, color: '#722ed1' }} />;
      case 'audio':
        return <AudioOutlined style={{ ...iconStyle, color: '#fa8c16' }} />;
      case 'archive':
        return <FileZipOutlined style={{ ...iconStyle, color: '#faad14' }} />;
      default:
        // Extension fallback: recognizable document types under the "other" category still get a dedicated icon
        if (DOCUMENT_EXTENSIONS.has(file.fileExtension?.toLowerCase() || '')) {
          return getDocumentIcon(file.fileExtension?.toLowerCase() || '', iconStyle);
        }
        return <FileOutlined style={iconStyle} />;
    }
  };

  // Document type icon (refined by file extension)
  const getDocumentIcon = (ext: string, iconStyle: React.CSSProperties) => {
    switch (ext) {
      case 'pdf':
        return <FilePdfOutlined style={{ ...iconStyle, color: '#ff4d4f' }} />;
      case 'doc':
      case 'docx':
        return <FileWordOutlined style={{ ...iconStyle, color: '#2b579a' }} />;
      case 'xls':
      case 'xlsx':
        return <FileExcelOutlined style={{ ...iconStyle, color: '#217346' }} />;
      case 'ppt':
      case 'pptx':
        return <FilePptOutlined style={{ ...iconStyle, color: '#d24726' }} />;
      case 'md':
      case 'markdown':
        return <FileMarkdownOutlined style={{ ...iconStyle, color: '#6366f1' }} />;
      case 'txt':
        return <FileTextOutlined style={{ ...iconStyle, color: '#64748b' }} />;
      default:
        return <FileOutlined style={iconStyle} />;
    }
  };

  // Get the file type tag color
  const getFileTypeColor = (category: string) => {
    const categoryMap: Record<string, string> = {
      image: 'green',
      document: 'blue',
      video: 'purple',
      audio: 'orange',
      archive: 'gold',
      other: 'default',
    };
    return categoryMap[category] || 'default';
  };

  // Handle file upload
  const handleUpload = async (file: File) => {
    // Client-side file type validation
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    const allowedExts = allowedFileTypes.split(',').map(t => {
      const trimmed = t.trim().toLowerCase();
      return trimmed.startsWith('.') ? trimmed : '.' + trimmed;
    });
    if (!allowedExts.includes(ext)) {
      message.error(`Unsupported file type: ${ext}. Allowed types: ${allowedFileTypes}`);
      return false;
    }
    // Client-side file size validation
    if (file.size > maxFileSize) {
      const maxMB = Math.round(maxFileSize / 1048576 * 10) / 10;
      message.error(`File size cannot exceed ${maxMB}MB`);
      return false;
    }

    try {
      setUploadProgress(0);

      // Simulate upload progress
      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 200);

      await uploadFile(file, false);

      clearInterval(progressInterval);
      setUploadProgress(100);

      message.success('File uploaded successfully');
      setUploadModalVisible(false);
      setUploadFileList([]);
      setUploadProgress(0);
    } catch (error: any) {
      message.error('Failed to upload file: ' + (error.message || 'Unknown error'));
      setUploadProgress(0);
    }

    return false; // Prevent the default upload behavior
  };

  // Handle file deletion
  const handleDelete = async (fileId: number) => {
    try {
      await deleteFile(fileId);
      message.success('File deleted successfully');
    } catch (error: any) {
      message.error('Failed to delete file: ' + (error.message || 'Unknown error'));
    }
  };

  // Handle batch deletion
  const handleBatchDelete = async () => {
    if (selectedFiles.length === 0) {
      message.warning('Please select the files to delete first');
      return;
    }

    try {
      const count = await batchDeleteFiles(selectedFiles);
      message.success(`Successfully deleted ${count} file(s)`);
    } catch (error: any) {
      message.error('Batch deletion failed: ' + (error.message || 'Unknown error'));
    }
  };

  // Handle file rename
  const handleRename = async () => {
    if (!currentFile || !newFileName.trim()) {
      message.warning('Please enter a new file name');
      return;
    }

    try {
      await renameFile(currentFile.id, newFileName.trim());
      message.success('File renamed successfully');
      setRenameModalVisible(false);
      setNewFileName('');
      setCurrentFile(null);
    } catch (error: any) {
      message.error('Failed to rename file: ' + (error.message || 'Unknown error'));
    }
  };

  // Handle permission update
  const handlePermissionChange = async (fileId: number, isPublic: boolean) => {
    try {
      await updateFilePermission(fileId, isPublic);
      message.success(isPublic ? 'File set to public' : 'File set to private');
    } catch (error: any) {
      message.error('Failed to update permission: ' + (error.message || 'Unknown error'));
    }
  };

  // Handle file copy
  const handleCopy = async (fileId: number) => {
    try {
      await copyFile(fileId);
      message.success('File copied successfully');
    } catch (error: any) {
      message.error('Failed to copy file: ' + (error.message || 'Unknown error'));
    }
  };

  // Handle file download
  const handleDownload = async (file: FileMetadata) => {
    try {
      await fileManagementService.downloadFile(file.id);

      // Create the download link
      const link = document.createElement('a');
      link.href = file.accessUrl;
      link.download = file.originalFileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      message.success('File downloaded successfully');
    } catch (error: any) {
      message.error('Failed to download file: ' + (error.message || 'Unknown error'));
    }
  };

  // Handle search
  const handleSearch = (value: string) => {
    if (value.trim()) {
      searchFiles(value.trim());
    } else {
      loadFileList(currentCategory);
    }
  };

  // Handle category switch
  const handleCategoryChange = (category: string) => {
    setCurrentCategory(category);
    loadFileList(category);
  };

  // Open the rename dialog
  const openRenameModal = (file: FileMetadata) => {
    setCurrentFile(file);
    setNewFileName(file.fileName);
    setRenameModalVisible(true);
  };

  // Open the preview dialog
  const openPreviewModal = (file: FileMetadata) => {
    console.log('🖼️ [openPreviewModal] Opening preview:');
    console.log('  - File ID:', file.id);
    console.log('  - File name:', file.fileName);
    console.log('  - Category:', file.fileCategory);
    console.log('  - Type:', file.contentType);
    console.log('  - Transcode status:', file.transcodeStatus);
    if (file.fileCategory === 'audio' || file.fileCategory === 'video') {
      setMediaLoading(true);
    }
    setCurrentFile(file);
    setPreviewVisible(true);
  };

  // Refresh
  const handleRefresh = () => {
    loadFileList(currentCategory);
    loadStatistics();
  };

  // Table column definitions
  const columns: ColumnsType<FileMetadata> = [
    {
      title: 'File Name',
      dataIndex: 'fileName',
      key: 'fileName',
      width: '30%',
      render: (fileName: string, record: FileMetadata) => {
        const isPlayable = record.fileCategory === 'video' || record.fileCategory === 'audio'
          || record.fileCategory === 'image' || isDocumentPreviewable(record);
        return (
          <Space>
            {getFileIcon(record)}
            <Tooltip title={isPlayable ? 'Click to view' : fileName}>
              <span
                className="cursor-pointer hover:text-blue-500"
                style={{ color: isPlayable ? '#1890ff' : undefined }}
                onClick={() => {
                  if (isPlayable) {
                    openPreviewModal(record);
                  }
                }}
              >
                {fileName || 'Untitled file'}
              </span>
            </Tooltip>
          </Space>
        );
      },
    },
    {
      title: 'Category',
      dataIndex: 'fileCategory',
      key: 'fileCategory',
      width: '10%',
      render: (category: string) => (
        <Tag color={getFileTypeColor(category)}>
          {category === 'image' ? 'Image' :
           category === 'document' ? 'Document' :
           category === 'video' ? 'Video' :
           category === 'audio' ? 'Audio' :
           category === 'archive' ? 'Archive' : 'Other'}
        </Tag>
      ),
    },
    {
      title: 'Size',
      dataIndex: 'fileSizeReadable',
      key: 'fileSize',
      width: '10%',
      render: (size: string) => size || '0 B',
    },
    {
      title: 'Permission',
      dataIndex: 'isPublic',
      key: 'isPublic',
      width: '8%',
      render: (isPublic: boolean, record: FileMetadata) => (
        <Tooltip title={isPublic ? 'Public file' : 'Private file'}>
          <Button
            type="text"
            icon={isPublic ? <EyeOutlined /> : <EyeInvisibleOutlined />}
            onClick={() => handlePermissionChange(record.id, !isPublic)}
            style={{ color: isPublic ? '#52c41a' : '#8c8c8c' }}
          />
        </Tooltip>
      ),
    },
    {
      title: 'Downloads',
      dataIndex: 'downloadCount',
      key: 'downloadCount',
      width: '10%',
      render: (count: number) => count || 0,
    },
    {
      title: 'Uploaded',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: '15%',
      render: (date: string) => (
        <Tooltip title={date}>
          <span>{date ? dayjs(date).fromNow() : '-'}</span>
        </Tooltip>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: '20%',
      render: (_: any, record: FileMetadata) => (
        <Space size="small">
          <Tooltip title="Download">
            <Button
              type="text"
              icon={<DownloadOutlined />}
              onClick={() => handleDownload(record)}
              size="small"
            />
          </Tooltip>
          <Tooltip title="Rename">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => openRenameModal(record)}
              size="small"
            />
          </Tooltip>
          <Tooltip title="Copy">
            <Button
              type="text"
              icon={<CopyOutlined />}
              onClick={() => handleCopy(record.id)}
              size="small"
            />
          </Tooltip>
          {record.fileCategory === 'image' && (
            <Tooltip title="Preview">
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => openPreviewModal(record)}
                size="small"
              />
            </Tooltip>
          )}
          {(record.fileCategory === 'video' || record.fileCategory === 'audio') && (
            <Tooltip title={record.fileCategory === 'audio' ? 'Play' : (record.transcodeStatus === 'DONE' ? 'HLS Playback' : 'Play (Original File)')}>
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => openPreviewModal(record)}
                size="small"
              />
            </Tooltip>
          )}
          {isDocumentPreviewable(record) && (
            <Tooltip title="Preview">
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => openPreviewModal(record)}
                size="small"
              />
            </Tooltip>
          )}
          <Popconfirm
            title="Are you sure you want to delete this file?"
            onConfirm={() => handleDelete(record.id)}
            okText="OK"
            cancelText="Cancel"
          >
            <Tooltip title="Delete">
              <Button
                type="text"
                icon={<DeleteOutlined />}
                danger
                size="small"
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  // Render preview content
  const renderPreviewContent = (file: FileMetadata) => {
    // Image preview
    if (file.fileCategory === 'image') {
      return (
        <div style={{ textAlign: 'center' }}>
          <Image
            src={file.accessUrl}
            alt={file.fileName}
            style={{ maxWidth: '100%', maxHeight: '60vh' }}
            preview={false}
          />
        </div>
      );
    }

    // Video preview (uses a native HTML5 video element, supports HTTP Range streaming)
    if (file.fileCategory === 'video') {
      const videoUrl = fileManagementService.getMediaStreamUrl(file.id);

      return (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <div style={{ marginBottom: 16 }}>
            <VideoCameraOutlined style={{ fontSize: 64, color: '#1890ff' }} />
          </div>
          <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 16 }}>
            {file.fileName}
          </div>
          <div style={{ position: 'relative', minHeight: 54 }}>
            {mediaLoading && (
              <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'rgba(255,255,255,0.8)',
                borderRadius: 8,
                zIndex: 1,
              }}>
                <Spin tip="Loading..." />
              </div>
            )}
            <video
              src={videoUrl}
              controls
              controlsList="nodownload"
              style={{ width: '100%', maxHeight: '60vh', outline: 'none' }}
              onCanPlay={() => setMediaLoading(false)}
              onError={(e) => {
                setMediaLoading(false);
                const video = e.currentTarget;
                const error = video.error;
                console.error('❌ [Video] Playback error: code=', error?.code, 'msg=', error?.message);
              }}
            />
          </div>
          {file.duration && (
            <div style={{ marginTop: 12, color: '#64748b', fontSize: 13 }}>
              Duration: {Math.floor(file.duration / 60)}m {file.duration % 60}s
              {file.resolution && ` | Resolution: ${file.resolution}`}
              {file.bitrate && ` | Bitrate: ${file.bitrate} kbps`}
            </div>
          )}
          {file.transcodeStatus && file.transcodeStatus !== 'DONE' && (
            <div style={{ marginTop: 8 }}>
              <Tag color="processing">Transcoding</Tag>
              <span style={{ color: '#64748b', fontSize: 12, marginLeft: 8 }}>
                Currently playing the original file directly; HLS adaptive bitrate streaming will be available once transcoding completes
              </span>
            </div>
          )}
        </div>
      );
    }

    // Audio preview (uses a native HTML5 audio element)
    if (file.fileCategory === 'audio') {
      const audioUrl = fileManagementService.getMediaStreamUrl(file.id);

      return (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <div style={{ marginBottom: 16 }}>
            <AudioOutlined style={{ fontSize: 64, color: '#fa8c16' }} />
          </div>
          <div style={{ fontSize: 16, fontWeight: 500, marginBottom: 16 }}>
            {file.fileName}
          </div>
          <div style={{ position: 'relative', minHeight: 54 }}>
            {mediaLoading && (
              <div style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'rgba(255,255,255,0.8)',
                borderRadius: 8,
                zIndex: 1,
              }}>
                <Spin tip="Loading..." />
              </div>
            )}
            <audio
              src={audioUrl}
              controls
              controlsList="nodownload"
              style={{ width: '100%', outline: 'none' }}
              onCanPlay={() => setMediaLoading(false)}
              onError={(e) => {
                setMediaLoading(false);
                const audio = e.currentTarget;
                const error = audio.error;
                console.error('❌ [Audio] Playback error: code=', error?.code, 'msg=', error?.message);
              }}
            />
          </div>
          {file.duration && (
            <div style={{ marginTop: 12, color: '#64748b', fontSize: 13 }}>
              Duration: {Math.floor(file.duration / 60)}m {file.duration % 60}s
              {file.bitrate && ` | Bitrate: ${file.bitrate} kbps`}
            </div>
          )}
        </div>
      );
    }

    // Document preview (with extension fallback, compatible with legacy files categorized as "other")
    if (isDocumentPreviewable(file)) {
      const ext = file.fileExtension?.toLowerCase();

      // PDF: rendered with PDF.js, supports pagination, zoom, text selection, and the annotation layer
      if (ext === 'pdf') {
        return <PDFPreview file={file} />;
      }

      // Markdown
      if (ext === 'md' || ext === 'markdown') {
        if (mdLoading) {
          return <div style={{ textAlign: 'center', padding: 40 }}><Spin tip="Loading..." /></div>;
        }
        if (mdError) {
          return <PreviewError message={mdError} onDownload={() => handleDownload(file)} />;
        }
        return (
          <div className="markdown-preview" style={{ maxHeight: '70vh', overflow: 'auto', padding: 16 }}>
            <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>{normalizeMarkdown(mdContent || '')}</ReactMarkdown>
          </div>
        );
      }

      // Plain text
      if (ext === 'txt') {
        if (txtLoading) {
          return <div style={{ textAlign: 'center', padding: 40 }}><Spin tip="Loading..." /></div>;
        }
        if (txtError) {
          return <PreviewError message={txtError} onDownload={() => handleDownload(file)} />;
        }
        return (
          <pre className="txt-preview">{txtContent || ''}</pre>
        );
      }

      // Word (.docx)
      if (ext === 'docx') {
        if (docxLoading) {
          return <div style={{ textAlign: 'center', padding: 40 }}><Spin tip="Loading..." /></div>;
        }
        if (docxError) {
          return <PreviewError message={docxError} onDownload={() => handleDownload(file)} />;
        }
        return (
          <div
            className="docx-preview"
            style={{ maxHeight: '70vh', overflow: 'auto', padding: 16 }}
            dangerouslySetInnerHTML={{ __html: docxHtml || '' }}
          />
        );
      }

      // Excel (.xlsx, .xls)
      if (ext === 'xlsx' || ext === 'xls') {
        if (xlsxLoading) {
          return <div style={{ textAlign: 'center', padding: 40 }}><Spin tip="Loading..." /></div>;
        }
        if (xlsxError) {
          return <PreviewError message={xlsxError} onDownload={() => handleDownload(file)} />;
        }
        const html = xlsxWb ? xlsxToStyledHtml(xlsxWb) : '<p>Empty table</p>';
        return (
          <div className="xlsx-preview" style={{ maxHeight: '70vh', overflow: 'auto' }}
               dangerouslySetInnerHTML={{ __html: html }} />
        );
      }

      // PPT slide preview (rendered to images by Apache POI on the backend)
      if (ext === 'pptx' || ext === 'ppt') {
        if (pptLoading) {
          return (
            <div className="ppt-loading-skeleton">
              <div className="ppt-skeleton-toolbar">
                <FilePptOutlined style={{ fontSize: 16, color: '#d24726', marginRight: 8 }} />
                <span style={{ fontSize: 13, color: '#94a3b8' }}>Loading PPT...</span>
              </div>
              <div className="ppt-skeleton-body">
                <div className="ppt-skeleton-thumbnails">
                  {[1, 2, 3, 4, 5].map(i => (
                    <div key={i} className="ppt-skeleton-thumb" />
                  ))}
                </div>
                <div className="ppt-loading-spinner-wrap">
                  <Spin size="large" />
                  <div style={{ marginTop: 16, color: '#64748b', fontSize: 14 }}>
                    Rendering slides, please wait...
                  </div>
                </div>
              </div>
            </div>
          );
        }
        if (pptError) {
          return <PreviewError message={pptError} onDownload={() => handleDownload(file)} />;
        }
        if (!pptSlideImages || pptSlideImages.length === 0) {
          return (
            <div style={{ textAlign: 'center', padding: '40px 0' }}>
              <FilePptOutlined style={{ fontSize: 64, color: '#d24726' }} />
              <p style={{ marginTop: 16, color: '#64748b' }}>Unable to parse slide content</p>
              <Button type="primary" icon={<DownloadOutlined />} onClick={() => handleDownload(file)}>
                Download File
              </Button>
            </div>
          );
        }

        const currentImg = pptSlideImages[pptCurrentSlide] || '';
        const totalSlides = pptSlideImages.length;

        return (
          <div className="ppt-viewer">
            {/* Top toolbar */}
            <div className="ppt-viewer-toolbar">
              <div className="ppt-viewer-toolbar-left">
                <FilePptOutlined style={{ fontSize: 16, color: '#d24726' }} />
                <span className="ppt-viewer-slide-count">
                  Slide {pptCurrentSlide + 1} / {totalSlides}
                </span>
              </div>
              <div className="ppt-viewer-toolbar-right">
                <Tooltip title="Previous">
                  <Button
                    type="text" size="small" icon={<ZoomOutOutlined style={{ transform: 'rotate(90deg)' }} />}
                    disabled={pptCurrentSlide <= 0}
                    onClick={() => setPptCurrentSlide(i => Math.max(0, i - 1))}
                  />
                </Tooltip>
                <Tooltip title="Next">
                  <Button
                    type="text" size="small" icon={<ZoomOutOutlined style={{ transform: 'rotate(-90deg)' }} />}
                    disabled={pptCurrentSlide >= totalSlides - 1}
                    onClick={() => setPptCurrentSlide(i => Math.min(totalSlides - 1, i + 1))}
                  />
                </Tooltip>
                <Button
                  type="primary" size="small" ghost icon={<DownloadOutlined />}
                  onClick={() => handleDownload(file)}
                >
                  Download
                </Button>
              </div>
            </div>

            {/* Body: thumbnails on the left, main image on the right */}
            <div className="ppt-viewer-body">
              {/* Left-side thumbnail list */}
              <div className="ppt-viewer-thumbnails">
                {pptSlideImages.map((img, idx) => (
                  <div
                    key={idx}
                    className={`ppt-viewer-thumb ${idx === pptCurrentSlide ? 'active' : ''}`}
                    onClick={() => setPptCurrentSlide(idx)}
                    title={`Slide ${idx + 1}`}
                  >
                    <img src={img} alt={`Slide ${idx + 1}`} />
                    <span className="ppt-viewer-thumb-num">{idx + 1}</span>
                  </div>
                ))}
              </div>

              {/* Right side main view: JS dynamically measures the container width then writes the img's pixel size */}
              <div className="ppt-viewer-main" ref={pptMainRef}>
                <img
                  src={currentImg}
                  alt={`Slide ${pptCurrentSlide + 1}`}
                  style={{ display: 'none' }}
                />
              </div>
            </div>

            {/* Bottom progress bar */}
            <div className="ppt-viewer-progress">
              <div
                className="ppt-viewer-progress-bar"
                style={{ width: `${((pptCurrentSlide + 1) / totalSlides) * 100}%` }}
              />
            </div>
          </div>
        );
      }

      // Legacy .doc format
      if (ext === 'doc') {
        return (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <FileWordOutlined style={{ fontSize: 64, color: '#2b579a' }} />
            <p style={{ marginTop: 16, color: '#64748b' }}>
              The legacy .doc format is not yet supported for online preview. Please use the .docx format.
            </p>
            <Button type="primary" icon={<DownloadOutlined />} onClick={() => handleDownload(file)}>
              Download File
            </Button>
          </div>
        );
      }

      // Fallback for other document types
      return (
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <FileOutlined style={{ fontSize: 64, color: '#8c8c8c' }} />
          <p style={{ marginTop: 16, color: '#64748b' }}>
            This file type is not yet supported for online preview
          </p>
          <Button type="primary" icon={<DownloadOutlined />} onClick={() => handleDownload(file)}>
            Download File
          </Button>
        </div>
      );
    }

    // Other file types
    return (
      <div style={{ textAlign: 'center', padding: '40px 0' }}>
        <FileOutlined style={{ fontSize: 64, color: '#8c8c8c' }} />
        <p style={{ marginTop: 16, color: '#64748b' }}>
          This file type is not yet supported for online preview
        </p>
        <Button
          type="primary"
          icon={<DownloadOutlined />}
          style={{ marginTop: 16 }}
          onClick={() => handleDownload(file)}
        >
          Download File
        </Button>
      </div>
    );
  };

  // Preview error component
  const PreviewError = ({ message: errMsg, onDownload }: { message: string; onDownload: () => void }) => (
    <div style={{ textAlign: 'center', padding: '40px 0' }}>
      <p style={{ color: '#ff4d4f' }}>Failed to load: {errMsg}</p>
      <Button style={{ marginRight: 8 }} onClick={() => {
        if (currentFile) openPreviewModal(currentFile);
      }}>Retry</Button>
      <Button type="primary" icon={<DownloadOutlined />} onClick={onDownload}>
        Download File
      </Button>
    </div>
  );

  // PDF preview component (professional rendering powered by PDF.js)
  const PDFPreview = ({ file }: { file: FileMetadata }) => {
    const pdfUrl = fileManagementService.getMediaStreamUrl(file.id);

    const onDocumentLoadSuccess = ({ numPages }: { numPages: number }) => {
      setPdfNumPages(numPages);
      setPdfLoadError(null);
    };

    const onDocumentLoadError = (err: Error) => {
      console.error('Failed to load PDF:', err);
      setPdfLoadError(err.message);
    };

    if (pdfLoadError) {
      return <PreviewError message={pdfLoadError} onDownload={() => handleDownload(file)} />;
    }

    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        {/* Toolbar */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8,
          padding: '8px 16px', background: '#f8fafc', borderRadius: 8,
          marginBottom: 16, width: '100%', flexWrap: 'wrap',
          border: '1px solid #e2e8f0',
        }}>
          <FilePdfOutlined style={{ fontSize: 16, color: '#ff4d4f' }} />
          <span style={{ fontSize: 13, color: '#334155', fontWeight: 500 }}>
            {pdfNumPages || '?'} pages total
          </span>
          <div style={{ width: 1, height: 20, background: '#d1d5db', margin: '0 4px' }} />
          <Tooltip title="Zoom out">
            <Button
              type="text" size="small" icon={<ZoomOutOutlined />}
              disabled={pdfScale <= 0.5}
              onClick={() => setPdfScale(s => Math.max(0.5, s - 0.2))}
            />
          </Tooltip>
          <span style={{ fontSize: 12, color: '#64748b', minWidth: 50, textAlign: 'center' }}>
            {Math.round(pdfScale * 100)}%
          </span>
          <Tooltip title="Zoom in">
            <Button
              type="text" size="small" icon={<ZoomInOutlined />}
              disabled={pdfScale >= 3.0}
              onClick={() => setPdfScale(s => Math.min(3.0, s + 0.2))}
            />
          </Tooltip>
          <div style={{ flex: 1 }} />
          <Button
            type="primary" size="small" ghost icon={<DownloadOutlined />}
            onClick={() => handleDownload(file)}
          >
            Download
          </Button>
        </div>

        {/* PDF document — continuous scroll displaying all pages */}
        <div className="pdf-preview-container" style={{
          maxHeight: '62vh', overflow: 'auto', borderRadius: 8,
          border: '1px solid #e2e8f0', background: '#f1f5f9',
          padding: 16, width: '100%', textAlign: 'center',
          minHeight: 300,
        }}>
          <Document
            key={pdfNumPages ?? 0}
            file={pdfUrl}
            onLoadSuccess={onDocumentLoadSuccess}
            onLoadError={onDocumentLoadError}
            loading={
              <div style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center',
                justifyContent: 'center', padding: 80, minHeight: 300,
              }}>
                <Spin size="large" />
                <div style={{ marginTop: 20, color: '#64748b', fontSize: 14 }}>
                  Loading and parsing PDF document...
                </div>
                <div style={{ marginTop: 8, color: '#94a3b8', fontSize: 12 }}>
                  This may take a few seconds for large files
                </div>
              </div>
            }
            error={
              <div style={{ textAlign: 'center', padding: 60 }}>
                <FilePdfOutlined style={{ fontSize: 48, color: '#ff4d4f' }} />
                <p style={{ marginTop: 16, color: '#ff4d4f', fontSize: 14 }}>Failed to load PDF, please retry</p>
              </div>
            }
          >
            {pdfNumPages != null && pdfNumPages > 0
              ? Array.from({ length: pdfNumPages }, (_, i) => (
                  <Page
                    key={`page_${i + 1}`}
                    pageNumber={i + 1}
                    scale={pdfScale}
                    renderTextLayer={true}
                    renderAnnotationLayer={true}
                    loading={
                      <div style={{
                        display: 'flex', flexDirection: 'column', alignItems: 'center',
                        justifyContent: 'center',
                        padding: 60,
                        margin: '0 auto 16px',
                        width: '100%',
                        maxWidth: 700,
                        minHeight: 200,
                        background: '#fff',
                        borderRadius: 4,
                        border: '1px solid #e2e8f0',
                        boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                      }}>
                        <Spin />
                        <div style={{ marginTop: 12, color: '#94a3b8', fontSize: 13 }}>
                          Rendering page {i + 1}...
                        </div>
                        {/* Loading progress bar animation */}
                        <div style={{
                          marginTop: 12, width: 140, height: 3,
                          background: 'linear-gradient(90deg, #e2e8f0 25%, #3b82f6 50%, #e2e8f0 75%)',
                          backgroundSize: '200% 100%',
                          borderRadius: 2,
                          animation: 'pdf-loading-bar 1.4s ease-in-out infinite',
                        }} />
                      </div>
                    }
                  />
                ))
              : null}
          </Document>
        </div>
      </div>
    );
  };

  // Row selection configuration
  const rowSelection = {
    selectedRowKeys: selectedFiles,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedFiles(newSelectedRowKeys as number[]);
    },
  };

  return (
    <div className="file-management-page" style={{ padding: '24px' }}>
      {/* Page header */}
      <div className="page-header" style={{ marginBottom: '24px' }}>
        <Row gutter={24}>
          <Col span={18}>
            <h1 style={{ fontSize: '28px', fontWeight: 700, color: '#0f172a', margin: 0 }}>
              <CloudServerOutlined style={{ marginRight: '12px', color: '#2563eb' }} />
              File Management Center
            </h1>
            <p style={{ color: '#64748b', marginTop: '8px', fontSize: '14px' }}>
              Manage all your uploaded files — preview, download, delete, and more
            </p>
          </Col>
          <Col span={6} style={{ textAlign: 'right' }}>
            <Button
              type="primary"
              icon={<CloudUploadOutlined />}
              size="large"
              onClick={() => {
                setUploadFileList([]);
                setUploadProgress(0);
                setUploadModalVisible(true);
              }}
            >
              Upload New File
            </Button>
          </Col>
        </Row>
      </div>

      {/* Statistics */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Total Files"
              value={statistics?.totalCount || 0}
              prefix={<AppstoreOutlined />}
              valueStyle={{ color: '#2563eb' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Total Storage"
              value={statistics?.totalSizeReadable || '0 B'}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#10b981' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Uploaded Today"
              value={statistics?.todayCount || 0}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#f59e0b' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Selected"
              value={selectedFiles.length}
              prefix={<SafetyOutlined />}
              valueStyle={{ color: selectedFiles.length > 0 ? '#ef4444' : '#8b5cf6' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Action bar */}
      <Card variant="borderless" style={{ marginBottom: '16px' }}>
        <Space wrap>
          <Select
            value={currentCategory}
            onChange={handleCategoryChange}
            style={{ width: 120 }}
            options={categoryOptions}
          />
          <Input.Search
            placeholder="Search file name"
            allowClear
            onSearch={handleSearch}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={isLoading}
          >
            Refresh
          </Button>
          {selectedFiles.length > 0 && (
            <Popconfirm
              title={`Are you sure you want to delete the selected ${selectedFiles.length} file(s)?`}
              onConfirm={handleBatchDelete}
              okText="OK"
              cancelText="Cancel"
            >
              <Button danger icon={<DeleteOutlined />}>
                Delete Selected ({selectedFiles.length})
              </Button>
            </Popconfirm>
          )}
        </Space>
      </Card>

      {/* File list */}
      <Card variant="borderless">
        <Table
          columns={columns}
          dataSource={files}
          rowKey="id"
          loading={isLoading}
          rowSelection={rowSelection}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `${total} files total`,
          }}
          onRow={(record) => ({
            onDoubleClick: () => {
            if (record.fileCategory === 'image' ||
                record.fileCategory === 'video' ||
                record.fileCategory === 'audio' ||
                isDocumentPreviewable(record)) {
              openPreviewModal(record);
            }
          },
          })}
        />
      </Card>

      {/* Upload file dialog */}
      <Modal
        title="Upload File"
        open={uploadModalVisible}
        onCancel={() => {
          setUploadModalVisible(false);
          setUploadFileList([]);
          setUploadProgress(0);
        }}
        footer={null}
        width={600}
      >
        <Upload.Dragger
          fileList={uploadFileList}
          onChange={({ fileList }) => setUploadFileList(fileList)}
          beforeUpload={handleUpload}
          multiple
          showUploadList={true}
          accept={allowedFileTypes.split(',').map(t => {
            const ext = t.trim().toLowerCase();
            // PDF needs the MIME type too for macOS Safari compatibility
            if (ext === 'pdf') return 'application/pdf,.pdf';
            return '.' + ext;
          }).join(',')}
        >
          <p className="ant-upload-drag-icon">
            <CloudUploadOutlined style={{ fontSize: 48 }} />
          </p>
          <p className="ant-upload-text">Click or drag a file to this area to upload</p>
          <p className="ant-upload-hint" style={{ wordBreak: 'break-word', lineHeight: 1.6 }}>
            Supports single or batch upload. Allowed file types: {allowedFileTypes.split(',').join(', ')}. Max file size: {Math.round(maxFileSize / 1048576 * 10) / 10}MB
          </p>
        </Upload.Dragger>

        {uploadProgress > 0 && uploadProgress < 100 && (
          <Progress percent={uploadProgress} status="active" />
        )}
      </Modal>

      {/* Rename dialog */}
      <Modal
        title="Rename File"
        open={renameModalVisible}
        onOk={handleRename}
        onCancel={() => {
          setRenameModalVisible(false);
          setNewFileName('');
          setCurrentFile(null);
        }}
      >
        <Input
          value={newFileName}
          onChange={(e) => setNewFileName(e.target.value)}
          placeholder="Enter a new file name"
          onPressEnter={handleRename}
          autoFocus
        />
      </Modal>

      {/* File preview dialog (image/video/audio) */}
      <Modal
        title={currentFile?.fileName}
        open={previewVisible}
        onCancel={closePreview}
        footer={[
          <Button key="close" onClick={closePreview}>
            Close
          </Button>,
          <Button
            key="download"
            type="primary"
            icon={<DownloadOutlined />}
            onClick={() => {
              if (currentFile) {
                handleDownload(currentFile);
              }
            }}
          >
            Download
          </Button>,
        ]}
        width={currentFile?.fileCategory === 'image' ? 800 : 900}
      >
        {currentFile && (
          <div ref={previewContentRef} style={{ maxWidth: '100%' }}>
            {renderPreviewContent(currentFile)}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default FileManagementPage;
