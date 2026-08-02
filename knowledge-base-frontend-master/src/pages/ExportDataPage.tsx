import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Radio,
  Row,
  Col,
  Typography,
  Alert,
  Tag,
} from 'antd';
import { App } from 'antd';
import {
  FileTextOutlined,
  FilePdfOutlined,
  FileMarkdownOutlined,
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { documentService } from '@/services';
import { Document } from '@/types';

const { Title, Text } = Typography;

const ExportDataPage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [total, setTotal] = useState(0);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [exportFormat, setExportFormat] = useState<'pdf' | 'markdown'>('pdf');
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10 });

  const fetchDocuments = useCallback(async () => {
    setLoading(true);
    try {
      const result = await documentService.getDocuments({
        keyword: keyword || undefined,
        status: statusFilter,
        page: pagination.current,
        pageSize: pagination.pageSize,
        sortBy: 'updatedAt',
        sortOrder: 'desc',
      });
      setDocuments(result.list);
      setTotal(result.total);
    } catch {
      message.error('Failed to fetch document list');
    } finally {
      setLoading(false);
    }
  }, [keyword, statusFilter, pagination.current, pagination.pageSize, message]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const handleExport = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('Please select documents to export first');
      return;
    }
    setExporting(true);
    try {
      const ids = selectedRowKeys.map(String);
      await documentService.batchExportDocuments(ids, exportFormat);
      message.success(`Successfully exported ${ids.length} document(s)`);
    } catch {
      message.error('Export failed, please try again');
    } finally {
      setExporting(false);
    }
  };

  const handleClearSelection = () => {
    setSelectedRowKeys([]);
  };

  const handleTableChange = (pag: any) => {
    setPagination({ current: pag.current, pageSize: pag.pageSize });
  };

  const statusMap: Record<number, { label: string; color: string }> = {
    0: { label: 'Draft', color: 'default' },
    1: { label: 'Published', color: 'green' },
    2: { label: 'Archived', color: 'orange' },
    3: { label: 'Pending Review', color: 'blue' },
  };

  const columns: ColumnsType<Document> = [
    {
      title: 'Document Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
      render: (text: string) => (
        <Space>
          <FileTextOutlined style={{ color: '#2563eb' }} />
          <Text ellipsis={{ tooltip: text }} style={{ maxWidth: 300 }}>
            {text}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Author',
      dataIndex: 'authorName',
      key: 'authorName',
      width: 120,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => {
        const info = statusMap[status] || { label: 'Unknown', color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: 'Updated At',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
      sorter: true,
    },
  ];

  const hasSelected = selectedRowKeys.length > 0;

  return (
    <div style={{ padding: '16px 16px 32px 16px', minHeight: '100vh', background: '#f8fafc' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <Title level={1} style={{ fontSize: 32, fontWeight: 700, color: '#0f172a', marginBottom: 8, letterSpacing: '-0.02em' }}>
            Export Data
          </Title>
          <Text style={{ fontSize: 16, color: '#475569' }}>
            Select documents and export them as a ZIP archive in PDF or Markdown format
          </Text>
        </div>
      </div>

      {/* Filter Bar */}
      <Card style={{ borderRadius: 12, marginBottom: 16, border: '1px solid #f1f5f9', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }} variant="borderless">
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} sm={8} lg={6}>
            <Input
              placeholder="Search document title"
              allowClear
              prefix={<SearchOutlined />}
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setPagination((prev) => ({ ...prev, current: 1 }));
              }}
            />
          </Col>
          <Col xs={24} sm={6} lg={4}>
            <Select
              placeholder="Document status"
              allowClear
              style={{ width: '100%' }}
              value={statusFilter}
              onChange={(val: number | string) => {
                // The "all" string also means no filtering, convert it to undefined
                setStatusFilter(val === 'all' ? undefined : (val as number));
                setPagination((prev) => ({ ...prev, current: 1 }));
              }}
            >
              <Select.Option value="all">All Statuses</Select.Option>
              <Select.Option value={0}>Draft</Select.Option>
              <Select.Option value={1}>Published</Select.Option>
              <Select.Option value={2}>Archived</Select.Option>
              <Select.Option value={3}>Pending Review</Select.Option>
            </Select>
          </Col>
          <Col xs={24} sm={10} lg={14}>
            <Space>
              <Button icon={<ReloadOutlined />} onClick={fetchDocuments}>
                Refresh
              </Button>
              {hasSelected && (
                <>
                  <Button icon={<ClearOutlined />} onClick={handleClearSelection}>
                    Deselect
                  </Button>
                </>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Export Config Bar */}
      <Card style={{ borderRadius: 12, marginBottom: 16, border: '1px solid #f1f5f9', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }} variant="borderless">
        <Row gutter={[24, 16]} align="middle">
          <Col xs={24} md={14}>
            <Space size="middle">
              <Text strong>Export Format:</Text>
              <Radio.Group value={exportFormat} onChange={(e) => setExportFormat(e.target.value)}>
                <Radio.Button value="pdf">
                  <Space>
                    <FilePdfOutlined />
                    PDF
                  </Space>
                </Radio.Button>
                <Radio.Button value="markdown">
                  <Space>
                    <FileMarkdownOutlined />
                    Markdown
                  </Space>
                </Radio.Button>
              </Radio.Group>
            </Space>
          </Col>
          <Col xs={24} md={10}>
            <Space style={{ float: 'right' }}>
              {hasSelected && (
                <Text style={{ color: '#2563eb', fontWeight: 500 }}>
                  Selected <Text strong style={{ fontSize: 18 }}>{selectedRowKeys.length}</Text> document(s)
                </Text>
              )}
              <Button
                type="primary"
                icon={<DownloadOutlined />}
                onClick={handleExport}
                loading={exporting}
                disabled={!hasSelected}
                size="large"
                style={{
                  background: 'linear-gradient(135deg, #2563eb, #1e40af)',
                  border: 'none',
                  color: '#fff',
                  boxShadow: '0 4px 14px rgba(37, 99, 235, 0.15)',
                }}
              >
                Export Selected Documents
              </Button>
            </Space>
          </Col>
        </Row>
        {selectedRowKeys.length > 0 && (
          <Alert
            message={`${selectedRowKeys.length} document(s) will be exported as a ZIP archive in ${exportFormat.toUpperCase()} format`}
            type="info"
            showIcon
            style={{ marginTop: 12 }}
          />
        )}
      </Card>

      {/* Document Table */}
      <Card style={{ borderRadius: 12, border: '1px solid #f1f5f9', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }} variant="borderless">
        <Table
          rowKey="id"
          columns={columns}
          dataSource={documents}
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
          }}
          onChange={handleTableChange}
          pagination={{
            ...pagination,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `${t} in total`,
          }}
          scroll={{ x: 600 }}
        />
      </Card>
    </div>
  );
};

export default ExportDataPage;
