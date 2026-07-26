import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  DatePicker,
  Modal,
  Tag,
  Row,
  Col,
  Statistic,
  Tooltip,
  Progress,
  Radio,
  Typography,
} from 'antd';
import { App } from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EyeOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { foundationService } from '@/services';
import type { OperationLog, LogStatistics } from '@/services/foundation.service';
import dayjs from 'dayjs';
import ReactECharts from 'echarts-for-react';

const { RangePicker } = DatePicker;
const { Option } = Select;
const { Text } = Typography;

const MODULE_OPTIONS = [
  { value: 'DOCUMENT', label: 'Document Management' },
  { value: 'USER', label: 'User Management' },
  { value: 'AUTH', label: 'Authentication' },
  { value: 'AI', label: 'AI Service' },
  { value: 'SYSTEM', label: 'System Config' },
  { value: 'FILE', label: 'File Management' },
  { value: 'SEARCH', label: 'Search Service' },
  { value: 'GRAPH', label: 'Knowledge Graph' },
];

const OPERATION_TYPES = [
  { value: 'CREATE', label: 'Create' },
  { value: 'UPDATE', label: 'Update' },
  { value: 'DELETE', label: 'Delete' },
  { value: 'QUERY', label: 'Query' },
  { value: 'LOGIN', label: 'Login' },
  { value: 'LOGOUT', label: 'Logout' },
  { value: 'EXPORT', label: 'Export' },
  { value: 'IMPORT', label: 'Import' },
];

export const OperationLogPage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [logs, setLogs] = useState<OperationLog[]>([]);
  const [statistics, setStatistics] = useState<LogStatistics | null>(null);
  const [selectedLog, setSelectedLog] = useState<OperationLog | null>(null);
  const [isDetailModalVisible, setIsDetailModalVisible] = useState(false);
  const [isStatisticsModalVisible, setIsStatisticsModalVisible] = useState(false);
  const [chartType, setChartType] = useState<'bar' | 'line' | 'pie'>('bar');

  // Filter conditions
  const [filters, setFilters] = useState({
    module: undefined as string | undefined,
    operationType: undefined as string | undefined,
    username: '',
    startTime: undefined as dayjs.Dayjs | undefined,
    endTime: undefined as dayjs.Dayjs | undefined,
  });

  // Pagination
  const [pagination, setPagination] = useState({
    current: 1,
    size: 10,
    total: 0,
  });

  useEffect(() => {
    fetchLogs();
    fetchStatistics();
  }, [pagination.current, pagination.size]);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const response = await foundationService.log.list({
        current: pagination.current,
        size: pagination.size,
        module: filters.module,
        operationType: filters.operationType,
        username: filters.username || undefined,
        startTime: filters.startTime?.format('YYYY-MM-DD HH:mm:ss'),
        endTime: filters.endTime?.format('YYYY-MM-DD HH:mm:ss'),
      });

      setLogs(response.list);
      setPagination((prev) => ({
        ...prev,
        total: response.total,
      }));
    } catch (error) {
      message.error('Failed to fetch log list');
    } finally {
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const stats = await foundationService.log.statistics({
        startTime: filters.startTime?.format('YYYY-MM-DD HH:mm:ss'),
        endTime: filters.endTime?.format('YYYY-MM-DD HH:mm:ss'),
      });
      setStatistics(stats);
    } catch (error) {
      message.error('Failed to fetch statistics');
    }
  };

  const handleSearch = () => {
    setPagination((prev) => ({ ...prev, current: 1 }));
    fetchLogs();
    fetchStatistics();
  };

  const handleReset = () => {
    setFilters({
      module: undefined,
      operationType: undefined,
      username: '',
      startTime: undefined,
      endTime: undefined,
    });
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleViewDetail = (log: OperationLog) => {
    setSelectedLog(log);
    setIsDetailModalVisible(true);
  };

  const handleBatchDelete = async () => {
    Modal.confirm({
      title: 'Confirm Deletion',
      content: 'Are you sure you want to delete all logs matching the current filters? This action cannot be undone.',
      okText: 'OK',
      cancelText: 'Cancel',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          // Use the end time from the current filters, or the current time
          const beforeDate = filters.endTime?.format('YYYY-MM-DD') || dayjs().format('YYYY-MM-DD');
          await foundationService.log.deleteBeforeDate(beforeDate);
          message.success('Deleted successfully');
          fetchLogs();
          fetchStatistics();
        } catch (error) {
          message.error('Delete failed');
        }
      },
    });
  };

  const getModuleInfo = (module: string) => {
    return MODULE_OPTIONS.find((m) => m.value === module) || {
      label: module,
      color: 'default',
    };
  };

  const getOperationTypeInfo = (type: string) => {
    return OPERATION_TYPES.find((t) => t.value === type) || {
      label: type,
      color: 'default',
    };
  };

  const columns: ColumnsType<OperationLog> = [
    {
      title: 'Module',
      dataIndex: 'module',
      key: 'module',
      width: 120,
      render: (module) => {
        const info = getModuleInfo(module);
        return <Tag color="blue">{info.label}</Tag>;
      },
      filters: MODULE_OPTIONS.map((m) => ({ text: m.label, value: m.value })),
    },
    {
      title: 'Operation Type',
      dataIndex: 'operationType',
      key: 'operationType',
      width: 100,
      render: (type) => {
        const info = getOperationTypeInfo(type);
        return <Tag>{info.label}</Tag>;
      },
    },
    {
      title: 'Description',
      dataIndex: 'operationDesc',
      key: 'operationDesc',
      width: 200,
      ellipsis: true,
    },
    {
      title: 'User',
      dataIndex: 'username',
      key: 'username',
      width: 120,
    },
    {
      title: 'Method',
      dataIndex: 'requestMethod',
      key: 'requestMethod',
      width: 100,
      render: (method) => {
        const colorMap: Record<string, string> = {
          GET: 'green',
          POST: 'blue',
          PUT: 'orange',
          DELETE: 'red',
        };
        return <Tag color={colorMap[method] || 'default'}>{method}</Tag>;
      },
    },
    {
      title: 'Request URL',
      dataIndex: 'requestUrl',
      key: 'requestUrl',
      width: 250,
      ellipsis: true,
      render: (url) => (
        <Tooltip title={url}>
          <span style={{ fontSize: 12 }}>{url}</span>
        </Tooltip>
      ),
    },
    {
      title: 'IP Address',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 140,
      render: (ip, record) => (
        <div>
          <div>{ip}</div>
          {record.location && (
            <div style={{ fontSize: 12, color: '#999' }}>{record.location}</div>
          )}
        </div>
      ),
    },
    {
      title: 'Duration',
      dataIndex: 'executeTime',
      key: 'executeTime',
      width: 100,
      render: (time) => (
        <span style={{ color: time > 3000 ? '#ff4d4f' : time > 1000 ? '#faad14' : '#52c41a' }}>
          {time}ms
        </span>
      ),
      sorter: (a, b) => (a.executeTime || 0) - (b.executeTime || 0),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) =>
        status === 1 ? (
          <Tag icon={<CheckCircleOutlined />} color="success">
            Success
          </Tag>
        ) : (
          <Tag icon={<CloseCircleOutlined />} color="error">
            Failed
          </Tag>
        ),
    },
    {
      title: 'Time',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
      sorter: (a, b) => dayjs(a.createdAt).unix() - dayjs(b.createdAt).unix(),
    },
    {
      title: 'Actions',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record)}
        >
          Details
        </Button>
      ),
    },
  ];

  // Chart config
  const getChartOption = () => {
    if (!statistics) return {};

    const isPie = chartType === 'pie';

    if (isPie) {
      return {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)',
        },
        legend: {
          orient: 'vertical',
          left: 'left',
        },
        series: [
          {
            name: 'Operation Type',
            type: 'pie',
            radius: '50%',
            data: Object.entries(statistics.operationTypeStats || {}).map(
              ([name, value]) => ({ name, value })
            ),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
              },
            },
          },
        ],
      };
    }

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: chartType === 'line' ? 'cross' : 'shadow',
        },
      },
      xAxis: {
        type: 'category',
        data: Object.keys(statistics.operationTypeStats || {}),
        axisLabel: {
          rotate: 45,
        },
      },
      yAxis: {
        type: 'value',
      },
      series: [
        {
          name: 'Operation Count',
          type: chartType,
          data: Object.values(statistics.operationTypeStats || {}),
          smooth: chartType === 'line',
          itemStyle: {
            color: '#1890ff',
          },
        },
      ],
    };
  };

  return (
    <div>
      <Card
        title={
          <Space>
            <FileTextOutlined />
            <span>Operation Logs</span>
          </Space>
        }
        style={{ borderRadius: 12 }}
      >
        {/* Stats cards */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Total Logs"
                value={statistics?.totalLogs || 0}
                prefix={<FileTextOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Successful Logs"
                value={statistics?.successLogs || 0}
                prefix={<CheckCircleOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Failed Logs"
                value={statistics?.failedLogs || 0}
                prefix={<CloseCircleOutlined />}
                valueStyle={{ color: '#ff4d4f' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card>
              <Statistic
                title="Success Rate"
                value={
                  statistics?.totalLogs
                    ? ((statistics.successLogs / statistics.totalLogs) * 100).toFixed(2)
                    : '0.00'
                }
                suffix="%"
                prefix={<CheckCircleOutlined />}
                valueStyle={{
                  color:
                    statistics && statistics.successLogs / statistics.totalLogs > 0.95
                      ? '#52c41a'
                      : '#faad14',
                }}
              />
            </Card>
          </Col>
        </Row>

        {/* Filters */}
        <Card size="small" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Input
                placeholder="Search username"
                allowClear
                value={filters.username}
                onChange={(e) => setFilters({ ...filters, username: e.target.value })}
                prefix={<SearchOutlined />}
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Select
                placeholder="Select module"
                allowClear
                style={{ width: '100%' }}
                value={filters.module}
                onChange={(value) => setFilters({ ...filters, module: value })}
              >
                {MODULE_OPTIONS.map((module) => (
                  <Option key={module.value} value={module.value}>
                    {module.label}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Select
                placeholder="Select operation type"
                allowClear
                style={{ width: '100%' }}
                value={filters.operationType}
                onChange={(value) => setFilters({ ...filters, operationType: value })}
              >
                {OPERATION_TYPES.map((type) => (
                  <Option key={type.value} value={type.value}>
                    {type.label}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <RangePicker
                style={{ width: '100%' }}
                showTime
                value={[filters.startTime || null, filters.endTime || null]}
                onChange={(dates) =>
                  setFilters({
                    ...filters,
                    startTime: dates?.[0] || undefined,
                    endTime: dates?.[1] || undefined,
                  })
                }
              />
            </Col>
            <Col xs={24} sm={24} lg={24}>
              <Space>
                <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                  Search
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset}>
                  Reset
                </Button>
                <Button
                  icon={<BarChartOutlined />}
                  onClick={() => setIsStatisticsModalVisible(true)}
                >
                  View Statistics
                </Button>
                <Button danger icon={<DeleteOutlined />} onClick={handleBatchDelete}>
                  Batch Delete
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        {/* Log table */}
        <Table
          columns={columns}
          dataSource={logs}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1600 }}
          pagination={{
            current: pagination.current,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `${total} total`,
            onChange: (page, size) =>
              setPagination((prev) => ({ ...prev, current: page, size })),
          }}
        />
      </Card>

      {/* Log detail modal */}
      <Modal
        title="Log Details"
        open={isDetailModalVisible}
        onCancel={() => setIsDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setIsDetailModalVisible(false)}>
            Close
          </Button>,
        ]}
        width={800}
      >
        {selectedLog && (
          <Card size="small">
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <TextWithLabel label="Module" value={getModuleInfo(selectedLog.module).label} />
                  <TextWithLabel label="Operation Type" value={getOperationTypeInfo(selectedLog.operationType).label} />
                  <TextWithLabel label="Description" value={selectedLog.operationDesc} />
                  <TextWithLabel label="User" value={selectedLog.username} />
                </Space>
              </Col>
              <Col span={12}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <TextWithLabel label="Method" value={selectedLog.requestMethod} />
                  <TextWithLabel label="IP Address" value={selectedLog.ipAddress} />
                  {selectedLog.location && (
                    <TextWithLabel label="Location" value={selectedLog.location} />
                  )}
                  <TextWithLabel
                    label="Duration"
                    value={`${selectedLog.executeTime}ms`}
                    valueStyle={{
                      color:
                        selectedLog.executeTime && selectedLog.executeTime > 3000
                          ? '#ff4d4f'
                          : selectedLog.executeTime && selectedLog.executeTime > 1000
                          ? '#faad14'
                          : '#52c41a',
                    }}
                  />
                  <TextWithLabel
                    label="Status"
                    value={selectedLog.status === 1 ? 'Success' : 'Failed'}
                    valueStyle={{ color: selectedLog.status === 1 ? '#52c41a' : '#ff4d4f' }}
                  />
                </Space>
              </Col>
              <Col span={24}>
                <div style={{ marginBottom: 8 }}>
                  <Text type="secondary">Request URL:</Text>
                </div>
                <div>
                  <Text code style={{ wordBreak: 'break-all' }}>
                    {selectedLog.requestUrl}
                  </Text>
                </div>
              </Col>
              {selectedLog.requestParams && (
                <Col span={24}>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary">Request Params:</Text>
                  </div>
                  <pre
                    style={{
                      background: '#f5f5f5',
                      padding: 12,
                      borderRadius: 4,
                      maxHeight: 200,
                      overflow: 'auto',
                    }}
                  >
                    {selectedLog.requestParams}
                  </pre>
                </Col>
              )}
              {selectedLog.responseResult && (
                <Col span={24}>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary">Response Result:</Text>
                  </div>
                  <pre
                    style={{
                      background: '#f5f5f5',
                      padding: 12,
                      borderRadius: 4,
                      maxHeight: 200,
                      overflow: 'auto',
                    }}
                  >
                    {selectedLog.responseResult}
                  </pre>
                </Col>
              )}
              {selectedLog.errorMsg && (
                <Col span={24}>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary">Error Message:</Text>
                  </div>
                  <div style={{ color: '#ff4d4f', whiteSpace: 'pre-wrap' }}>
                    {selectedLog.errorMsg}
                  </div>
                </Col>
              )}
              {selectedLog.userAgent && (
                <Col span={24}>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary">User Agent:</Text>
                  </div>
                  <div style={{ fontSize: 12, color: '#999', wordBreak: 'break-all' }}>
                    {selectedLog.userAgent}
                  </div>
                </Col>
              )}
              <Col span={24}>
                <TextWithLabel
                  label="Time"
                  value={dayjs(selectedLog.createdAt).format('YYYY-MM-DD HH:mm:ss')}
                />
              </Col>
            </Row>
          </Card>
        )}
      </Modal>

      {/* Statistics chart modal */}
      <Modal
        title="Log Statistics"
        open={isStatisticsModalVisible}
        onCancel={() => setIsStatisticsModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setIsStatisticsModalVisible(false)}>
            Close
          </Button>,
        ]}
        width={900}
      >
        <Space direction="vertical" style={{ width: '100%' }} size="large">
          <Card size="small" title="Operation Type Distribution">
            <Radio.Group
              value={chartType}
              onChange={(e) => setChartType(e.target.value)}
              style={{ marginBottom: 16 }}
            >
              <Radio.Button value="bar">Bar Chart</Radio.Button>
              <Radio.Button value="line">Line Chart</Radio.Button>
              <Radio.Button value="pie">Pie Chart</Radio.Button>
            </Radio.Group>
            <ReactECharts
              option={getChartOption()}
              style={{ height: 300 }}
              opts={{ renderer: 'svg' }}
            />
          </Card>

          {statistics && statistics.moduleStats && (
            <Card size="small" title="Module Distribution">
              <Row gutter={[16, 16]}>
                {Object.entries(statistics.moduleStats).map(([module, count]: [string, number]) => (
                  <Col span={12} key={module}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                        <Text>{getModuleInfo(module).label}</Text>
                        <Text strong>{count}</Text>
                      </div>
                      <Progress percent={(count / statistics.totalLogs) * 100} showInfo={false} />
                    </div>
                  </Col>
                ))}
              </Row>
            </Card>
          )}

          {statistics && statistics.userStats && statistics.userStats.length > 0 && (
            <Card size="small" title="Active Users">
              <Row gutter={[16, 16]}>
                {statistics.userStats.slice(0, 10).map((user: { username: string; count: number }) => (
                  <Col span={12} key={user.username}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                        <Text>{user.username}</Text>
                        <Text strong>{user.count}</Text>
                      </div>
                      <Progress
                        percent={(user.count / statistics.userStats[0].count) * 100}
                        showInfo={false}
                      />
                    </div>
                  </Col>
                ))}
              </Row>
            </Card>
          )}

          {statistics && statistics.trendData && (
            <Card size="small" title="Operation Trend">
              <ReactECharts
                option={{
                  tooltip: {
                    trigger: 'axis',
                  },
                  xAxis: {
                    type: 'category',
                    data: statistics.trendData.map((d) => d.date),
                  },
                  yAxis: {
                    type: 'value',
                  },
                  series: [
                    {
                      name: 'Operation Count',
                      type: 'line',
                      data: statistics.trendData.map((d) => d.count),
                      smooth: true,
                      areaStyle: {},
                    },
                  ],
                }}
                style={{ height: 300 }}
                opts={{ renderer: 'svg' }}
              />
            </Card>
          )}
        </Space>
      </Modal>
    </div>
  );
};

// Helper component
const TextWithLabel: React.FC<{
  label: string;
  value: string | number | undefined;
  valueStyle?: React.CSSProperties;
}> = ({ label, value, valueStyle }) => {
  return (
    <div>
      <Text type="secondary" style={{ fontSize: 12 }}>
        {label}:
      </Text>
      <div>
        <Text style={{ fontWeight: 500, ...valueStyle }}>{value ?? '-'}</Text>
      </div>
    </div>
  );
};

export default OperationLogPage;
