import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CloudUploadOutlined,
  FileTextOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  ReloadOutlined,
  InboxOutlined,
  InfoCircleOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  FileMarkdownOutlined,
  EyeOutlined,
  ClearOutlined,
  RocketOutlined,
  SafetyOutlined,
  ThunderboltOutlined,
  FolderOpenOutlined,
} from '@ant-design/icons';
import { App } from 'antd';
import { useAppStore } from '@/stores';
import { documentService } from '@/services';
import { formatFileSize } from '@/utils';

import './ImportDocumentPage.css';

interface FileItem {
  id: string;
  file: File;
  status: 'pending' | 'uploading' | 'success' | 'error';
  progress: number;
  error?: string;
  documentId?: string;
}

export const ImportDocumentPage: React.FC = () => {
  const { message } = App.useApp();
  const maxFileSize = useAppStore((s) => s.maxFileSize);
  const allowedFileTypes = useAppStore((s) => s.allowedFileTypes);
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [fileList, setFileList] = useState<FileItem[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [hasImported, setHasImported] = useState(false);


  const supportedFormats = [
    {
      name: 'PDF Document',
      ext: '.pdf',
      icon: <FilePdfOutlined />,
      color: '#FF5722',
    },
    {
      name: 'Word Document',
      ext: '.doc, .docx',
      icon: <FileWordOutlined />,
      color: '#2B579A',
    },
    {
      name: 'Excel Spreadsheet',
      ext: '.xls, .xlsx',
      icon: <FileExcelOutlined />,
      color: '#217346',
    },
    {
      name: 'PPT Presentation',
      ext: '.ppt, .pptx',
      icon: <FilePptOutlined />,
      color: '#D24726',
    },
    {
      name: 'Plain Text',
      ext: '.txt',
      icon: <FileTextOutlined />,
      color: '#616161',
    },
    {
      name: 'Markdown',
      ext: '.md',
      icon: <FileMarkdownOutlined />,
      color: '#083FA1',
    },
  ];

  const features = [
    {
      icon: <RocketOutlined />,
      title: 'Fast Import',
      description: 'Batch upload, efficient processing',
    },
    {
      icon: <SafetyOutlined />,
      title: 'Secure & Reliable',
      description: 'Local storage, data security',
    },
    {
      icon: <ThunderboltOutlined />,
      title: 'Smart Parsing',
      description: 'Automatic detection, precise extraction',
    },
  ];

  const handleFileSelect = (files: FileList | null) => {
    if (!files || files.length === 0) return;

    const allowedExts = allowedFileTypes.split(',').map(t => {
      const trimmed = t.trim().toLowerCase();
      return trimmed.startsWith('.') ? trimmed : '.' + trimmed;
    });

    const newFiles: FileItem[] = [];

    for (const file of Array.from(files)) {
      // Client-side file type validation
      const ext = '.' + file.name.split('.').pop()?.toLowerCase();
      if (!allowedExts.includes(ext)) {
        message.error(`Unsupported file type: ${ext}. Allowed types: ${allowedFileTypes}`);
        continue;
      }
      // Client-side file size validation
      if (file.size > maxFileSize) {
        const maxMB = Math.round(maxFileSize / 1048576 * 10) / 10;
        message.error(`File ${file.name} exceeds the ${maxMB}MB size limit`);
        continue;
      }

      newFiles.push({
        id: `${Date.now()}-${Math.random()}`,
        file,
        status: 'pending',
        progress: 0,
      });
    }

    if (newFiles.length > 0) {
      setFileList((prev) => [...prev, ...newFiles]);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const files = e.dataTransfer.files;
    handleFileSelect(files);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const handleRemoveFile = (id: string) => {
    setFileList((prev) => prev.filter((item) => item.id !== id));
  };

  const handleClearAll = () => {
    setFileList([]);
  };


  const getFileIcon = (fileName: string): { icon: React.ReactNode; color: string } => {
    const ext = fileName.split('.').pop()?.toLowerCase();
    const iconMap: Record<string, { icon: React.ReactNode; color: string }> = {
      pdf: { icon: <FilePdfOutlined />, color: '#FF5722' },
      doc: { icon: <FileWordOutlined />, color: '#2B579A' },
      docx: { icon: <FileWordOutlined />, color: '#2B579A' },
      xls: { icon: <FileExcelOutlined />, color: '#217346' },
      xlsx: { icon: <FileExcelOutlined />, color: '#217346' },
      ppt: { icon: <FilePptOutlined />, color: '#D24726' },
      pptx: { icon: <FilePptOutlined />, color: '#D24726' },
      txt: { icon: <FileTextOutlined />, color: '#616161' },
      md: { icon: <FileMarkdownOutlined />, color: '#083FA1' },
    };
    return iconMap[ext || ''] || { icon: <FileTextOutlined />, color: '#1890FF' };
  };

  const handleUpload = async () => {
    const pendingFiles = fileList.filter((item) => item.status === 'pending');
    if (pendingFiles.length === 0) {
      message.warning('Please select the files to upload first');
      return;
    }

    setIsUploading(true);

    for (const fileItem of pendingFiles) {
      try {
        setFileList((prev) =>
          prev.map((item) =>
            item.id === fileItem.id ? { ...item, status: 'uploading', progress: 0 } : item
          )
        );

        // Timer to simulate progress
        const progressInterval = setInterval(() => {
          setFileList((prev) =>
            prev.map((item) => {
              if (item.id === fileItem.id && item.progress < 90) {
                return { ...item, progress: item.progress + 10 };
              }
              return item;
            })
          );
        }, 200);

        // Use uploadAndParseDocument to handle all file types uniformly (PDF/DOCX/XLSX/PPTX/TXT/MD)
        const result = await documentService.uploadAndParseDocument(fileItem.file);

        clearInterval(progressInterval);

        setFileList((prev) =>
          prev.map((item) =>
            item.id === fileItem.id
              ? { ...item, status: 'success', progress: 100, documentId: result.documentId }
              : item
          )
        );

        setHasImported(true);
        message.success(`${fileItem.file.name} imported successfully — ${result.contentLength?.toLocaleString() || 0} characters parsed`);
      } catch (error) {
        console.error('Upload failed:', error);
        setFileList((prev) =>
          prev.map((item) =>
            item.id === fileItem.id
              ? { ...item, status: 'error', error: 'Upload failed, please try again' }
              : item
          )
        );
        message.error(`${fileItem.file.name} import failed`);
      }
    }

    setIsUploading(false);
  };

  const handleViewDocument = (documentId: string) => {
    navigate(`/documents/${documentId}`);
  };

  const handleViewDrafts = () => {
    navigate('/drafts');
  };

  const retryFile = async (fileItem: FileItem) => {
    try {
      setFileList((prev) =>
        prev.map((item) =>
          item.id === fileItem.id ? { ...item, status: 'uploading', progress: 0, error: undefined } : item
        )
      );

      const progressInterval = setInterval(() => {
        setFileList((prev) =>
          prev.map((item) => {
            if (item.id === fileItem.id && item.progress < 90) {
              return { ...item, progress: item.progress + 10 };
            }
            return item;
          })
        );
      }, 200);

      // Retry using uploadAndParseDocument
      const result = await documentService.uploadAndParseDocument(fileItem.file);

      clearInterval(progressInterval);

      setFileList((prev) =>
        prev.map((item) =>
          item.id === fileItem.id
            ? { ...item, status: 'success', progress: 100, documentId: result.documentId }
            : item
        )
      );

      setHasImported(true);
      message.success(`${fileItem.file.name} re-imported successfully`);
    } catch (error) {
      console.error('Retry upload failed:', error);
      setFileList((prev) =>
        prev.map((item) =>
          item.id === fileItem.id
            ? { ...item, status: 'error', error: 'Upload failed, please try again' }
            : item
        )
      );
      message.error(`${fileItem.file.name} re-import failed`);
    }
  };

  return (
    <div className="import-document-page">
      <div className="import-header">
        <div className="header-content">
          <div className="header-icon">
            <CloudUploadOutlined />
          </div>
          <div className="header-text">
            <h1>Import Document</h1>
            <p>Batch import multiple file formats, automatically creating document records</p>
          </div>
        </div>
      </div>

      <div className="import-content">
        <div className="upload-section">
          <div
            className={`upload-area ${fileList.length > 0 ? 'has-files' : ''}`}
            onClick={() => fileInputRef.current?.click()}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
          >
            <div className="upload-content">
              <div className="upload-icon-wrapper">
                <CloudUploadOutlined className="upload-main-icon" />
                <div className="upload-bg-effect">
                  <CloudUploadOutlined />
                </div>
              </div>
              <div className="upload-text">
                <h3>Click or drag files here to upload</h3>
                <p>Supports single or batch upload, up to {Math.round(maxFileSize / 1048576 * 10) / 10}MB per file</p>
              </div>
              <button
                className="upload-button"
                onClick={(e) => {
                  e.stopPropagation();
                  fileInputRef.current?.click();
                }}
              >
                <CloudUploadOutlined />
                Choose Files
              </button>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept={allowedFileTypes.split(',').map(t => {
                  const ext = t.trim().toLowerCase();
                  if (ext === 'pdf') return 'application/pdf,.pdf';
                  return '.' + ext;
                }).join(',')}
                style={{ display: 'none' }}
                onChange={(e) => handleFileSelect(e.target.files)}
              />
            </div>
          </div>

          {fileList.length > 0 && (
            <>
              <div className="file-list">
                <div className="file-list-header">
                  <h3>File List ({fileList.length})</h3>
                  <button
                    className="clear-button"
                    onClick={handleClearAll}
                    disabled={isUploading}
                  >
                    <ClearOutlined />
                    Clear List
                  </button>
                </div>
                {fileList.map((item) => {
                  const { icon, color } = getFileIcon(item.file.name);
                  return (
                    <div key={item.id} className={`file-item ${item.status}`}>
                      <div className="file-icon" style={{ color }}>
                        {icon}
                      </div>
                      <div className="file-info">
                        <div className="file-name">{item.file.name}</div>
                        <div className="file-meta">
                          <span className="file-size">{formatFileSize(item.file.size)}</span>
                          {item.status === 'uploading' && (
                            <span className="file-status-text">Uploading {item.progress}%</span>
                          )}
                          {item.status === 'success' && (
                            <span className="file-status-text success">Upload succeeded</span>
                          )}
                          {item.status === 'error' && (
                            <span className="file-status-text error">Upload failed</span>
                          )}
                        </div>
                        {item.status === 'uploading' && (
                          <div className="progress-bar">
                            <div
                              className="progress-fill"
                              style={{ width: `${item.progress}%` }}
                            />
                          </div>
                        )}
                        {item.status === 'error' && item.error && (
                          <div className="error-message">{item.error}</div>
                        )}
                      </div>
                      <div className="file-actions">
                        {item.status === 'error' && (
                          <button
                            className="action-button retry"
                            onClick={() => retryFile(item)}
                            title="Retry"
                          >
                            <ReloadOutlined />
                          </button>
                        )}
                        {item.status === 'success' && item.documentId && (
                          <button
                            className="action-button view"
                            onClick={() => handleViewDocument(item.documentId!)}
                            title="View Document"
                          >
                            <EyeOutlined />
                          </button>
                        )}
                        <button
                          className="action-button delete"
                          onClick={() => handleRemoveFile(item.id)}
                          title="Delete"
                          disabled={item.status === 'uploading'}
                        >
                          <DeleteOutlined />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="action-buttons">
                <button
                  className="btn-secondary"
                  onClick={handleClearAll}
                  disabled={isUploading}
                >
                  <ClearOutlined />
                  Clear List
                </button>
                <button
                  className="btn-primary"
                  onClick={handleUpload}
                  disabled={isUploading || fileList.every((item) => item.status !== 'pending')}
                >
                  {isUploading ? (
                    <>
                      <LoadingOutlined />
                      Uploading...
                    </>
                  ) : (
                    <>
                      <CloudUploadOutlined />
                      Start Upload
                    </>
                  )}
                </button>
                {hasImported && (
                  <button
                    className="btn-drafts"
                    onClick={handleViewDrafts}
                  >
                    <FolderOpenOutlined />
                    View Drafts
                  </button>
                )}
              </div>
            </>
          )}
        </div>

        <div className="sidebar">
          <div className="sidebar-card">
            <h3>
              <FileTextOutlined />
              Supported Formats
            </h3>
            <div className="format-list">
              {supportedFormats.map((format) => (
                <div key={format.name} className="format-item">
                  <div className="format-info">
                    <div className="format-icon" style={{ color: format.color }}>
                      {format.icon}
                    </div>
                    <div className="format-details">
                      <div className="format-name">{format.name}</div>
                      <div className="format-ext">{format.ext}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="sidebar-card">
            <h3>
              <ThunderboltOutlined />
              Features
            </h3>
            <div className="feature-list">
              {features.map((feature, index) => (
                <div key={index} className="feature-item">
                  <div className="feature-icon">{feature.icon}</div>
                  <div className="feature-content">
                    <div className="feature-title">{feature.title}</div>
                    <div className="feature-description">{feature.description}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="sidebar-card info-card">
            <div className="info-header">
              <InfoCircleOutlined />
              <h3>Import Notes</h3>
            </div>
            <div className="info-content">
              <ul>
                <li>Up to {Math.round(maxFileSize / 1048576 * 10) / 10}MB per file</li>
                <li>A draft document is automatically created after upload</li>
                <li>You can continue editing it from the document list</li>
                <li>Files are organized and stored by date</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ImportDocumentPage;
