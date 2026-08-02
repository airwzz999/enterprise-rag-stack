import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Table,
  Button,
  Input,
  Space,
  Tag,
  Tooltip,
  Dropdown,
  Row,
  Col,
  Statistic,
  Select,
  Popconfirm,
  Empty,
} from 'antd';
import { App } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  FileTextOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  ClockCircleOutlined,
  FolderOutlined,
  FileMarkdownOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  ThunderboltOutlined,
  BarChartOutlined,
  ExclamationCircleOutlined,
  HistoryOutlined,
  SortAscendingOutlined,
  UploadOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { useAuthStore, useDocumentStore } from '@/stores';
import { documentService, categoryService } from '@/services';
import type { DocumentFilter } from '@/types';
import { PERMISSIONS, hasPermission } from '@/utils/permission';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Search } = Input;
const { Option } = Select;

export const DraftsPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const {
    documents,
    isLoading,
    total,
    currentPage,
    pageSize,
    fetchDocuments,
    setFilter,
    reset,
  } = useDocumentStore();

  const [selectedDrafts, setSelectedDrafts] = useState<string[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>();
  const [, setSearchKeyword] = useState<string>('');
  const [sortBy, setSortBy] = useState<string>('updatedAt');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');
  const canCreateDocument = hasPermission(user, PERMISSIONS.documentCreate);
  const canEditDocument = hasPermission(user, PERMISSIONS.documentEdit);
  const canDeleteDocument = hasPermission(user, PERMISSIONS.documentDelete);
  const canSubmitReview = hasPermission(user, PERMISSIONS.documentEdit);
  const canSelectDrafts = canDeleteDocument || canSubmitReview;

  // Draft statistics data
  const draftStats = {
    total: documents.length,
    thisWeek: documents.filter(doc => {
      const createdDate = doc.createdAt;
      const isDraft = doc.status === 0 || doc.status === 'draft';
      return isDraft && createdDate && dayjs(createdDate).isAfter(dayjs().subtract(7, 'day'));
    }).length,
    thisMonth: documents.filter(doc => {
      const createdDate = doc.createdAt;
      const isDraft = doc.status === 0 || doc.status === 'draft';
      return isDraft && createdDate && dayjs(createdDate).isAfter(dayjs().subtract(30, 'day'));
    }).length,
    needAttention: documents.filter(doc => {
      const createdDate = doc.createdAt;
      const isDraft = doc.status === 0 || doc.status === 'draft';
      return isDraft && createdDate && dayjs(createdDate).isBefore(dayjs().subtract(7, 'day'));
    }).length,
  };

  // Load category data
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const data = await categoryService.getCategoryTree();
        setCategories(flattenCategories(data));
      } catch (error) {
        console.error('Failed to fetch categories:', error);
      }
    };
    loadCategories();
  }, []);

  // Initial load of the draft list - sorted by update time descending, drafts only
  useEffect(() => {
    // Clear the list first to avoid showing data from the previous page
    reset();
    // Explicitly set the draft filter state
    setFilter({ status: 0, page: 1, pageSize: 20, sortBy: 'updatedAt', sortOrder: 'desc' });
    fetchDocuments({ status: 0, page: 1, pageSize: 20, sortBy: 'updatedAt', sortOrder: 'desc' });
  }, []);

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

  const getCategoryDisplayStyle = (categoryName: string): { bg: string; color: string; borderColor: string } => {
    const name = categoryName.toLowerCase();
    if (name.includes('technical') || name.includes('development')) {
      return { bg: 'rgba(37, 99, 235, 0.1)', color: '#2563eb', borderColor: 'rgba(37, 99, 235, 0.25)' };
    }
    if (name.includes('business')) {
      return { bg: 'rgba(16, 185, 129, 0.1)', color: '#10b981', borderColor: 'rgba(16, 185, 129, 0.25)' };
    }
    return { bg: 'rgba(139, 92, 246, 0.1)', color: '#8b5cf6', borderColor: 'rgba(139, 92, 246, 0.25)' };
  };

  // Get file icon
  const getFileIcon = (title: string, content: string) => {
    if (content?.includes('```') || title?.endsWith('.md')) {
      return <FileMarkdownOutlined style={{ fontSize: 24, color: '#8b5cf6' }} />;
    }
    if (title?.endsWith('.pdf')) {
      return <FilePdfOutlined style={{ fontSize: 24, color: '#ef4444' }} />;
    }
    if (title?.endsWith('.doc') || title?.endsWith('.docx')) {
      return <FileWordOutlined style={{ fontSize: 24, color: '#2563eb' }} />;
    }
    if (title?.endsWith('.xls') || title?.endsWith('.xlsx')) {
      return <FileExcelOutlined style={{ fontSize: 24, color: '#10b981' }} />;
    }
    if (title?.endsWith('.ppt') || title?.endsWith('.pptx')) {
      return <FilePptOutlined style={{ fontSize: 24, color: '#f97316' }} />;
    }
    return <FileTextOutlined style={{ fontSize: 24, color: '#64748b' }} />;
  };

  // Handle search
  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    setFilter({ keyword: value || undefined, page: 1 });
    fetchDocuments({ status: 0, keyword: value || undefined, page: 1, sortBy: 'updatedAt', sortOrder: 'desc' });
  };

  // Handle category filter
  const handleCategoryChange = (value: string) => {
    setSelectedCategory(value);
    setFilter({ categoryId: value || undefined, page: 1 });
    fetchDocuments({ status: 0, categoryId: value || undefined, page: 1, sortBy: 'updatedAt', sortOrder: 'desc' });
  };

  // Handle sorting
  const handleSortChange = (field: string) => {
    let newSortBy = sortBy;
    let newSortOrder = sortOrder;
    if (sortBy === field) {
      newSortOrder = sortOrder === 'asc' ? 'desc' : 'asc';
    } else {
      newSortBy = field;
      newSortOrder = 'desc';
    }
    setSortBy(newSortBy);
    setSortOrder(newSortOrder);
    fetchDocuments({ status: 0, page: 1, sortBy: newSortBy as DocumentFilter['sortBy'], sortOrder: newSortOrder });
  };

  // Refresh
  const handleRefresh = () => {
    fetchDocuments({ status: 0, page: currentPage, pageSize, sortBy: 'updatedAt', sortOrder: 'desc' });
  };

  // Delete draft
  const handleDelete = async (draftId: string) => {
    try {
      await documentService.deleteDocument(draftId);
      message.success('Draft deleted successfully');
      fetchDocuments({ status: 0, page: currentPage, pageSize, sortBy: 'updatedAt', sortOrder: 'desc' });
    } catch (error) {
      console.error('Failed to delete draft:', error);
      message.error('Failed to delete draft');
    }
  };

  // Bulk delete
  const handleBatchDelete = async () => {
    if (selectedDrafts.length === 0) {
      message.warning('Please select drafts to delete first');
      return;
    }

    try {
      await Promise.all(selectedDrafts.map(id => documentService.deleteDocument(id)));
      message.success(`Successfully deleted ${selectedDrafts.length} draft(s)`);
      setSelectedDrafts([]);
      fetchDocuments({ status: 0, page: currentPage, pageSize, sortBy: 'updatedAt', sortOrder: 'desc' });
    } catch (error) {
      console.error('Bulk delete failed:', error);
      message.error('Bulk delete failed');
    }
  };

  // Publish draft
  const handlePublish = async (draftId: string) => {
    try {
      await documentService.publishDocument(draftId);
      message.success('Submitted for review');
      fetchDocuments({ status: 0, page: currentPage, pageSize, sortBy: 'updatedAt', sortOrder: 'desc' });
    } catch (error) {
      console.error('Publish failed:', error);
      message.error('Publish failed');
    }
  };

  // Bulk publish
  const handleBatchPublish = async () => {
    if (selectedDrafts.length === 0) {
      message.warning('Please select drafts to publish first');
      return;
    }

    try {
      await Promise.all(selectedDrafts.map(id => documentService.publishDocument(id)));
      message.success(`Submitted ${selectedDrafts.length} document(s) for review`);
      setSelectedDrafts([]);
      fetchDocuments({ status: 0, page: currentPage, pageSize, sortBy: 'updatedAt', sortOrder: 'desc' });
    } catch (error) {
      console.error('Bulk publish failed:', error);
      message.error('Bulk publish failed');
    }
  };

  // Edit draft
  const handleEdit = (draftId: string) => {
    navigate(`/documents/${draftId}/edit?from=drafts`);
  };

  // View draft
  const handleView = (draftId: string) => {
    window.open(`/documents/${draftId}`, '_blank');
  };

  // Table column definitions
  const columns: ColumnsType<any> = [
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      width: '27%',
      render: (title: string, record: any) => (
        <Space style={{ display: 'flex', overflow: 'hidden' }}>
          <span key="icon">{getFileIcon(title, record.content)}</span>
          <a
            key="title"
            onClick={() => handleView(record.id)}
            style={{ fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
          >
            {title || 'Untitled Draft'}
          </a>
          {record.autoSaveDismissed === 0 && (
            <Tag
              key="autosave"
              color="blue"
              style={{ flexShrink: 0, fontSize: 11, lineHeight: '18px', padding: '0 6px' }}
            >
              Auto-saved
            </Tag>
          )}
        </Space>
      ),
    },
    {
      title: 'Category',
      dataIndex: 'categoryId',
      key: 'category',
      width: '11%',
      render: (categoryId: string) => {
        const category = categories.find(c => String(c.id) === String(categoryId));
        const name = category?.name;
        if (!name) {
          return <Tag style={{ whiteSpace: 'nowrap' }}>Uncategorized</Tag>;
        }
        const style = getCategoryDisplayStyle(name);
        return (
          <Tag
            icon={<FolderOutlined />}
            style={{
              whiteSpace: 'nowrap',
              backgroundColor: style.bg,
              color: style.color,
              borderColor: style.borderColor,
              fontWeight: 500,
            }}
          >
            {name}
          </Tag>
        );
      },
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: '18%',
      sorter: true,
      render: (date: string, _record: any) => {
        // Backend now consistently returns createdAt
        const createdDate = date;
        return (
          <Tooltip title={createdDate}>
            <span style={{ whiteSpace: 'nowrap', display: 'inline-block' }}>
              <ClockCircleOutlined style={{ marginRight: 4 }} />
              {createdDate ? dayjs(createdDate).fromNow() : '-'}
            </span>
          </Tooltip>
        );
      },
    },
    {
      title: 'Last Modified',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: '15%',
      sorter: true,
      render: (date: string, _record: any) => {
        // Backend now consistently returns updatedAt
        const updatedDate = date;
        return (
          <Tooltip title={updatedDate}>
            <span style={{ whiteSpace: 'nowrap', display: 'inline-block' }}>
              {updatedDate ? dayjs(updatedDate).fromNow() : '-'}
            </span>
          </Tooltip>
        );
      },
    },
    {
      title: 'Word Count',
      dataIndex: 'contentLength',
      key: 'wordCount',
      width: '7%',
      render: (contentLength: number) => {
        if (!contentLength) return '0';
        return (
          <span style={{ whiteSpace: 'nowrap' }}>
            {contentLength > 1000 ? `${(contentLength / 1000).toFixed(1)}k` : contentLength}
          </span>
        );
      },
    },
    {
      title: 'Visibility',
      dataIndex: 'isPublic',
      key: 'visibility',
      width: '8%',
      render: (isPublic: any) => {
        const isPublicVal = Number(isPublic);
        return (
          <Tag
            color={isPublicVal === 1 ? 'green' : 'orange'}
            style={{ whiteSpace: 'nowrap' }}
          >
            {isPublicVal === 1 ? 'Visible to Everyone' : 'Visible to Team'}
          </Tag>
        );
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: '16%',
      render: (_, record: any) => (
        <Space size="small" style={{ whiteSpace: 'nowrap' }}>
          <Tooltip key="view" title="View">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handleView(record.id)}
            />
          </Tooltip>
          <Tooltip key="history" title="View Local History">
            <Button
              type="text"
              icon={<HistoryOutlined />}
              onClick={() => navigate(`/documents/${record.id}/autosave-history`)}
            />
          </Tooltip>
          {canEditDocument && (
            <Tooltip key="edit" title="Edit">
              <Button
                type="text"
                icon={<EditOutlined />}
                onClick={() => handleEdit(record.id)}
              />
            </Tooltip>
          )}
          {canSubmitReview && (
            <Popconfirm
              key="publish"
              title="Confirm submission for review?"
              description="Once submitted, it will be published after being reviewed"
              onConfirm={() => handlePublish(record.id)}
              okText="Confirm"
              cancelText="Cancel"
            >
              <Tooltip title="Submit for Review">
                <Button type="text" style={{ color: '#1677ff' }} icon={<SendOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
          {canDeleteDocument && (
            <Popconfirm
              key="delete"
              title="Confirm deletion of this draft?"
              description="This action cannot be undone"
              onConfirm={() => handleDelete(record.id)}
              okText="Confirm"
              cancelText="Cancel"
            >
              <Tooltip title="Delete">
                <Button type="text" danger icon={<DeleteOutlined />} />
              </Tooltip>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  // Row selection configuration
  const rowSelection = {
    selectedRowKeys: selectedDrafts,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedDrafts(newSelectedRowKeys as string[]);
    },
  };

  return (
    <div className="drafts-page" style={{ padding: '16px', marginLeft: '-16px', marginTop: '-16px' }}>
      {/* Page header */}
      <div className="page-header" style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 600, color: '#0f172a', margin: 0 }}>
            <FileTextOutlined style={{ marginRight: '8px', color: '#f59e0b' }} />
            Drafts
          </h1>
          <p style={{ color: '#64748b', marginTop: '4px', fontSize: '13px', marginBottom: 0 }}>
            View and manage your unpublished document drafts
          </p>
        </div>
        <Space key="header-actions">
          <Button
            key="refresh"
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
          >
            Refresh
          </Button>
          {canCreateDocument && (
            <Button
              key="import"
              icon={<UploadOutlined />}
              onClick={() => navigate('/documents/import')}
            >
              Import Document
            </Button>
          )}
          {canCreateDocument && (
            <Button
              key="create"
              type="primary"
              icon={<EditOutlined />}
              onClick={() => navigate('/documents/new')}
            >
              New Draft
            </Button>
          )}
        </Space>
      </div>

      {/* Draft statistics */}
      <Row gutter={16} style={{ marginBottom: '16px' }}>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Total Drafts"
              value={draftStats.total}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#f59e0b' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Added This Week"
              value={draftStats.thisWeek}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#10b981' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Added This Month"
              value={draftStats.thisMonth}
              prefix={<BarChartOutlined />}
              valueStyle={{ color: '#3b82f6' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Pending"
              value={draftStats.needAttention}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ef4444' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter and search */}
      <Card
        variant="borderless"
        style={{ marginBottom: '12px' }}
        extra={
          selectedDrafts.length > 0 && canSelectDrafts && (
            <Space key="batch-actions">
              <span key="selected-count" style={{ color: '#64748b' }}>{selectedDrafts.length} item(s) selected</span>
              {canSubmitReview && (
                <Popconfirm
                  key="batch-publish"
                  title="Confirm submitting the selected drafts for review?"
                  description="Once published, it will be visible to everyone"
                  onConfirm={handleBatchPublish}
                  okText="Confirm"
                  cancelText="Cancel"
                >
                  <Button type="primary" icon={<SendOutlined />}>
                    Bulk Publish
                  </Button>
                </Popconfirm>
              )}
              {canDeleteDocument && (
                <Popconfirm
                  key="batch-delete"
                  title="Confirm deletion of the selected drafts?"
                  description="This action cannot be undone"
                  onConfirm={handleBatchDelete}
                  okText="Confirm"
                  cancelText="Cancel"
                >
                  <Button danger icon={<DeleteOutlined />}>
                    Bulk Delete
                  </Button>
                </Popconfirm>
              )}
            </Space>
          )
        }
      >
        <Space size="middle" style={{ width: '100%' }}>
          <Search
            placeholder="Search draft title or content"
            allowClear
            onSearch={handleSearch}
            style={{ width: 300 }}
            prefix={<SearchOutlined />}
            enterButton
          />
          <Select
            placeholder="Select category"
            allowClear
            style={{ width: 200 }}
            value={selectedCategory}
            onChange={handleCategoryChange}
          >
            {categories.map((cat, index) => (
              <Option key={`cat-${cat.id || index}`} value={cat.id}>
                {cat.label}
              </Option>
            ))}
          </Select>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'updatedAt',
                  label: 'By Last Modified',
                  onClick: () => handleSortChange('updatedAt'),
                },
                {
                  key: 'createdAt',
                  label: 'By Created Time',
                  onClick: () => handleSortChange('createdAt'),
                },
                {
                  key: 'title',
                  label: 'By Title',
                  onClick: () => handleSortChange('title'),
                },
              ],
            }}
          >
            <Button icon={<SortAscendingOutlined />}>
              Sort {sortOrder === 'asc' ? '↑' : '↓'}
            </Button>
          </Dropdown>
        </Space>
      </Card>

      {/* Draft list */}
      <Card variant="borderless">
        {documents.length === 0 && !isLoading ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            imageStyle={{ height: 60 }}
            description={
              <div style={{ textAlign: 'center' }}>
                <p style={{ color: '#64748b', marginBottom: '16px', fontSize: 16 }}>
                  No draft documents yet
                </p>
                {canCreateDocument && (
                  <Button
                    type="primary"
                    icon={<EditOutlined />}
                    onClick={() => navigate('/documents/new')}
                  >
                    Create Your First Draft
                  </Button>
                )}
              </div>
            }
          />
        ) : (
          <Table
            columns={columns}
            dataSource={documents}
            loading={isLoading}
            rowKey="id"
            rowSelection={canSelectDrafts ? rowSelection : undefined}
            scroll={{ x: 'max-content' }}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: total,
              showSizeChanger: true,
              showTotal: (total) => `${total} draft(s) in total`,
              onChange: (page, size) => {
                setFilter({ page, pageSize: size });
                fetchDocuments({ status: 0, page, pageSize: size, sortBy: 'updatedAt', sortOrder: 'desc' });
              },
            }}
            onRow={(record) => ({
              onDoubleClick: () => handleView(record.id),
            })}
          />
        )}
      </Card>
    </div>
  );
};

export default DraftsPage;
