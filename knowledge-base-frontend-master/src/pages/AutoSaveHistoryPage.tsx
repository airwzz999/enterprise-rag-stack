import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Button, List, Spin, message, Pagination, Tag } from 'antd';
import {
  HistoryOutlined,
  RollbackOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { documentService } from '@/services';
import ReactMarkdown from 'react-markdown';

interface AutoSaveSnapshot {
  id: string;
  documentId: number;
  title: string;
  contentPreview: string;
  content?: string;
  contentLength: number;
  savedAt: string;
}

const AutoSaveHistoryPage: React.FC = () => {
  const { id: documentId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [contentLoading, setContentLoading] = useState(false);
  const [snapshots, setSnapshots] = useState<AutoSaveSnapshot[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selectedSnapshot, setSelectedSnapshot] = useState<AutoSaveSnapshot | null>(null);
  const [selectedContent, setSelectedContent] = useState<string | null>(null);

  const fetchHistory = useCallback(async () => {
    if (!documentId) return;
    setLoading(true);
    try {
      const data = await documentService.getAutoSaveHistory(documentId, page, pageSize);
      setSnapshots(data.list);
      setTotal(data.total);
    } catch (error) {
      console.error('Failed to fetch auto-save history:', error);
      message.error('Failed to fetch auto-save history');
    } finally {
      setLoading(false);
    }
  }, [documentId, page, pageSize]);

  const fetchSnapshotDetail = async (snapshot: AutoSaveSnapshot) => {
    if (!documentId) return;
    setSelectedSnapshot(snapshot);
    setContentLoading(true);
    try {
      const detail = await documentService.getAutoSaveSnapshot(documentId, snapshot.id);
      setSelectedContent(detail.content || null);
    } catch (error) {
      console.error('Failed to fetch snapshot details:', error);
      message.error('Failed to fetch snapshot details');
      setSelectedContent(null);
    } finally {
      setContentLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  // Automatically select the first snapshot after the initial load
  useEffect(() => {
    if (snapshots.length > 0 && !selectedSnapshot) {
      fetchSnapshotDetail(snapshots[0]);
    }
  }, [snapshots, selectedSnapshot]);

  const formatTime = (dateStr: string): string => {
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const formatRelativeTime = (dateStr: string): string => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return `${diffSec}s ago`;
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour}h ago`;
    return formatTime(dateStr);
  };

  return (
    <div style={{ padding: '8px 24px 24px 8px', maxWidth: 1400, margin: '0', height: 'calc(100vh - 32px)', display: 'flex', flexDirection: 'column' }}>
      {/* Page header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 16,
        flexShrink: 0,
      }}>
        <div>
          <h1 style={{
            fontSize: 20,
            fontWeight: 700,
            color: '#1e293b',
            margin: 0,
            display: 'flex',
            alignItems: 'center',
            gap: 10,
          }}>
            <HistoryOutlined style={{ color: '#2563eb' }} />
            Auto-Save History
          </h1>
          <p style={{ color: '#64748b', margin: '4px 0 0 0', fontSize: 13 }}>
            {total} snapshots in total · Select a snapshot on the left to view its content
          </p>
        </div>
        <div style={{ display: 'flex', gap: 12 }}>
          <Button
            icon={<RollbackOutlined />}
            onClick={() => navigate(`/drafts`)}
          >
            Back to Drafts
          </Button>
          <Button
            type="primary"
            icon={<FileTextOutlined />}
            onClick={() => navigate(`/documents/${documentId}/edit`)}
          >
            Edit Document
          </Button>
        </div>
      </div>

      {/* Body: left list + right content */}
      <div style={{
        flex: 1,
        display: 'flex',
        gap: 16,
        minHeight: 0,
      }}>
        {/* Left: snapshot list */}
        <div style={{
          width: 340,
          flexShrink: 0,
          background: '#fff',
          borderRadius: 12,
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}>
          <div style={{
            padding: '12px 16px',
            borderBottom: '1px solid #f1f5f9',
            fontWeight: 600,
            fontSize: 14,
            color: '#334155',
            flexShrink: 0,
          }}>
            <ClockCircleOutlined style={{ marginRight: 8, color: '#94a3b8' }} />
            Snapshot List
          </div>
          <div style={{ flex: 1, overflow: 'auto' }}>
            <Spin spinning={loading}>
              {snapshots.length === 0 && !loading ? (
                <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>
                  No auto-save history yet
                </div>
              ) : (
                <List
                  dataSource={snapshots}
                  split={false}
                  renderItem={(item) => {
                    const isActive = selectedSnapshot?.id === item.id;
                    return (
                      <div
                        key={item.id}
                        onClick={() => fetchSnapshotDetail(item)}
                        style={{
                          padding: '12px 16px',
                          cursor: 'pointer',
                          borderLeft: isActive ? '3px solid #2563eb' : '3px solid transparent',
                          background: isActive ? '#eff6ff' : 'transparent',
                          borderBottom: '1px solid #f8fafc',
                          transition: 'background 0.15s',
                        }}
                        onMouseEnter={(e) => {
                          if (!isActive) (e.currentTarget as HTMLDivElement).style.background = '#f8fafc';
                        }}
                        onMouseLeave={(e) => {
                          if (!isActive) (e.currentTarget as HTMLDivElement).style.background = 'transparent';
                        }}
                      >
                        <div style={{
                          fontWeight: isActive ? 600 : 500,
                          fontSize: 13,
                          color: isActive ? '#1e293b' : '#334155',
                          marginBottom: 6,
                          lineHeight: 1.4,
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                          overflow: 'hidden',
                        }}>
                          {item.title || 'Untitled Document'}
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: 12, color: '#94a3b8' }}>
                          <span>
                            <ClockCircleOutlined style={{ marginRight: 4 }} />
                            {formatRelativeTime(item.savedAt)}
                          </span>
                          <span>{item.contentLength?.toLocaleString() || 0} characters</span>
                        </div>
                      </div>
                    );
                  }}
                />
              )}
            </Spin>
          </div>
          {/* Bottom pagination */}
          {total > pageSize && (
            <div style={{
              padding: '8px 16px',
              borderTop: '1px solid #f1f5f9',
              textAlign: 'center',
              flexShrink: 0,
            }}>
              <Pagination
                size="small"
                current={page}
                pageSize={pageSize}
                total={total}
                showSizeChanger={false}
                onChange={(p, s) => {
                  setPage(p);
                  setPageSize(s);
                }}
              />
            </div>
          )}
        </div>

        {/* Right: content preview */}
        <div style={{
          flex: 1,
          background: '#fff',
          borderRadius: 12,
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}>
          <div style={{
            padding: '12px 16px',
            borderBottom: '1px solid #f1f5f9',
            fontWeight: 600,
            fontSize: 14,
            color: '#334155',
            flexShrink: 0,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}>
            <span>
              <FileTextOutlined style={{ marginRight: 8, color: '#94a3b8' }} />
              {selectedSnapshot ? selectedSnapshot.title || 'Untitled Document' : 'Select a snapshot to view its content'}
            </span>
            {selectedSnapshot && (
              <Tag color="blue">
                {formatTime(selectedSnapshot.savedAt)}
                {' · '}
                {selectedSnapshot.contentLength?.toLocaleString() || 0} characters
              </Tag>
            )}
          </div>
          <div style={{
            flex: 1,
            overflow: 'auto',
            padding: 24,
          }}>
            <Spin spinning={contentLoading}>
              {selectedContent ? (
                <div style={{
                  maxWidth: 800,
                  margin: '0 auto',
                }}>
                  <ReactMarkdown
                    components={{
                      h1: ({ children }) => (
                        <h1 style={{ fontSize: 24, fontWeight: 700, color: '#0f172a', margin: '24px 0 12px', borderBottom: '1px solid #e2e8f0', paddingBottom: 8 }}>{children}</h1>
                      ),
                      h2: ({ children }) => (
                        <h2 style={{ fontSize: 20, fontWeight: 600, color: '#1e293b', margin: '20px 0 10px' }}>{children}</h2>
                      ),
                      h3: ({ children }) => (
                        <h3 style={{ fontSize: 17, fontWeight: 600, color: '#334155', margin: '16px 0 8px' }}>{children}</h3>
                      ),
                      p: ({ children }) => (
                        <p style={{ fontSize: 15, lineHeight: 1.8, color: '#475569', margin: '8px 0' }}>{children}</p>
                      ),
                      code: ({ children, className }) => {
                        const isInline = !className;
                        return isInline ? (
                          <code style={{
                            background: '#f1f5f9',
                            padding: '2px 6px',
                            borderRadius: 4,
                            fontSize: '0.9em',
                            color: '#e11d48',
                          }}>{children}</code>
                        ) : (
                          <pre style={{
                            background: '#1e293b',
                            color: '#e2e8f0',
                            padding: '16px 20px',
                            borderRadius: 8,
                            overflow: 'auto',
                            fontSize: 13,
                            lineHeight: 1.6,
                          }}>
                            <code>{children}</code>
                          </pre>
                        );
                      },
                      ul: ({ children }) => (
                        <ul style={{ paddingLeft: 24, margin: '8px 0' }}>{children}</ul>
                      ),
                      ol: ({ children }) => (
                        <ol style={{ paddingLeft: 24, margin: '8px 0' }}>{children}</ol>
                      ),
                      li: ({ children }) => (
                        <li style={{ fontSize: 15, lineHeight: 1.8, color: '#475569' }}>{children}</li>
                      ),
                      blockquote: ({ children }) => (
                        <blockquote style={{
                          borderLeft: '4px solid #94a3b8',
                          paddingLeft: 16,
                          margin: '12px 0',
                          color: '#64748b',
                          fontStyle: 'italic',
                        }}>{children}</blockquote>
                      ),
                      hr: () => <hr style={{ border: 'none', borderTop: '1px solid #e2e8f0', margin: '24px 0' }} />,
                    }}
                  >
                    {selectedContent}
                  </ReactMarkdown>
                </div>
              ) : !contentLoading && selectedSnapshot ? (
                <div style={{ textAlign: 'center', color: '#94a3b8', padding: 40 }}>
                  This snapshot has no content
                </div>
              ) : !contentLoading ? (
                <div style={{ textAlign: 'center', color: '#94a3b8', padding: 40 }}>
                  Please select a snapshot on the left to view its content
                </div>
              ) : null}
            </Spin>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AutoSaveHistoryPage;
