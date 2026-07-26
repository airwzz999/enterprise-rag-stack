import React, { useEffect, useState, useCallback } from 'react';
import {
  Button,
  Modal,
  Dropdown,
  Spin,
} from 'antd';
import { App } from 'antd';
import {
  MoreOutlined,
  EyeOutlined,
  EditOutlined,
  ShareAltOutlined,
  StarOutlined,
  DeleteOutlined,
  FileTextOutlined,
  FolderOutlined,
  CodeOutlined,
  DollarOutlined,
  TeamOutlined,
  LayoutOutlined,
  FileMarkdownOutlined,
  ClockCircleOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthStore, useFavoriteStore } from '@/stores';
import { documentService, reviewService } from '@/services';
import { Document, ReviewTask } from '@/types';
import UserAvatar from '@/components/common/UserAvatar';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/en';
import { PERMISSIONS, hasPermission } from '@/utils/permission';

dayjs.extend(relativeTime);
dayjs.locale('en');

const COLORS = {
  primary: '#2563eb',
  primaryDark: '#1e40af',
  primaryLight: '#3b82f6',
  secondary: '#8b5cf6',
  success: '#10b981',
  warning: '#f59e0b',
  danger: '#ef4444',
  bgPrimary: '#ffffff',
  bgSecondary: '#f8fafc',
  bgTertiary: '#f1f5f9',
  textPrimary: '#0f172a',
  textSecondary: '#475569',
  textMuted: '#94a3b8',
  borderColor: '#e2e8f0',
  borderColorLight: '#f1f5f9',
};

type TabKey = 'all' | 'draft' | 'pending_review' | 'published' | 'rejected';

interface TabConfig {
  key: TabKey;
  label: string;
}

const TABS: TabConfig[] = [
  { key: 'all', label: 'All Documents' },
  { key: 'draft', label: 'Drafts' },
  { key: 'pending_review', label: 'Under Review' },
  { key: 'published', label: 'Published' },
  { key: 'rejected', label: 'Rejected' },
];

interface StatCard {
  key: TabKey;
  label: string;
  count: number;
  color: string;
  bgColor: string;
}

const MyDocumentsPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const canCreateDocument = hasPermission(user, PERMISSIONS.documentCreate);
  const canEditDocument = hasPermission(user, PERMISSIONS.documentEdit);
  const canDeleteDocument = hasPermission(user, PERMISSIONS.documentDelete);
  const { toggleFavorite } = useFavoriteStore();

  const [activeTab, setActiveTab] = useState<TabKey>('all');
  const [documents, setDocuments] = useState<Document[]>([]);
  const [rejectedTasks, setRejectedTasks] = useState<ReviewTask[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(12);
  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  const [statCounts, setStatCounts] = useState<Record<TabKey, number>>({
    all: 0,
    draft: 0,
    pending_review: 0,
    published: 0,
    rejected: 0,
  });

  const authorId = user?.id ? String(user.id) : '';

  // Fetch documents based on active tab
  const fetchDocuments = useCallback(async (tab: TabKey, page: number = 1) => {
    if (!authorId) return;
    setIsLoading(true);
    try {
      if (tab === 'rejected') {
        const response = await reviewService.getMyRejectedDocuments({
          authorId,
          page,
          pageSize,
        });
        setRejectedTasks(response.list);
        setTotal(response.total);
        setDocuments([]);
      } else {
        const statusMap: Record<string, number | undefined> = {
          all: undefined,
          draft: 0,
          pending_review: 3,
          published: 1,
        };
        const status = statusMap[tab];
        const response = await documentService.getDocuments({
          authorId,
          status,
          page,
          pageSize,
          sortBy: 'updatedAt',
          sortOrder: 'desc',
        } as any);
        const normalized = (response.list || []).map(normalizeDocument);
        setDocuments(normalized);
        setTotal(response.total);
        setRejectedTasks([]);
      }
    } catch (error) {
      console.error('Failed to fetch documents:', error);
      message.error('Failed to fetch document list');
    } finally {
      setIsLoading(false);
    }
  }, [authorId, pageSize, message]);

  // Fetch stat counts
  const fetchStatCounts = useCallback(async () => {
    if (!authorId) return;
    try {
      const [allRes, draftRes, pendingRes, publishedRes, rejectedRes] = await Promise.all([
        documentService.getDocuments({ authorId, page: 1, pageSize: 1 } as any),
        documentService.getDocuments({ authorId, status: 0, page: 1, pageSize: 1 } as any),
        documentService.getDocuments({ authorId, status: 3, page: 1, pageSize: 1 } as any),
        documentService.getDocuments({ authorId, status: 1, page: 1, pageSize: 1 } as any),
        reviewService.getMyRejectedDocuments({ authorId, page: 1, pageSize: 1 }),
      ]);
      setStatCounts({
        all: allRes.total,
        draft: draftRes.total,
        pending_review: pendingRes.total,
        published: publishedRes.total,
        rejected: rejectedRes.total,
      });
    } catch (error) {
      console.error('Failed to fetch statistics:', error);
    }
  }, [authorId]);

  useEffect(() => {
    if (authorId) {
      fetchDocuments(activeTab, 1);
      fetchStatCounts();
    }
  }, [authorId, activeTab, fetchDocuments, fetchStatCounts]);

  const handleTabChange = (tab: TabKey) => {
    setActiveTab(tab);
    setCurrentPage(1);
    setSelectedDocuments([]);
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
    fetchDocuments(activeTab, page);
    setSelectedDocuments([]);
  };

  const normalizeDocument = (doc: any): any => ({
    ...doc,
    id: String(doc.id),
    tags: doc.tags ? (typeof doc.tags === 'string' ? doc.tags.split(',').filter(Boolean) : doc.tags) : [],
    author: doc.author || {
      id: String(doc.authorId),
      username: doc.authorName,
      avatar: undefined,
    },
    status: doc.status === 1 ? 'published' : doc.status === 2 ? 'archived' : doc.status === 3 ? 'pending_review' : 'draft',
    content: doc.content || '',
    summary: doc.summary || '',
    createdAt: doc.createdAt,
    updatedAt: doc.updatedAt,
    publishTime: doc.publishTime || (doc.status === 1 || doc.status === 3 ? doc.updatedAt : null),
    viewCount: doc.viewCount || 0,
    likeCount: doc.likeCount || 0,
    commentCount: doc.commentCount || 0,
  });

  const getCategoryIcon = (categoryName: string) => {
    const name = categoryName?.toLowerCase() || '';
    if (name.includes('technical') || name.includes('development') || name.includes('backend') || name.includes('frontend'))
      return <CodeOutlined style={{ fontSize: 16, color: COLORS.textSecondary }} />;
    if (name.includes('business') || name.includes('process'))
      return <LayoutOutlined style={{ fontSize: 16, color: COLORS.textSecondary }} />;
    if (name.includes('human') || name.includes('hr'))
      return <TeamOutlined style={{ fontSize: 16, color: COLORS.textSecondary }} />;
    if (name.includes('product') || name.includes('design'))
      return <LayoutOutlined style={{ fontSize: 16, color: COLORS.textSecondary }} />;
    if (name.includes('financial') || name.includes('expense'))
      return <DollarOutlined style={{ fontSize: 16, color: COLORS.textSecondary }} />;
    return <FolderOutlined style={{ fontSize: 16, color: COLORS.textSecondary }} />;
  };

  const getCategoryBadgeStyle = (categoryName: string) => {
    const name = categoryName.toLowerCase();
    if (name.includes('technical') || name.includes('development')) return 'tech';
    if (name.includes('business')) return 'business';
    return 'ai';
  };

  const handleSelectDocument = (documentId: string, checked: boolean) => {
    setSelectedDocuments(prev =>
      checked ? [...prev, documentId] : prev.filter(id => id !== documentId)
    );
  };

  const handleSelectAll = (checked: boolean) => {
    setSelectedDocuments(
      checked ? documents.map(doc => doc.id) : []
    );
  };

  const handleDeleteDocument = (documentId: string) => {
    Modal.confirm({
      title: 'Confirm Deletion',
      content: 'Are you sure you want to delete this document?',
      onOk: async () => {
        try {
          await documentService.deleteDocument(documentId);
          message.success('Deleted successfully');
          fetchDocuments(activeTab, currentPage);
          fetchStatCounts();
        } catch (error) {
          message.error('Failed to delete');
        }
      },
    });
  };

  const handleBatchDelete = async () => {
    if (selectedDocuments.length === 0) {
      message.warning('Please select the documents to delete first');
      return;
    }
    Modal.confirm({
      title: 'Confirm Deletion',
      content: `Are you sure you want to delete the selected ${selectedDocuments.length} document(s)?`,
      onOk: async () => {
        try {
          await Promise.all(selectedDocuments.map(id => documentService.deleteDocument(id)));
          message.success('Deleted successfully');
          setSelectedDocuments([]);
          fetchDocuments(activeTab, currentPage);
          fetchStatCounts();
        } catch (error) {
          message.error('Failed to delete');
        }
      },
    });
  };

  const handleDocumentAction = async (action: string, documentId: string, e?: React.MouseEvent) => {
    e?.stopPropagation();
    // Documents under review only allow viewing
    const doc = documents.find(d => d.id === documentId);
    if (doc && doc.status === 'pending_review' && action !== 'view') {
      message.warning('This action is not allowed on documents under review');
      return;
    }
    switch (action) {
      case 'view':
        navigate(`/documents/${documentId}`);
        break;
      case 'edit':
        window.open(`/documents/${documentId}/edit`, '_blank');
        break;
      case 'share':
        try {
          const result = await documentService.createShare({
            documentId,
            shareType: 1,
            expireType: 1,
            accessLimit: 0,
            requirePassword: 0,
          });
          const shareUrl = `${window.location.origin}/share/${result.shareId}`;
          await navigator.clipboard.writeText(shareUrl);
          message.success('Share link copied to clipboard');
        } catch (error: any) {
          message.error(error?.message || 'Operation failed');
        }
        break;
      case 'favorite':
        try {
          const newStatus = await toggleFavorite(documentId);
          message.success(newStatus ? 'Added to favorites' : 'Removed from favorites');
        } catch (error) {
          message.error('Operation failed, please try again');
        }
        break;
      case 'delete':
        handleDeleteDocument(documentId);
        break;
    }
  };

  const getActionMenuItems = (document: any) => {
    const isPendingReview = document.status === 'pending_review';
    if (isPendingReview) {
      return [
        { key: 'view', label: 'View Document', icon: <EyeOutlined /> },
      ];
    }
    return [
      { key: 'view', label: 'View Document', icon: <EyeOutlined /> },
      ...(canEditDocument ? [{ key: 'edit', label: 'Edit Document', icon: <EditOutlined /> }] : []),
      { key: 'share', label: 'Share Document', icon: <ShareAltOutlined /> },
      { key: 'favorite', label: 'Add to Favorites', icon: <StarOutlined /> },
      ...(canDeleteDocument
        ? [{ type: 'divider' as const }, { key: 'delete', label: 'Delete Document', icon: <DeleteOutlined />, danger: true }]
        : []),
    ];
  };

  const statCards: StatCard[] = [
    { key: 'all', label: 'All Documents', count: statCounts.all, color: COLORS.primary, bgColor: '#eff6ff' },
    { key: 'draft', label: 'Drafts', count: statCounts.draft, color: COLORS.secondary, bgColor: '#f5f3ff' },
    { key: 'pending_review', label: 'Under Review', count: statCounts.pending_review, color: COLORS.warning, bgColor: '#fffbeb' },
    { key: 'published', label: 'Published', count: statCounts.published, color: COLORS.success, bgColor: '#ecfdf5' },
    { key: 'rejected', label: 'Rejected', count: statCounts.rejected, color: COLORS.danger, bgColor: '#fef2f2' },
  ];

  const totalPages = Math.ceil(total / pageSize);

  return (
    <div style={{
      padding: '8px 12px 12px 8px',
      marginLeft: '-16px',
      marginTop: '-8px',
      backgroundColor: COLORS.bgSecondary,
      minHeight: 'calc(100vh - 64px)',
    }}>
      {/* Page Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '20px',
      }}>
        <div>
          <h1 style={{
            fontSize: '24px',
            fontWeight: 700,
            color: COLORS.textPrimary,
            margin: 0,
            marginBottom: '4px',
          }}>
            My Documents
          </h1>
          <p style={{
            fontSize: '14px',
            color: COLORS.textMuted,
            margin: 0,
          }}>
            View and manage all the documents you've created
          </p>
        </div>
        {canCreateDocument && (
          <button
            onClick={() => navigate('/documents/new')}
            style={{
              padding: '10px 20px',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all 0.2s',
              border: 'none',
              backgroundColor: COLORS.primary,
              color: '#ffffff',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = COLORS.primaryDark;
              e.currentTarget.style.boxShadow = '0 4px 12px rgba(37, 99, 235, 0.3)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = COLORS.primary;
              e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)';
            }}
          >
            <PlusOutlined />
            Create Document
          </button>
        )}
      </div>

      {/* Statistics Cards */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(5, 1fr)',
        gap: '12px',
        marginBottom: '20px',
      }}>
        {statCards.map(stat => (
          <div
            key={stat.key}
            onClick={() => handleTabChange(stat.key)}
            style={{
              backgroundColor: COLORS.bgPrimary,
              borderRadius: '12px',
              padding: '16px 20px',
              cursor: 'pointer',
              border: activeTab === stat.key
                ? `2px solid ${stat.color}`
                : `1px solid ${COLORS.borderColor}`,
              transition: 'all 0.2s',
              boxShadow: activeTab === stat.key
                ? `0 4px 12px ${stat.color}20`
                : '0 1px 3px rgba(0,0,0,0.04)',
            }}
          >
            <div style={{
              fontSize: '12px',
              fontWeight: 500,
              color: COLORS.textMuted,
              marginBottom: '8px',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}>
              {stat.label}
            </div>
            <div style={{
              fontSize: '28px',
              fontWeight: 700,
              color: stat.color,
              lineHeight: 1,
            }}>
              {stat.count}
            </div>
          </div>
        ))}
      </div>

      {/* Tab Bar */}
      <div style={{
        display: 'flex',
        gap: '4px',
        marginBottom: '16px',
        backgroundColor: COLORS.bgTertiary,
        borderRadius: '10px',
        padding: '4px',
        width: 'fit-content',
      }}>
        {TABS.map(tab => (
          <button
            key={tab.key}
            onClick={() => handleTabChange(tab.key)}
            style={{
              padding: '8px 20px',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: activeTab === tab.key ? 600 : 400,
              cursor: 'pointer',
              transition: 'all 0.2s',
              border: 'none',
              backgroundColor: activeTab === tab.key ? COLORS.bgPrimary : 'transparent',
              color: activeTab === tab.key ? COLORS.primary : COLORS.textSecondary,
              boxShadow: activeTab === tab.key ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Bulk Actions Bar */}
      {selectedDocuments.length > 0 && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          padding: '10px 16px',
          backgroundColor: COLORS.bgPrimary,
          borderRadius: '10px',
          marginBottom: '12px',
          border: `1px solid ${COLORS.primary}30`,
          boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
        }}>
          <span style={{ fontSize: '14px', color: COLORS.textSecondary, fontWeight: 500 }}>
            {selectedDocuments.length} selected
          </span>
          <Button
            type="link"
            size="small"
            onClick={() => setSelectedDocuments([])}
            style={{ color: COLORS.textMuted }}
          >
            Clear Selection
          </Button>
          <div style={{ flex: 1 }} />
          <Button danger size="small" onClick={handleBatchDelete}>
            Delete Selected
          </Button>
        </div>
      )}

      {/* Document Table */}
      {isLoading ? (
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '300px',
          backgroundColor: COLORS.bgPrimary,
          borderRadius: '12px',
          border: `1px solid ${COLORS.borderColor}`,
        }}>
          <Spin size="large" />
        </div>
      ) : documents.length === 0 && rejectedTasks.length === 0 ? (
        /* Empty State */
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '300px',
          backgroundColor: COLORS.bgPrimary,
          borderRadius: '12px',
          border: `1px solid ${COLORS.borderColor}`,
          padding: '48px',
        }}>
          <FileTextOutlined style={{ fontSize: '48px', color: COLORS.textMuted, marginBottom: '16px' }} />
          <p style={{ fontSize: '16px', color: COLORS.textSecondary, margin: '0 0 8px 0', fontWeight: 600 }}>
            No documents found
          </p>
          <p style={{ fontSize: '14px', color: COLORS.textMuted, margin: '0 0 20px 0' }}>
            {activeTab === 'all' && "You haven't created any documents yet"}
            {activeTab === 'pending_review' && 'You have no documents pending review'}
            {activeTab === 'published' && "You don't have any published documents yet"}
            {activeTab === 'rejected' && 'You have no rejected documents'}
            {activeTab === 'draft' && 'You have no draft documents'}
          </p>
          {canCreateDocument && (
            <button
              onClick={() => navigate('/documents/new')}
              style={{
                padding: '10px 20px',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: 600,
                cursor: 'pointer',
                border: 'none',
                backgroundColor: COLORS.primary,
                color: '#ffffff',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <PlusOutlined />
              Create Document
            </button>
          )}
        </div>
      ) : (
        <div style={{
          backgroundColor: COLORS.bgPrimary,
          borderRadius: '12px',
          border: `1px solid ${COLORS.borderColor}`,
          overflow: 'hidden',
        }}>
          {/* Table Header */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '40px 4fr 120px 70px 100px 80px 80px 140px 80px',
            padding: '12px 16px',
            borderBottom: `1px solid ${COLORS.borderColor}`,
            backgroundColor: COLORS.bgSecondary,
            fontSize: '13px',
            fontWeight: 600,
            color: COLORS.textSecondary,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <input
                type="checkbox"
                checked={selectedDocuments.length === documents.length && documents.length > 0}
                onChange={(e) => handleSelectAll(e.target.checked)}
                style={{ cursor: 'pointer', width: '16px', height: '16px' }}
              />
            </div>
            <div>Document Name</div>
            <div>Category</div>
            <div style={{ textAlign: 'center' }}>Views</div>
            <div>Author</div>
            <div style={{ textAlign: 'center' }}>Status</div>
            <div style={{ textAlign: 'center' }}>Visibility</div>
            <div>Updated</div>
            <div style={{ textAlign: 'center' }}>Actions</div>
          </div>

          {/* Table Body */}
          {documents.map((doc: any) => {
            const categoryName = doc.categoryName || 'Uncategorized';
            const badgeStyle = getCategoryBadgeStyle(categoryName);
            const statusText = doc.status === 'published' ? 'Published' : doc.status === 'pending_review' ? 'Under Review' : doc.status === 'draft' ? 'Draft' : 'Archived';
            const statusColor = doc.status === 'published' ? COLORS.success : doc.status === 'pending_review' ? COLORS.warning : doc.status === 'draft' ? COLORS.secondary : COLORS.textMuted;

            return (
              <div
                key={doc.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 4fr 120px 70px 100px 80px 80px 140px 80px',
                  padding: '14px 16px',
                  borderBottom: `1px solid ${COLORS.borderColor}`,
                  alignItems: 'center',
                  fontSize: '14px',
                  transition: 'background-color 0.15s',
                  cursor: 'pointer',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = COLORS.bgSecondary;
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent';
                }}
                onClick={() => navigate(`/documents/${doc.id}`)}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <input
                    type="checkbox"
                    checked={selectedDocuments.includes(doc.id)}
                    onChange={(e) => {
                      e.stopPropagation();
                      handleSelectDocument(doc.id, e.target.checked);
                    }}
                    style={{ cursor: 'pointer', width: '16px', height: '16px' }}
                  />
                </div>

                {/* Document Name */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
                  {doc.documentType === 2 ? (
                    <FileMarkdownOutlined style={{ fontSize: '20px', color: COLORS.textSecondary, flexShrink: 0 }} />
                  ) : (
                    <FileTextOutlined style={{ fontSize: '20px', color: COLORS.textSecondary, flexShrink: 0 }} />
                  )}
                  <div style={{ minWidth: 0 }}>
                    <div style={{
                      fontSize: '14px',
                      fontWeight: 600,
                      color: COLORS.textPrimary,
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}>
                      {doc.title}
                    </div>
                    <div style={{
                      fontSize: '12px',
                      color: COLORS.textMuted,
                      marginTop: '2px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}>
                      <span>
                        <ClockCircleOutlined style={{ marginRight: '3px', fontSize: '11px' }} />
                        {doc.updatedAt ? dayjs(doc.updatedAt).fromNow() : '—'}
                      </span>
                      {doc.fileSize != null && (
                        <span style={{ color: COLORS.textMuted }}>
                          {doc.fileSize > 1024 * 1024
                            ? `${(doc.fileSize / (1024 * 1024)).toFixed(1)} MB`
                            : doc.fileSize > 1024
                              ? `${(doc.fileSize / 1024).toFixed(1)} KB`
                              : `${doc.fileSize} B`}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Category */}
                <div>
                  <span style={{
                    display: 'inline-block',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    fontSize: '12px',
                    fontWeight: 500,
                    backgroundColor: badgeStyle === 'tech' ? '#dbeafe' : badgeStyle === 'business' ? '#d1fae5' : '#ede9fe',
                    color: badgeStyle === 'tech' ? '#1e40af' : badgeStyle === 'business' ? '#065f46' : '#6b21a8',
                  }}>
                    {getCategoryIcon(categoryName)}
                    <span style={{ marginLeft: '4px' }}>{categoryName}</span>
                  </span>
                </div>

                {/* View Count */}
                <div style={{ textAlign: 'center', color: COLORS.textSecondary, fontSize: '13px' }}>
                  {doc.viewCount || 0}
                </div>

                {/* Author */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <UserAvatar
                    src={doc.author?.avatar}
                    alt=""
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      objectFit: 'cover',
                    }}
                  />
                  <span style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                    {doc.author?.username || doc.authorName || user?.username || '—'}
                  </span>
                </div>

                {/* Status */}
                <div style={{ textAlign: 'center' }}>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    fontSize: '12px',
                    fontWeight: 500,
                    color: statusColor,
                  }}>
                    <span style={{
                      width: '6px',
                      height: '6px',
                      borderRadius: '50%',
                      backgroundColor: statusColor,
                      display: 'inline-block',
                    }} />
                    {statusText}
                  </span>
                </div>

                {/* Visibility */}
                <div style={{ textAlign: 'center' }}>
                  <span style={{
                    fontSize: '12px',
                    color: doc.isPublic ? COLORS.success : COLORS.warning,
                    fontWeight: 500,
                  }}>
                    {doc.isPublic ? 'Visible to Everyone' : 'Team Visible'}
                  </span>
                </div>

                {/* Publish Time */}
                <div style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                  {doc.updatedAt ? dayjs(doc.updatedAt).format('YYYY-MM-DD HH:mm') : '—'}
                </div>

                {/* Actions */}
                <div style={{ textAlign: 'center' }}>
                  <Dropdown
                    menu={{
                      items: getActionMenuItems(doc),
                      onClick: ({ key }: any) => handleDocumentAction(key, doc.id),
                    }}
                    trigger={['click']}
                  >
                    <Button
                      type="text"
                      size="small"
                      icon={<MoreOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </Dropdown>
                </div>
              </div>
            );
          })}

          {/* Render rejected review tasks if on rejected tab */}
          {activeTab === 'rejected' && rejectedTasks.map((task: ReviewTask) => (
            <div
              key={task.id}
              style={{
                display: 'grid',
                gridTemplateColumns: '40px 4fr 120px 70px 100px 80px 80px 140px 80px',
                padding: '14px 16px',
                borderBottom: `1px solid ${COLORS.borderColor}`,
                alignItems: 'center',
                fontSize: '14px',
                transition: 'background-color 0.15s',
                cursor: 'pointer',
                backgroundColor: '#fef2f2',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = '#fee2e2';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = '#fef2f2';
              }}
              onClick={() => navigate(`/documents/${task.documentId}`)}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }} />
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
                <FileTextOutlined style={{ fontSize: '20px', color: COLORS.danger, flexShrink: 0 }} />
                <div style={{ minWidth: 0 }}>
                  <div style={{
                    fontSize: '14px',
                    fontWeight: 600,
                    color: COLORS.textPrimary,
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}>
                    {task.documentTitle}
                  </div>
                  {task.comment && (
                    <div style={{
                      fontSize: '12px',
                      color: COLORS.danger,
                      marginTop: '2px',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}>
                      Rejection reason: {task.comment}
                    </div>
                  )}
                  <div style={{ fontSize: '12px', color: COLORS.textMuted, marginTop: '2px' }}>
                    {task.createdAt ? dayjs(task.createdAt).fromNow() : '—'}
                  </div>
                </div>
              </div>
              <div>
                {task.categoryName ? (
                  <span style={{
                    display: 'inline-block',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    fontSize: '12px',
                    fontWeight: 500,
                    backgroundColor: '#dbeafe',
                    color: '#1e40af',
                  }}>
                    {getCategoryIcon(task.categoryName)}
                    <span style={{ marginLeft: '4px' }}>{task.categoryName}</span>
                  </span>
                ) : (
                  <span style={{ color: COLORS.textMuted, fontSize: '12px' }}>Uncategorized</span>
                )}
              </div>
              <div style={{ textAlign: 'center', color: COLORS.textSecondary, fontSize: '13px' }}>—</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                {task.documentAuthor?.avatar ? (
                  <UserAvatar
                    src={task.documentAuthor.avatar}
                    alt=""
                    style={{ width: '24px', height: '24px', borderRadius: '50%', objectFit: 'cover' }}
                  />
                ) : (
                  <div style={{
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    backgroundColor: COLORS.bgTertiary,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '11px',
                    color: COLORS.textMuted,
                    flexShrink: 0,
                  }}>
                    {(task.documentAuthor?.username || user?.username || '—').charAt(0)}
                  </div>
                )}
                <span style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                  {task.documentAuthor?.username || user?.username || '—'}
                </span>
              </div>
              <div style={{ textAlign: 'center' }}>
                <span style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: '12px',
                  fontWeight: 500,
                  color: COLORS.danger,
                }}>
                  <span style={{
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor: COLORS.danger,
                    display: 'inline-block',
                  }} />
                  Rejected
                </span>
              </div>
              <div style={{ textAlign: 'center' }}>
                <span style={{ fontSize: '12px', color: COLORS.textMuted }}>—</span>
              </div>
              <div style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                {task.createdAt ? dayjs(task.createdAt).format('YYYY-MM-DD HH:mm') : '—'}
              </div>
              <div style={{ textAlign: 'center' }}>
                <Button
                  type="primary"
                  size="small"
                  style={{ fontSize: '12px' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`/documents/${task.documentId}/edit`);
                  }}
                >
                  Re-edit
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {total > 0 && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: '16px',
          padding: '12px 0',
        }}>
          <div style={{ fontSize: '14px', color: COLORS.textSecondary }}>
            Showing {total === 0 ? 0 : (currentPage - 1) * pageSize + 1}-{Math.min(currentPage * pageSize, total)} of {total} documents
          </div>
          <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage <= 1}
              style={{
                padding: '8px 12px',
                borderRadius: '8px',
                border: `1px solid ${COLORS.borderColor}`,
                backgroundColor: currentPage <= 1 ? COLORS.bgSecondary : COLORS.bgPrimary,
                color: currentPage <= 1 ? COLORS.textMuted : COLORS.textPrimary,
                fontSize: '14px',
                cursor: currentPage <= 1 ? 'not-allowed' : 'pointer',
                fontWeight: 500,
              }}
            >
              Previous
            </button>
            {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
              let pageNum: number;
              if (totalPages <= 7) {
                pageNum = i + 1;
              } else if (currentPage <= 4) {
                pageNum = i + 1;
              } else if (currentPage >= totalPages - 3) {
                pageNum = totalPages - 6 + i;
              } else {
                pageNum = currentPage - 3 + i;
              }
              return (
                <button
                  key={pageNum}
                  onClick={() => handlePageChange(pageNum)}
                  style={{
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: currentPage === pageNum ? `1px solid ${COLORS.primary}` : `1px solid ${COLORS.borderColor}`,
                    backgroundColor: currentPage === pageNum ? COLORS.primary : COLORS.bgPrimary,
                    color: currentPage === pageNum ? '#ffffff' : COLORS.textPrimary,
                    fontSize: '14px',
                    fontWeight: currentPage === pageNum ? 600 : 400,
                    cursor: 'pointer',
                    minWidth: '38px',
                    textAlign: 'center',
                  }}
                >
                  {pageNum}
                </button>
              );
            })}
            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage >= totalPages}
              style={{
                padding: '8px 12px',
                borderRadius: '8px',
                border: `1px solid ${COLORS.borderColor}`,
                backgroundColor: currentPage >= totalPages ? COLORS.bgSecondary : COLORS.bgPrimary,
                color: currentPage >= totalPages ? COLORS.textMuted : COLORS.textPrimary,
                fontSize: '14px',
                cursor: currentPage >= totalPages ? 'not-allowed' : 'pointer',
                fontWeight: 500,
              }}
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default MyDocumentsPage;
