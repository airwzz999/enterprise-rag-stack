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
  Empty,
  Popconfirm,
  Row,
  Col,
  Statistic,
} from 'antd';
import { App } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  FileTextOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EyeOutlined,
  StarFilled,
  ClockCircleOutlined,
  FolderOutlined,
  UserOutlined,
  FileMarkdownOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  ThunderboltOutlined,
  BarChartOutlined,
  HeartOutlined,
} from '@ant-design/icons';
import { useFavoriteStore } from '@/stores';
import { favoriteService } from '@/services';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/en';

dayjs.extend(relativeTime);
dayjs.locale('en');

const { Search } = Input;

export const FavoritesPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const {
    favoriteDocuments,
    isLoading,
    loadFavorites,
  } = useFavoriteStore();

  const [selectedFavorites, setSelectedFavorites] = useState<string[]>([]);
  const [searchKeyword, setSearchKeyword] = useState<string>('');

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

  // Load the favorites list on initial mount
  useEffect(() => {
    loadFavorites();
  }, []);

  // Handle search
  const handleSearch = (value: string) => {
    setSearchKeyword(value);
  };

  // Refresh
  const handleRefresh = () => {
    loadFavorites();
  };

  // Remove a favorite
  const handleRemoveFavorite = async (documentId: string) => {
    try {
      await favoriteService.removeFavorite(documentId);
      message.success('Removed from favorites');
      loadFavorites();
    } catch (error) {
      console.error('Failed to remove favorite:', error);
      message.error('Failed to remove from favorites');
    }
  };

  // Batch remove favorites
  const handleBatchRemove = async () => {
    if (selectedFavorites.length === 0) {
      message.warning('Please select the documents to remove from favorites first');
      return;
    }

    try {
      await Promise.all(selectedFavorites.map(id => favoriteService.removeFavorite(id)));
      message.success(`Removed ${selectedFavorites.length} document(s) from favorites`);
      setSelectedFavorites([]);
      loadFavorites();
    } catch (error) {
      console.error('Failed to batch remove favorites:', error);
      message.error('Failed to remove favorites');
    }
  };

  // View document
  const handleView = (documentId: string) => {
    navigate(`/documents/${documentId}`);
  };

  // Get the file icon
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

  // Filter data
  const filteredDocuments = favoriteDocuments.filter(doc => {
    if (searchKeyword) {
      const keyword = searchKeyword.toLowerCase();
      return (
        (doc.documentTitle && doc.documentTitle.toLowerCase().includes(keyword)) ||
        (doc.documentSummary && doc.documentSummary.toLowerCase().includes(keyword))
      );
    }
    return true;
  });

  // Table column definitions
  const columns: ColumnsType<any> = [
    {
      title: 'Title',
      dataIndex: 'documentTitle',
      key: 'title',
      width: '35%',
      render: (title: string, record: any) => (
        <Space style={{ display: 'flex', overflow: 'hidden' }}>
          <span key="icon">{getFileIcon(title, record.content)}</span>
          <a
            key="title"
            onClick={() => handleView(record.documentId)}
            style={{ fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
          >
            {title || 'Untitled document'}
          </a>
        </Space>
      ),
    },
    {
      title: 'Category',
      dataIndex: 'documentCategoryName',
      key: 'category',
      width: '12%',
      render: (categoryName: string) => {
        if (!categoryName) {
          return <Tag style={{ whiteSpace: 'nowrap' }}>Uncategorized</Tag>;
        }
        const style = getCategoryDisplayStyle(categoryName);
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
            {categoryName}
          </Tag>
        );
      },
    },
    {
      title: 'Author',
      dataIndex: 'documentAuthorName',
      key: 'author',
      width: '10%',
      render: (authorName: string) => (
        <Space style={{ fontSize: '13px' }}>
          <UserOutlined style={{ color: '#64748b' }} />
          <span>{authorName || 'Unknown'}</span>
        </Space>
      ),
    },
    {
      title: 'Favorited On',
      dataIndex: 'favoriteTime',
      key: 'favoriteTime',
      width: '18%',
      render: (time: string) => (
        <Tooltip title={time}>
          <span style={{ whiteSpace: 'nowrap', display: 'inline-block' }}>
            <ClockCircleOutlined style={{ marginRight: 4 }} />
            {time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </span>
        </Tooltip>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: '15%',
      render: (_, record: any) => (
        <Space size="small" style={{ whiteSpace: 'nowrap' }}>
          <Tooltip key="view" title="View">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handleView(record.documentId)}
            />
          </Tooltip>
          <Tooltip key="remove" title="Remove from favorites">
            <Popconfirm
              title="Remove from favorites?"
              description="It will no longer appear in your favorites list"
              onConfirm={() => handleRemoveFavorite(record.documentId)}
              okText="Confirm"
              cancelText="Cancel"
            >
              <Button type="text" danger icon={<DeleteOutlined />} />
            </Popconfirm>
          </Tooltip>
        </Space>
      ),
    },
  ];

  // Row selection configuration
  const rowSelection = {
    selectedRowKeys: selectedFavorites,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedFavorites(newSelectedRowKeys as string[]);
    },
  };

  return (
    <div className="favorites-page" style={{ padding: '16px', marginLeft: '-16px', marginTop: '-16px' }}>
      {/* Page header */}
      <div className="page-header" style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 600, color: '#0f172a', margin: 0 }}>
            <StarFilled style={{ marginRight: '8px', color: '#f59e0b' }} />
            My Favorites
          </h1>
          <p style={{ color: '#64748b', marginTop: '4px', fontSize: '13px', marginBottom: 0 }}>
            View and manage your favorited documents
          </p>
        </div>
        <Space>
          <Button
            key="refresh"
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
          >
            Refresh
          </Button>
        </Space>
      </div>

      {/* Favorites statistics */}
      <Row gutter={16} style={{ marginBottom: '16px' }}>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Total Favorites"
              value={filteredDocuments.length}
              prefix={<HeartOutlined />}
              valueStyle={{ color: '#f59e0b' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Added This Week"
              value={filteredDocuments.filter(doc => {
                const createdDate = doc.favoriteTime;
                return createdDate && dayjs(createdDate).isAfter(dayjs().subtract(7, 'day'));
              }).length}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#10b981' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Added This Month"
              value={filteredDocuments.filter(doc => {
                const createdDate = doc.favoriteTime;
                return createdDate && dayjs(createdDate).isAfter(dayjs().subtract(30, 'day'));
              }).length}
              prefix={<BarChartOutlined />}
              valueStyle={{ color: '#3b82f6' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card variant="borderless">
            <Statistic
              title="Uncategorized"
              value={filteredDocuments.filter(doc => !doc.documentCategoryName).length}
              prefix={<FolderOutlined />}
              valueStyle={{ color: '#8b5cf6' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter and search */}
      <Card
        variant="borderless"
        style={{ marginBottom: '12px' }}
        extra={
          selectedFavorites.length > 0 && (
            <Space>
              <span key="selected-count" style={{ color: '#64748b' }}>{selectedFavorites.length} selected</span>
              <Popconfirm
                key="batch-remove"
                title="Remove the selected documents from favorites?"
                description="They will no longer appear in your favorites list"
                onConfirm={handleBatchRemove}
                okText="Confirm"
                cancelText="Cancel"
              >
                <Button danger icon={<DeleteOutlined />}>
                  Remove Selected
                </Button>
              </Popconfirm>
            </Space>
          )
        }
      >
        <Space size="middle" style={{ width: '100%' }}>
          <Search
            placeholder="Search favorited document titles or content"
            allowClear
            onSearch={handleSearch}
            style={{ width: 300 }}
            prefix={<SearchOutlined />}
            enterButton
          />
        </Space>
      </Card>

      {/* Favorites list */}
      <Card variant="borderless">
        {filteredDocuments.length === 0 && !isLoading ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            imageStyle={{ height: 60 }}
            description={
              <div style={{ textAlign: 'center' }}>
                <p style={{ color: '#64748b', marginBottom: '16px', fontSize: 16 }}>
                  No favorited documents yet
                </p>
                <p style={{ color: '#94a3b8', fontSize: 14 }}>
                  Head to the document center to discover and favorite documents you're interested in
                </p>
                <Button
                  type="primary"
                  icon={<FolderOutlined />}
                  onClick={() => navigate('/documents')}
                >
                  Go to Document Center
                </Button>
              </div>
            }
          />
        ) : (
          <Table
            columns={columns}
            dataSource={filteredDocuments}
            loading={isLoading}
            rowSelection={rowSelection}
            scroll={{ x: 'max-content' }}
            pagination={{
              showSizeChanger: true,
              showTotal: (total) => `${total} favorites in total`,
            }}
            onRow={(record) => ({
              onDoubleClick: () => handleView(record.documentId),
            })}
          />
        )}
      </Card>
    </div>
  );
};

export default FavoritesPage;
