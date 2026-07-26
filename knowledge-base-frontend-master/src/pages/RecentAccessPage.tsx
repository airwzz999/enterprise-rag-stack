import React, { useState, useEffect } from 'react';
import {
  List,
  Card,
  Button,
  Empty,
  Popconfirm,
  Tag,
  Tooltip,
} from 'antd';
import {
  ClockCircleOutlined,
  DeleteOutlined,
  ClearOutlined,
  FileTextOutlined,
  CalendarOutlined,
  FolderOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { accessService } from '../services/access.service';
import { useNavigate } from 'react-router-dom';
import './RecentAccessPage.css';

const COLORS = {
  primary: '#2563eb',
  bgPrimary: '#ffffff',
  bgSecondary: '#f8fafc',
  bgTertiary: '#f1f5f9',
  textPrimary: '#0f172a',
  textSecondary: '#475569',
  textTertiary: '#94a3b8',
  borderColor: '#e2e8f0',
};

/**
 * Document access record type definition
 */
interface DocumentAccess {
  id: string;
  userId: string;
  documentId: string;
  documentTitle: string;
  summary: string;
  categoryName: string;
  authorName: string;
  accessTime: string;
  status: number;
}

/**
 * Recent Access page
 *
 * Designed to top-tier industry standards for a professional user experience
 */
const RecentAccessPage: React.FC = () => {
  const [accessList, setAccessList] = useState<DocumentAccess[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  /**
   * Fetch recent access records
   */
  const fetchRecentAccess = async () => {
    setLoading(true);
    try {
      const data = await accessService.getRecentAccess(20);
      setAccessList(data);
    } catch (error) {
      console.error('Failed to fetch recent access records:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Delete a single access record
   */
  const handleDelete = async (documentId: string) => {
    try {
      await accessService.deleteAccess(documentId);
      setAccessList((prev) => prev.filter((item) => item.documentId !== documentId));
    } catch (error) {
      console.error('Failed to delete access record:', error);
    }
  };

  /**
   * Clear all access records
   */
  const handleClearAll = async () => {
    try {
      await accessService.clearAllAccess();
      setAccessList([]);
    } catch (error) {
      console.error('Failed to clear access records:', error);
    }
  };

  /**
   * Navigate to the document detail page
   */
  const handleViewDocument = (documentId: string) => {
    navigate(`/documents/${documentId}`);
  };

  /**
   * Format the time for display
   */
  const formatTime = (timeStr: string): string => {
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    // Less than 1 minute
    if (diff < 60000) {
      return 'Just now';
    }
    // Less than 1 hour
    if (diff < 3600000) {
      return `${Math.floor(diff / 60000)}m ago`;
    }
    // Less than 24 hours
    if (diff < 86400000) {
      return `${Math.floor(diff / 3600000)}h ago`;
    }
    // Less than 7 days
    if (diff < 604800000) {
      return `${Math.floor(diff / 86400000)}d ago`;
    }
    // Show the full date
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  /**
   * Get the status tag
   */
  const getStatusTag = (status: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      0: { color: 'default', text: 'Draft' },
      1: { color: 'success', text: 'Published' },
      2: { color: 'warning', text: 'Archived' },
      3: { color: 'processing', text: 'Pending Review' },
    };
    const item = statusMap[status] || { color: 'default', text: 'Unknown' };
    return (
      <Tag color={item.color}>{item.text}</Tag>
    );
  };

  useEffect(() => {
    fetchRecentAccess();
  }, []);

  return (
    <div style={{
      padding: '8px 12px 12px 8px',
      marginLeft: '-16px',
      marginTop: '-8px',
      backgroundColor: COLORS.bgSecondary,
      minHeight: 'calc(100vh - 64px)',
    }}>
      {/* Page header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '16px',
      }}>
        <h1 style={{
          fontSize: '24px',
          fontWeight: 700,
          color: COLORS.textPrimary,
          margin: 0,
        }}>
          Recent Access
        </h1>
        {accessList.length > 0 && (
          <Popconfirm
            title="Are you sure you want to clear all access records?"
            onConfirm={handleClearAll}
            okText="OK"
            cancelText="Cancel"
          >
            <Button
              type="text"
              danger
              icon={<ClearOutlined />}
            >
              Clear Records
            </Button>
          </Popconfirm>
        )}
      </div>

      <div style={{
        padding: '24px',
        maxWidth: '1600px',
        margin: '0 auto',
      }}>
        {loading ? (
          <div className="loading-container">
            <Card loading style={{ width: '100%' }} />
          </div>
        ) : accessList.length === 0 ? (
          <Card className="empty-card">
            <Empty
              description="No access records yet"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button
                type="primary"
                onClick={() => navigate('/documents')}
              >
                Browse Documents
              </Button>
            </Empty>
          </Card>
        ) : (
          <div className="access-list-container">
            <div className="access-count-badge">
              <ClockCircleOutlined />
              <span>{accessList.length} record(s) total</span>
            </div>

            <List
              grid={{ gutter: 16, column: 1 }}
              dataSource={accessList}
              renderItem={(item) => (
                <List.Item key={item.id}>
                  <Card
                    className="access-card"
                    hoverable
                    onClick={() => handleViewDocument(item.documentId)}
                  >
                    <div className="card-content">
                      <div className="card-header">
                        <div className="title-row">
                          <FileTextOutlined className="title-icon" />
                          <span className="document-title">{item.documentTitle}</span>
                          {getStatusTag(item.status)}
                        </div>
                        <div className="card-actions">
                          <Tooltip title="Delete Record">
                            <Popconfirm
                              title="Are you sure you want to delete this access record?"
                              onConfirm={() => handleDelete(item.documentId)}
                              okText="OK"
                              cancelText="Cancel"
                            >
                              <Button
                                type="text"
                                danger
                                icon={<DeleteOutlined />}
                                onClick={(e) => e.stopPropagation()}
                              />
                            </Popconfirm>
                          </Tooltip>
                        </div>
                      </div>

                      {item.summary && (
                        <p className="document-summary">{item.summary}</p>
                      )}

                      <div className="card-meta">
                        {item.categoryName && (
                          <span className="meta-item">
                            <FolderOutlined className="meta-icon" />
                            {item.categoryName}
                          </span>
                        )}
                        {item.authorName && (
                          <span className="meta-item">
                            <UserOutlined className="meta-icon" />
                            {item.authorName}
                          </span>
                        )}
                        <span className="meta-item access-time">
                          <CalendarOutlined className="meta-icon" />
                          {formatTime(item.accessTime)}
                        </span>
                      </div>
                    </div>
                  </Card>
                </List.Item>
              )}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default RecentAccessPage;
