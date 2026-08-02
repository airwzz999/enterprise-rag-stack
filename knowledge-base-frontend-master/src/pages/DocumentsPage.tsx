import React, { useEffect, useState } from 'react';
import {
  Input,
  Button,
  Modal,
  Dropdown,
} from 'antd';
import { App } from 'antd';
import {
  MoreOutlined,
  EyeOutlined,
  FileTextOutlined,
  DeleteOutlined,
  ExportOutlined,
  EditOutlined,
  ShareAltOutlined,
  DownloadOutlined,
  StarOutlined,
  FileMarkdownOutlined,
} from '@ant-design/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore, useDocumentStore, useFavoriteStore, useTeamStore } from '@/stores';
import { formatFileSize } from '@/utils';
import { documentService, categoryService } from '@/services';
import UserAvatar from '@/components/common/UserAvatar';
import TeamIcon from '@/components/common/TeamIcon';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { PERMISSIONS, hasPermission } from '@/utils/permission';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Search } = Input;

// Style constants, following the prototype design exactly
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

export const DocumentsPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const user = useAuthStore((state) => state.user);
  const canCreateDocument = hasPermission(user, PERMISSIONS.documentCreate);
  const canEditDocument = hasPermission(user, PERMISSIONS.documentEdit);
  const canDeleteDocument = hasPermission(user, PERMISSIONS.documentDelete);
  const {
    documents,
    isLoading,
    total,
    currentPage,
    pageSize,
    fetchDocuments,
    setFilter,
  } = useDocumentStore();
  const { toggleFavorite } = useFavoriteStore();
  const { teamTree, selectedTeam, setSelectedTeam } = useTeamStore();

  const [selectedDocuments, setSelectedDocuments] = useState<string[]>([]);
  const [batchLoading, setBatchLoading] = useState(false);
  const [categories, setCategories] = useState<any[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>();
  const [selectedTag, setSelectedTag] = useState<string>('All');
  const [selectedStatus, setSelectedStatus] = useState<string>();
  const [sortBy, setSortBy] = useState<string>('updatedAt');
  const [searchKeyword, setSearchKeyword] = useState<string>('');
  const [, setFavoriteLoading] = useState<string | null>(null);

  // Runs on component mount: read category and team filters from URL params
  useEffect(() => {
    fetchCategories();
  }, []);

  // Sync quick tag highlighting: update when selectedCategory or categories change
  useEffect(() => {
    if (!selectedCategory) {
      setSelectedTag('All');
    } else if (categories.length > 0) {
      const matched = categories.find((cat: any) => String(cat.id) === String(selectedCategory));
      if (matched) {
        setSelectedTag(matched.name);
      }
    }
  }, [selectedCategory, categories]);

  // Listen for URL param changes (the component does not remount when the sidebar navigates to the same page)
  useEffect(() => {
    const categoryIdFromUrl = searchParams.get('category');
    const teamIdFromUrl = searchParams.get('team');

    // Sync the team workspace context to the store
    if (teamIdFromUrl) {
      const findTeam = (teams: any[], id: string): any | undefined => {
        for (const t of teams) {
          if (String(t.id) === String(id)) return t;
          if (t.children?.length) {
            const found = findTeam(t.children, id);
            if (found) return found;
          }
        }
        return undefined;
      };
      const found = findTeam(teamTree, teamIdFromUrl);
      if (found) {
        setSelectedTeam(found);
      }
    } else {
      setSelectedTeam(null);
    }

    // Sync URL params to filter state and fetch data
    const newFilter: any = { status: 1, page: 1, pageSize: 12, sortBy: 'publishTime', sortOrder: 'desc' as const };
    if (categoryIdFromUrl) {
      newFilter.categoryId = categoryIdFromUrl;
      setSelectedCategory(categoryIdFromUrl);
    } else {
      setSelectedCategory(undefined);
    }
    if (teamIdFromUrl) {
      newFilter.teamId = teamIdFromUrl;
    }

    setFilter(newFilter);
    fetchDocuments(newFilter);
  }, [searchParams, teamTree]);

  const fetchCategories = async () => {
    try {
      const data = await categoryService.getCategoryTree();
      setCategories(flattenCategories(data));
    } catch (error) {
      console.error('Failed to fetch categories:', error);
    }
  };

  const flattenCategories = (categories: any[], prefix = ''): any[] => {
    const result: any[] = [];
    categories.forEach((cat) => {
      result.push({
        id: cat.id,
        name: cat.name,
        label: prefix ? `${prefix} / ${cat.name}` : cat.name,
        documentCount: cat.documentCount || 0,
      });
      if (cat.children && Array.isArray(cat.children) && cat.children.length > 0) {
        result.push(...flattenCategories(cat.children, cat.name));
      }
    });
    return result;
  };

  // Build the base filter (preserving teamId, search keyword, selected category, and status from the URL)
  const buildFilter = (overrides: Record<string, any> = {}): any => {
    const teamIdFromUrl = searchParams.get('team');
    const base = { status: 1, page: 1, pageSize: 12, sortBy: 'publishTime' as const, sortOrder: 'desc' as const };
    return {
      ...base,
      ...(teamIdFromUrl ? { teamId: teamIdFromUrl } : {}),
      ...(searchKeyword ? { keyword: searchKeyword } : {}),
      ...(selectedCategory ? { categoryId: selectedCategory } : {}),
      ...(selectedStatus ? { status: selectedStatus === 'published' ? 1 : selectedStatus === 'archived' ? 2 : selectedStatus === 'draft' ? 0 : selectedStatus === 'pending_review' ? 3 : 1 } : {}),
      ...overrides,
    };
  };

  // Search handling
  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    const newFilter = buildFilter({ keyword: value || undefined });
    setFilter(newFilter);
    fetchDocuments(newFilter);
  };

  // Category filter
  const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    setSelectedCategory(value);
    // Sync quick tag highlighting
    if (!value) {
      setSelectedTag('All');
    } else {
      const matchedCat = categories.find((cat: any) => String(cat.id) === value);
      setSelectedTag(matchedCat ? matchedCat.name : '');
    }
    const newFilter = buildFilter({ categoryId: value || undefined });
    setFilter(newFilter);
    fetchDocuments(newFilter);
  };

  // Status filter
  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    setSelectedStatus(value);
    // If "All" or an empty value is selected, show published documents by default
    const statusValue = value === 'published' ? 1 : value === 'archived' ? 2 : value === 'draft' ? 0 : value === 'pending_review' ? 3 : 1;
    const newFilter = buildFilter({ status: statusValue });
    setFilter(newFilter);
    fetchDocuments(newFilter);
  };

  // Sort filter
  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    setSortBy(value);
    const newFilter = buildFilter({ sortBy: value as 'updatedAt' | 'createdAt' | 'publishTime' });
    setFilter(newFilter);
    fetchDocuments(newFilter);
  };

  // Pagination handling
  const handlePageChange = (page: number, size?: number) => {
    const newPageSize = size || pageSize;
    const newFilter = buildFilter({ page, pageSize: newPageSize });
    setFilter(newFilter);
    fetchDocuments(newFilter);
  };

  // Helper function: process document data returned by the backend
  const normalizeDocument = (doc: any): any => {
    return {
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
    };
  };

  const normalizedDocuments = documents.map(normalizeDocument);

  // Get category name (prefer the categoryName from the API, fall back to local lookup)
  const getCategoryName = (categoryId?: string | number, apiCategoryName?: string) => {
    if (apiCategoryName) return apiCategoryName;
    if (categoryId == null) return 'Uncategorized';
    const cat = categories.find(c => String(c.id) === String(categoryId));
    return cat?.name || 'Uncategorized';
  };

  // Get category badge style (based on category name)
  const getCategoryBadgeStyle = (categoryName: string) => {
    const name = categoryName.toLowerCase();
    if (name.includes('technical') || name.includes('development')) {
      return 'tech';
    }
    if (name.includes('business')) {
      return 'business';
    }
    return 'ai';
  };

  // Handle checkbox selection
  const handleSelectDocument = (documentId: string, checked: boolean) => {
    if (checked) {
      setSelectedDocuments([...selectedDocuments, documentId]);
    } else {
      setSelectedDocuments(selectedDocuments.filter(id => id !== documentId));
    }
  };

  // Select all / deselect all
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedDocuments(normalizedDocuments.map(doc => doc.id));
    } else {
      setSelectedDocuments([]);
    }
  };

  // Bulk delete
  const handleBatchDelete = async () => {
    if (selectedDocuments.length === 0) {
      message.warning('Please select documents to delete first');
      return;
    }

    Modal.confirm({
      title: 'Confirm Deletion',
      content: `Are you sure you want to delete the selected ${selectedDocuments.length} document(s)?`,
      onOk: async () => {
        setBatchLoading(true);
        try {
          await Promise.all(selectedDocuments.map(id => documentService.deleteDocument(id)));
          message.success('Deleted successfully');
          setSelectedDocuments([]);
          fetchDocuments(buildFilter());
        } catch (error) {
          message.error('Delete failed');
        } finally {
          setBatchLoading(false);
        }
      },
    });
  };

  // Delete a single document
  const handleDeleteDocument = (documentId: string) => {
    Modal.confirm({
      title: 'Confirm Deletion',
      content: 'Are you sure you want to delete this document?',
      onOk: async () => {
        try {
          await documentService.deleteDocument(documentId);
          message.success('Deleted successfully');
          fetchDocuments(buildFilter());
        } catch (error) {
          message.error('Delete failed');
        }
      },
    });
  };

  // Document actions
  const handleDocumentAction = async (action: string, documentId: string, e?: React.MouseEvent) => {
    // Prevent event bubbling to avoid triggering the row click event
    if (e) {
      e.stopPropagation();
    }

    switch (action) {
      case 'view':
        navigate(`/documents/${documentId}`);
        break;
      case 'edit':
        navigate(`/documents/${documentId}/edit`);
        break;
      case 'share':
        try {
          const shareData = {
            documentId: documentId,
            shareType: 1,
            expireType: 1,
            accessLimit: 0,
            requirePassword: 0,
          };
          const result = await documentService.createShare(shareData);
          const shareUrl = `${window.location.origin}/share/${result.shareId}`;
          await navigator.clipboard.writeText(shareUrl);
          message.success('Share link copied to clipboard');
        } catch (error: any) {
          const errorMessage = error?.message || 'Operation failed';
          message.error(errorMessage);
        }
        break;
      case 'favorite':
        setFavoriteLoading(documentId);
        try {
          const newStatus = await toggleFavorite(documentId);
          message.success(newStatus ? 'Added to favorites' : 'Removed from favorites');
        } catch (error) {
          console.error('Favorite operation failed:', error);
          message.error('Operation failed, please try again');
        } finally {
          setFavoriteLoading(null);
        }
        break;
      case 'download':
        message.warning('The download feature is not yet implemented');
        break;
      case 'delete':
        handleDeleteDocument(documentId);
        break;
    }
  };

  // Action menu items
  const getActionMenuItems = () => {
    const items: any[] = [
      {
        key: 'view',
        label: 'View Document',
        icon: <EyeOutlined />,
      },
      {
        key: 'share',
        label: 'Share Document',
        icon: <ShareAltOutlined />,
      },
      {
        key: 'favorite',
        label: 'Add to Favorites',
        icon: <StarOutlined />,
      },
      {
        key: 'download',
        label: 'Download Document',
        icon: <DownloadOutlined />,
      },
    ];
    if (canEditDocument) {
      items.splice(1, 0, {
        key: 'edit',
        label: 'Edit Document',
        icon: <EditOutlined />,
      });
    }
    if (canDeleteDocument) {
      items.push(
        {
          type: 'divider',
        },
        {
          key: 'delete',
          label: 'Delete Document',
          icon: <DeleteOutlined />,
          danger: true,
        }
      );
    }
    return items;
  };

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
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <h1 style={{
            fontSize: '24px',
            fontWeight: 700,
            color: COLORS.textPrimary,
            margin: 0,
          }}>
            Document Center
          </h1>
          {selectedTeam && (
            <span style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 12px',
              borderRadius: '20px',
              background: 'linear-gradient(135deg, #eff6ff, #dbeafe)',
              color: '#3b82f6',
              fontSize: '13px',
              fontWeight: 500,
              border: '1px solid #bfdbfe',
            }}>
              <TeamIcon icon={selectedTeam.icon} variant="sidebar" />
              {selectedTeam.teamName || selectedTeam.name}
            </span>
          )}
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          {canCreateDocument && (
            <button
              onClick={() => navigate('/documents/import')}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: 600,
                cursor: 'pointer',
                transition: 'all 0.2s',
                border: `1px solid ${COLORS.borderColor}`,
                backgroundColor: COLORS.bgPrimary,
                color: COLORS.textPrimary,
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = COLORS.bgTertiary;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = COLORS.bgPrimary;
              }}
            >
              <ExportOutlined />
              Import Document
            </button>
          )}
          {canCreateDocument && (
            <button
              onClick={() => {
                const teamId = searchParams.get('team');
                navigate(teamId ? `/documents/new?team=${teamId}` : '/documents/new');
              }}
              style={{
                padding: '10px 20px',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: 600,
                cursor: 'pointer',
                transition: 'all 0.2s',
                border: 'none',
                background: 'linear-gradient(135deg, #2563eb, #1e40af)',
                color: 'white',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                boxShadow: '0 4px 12px rgba(37, 99, 235, 0.3)',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-1px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(37, 99, 235, 0.4)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(37, 99, 235, 0.3)';
              }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              New Document
            </button>
          )}
        </div>
      </div>

      {/* Filter bar */}
      <div style={{
        background: COLORS.bgPrimary,
        borderRadius: '16px',
        padding: '16px',
        marginBottom: '16px',
        border: `1px solid ${COLORS.borderColor}`,
        boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.08)',
      }}>
        <div style={{
          display: 'flex',
          gap: '12px',
          alignItems: 'center',
          flexWrap: 'wrap',
        }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <Search
              placeholder="Search by title, summary, or tags..."
              allowClear
              value={searchKeyword}
              onChange={(e) => {
                setSearchKeyword(e.target.value);
                if (!e.target.value) {
                  handleSearch('');
                }
              }}
              onSearch={handleSearch}
              style={{ width: 260 }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{
              fontSize: '14px',
              fontWeight: 600,
              color: COLORS.textSecondary,
            }}>
              Category:
            </span>
            <select
              value={selectedCategory || ''}
              onChange={handleCategoryChange}
              style={{
                padding: '8px 16px',
                border: `1px solid ${COLORS.borderColor}`,
                borderRadius: '8px',
                fontSize: '14px',
                backgroundColor: COLORS.bgPrimary,
                color: COLORS.textPrimary,
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              <option value="">All Categories</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{
              fontSize: '14px',
              fontWeight: 600,
              color: COLORS.textSecondary,
            }}>
              Status:
            </span>
            <select
              value={selectedStatus || ''}
              onChange={handleStatusChange}
              style={{
                padding: '8px 16px',
                border: `1px solid ${COLORS.borderColor}`,
                borderRadius: '8px',
                fontSize: '14px',
                backgroundColor: COLORS.bgPrimary,
                color: COLORS.textPrimary,
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              <option value="">All Statuses</option>
              <option value="published">Published</option>
              <option value="pending_review">Pending Review</option>
              <option value="draft">Draft</option>
              <option value="archived">Archived</option>
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{
              fontSize: '14px',
              fontWeight: 600,
              color: COLORS.textSecondary,
            }}>
              Sort:
            </span>
            <select
              value={sortBy}
              onChange={handleSortChange}
              style={{
                padding: '8px 16px',
                border: `1px solid ${COLORS.borderColor}`,
                borderRadius: '8px',
                fontSize: '14px',
                backgroundColor: COLORS.bgPrimary,
                color: COLORS.textPrimary,
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
            >
              <option value="updatedAt">Recently Updated</option>
              <option value="createdAt">Recently Created</option>
              <option value="viewCount">Most Viewed</option>
              <option value="likeCount">Most Liked</option>
            </select>
          </div>

          {selectedDocuments.length > 0 && (
            <div style={{ marginLeft: 'auto', display: 'flex', gap: '8px', alignItems: 'center' }}>
              <span style={{ fontSize: '14px', color: COLORS.textSecondary }}>
                {selectedDocuments.length} item(s) selected
              </span>
              <Button size="small" onClick={() => setSelectedDocuments([])}>
                Clear Selection
              </Button>
              <Button
                danger
                size="small"
                icon={<DeleteOutlined />}
                loading={batchLoading}
                onClick={handleBatchDelete}
              >
                Bulk Delete
              </Button>
            </div>
          )}
        </div>

        {/* Tag filter */}
        <div style={{
          display: 'flex',
          gap: '8px',
          flexWrap: 'wrap',
          marginTop: '12px',
        }}>
          {(() => {
            // Use top-level categories from the category data as quick tags
            const topCategories = categories.filter((cat: any) => cat.name === cat.label);
            const tags = ['All', ...topCategories.slice(0, 8).map((cat: any) => cat.name)];
            return tags.map((tag) => {
              const isActive = selectedTag === tag;
              return (
                <span
                  key={tag}
                  style={{
                    padding: '4px 10px',
                    borderRadius: '8px',
                    fontSize: '13px',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    border: `1px solid ${isActive ? COLORS.primary : COLORS.borderColor}`,
                    backgroundColor: isActive ? COLORS.primary : COLORS.bgPrimary,
                    color: isActive ? 'white' : COLORS.textSecondary,
                  }}
                  onMouseEnter={(e) => {
                    if (!isActive) {
                      e.currentTarget.style.backgroundColor = COLORS.primary;
                      e.currentTarget.style.color = 'white';
                      e.currentTarget.style.borderColor = COLORS.primary;
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!isActive) {
                      e.currentTarget.style.backgroundColor = COLORS.bgPrimary;
                      e.currentTarget.style.color = COLORS.textSecondary;
                      e.currentTarget.style.borderColor = COLORS.borderColor;
                    }
                  }}
                  onClick={() => {
                    setSelectedTag(tag);
                    if (tag === 'All') {
                      setSelectedCategory(undefined);
                      const newFilter = buildFilter({ categoryId: undefined });
                      setFilter(newFilter);
                      fetchDocuments(newFilter);
                    } else {
                      // Match by name against the category list to get the category ID
                      const matchedCategory = categories.find(
                        (cat: any) => cat.name === tag
                      );
                      if (matchedCategory) {
                        setSelectedCategory(String(matchedCategory.id));
                        const newFilter = buildFilter({ categoryId: matchedCategory.id });
                        setFilter(newFilter);
                        fetchDocuments(newFilter);
                      }
                    }
                  }}
                >
                  {tag}
                </span>
              );
            });
          })()}
        </div>
      </div>

      {/* Document list */}
      {isLoading ? (
        <div style={{
          background: COLORS.bgPrimary,
          borderRadius: '16px',
          border: `1px solid ${COLORS.borderColorLight}`,
          padding: '60px 24px',
          textAlign: 'center',
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.08)',
        }}>
          <FileTextOutlined style={{ fontSize: 48, color: COLORS.textMuted }} />
          <div style={{ marginTop: 16, fontSize: 16, color: COLORS.textSecondary }}>
            Loading...
          </div>
        </div>
      ) : normalizedDocuments.length === 0 ? (
        <div style={{
          background: COLORS.bgPrimary,
          borderRadius: '16px',
          border: `1px solid ${COLORS.borderColorLight}`,
          padding: '80px 24px',
          textAlign: 'center',
          boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.08)',
        }}>
          <FileTextOutlined style={{ fontSize: 64, color: '#d1d5db' }} />
          <div style={{
            marginTop: 16,
            marginBottom: 8,
            fontSize: 20,
            fontWeight: 600,
            color: COLORS.textPrimary,
          }}>
            No documents yet
          </div>
          <div style={{
            fontSize: 14,
            color: COLORS.textSecondary,
            marginBottom: 24,
          }}>
            Start by creating your first document
          </div>
          {canCreateDocument && (
            <Button
              type="primary"
              icon={<FileMarkdownOutlined />}
              onClick={() => {
                const teamId = searchParams.get('team');
                navigate(teamId ? `/documents/new?team=${teamId}` : '/documents/new');
              }}
            >
              Create Document
            </Button>
          )}
        </div>
      ) : (
        <>
          {/* Document table */}
          <div style={{
            background: COLORS.bgPrimary,
            borderRadius: '16px',
            border: `1px solid ${COLORS.borderColorLight}`,
            overflow: 'hidden',
            boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.08)',
          }}>
            {/* Table header */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: '40px 4fr 120px 70px 100px 80px 80px 140px 80px',
              gap: '12px',
              padding: '12px 20px',
              backgroundColor: COLORS.bgTertiary,
              borderBottom: `1px solid ${COLORS.borderColor}`,
              fontSize: '13px',
              fontWeight: 600,
              color: COLORS.textSecondary,
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
              alignItems: 'center',
            }}>
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <input
                  type="checkbox"
                  checked={selectedDocuments.length > 0 && selectedDocuments.length === normalizedDocuments.length}
                  onChange={(e) => handleSelectAll(e.target.checked)}
                  style={{
                    width: '18px',
                    height: '18px',
                    border: `2px solid ${COLORS.borderColor}`,
                    borderRadius: '4px',
                    cursor: 'pointer',
                  }}
                />
              </div>
              <div>Document Name</div>
              <div>Category</div>
              <div>Views</div>
              <div>Author</div>
              <div>Status</div>
              <div>Visibility</div>
              <div>Published At</div>
              <div style={{ display: 'flex', justifyContent: 'center' }}>Actions</div>
            </div>

            {/* Table rows */}
            {normalizedDocuments.map((doc) => (
              <div
                key={`${doc.id}-${doc.createdAt}`}
                onClick={(e) => {
                  e.preventDefault();
                  window.open(`/documents/${doc.id}`, '_blank');
                }}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 4fr 120px 70px 100px 80px 80px 140px 80px',
                  gap: '12px',
                  padding: '16px 20px',
                  borderBottom: `1px solid ${COLORS.borderColorLight}`,
                  alignItems: 'center',
                  cursor: 'pointer',
                  transition: 'all 0.15s',
                  position: 'relative',
                  minHeight: '64px',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = COLORS.bgSecondary;
                  e.currentTarget.style.transform = 'translateX(2px)';
                  const leftBorder = e.currentTarget.querySelector('.left-border');
                  if (leftBorder) {
                    (leftBorder as HTMLElement).style.opacity = '1';
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent';
                  e.currentTarget.style.transform = 'translateX(0)';
                  const leftBorder = e.currentTarget.querySelector('.left-border');
                  if (leftBorder) {
                    (leftBorder as HTMLElement).style.opacity = '0';
                  }
                }}
              >
                {/* Left accent bar */}
                <div
                  className="left-border"
                  style={{
                    position: 'absolute',
                    left: 0,
                    top: 0,
                    bottom: 0,
                    width: '3px',
                    background: 'linear-gradient(180deg, #2563eb, #8b5cf6)',
                    opacity: 0,
                    transition: 'opacity 0.15s',
                  }}
                />

                {/* Checkbox */}
                <div style={{ display: 'flex', justifyContent: 'center' }}>
                  <input
                    type="checkbox"
                    checked={selectedDocuments.includes(doc.id)}
                    onChange={(e) => {
                      e.stopPropagation();
                      handleSelectDocument(doc.id, e.target.checked);
                    }}
                    onClick={(e) => e.stopPropagation()}
                    style={{
                      width: '18px',
                      height: '18px',
                      border: `2px solid ${COLORS.borderColor}`,
                      borderRadius: '4px',
                      cursor: 'pointer',
                    }}
                  />
                </div>

                {/* Document name */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <div style={{
                      width: '36px',
                      height: '36px',
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      backgroundColor: COLORS.bgTertiary,
                      color: COLORS.textSecondary,
                    }}>
                      <FileMarkdownOutlined style={{ fontSize: 20 }} />
                    </div>
                    <div>
                      <div style={{
                        fontSize: '14px',
                        fontWeight: 600,
                        color: COLORS.textPrimary,
                        marginBottom: '4px',
                        lineHeight: '1.4',
                      }}>
                        {doc.title}
                      </div>
                      <div style={{
                        fontSize: '12px',
                        color: COLORS.textMuted,
                        display: 'flex',
                        gap: '8px',
                        alignItems: 'center',
                      }}>
                        <span>Updated {dayjs(doc.updatedAt).fromNow()}</span>
                        <span>·</span>
                        <span>{formatFileSize(doc.fileSize || doc.contentLength || 0)}</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Category */}
                <div>
                  {(() => {
                    const categoryDisplayName = doc.categoryName || getCategoryName(doc.categoryId);
                    if (!categoryDisplayName || categoryDisplayName === 'Uncategorized') {
                      return (
                        <span style={{
                          padding: '3px 10px',
                          borderRadius: '8px',
                          fontSize: '12px',
                          fontWeight: 600,
                          backgroundColor: 'rgba(100, 116, 139, 0.1)',
                          color: COLORS.textMuted,
                        }}>
                          Uncategorized
                        </span>
                      );
                    }
                    const badgeStyle = getCategoryBadgeStyle(categoryDisplayName);
                    return (
                      <span style={{
                        padding: '3px 10px',
                        borderRadius: '8px',
                        fontSize: '12px',
                        fontWeight: 600,
                        textTransform: 'uppercase',
                        backgroundColor: badgeStyle === 'tech'
                          ? 'rgba(37, 99, 235, 0.1)'
                          : badgeStyle === 'business'
                          ? 'rgba(16, 185, 129, 0.1)'
                          : 'rgba(139, 92, 246, 0.1)',
                        color: badgeStyle === 'tech'
                          ? COLORS.primary
                          : badgeStyle === 'business'
                          ? COLORS.success
                          : COLORS.secondary,
                      }}>
                        {categoryDisplayName}
                      </span>
                    );
                  })()}
                </div>

                {/* Views */}
                <div>
                  <span style={{ fontSize: '13px', color: COLORS.textPrimary, fontWeight: 500 }}>
                    {doc.viewCount?.toLocaleString() || 0}
                  </span>
                </div>

                {/* Author */}
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <UserAvatar
                      src={doc.author?.avatar}
                      alt={doc.author?.username || doc.authorName || ''}
                      style={{
                        width: '24px',
                        height: '24px',
                        borderRadius: '50%',
                        objectFit: 'cover',
                      }}
                    />
                    <span style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                      {doc.author?.username || doc.authorName || 'Unknown'}
                    </span>
                  </div>
                </div>

                {/* Status */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    display: 'inline-block',
                    backgroundColor: doc.status === 'published' ? COLORS.success : doc.status === 'pending_review' ? COLORS.warning : COLORS.textMuted,
                  }} />
                  <span style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                    {doc.status === 'published' ? 'Published' : doc.status === 'pending_review' ? 'Pending Review' : doc.status === 'draft' ? 'Draft' : 'Archived'}
                  </span>
                </div>

                {/* Visibility */}
                <div>
                  <span style={{
                    padding: '3px 10px',
                    borderRadius: '8px',
                    fontSize: '12px',
                    fontWeight: 500,
                    backgroundColor: doc.isPublic === 1 || doc.isPublic === true
                      ? 'rgba(16, 185, 129, 0.1)'
                      : 'rgba(245, 158, 11, 0.1)',
                    color: doc.isPublic === 1 || doc.isPublic === true
                      ? COLORS.success
                      : '#d97706',
                  }}>
                    {doc.isPublic === 1 || doc.isPublic === true ? 'Visible to Everyone' : 'Visible to Team'}
                  </span>
                </div>

                {/* Published time */}
                <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                  <span style={{ fontSize: '13px', color: COLORS.textSecondary }}>
                    {doc.publishTime ? dayjs(doc.publishTime).format('YYYY-MM-DD HH:mm:ss') : doc.status === 'published' || doc.status === 'pending_review' ? dayjs(doc.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-'}
                  </span>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', justifyContent: 'center', position: 'relative' }}>
                  <Dropdown
                    menu={{
                      items: getActionMenuItems().map(item => ({
                        ...item,
                        onClick: ({ domEvent }) => {
                          handleDocumentAction(item.key, doc.id, domEvent as React.MouseEvent);
                        },
                        danger: item.danger,
                      })),
                    }}
                    trigger={['click']}
                  >
                    <div
                      onClick={(e) => e.stopPropagation()}
                      style={{
                        width: '28px',
                        height: '28px',
                        borderRadius: '8px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: 'pointer',
                        color: COLORS.textMuted,
                        transition: 'all 0.2s',
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.backgroundColor = COLORS.bgTertiary;
                        e.currentTarget.style.color = COLORS.textPrimary;
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.backgroundColor = 'transparent';
                        e.currentTarget.style.color = COLORS.textMuted;
                      }}
                    >
                      <MoreOutlined style={{ fontSize: 16 }} />
                    </div>
                  </Dropdown>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px 20px',
            backgroundColor: COLORS.bgPrimary,
            borderTop: `1px solid ${COLORS.borderColor}`,
            borderRadius: '0 0 16px 16px',
          }}>
            <span style={{ fontSize: '14px', color: COLORS.textSecondary }}>
              Showing {(currentPage - 1) * pageSize + 1}-{Math.min(currentPage * pageSize, total)} of {total.toLocaleString()} documents
            </span>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button
                onClick={() => currentPage > 1 && handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                style={{
                  padding: '6px 10px',
                  border: `1px solid ${COLORS.borderColor}`,
                  borderRadius: '8px',
                  backgroundColor: COLORS.bgPrimary,
                  color: currentPage === 1 ? COLORS.textMuted : COLORS.textSecondary,
                  cursor: currentPage === 1 ? 'not-allowed' : 'pointer',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  opacity: currentPage === 1 ? 0.5 : 1,
                }}
                onMouseEnter={(e) => {
                  if (currentPage !== 1) {
                    e.currentTarget.style.backgroundColor = COLORS.bgTertiary;
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = COLORS.bgPrimary;
                }}
              >
                Previous
              </button>

              {/* Compute the page numbers to display */}
              {(() => {
                const totalPages = Math.ceil(total / pageSize);
                const pages: (number | string)[] = [];

                // Always show the first page
                if (totalPages > 0) {
                  pages.push(1);
                }

                // If the current page is more than 2 away from the first page, add an ellipsis
                if (currentPage > 3) {
                  pages.push('...');
                }

                // Show the page numbers near the current page
                for (let i = Math.max(2, currentPage - 1); i <= Math.min(totalPages - 1, currentPage + 1); i++) {
                  pages.push(i);
                }

                // If the last page is more than 2 away from the current page, add an ellipsis
                if (currentPage < totalPages - 2) {
                  pages.push('...');
                }

                // Always show the last page
                if (totalPages > 1) {
                  pages.push(totalPages);
                }

                return pages.map((page, index) => {
                  if (page === '...') {
                    return (
                      <span
                        key={`ellipsis-${index}`}
                        style={{
                          padding: '8px 4px',
                          color: COLORS.textSecondary,
                          fontSize: '14px',
                        }}
                      >
                        ...
                      </span>
                    );
                  }

                  return (
                    <button
                      key={page}
                      onClick={() => handlePageChange(page as number)}
                      style={{
                        padding: '8px 12px',
                        border: `1px solid ${currentPage === page ? COLORS.primary : COLORS.borderColor}`,
                        borderRadius: '8px',
                        backgroundColor: currentPage === page ? COLORS.primary : COLORS.bgPrimary,
                        color: currentPage === page ? 'white' : COLORS.textSecondary,
                        cursor: 'pointer',
                        fontSize: '14px',
                        transition: 'all 0.2s',
                      }}
                      onMouseEnter={(e) => {
                        if (currentPage !== page) {
                          e.currentTarget.style.backgroundColor = COLORS.bgTertiary;
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (currentPage !== page) {
                          e.currentTarget.style.backgroundColor = COLORS.bgPrimary;
                        }
                      }}
                    >
                      {page}
                    </button>
                  );
                });
              })()}

              <button
                onClick={() => currentPage < Math.ceil(total / pageSize) && handlePageChange(currentPage + 1)}
                disabled={currentPage >= Math.ceil(total / pageSize)}
                style={{
                  padding: '6px 10px',
                  border: `1px solid ${COLORS.borderColor}`,
                  borderRadius: '8px',
                  backgroundColor: COLORS.bgPrimary,
                  color: currentPage >= Math.ceil(total / pageSize) ? COLORS.textMuted : COLORS.textSecondary,
                  cursor: currentPage >= Math.ceil(total / pageSize) ? 'not-allowed' : 'pointer',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  opacity: currentPage >= Math.ceil(total / pageSize) ? 0.5 : 1,
                }}
                onMouseEnter={(e) => {
                  if (currentPage < Math.ceil(total / pageSize)) {
                    e.currentTarget.style.backgroundColor = COLORS.bgTertiary;
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = COLORS.bgPrimary;
                }}
              >
                Next
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default DocumentsPage;
