import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Modal,
  Form,
  Tag,
  Tooltip,
  Popconfirm,
  Row,
  Col,
  Statistic,
  Tabs,
  Switch,
  InputNumber,
  Typography,
  Alert,
} from 'antd';
import { App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  SettingOutlined,
  CloudUploadOutlined,
  SecurityScanOutlined,
  BellOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { foundationService } from '@/services';
import type { SystemConfig } from '@/services/foundation.service';
import dayjs from 'dayjs';

const { Search } = Input;
const { Option } = Select;
const { Text } = Typography;

const CONFIG_CATEGORIES = [
  { value: 'AI', label: 'AI Config', icon: <RobotOutlined />, color: 'purple' },
  { value: 'STORAGE', label: 'Storage Config', icon: <CloudUploadOutlined />, color: 'blue' },
  { value: 'NOTIFICATION', label: 'Notification Config', icon: <BellOutlined />, color: 'orange' },
  { value: 'SECURITY', label: 'Security Config', icon: <SecurityScanOutlined />, color: 'red' },
  { value: 'SYSTEM', label: 'System Config', icon: <SettingOutlined />, color: 'green' },
];

const CONFIG_TYPES = [
  { value: 'string', label: 'String' },
  { value: 'number', label: 'Number' },
  { value: 'boolean', label: 'Boolean' },
  { value: 'json', label: 'JSON' },
];

export const SystemConfigPage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [configs, setConfigs] = useState<SystemConfig[]>([]);
  const [filteredConfigs, setFilteredConfigs] = useState<SystemConfig[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>('all');
  const [searchValue, setSearchValue] = useState('');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingConfig, setEditingConfig] = useState<SystemConfig | null>(null);
  const [form] = Form.useForm();
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  useEffect(() => {
    fetchConfigs();
  }, [pagination.current, pagination.pageSize, activeCategory]);

  useEffect(() => {
    filterConfigs();
  }, [configs, searchValue, activeCategory]);

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const response = await foundationService.config.list({
        current: pagination.current,
        size: pagination.pageSize,
        category: activeCategory === 'all' ? undefined : activeCategory,
      });

      setConfigs(response.list);
      setPagination((prev) => ({ ...prev, total: response.total }));
    } catch (error) {
      message.error('Failed to fetch config list');
    } finally {
      setLoading(false);
    }
  };

  const filterConfigs = () => {
    let filtered = [...configs];

    if (searchValue) {
      filtered = filtered.filter(
        (config) =>
          config.configKey.toLowerCase().includes(searchValue.toLowerCase()) ||
          config.configValue.toLowerCase().includes(searchValue.toLowerCase()) ||
          (config.description &&
            config.description.toLowerCase().includes(searchValue.toLowerCase()))
      );
    }

    setFilteredConfigs(filtered);
  };

  const handleAdd = () => {
    setEditingConfig(null);
    form.resetFields();
    form.setFieldsValue({
      configType: 'string',
      isPublic: 0,
      status: 1,
    });
    setIsModalVisible(true);
  };

  const handleEdit = (config: SystemConfig) => {
    setEditingConfig(config);
    form.setFieldsValue({
      configKey: config.configKey,
      configValue: config.configValue,
      configType: config.configType,
      category: config.category,
      description: config.description,
      isPublic: config.isPublic,
    });
    setIsModalVisible(true);
  };

  const handleDelete = async (key: string) => {
    try {
      await foundationService.config.delete(key);
      message.success('Deleted successfully');
      fetchConfigs();
    } catch (error) {
      message.error('Delete failed');
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();

      if (editingConfig) {
        await foundationService.config.update(editingConfig.configKey, values);
        message.success('Updated successfully');
      } else {
        await foundationService.config.create(values);
        message.success('Created successfully');
      }

      setIsModalVisible(false);
      form.resetFields();
      fetchConfigs();
    } catch (error) {
      message.error('Operation failed');
    }
  };

  const getCategoryInfo = (category: string) => {
    return CONFIG_CATEGORIES.find((c) => c.value === category) || {
      label: category,
      color: 'default',
    };
  };

  const renderConfigValue = (config: SystemConfig) => {
    const { configType, configValue } = config;

    switch (configType) {
      case 'boolean':
        return configValue === 'true' ? (
          <Tag color="green">Yes</Tag>
        ) : (
          <Tag color="red">No</Tag>
        );
      case 'number':
        return <Text code>{Number(configValue).toLocaleString()}</Text>;
      case 'json':
        try {
          const jsonObj = JSON.parse(configValue);
          return (
            <Tooltip title={<pre>{JSON.stringify(jsonObj, null, 2)}</pre>}>
              <Text ellipsis style={{ maxWidth: 200 }}>
                {JSON.stringify(jsonObj)}
              </Text>
            </Tooltip>
          );
        } catch {
          return <Text code>{configValue}</Text>;
        }
      default:
        return (
          <Text
            ellipsis={{
              tooltip: configValue,
            }}
            style={{ maxWidth: 300 }}
          >
            {configValue}
          </Text>
        );
    }
  };

  const columns: ColumnsType<SystemConfig> = [
    {
      title: 'Config Key',
      dataIndex: 'configKey',
      key: 'configKey',
      width: 200,
      render: (text) => <Text code>{text}</Text>,
    },
    {
      title: 'Config Value',
      dataIndex: 'configValue',
      key: 'configValue',
      render: (_, record) => renderConfigValue(record),
    },
    {
      title: 'Type',
      dataIndex: 'configType',
      key: 'configType',
      width: 100,
      render: (type) => {
        const typeInfo = CONFIG_TYPES.find((t) => t.value === type);
        return <Tag>{typeInfo?.label || type}</Tag>;
      },
    },
    {
      title: 'Category',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (category) => {
        const info = getCategoryInfo(category);
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      render: (text) => (
        <Text
          ellipsis={{
            tooltip: text,
          }}
        >
          {text || '-'}
        </Text>
      ),
    },
    {
      title: 'Public',
      dataIndex: 'isPublic',
      key: 'isPublic',
      width: 80,
      render: (isPublic) =>
        isPublic ? (
          <Tag color="blue">Public</Tag>
        ) : (
          <Tag color="default">Private</Tag>
        ),
    },
    {
      title: 'Updated At',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 180,
      render: (time) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
      sorter: (a, b) => dayjs(a.updatedAt).unix() - dayjs(b.updatedAt).unix(),
    },
    {
      title: 'Actions',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Popconfirm
            title="Confirm Deletion"
            description="Are you sure you want to delete this config?"
            onConfirm={() => handleDelete(record.configKey)}
            okText="OK"
            cancelText="Cancel"
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const renderValueInput = () => {
    const configType = Form.useWatch('configType', form);

    switch (configType) {
      case 'boolean':
        return (
          <Form.Item
            label="Config Value"
            name="configValue"
            rules={[{ required: true, message: 'Please select a config value' }]}
          >
            <Select>
              <Option value="true">true</Option>
              <Option value="false">false</Option>
            </Select>
          </Form.Item>
        );
      case 'number':
        return (
          <Form.Item
            label="Config Value"
            name="configValue"
            rules={[{ required: true, message: 'Please enter a config value' }]}
          >
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
        );
      case 'json':
        return (
          <Form.Item
            label="Config Value (JSON)"
            name="configValue"
            rules={[
              { required: true, message: 'Please enter a config value in JSON format' },
              {
                validator: (_, value) => {
                  try {
                    JSON.parse(value);
                    return Promise.resolve();
                  } catch {
                    return Promise.reject(new Error('Please enter valid JSON'));
                  }
                },
              },
            ]}
          >
            <Input.TextArea rows={6} placeholder='{"key": "value"}' />
          </Form.Item>
        );
      default:
        return (
          <Form.Item
            label="Config Value"
            name="configValue"
            rules={[{ required: true, message: 'Please enter a config value' }]}
          >
            <Input.TextArea rows={3} placeholder="Please enter the config value" />
          </Form.Item>
        );
    }
  };

  // Stats data
  const categoryStats = CONFIG_CATEGORIES.map((cat) => ({
    ...cat,
    count: configs.filter((c) => c.category === cat.value).length,
  }));

  return (
    <div>
      <Card
        title={
          <Space>
            <SettingOutlined />
            <span>System Config Management</span>
          </Space>
        }
        style={{ borderRadius: 12 }}
      >
        <Alert
          message="Config Management"
          description="System configuration affects how the system runs. Please make changes carefully, and consider backing up the current configuration before editing."
          type="warning"
          showIcon
          style={{ marginBottom: 24 }}
        />

        {/* Category stat cards */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={4}>
            <Card>
              <Statistic
                title="Total Configs"
                value={configs.length}
                prefix={<SettingOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          {categoryStats.map((stat) => (
            <Col xs={24} sm={12} lg={4} key={stat.value}>
              <Card>
                <Statistic
                  title={stat.label}
                  value={stat.count}
                  prefix={stat.icon}
                  valueStyle={{ color: stat.color === 'red' ? '#ff4d4f' : '#52c41a' }}
                />
              </Card>
            </Col>
          ))}
        </Row>

        {/* Category tabs */}
        <Tabs
          activeKey={activeCategory}
          onChange={setActiveCategory}
          items={[
            {
              key: 'all',
              label: (
                <span>
                  All Configs
                  <Tag style={{ marginLeft: 8 }}>{configs.length}</Tag>
                </span>
              ),
            },
            ...CONFIG_CATEGORIES.map((cat) => ({
              key: cat.value,
              label: (
                <span>
                  {cat.icon}
                  <span style={{ marginLeft: 8 }}>{cat.label}</span>
                  <Tag style={{ marginLeft: 8 }}>
                    {configs.filter((c) => c.category === cat.value).length}
                  </Tag>
                </span>
              ),
            })),
          ]}
        />

        <Card style={{ marginTop: 16 }}>
          <div style={{ marginBottom: 16 }}>
            <Space>
              <Search
                placeholder="Search by config key, value, or description"
                allowClear
                style={{ width: 400 }}
                onChange={(e) => setSearchValue(e.target.value)}
                prefix={<SearchOutlined />}
              />
              <Button icon={<ReloadOutlined />} onClick={fetchConfigs}>
                Refresh
              </Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                Add Config
              </Button>
            </Space>
          </div>

          <Table
            columns={columns}
            dataSource={filteredConfigs}
            rowKey="configKey"
            loading={loading}
            scroll={{ x: 1200 }}
            pagination={{
              current: pagination.current,
              pageSize: pagination.pageSize,
              total: pagination.total,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `${total} total`,
              onChange: (page, pageSize) =>
                setPagination({ current: page, pageSize, total: pagination.total }),
            }}
          />
        </Card>
      </Card>

      {/* Add/Edit Config modal */}
      <Modal
        title={editingConfig ? 'Edit Config' : 'Add Config'}
        open={isModalVisible}
        onOk={handleModalOk}
        onCancel={() => {
          setIsModalVisible(false);
          form.resetFields();
        }}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="Config Key"
            name="configKey"
            rules={[{ required: true, message: 'Please enter the config key' }]}
          >
            <Input
              placeholder="e.g. system.max_upload_size"
              disabled={!!editingConfig}
            />
          </Form.Item>

          <Form.Item
            label="Config Type"
            name="configType"
            rules={[{ required: true, message: 'Please select a config type' }]}
          >
            <Select>
              {CONFIG_TYPES.map((type) => (
                <Option key={type.value} value={type.value}>
                  {type.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          {renderValueInput()}

          <Form.Item
            label="Config Category"
            name="category"
            rules={[{ required: true, message: 'Please select a config category' }]}
          >
            <Select>
              {CONFIG_CATEGORIES.map((cat) => (
                <Option key={cat.value} value={cat.value}>
                  {cat.icon}
                  <span style={{ marginLeft: 8 }}>{cat.label}</span>
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="Description"
            name="description"
          >
            <Input.TextArea rows={3} placeholder="Please enter the config description" />
          </Form.Item>

          <Form.Item
            label="Public"
            name="isPublic"
            valuePropName="checked"
            getValueFromEvent={(checked: boolean) => checked ? 1 : 0}
            tooltip="Public configs can be accessed from the frontend (e.g. the login page's registration entry point)"
          >
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SystemConfigPage;
