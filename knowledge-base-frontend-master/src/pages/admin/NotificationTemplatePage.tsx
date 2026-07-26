import React, { useState, useEffect, useCallback, useMemo } from 'react';
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
  Popconfirm,
  Row,
  Col,
  Typography,
  Alert,
  Divider,
  Tabs,
} from 'antd';
import { App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  SendOutlined,
  EyeOutlined,
  BellOutlined,
  MailOutlined,
  MessageOutlined,
  WechatOutlined,
  DesktopOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { foundationService } from '@/services';
import { useAppStore } from '@/stores';
import type { EntityId } from '@/types';
import type { NotificationTemplate as NotificationTemplateDTO } from '@/services/foundation.service';

const { Search } = Input;
const { Option } = Select;
const { Text, Title } = Typography;
const { TextArea } = Input;

// Notification preview data
interface PreviewData {
  [key: string]: string;
}

interface NotificationTemplate extends Omit<NotificationTemplateDTO, 'id' | 'variables'> {
  id: EntityId;
  variables: string[];
}

const NOTIFICATION_TYPES = [
  { value: 'EMAIL', label: 'Email', icon: <MailOutlined />, color: 'blue' },
  { value: 'SMS', label: 'SMS', icon: <MessageOutlined />, color: 'green' },
  { value: 'WECHAT', label: 'WeChat', icon: <WechatOutlined />, color: 'green' },
  { value: 'SYSTEM', label: 'System', icon: <BellOutlined />, color: 'orange' },
  { value: 'BROWSER', label: 'Browser', icon: <DesktopOutlined />, color: 'purple' },
];

const COMMON_VARIABLES = [
  { name: '{{userName}}', description: 'Username' },
  { name: '{{userEmail}}', description: 'User email' },
  { name: '{{currentTime}}', description: 'Current time' },
  { name: '{{systemName}}', description: 'System name' },
  { name: '{{verifyCode}}', description: 'Verification code' },
  { name: '{{documentTitle}}', description: 'Document title' },
  { name: '{{documentUrl}}', description: 'Document link' },
  { name: '{{operatorName}}', description: 'Operator' },
];

export const NotificationTemplatePage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [templates, setTemplates] = useState<NotificationTemplate[]>([]);
  const [filteredTemplates, setFilteredTemplates] = useState<NotificationTemplate[]>([]);
  const [searchValue, setSearchValue] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [isTemplateModalVisible, setIsTemplateModalVisible] = useState(false);
  const [isPreviewModalVisible, setIsPreviewModalVisible] = useState(false);
  const [isTestModalVisible, setIsTestModalVisible] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<NotificationTemplate | null>(null);
  const [previewTemplate, setPreviewTemplate] = useState<NotificationTemplate | null>(null);
  const [previewData, setPreviewData] = useState<PreviewData>({});
  const [form] = Form.useForm();
  const [testForm] = Form.useForm();
  const [activeTab, setActiveTab] = useState('form');

  const enableEmail = useAppStore((s) => s.enableEmail);

  const availableTypes = useMemo(
    () => enableEmail ? NOTIFICATION_TYPES : NOTIFICATION_TYPES.filter(t => t.value !== 'EMAIL'),
    [enableEmail]
  );

  const fetchTemplates = useCallback(async () => {
    setLoading(true);
    try {
      const response: any = await foundationService.notificationTemplate.list({
        current: 1,
        size: 100,
        notificationType: typeFilter !== 'all' ? typeFilter : undefined,
      });
      const records = response?.records || [];
      // Parse variables from JSON string to array
      const parsedRecords = records.map((t: any) => ({
        ...t,
        variables: typeof t.variables === 'string' ? JSON.parse(t.variables) : (t.variables || []),
      }));
      setTemplates(parsedRecords);
    } catch {
      message.error('Failed to fetch template list');
    } finally {
      setLoading(false);
    }
  }, [typeFilter, message]);

  useEffect(() => {
    fetchTemplates();
  }, [fetchTemplates]);

  useEffect(() => {
    filterTemplates();
  }, [templates, searchValue]);

  const filterTemplates = () => {
    let filtered = [...templates];

    if (searchValue) {
      filtered = filtered.filter(
        (template) =>
          template.templateName.toLowerCase().includes(searchValue.toLowerCase()) ||
          template.templateCode.toLowerCase().includes(searchValue.toLowerCase()) ||
          template.title.toLowerCase().includes(searchValue.toLowerCase())
      );
    }

    if (typeFilter !== 'all') {
      filtered = filtered.filter((template) => template.notificationType === typeFilter);
    }

    setFilteredTemplates(filtered);
  };

  const handleAdd = () => {
    setEditingTemplate(null);
    form.resetFields();
    form.setFieldsValue({
      notificationType: 'SYSTEM',
      isActive: 1,
      variables: [],
    });
    setActiveTab('form');
    setIsTemplateModalVisible(true);
  };

  const handleEdit = (template: NotificationTemplate) => {
    setEditingTemplate(template);
    form.setFieldsValue({
      templateCode: template.templateCode,
      templateName: template.templateName,
      notificationType: template.notificationType,
      title: template.title,
      content: template.content,
      variables: template.variables,
      description: template.description,
      isActive: template.isActive,
    });
    setActiveTab('form');
    setIsTemplateModalVisible(true);
  };

  const handleDelete = async (id: EntityId) => {
    try {
      await foundationService.notificationTemplate.delete(id);
      setTemplates(templates.filter((t) => t.id !== id));
      message.success('Deleted successfully');
    } catch {
      message.error('Delete failed');
    }
  };

  const handleTemplateModalOk = async () => {
    try {
      const values = await form.validateFields();

      // Extract variables from title and content
      const variablePattern = /\{\{(\w+)\}\}/g;
      const titleVars = Array.from(values.title.matchAll(variablePattern), (m: RegExpMatchArray) => m[1]);
      const contentVars = Array.from(values.content.matchAll(variablePattern), (m: RegExpMatchArray) => m[1]);
      const allVars = Array.from(new Set([...titleVars, ...contentVars]));
      // variables stored as JSON string in backend
      const variablesJson = JSON.stringify(allVars);

      if (editingTemplate) {
        await foundationService.notificationTemplate.update(editingTemplate.id, {
          ...values,
          variables: variablesJson,
        });
        message.success('Updated successfully');
      } else {
        await foundationService.notificationTemplate.create({
          ...values,
          variables: variablesJson,
        });
        message.success('Created successfully');
      }

      setIsTemplateModalVisible(false);
      form.resetFields();
      // Refresh list after create/update
      fetchTemplates();
    } catch (error: any) {
      // form validation error doesn't show message
      if (error?.errorFields) return;
      message.error('Operation failed');
    }
  };

  const handlePreview = (template: NotificationTemplate) => {
    setPreviewTemplate(template);
    // Initialize preview data
    const initData: PreviewData = {};
    template.variables.forEach((v) => {
      initData[v] = `[${v}]`;
    });
    setPreviewData(initData);
    setIsPreviewModalVisible(true);
  };

  const handleTest = (template: NotificationTemplate) => {
    setPreviewTemplate(template);
    testForm.resetFields();
    testForm.setFieldsValue({
      testTarget: 'test@example.com',
    });
    setIsTestModalVisible(true);
  };

  const handleSendTest = async () => {
    if (!previewTemplate?.id) return;
    try {
      const values = await testForm.validateFields();
      await foundationService.notificationTemplate.test(previewTemplate.id, values.testTarget);
      message.success('Test sent successfully, please check the inbox');
      setIsTestModalVisible(false);
    } catch (error: any) {
      if (error?.errorFields) return;
      message.error('Send failed');
    }
  };

  const renderPreview = (text: string, data: PreviewData) => {
    let result = text;
    Object.keys(data).forEach((key) => {
      result = result.replace(new RegExp(`{{${key}}}`, 'g'), data[key] || `[${key}]`);
    });
    return result;
  };

  const getTypeInfo = (type: NotificationTemplate['notificationType']) => {
    return availableTypes.find((t) => t.value === type) || {
      label: type,
      color: 'default',
      icon: <BellOutlined />,
    };
  };

  const columns: ColumnsType<NotificationTemplate> = [
    {
      title: 'Template Code',
      dataIndex: 'templateCode',
      key: 'templateCode',
      width: 180,
      render: (text) => <Text code>{text}</Text>,
    },
    {
      title: 'Template Name',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 150,
    },
    {
      title: 'Notification Type',
      dataIndex: 'notificationType',
      key: 'notificationType',
      width: 100,
      render: (type) => {
        const info = getTypeInfo(type);
        return (
          <Tag icon={info.icon} color={info.color}>
            {info.label}
          </Tag>
        );
      },
      filters: availableTypes.map((t) => ({ text: t.label, value: t.value })),
    },
    {
      title: 'Title',
      dataIndex: 'title',
      key: 'title',
      width: 250,
      ellipsis: true,
      render: (text) => (
        <Text
          ellipsis={{
            tooltip: text,
          }}
        >
          {text}
        </Text>
      ),
    },
    {
      title: 'Variables',
      dataIndex: 'variables',
      key: 'variables',
      width: 200,
      render: (variables) => (
        <Space wrap>
          {variables?.map((v: string, i: number) => (
            <Tag key={i}>{`{{${v}}}`}</Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'isActive',
      width: 80,
      render: (isActive) =>
        isActive ? (
          <Tag color="green">Enabled</Tag>
        ) : (
          <Tag color="red">Disabled</Tag>
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
      width: 240,
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
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handlePreview(record)}
          >
            Preview
          </Button>
          <Button
            type="link"
            size="small"
            icon={<SendOutlined />}
            onClick={() => handleTest(record)}
          >
            Test
          </Button>
          <Popconfirm
            title="Confirm Deletion"
            description="Are you sure you want to delete this template?"
            onConfirm={() => handleDelete(record.id)}
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

  return (
    <div>
      <Card
        title={
          <Space>
            <BellOutlined />
            <span>Notification Template Management</span>
          </Space>
        }
        style={{ borderRadius: 12 }}
      >
        <Alert
          message="Template Management"
          description="Notification templates are used by the system to send various notifications. Templates support variables in the format {{variableName}}, which are automatically replaced with actual values when sent."
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
        />

        <Card size="small" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={8}>
              <Search
                placeholder="Search by template name, code, or title"
                allowClear
                value={searchValue}
                onChange={(e) => setSearchValue(e.target.value)}
                prefix={<SearchOutlined />}
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Select
                placeholder="Select notification type"
                allowClear
                style={{ width: '100%' }}
                value={typeFilter === 'all' ? undefined : typeFilter}
                onChange={(value) => setTypeFilter(value || 'all')}
              >
                {availableTypes.map((type) => (
                  <Option key={type.value} value={type.value}>
                    {type.icon}
                    <span style={{ marginLeft: 8 }}>{type.label}</span>
                  </Option>
                ))}
              </Select>
            </Col>
            <Col xs={24} sm={24} lg={10}>
              <Space>
                <Button icon={<ReloadOutlined />} onClick={fetchTemplates}>
                  Refresh
                </Button>
                <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                  Add Template
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>

        <Table
          columns={columns}
          dataSource={filteredTemplates}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `${total} total`,
          }}
        />
      </Card>

      {/* Add/Edit Template modal */}
      <Modal
        title={editingTemplate ? 'Edit Template' : 'Add Template'}
        open={isTemplateModalVisible}
        onOk={handleTemplateModalOk}
        onCancel={() => {
          setIsTemplateModalVisible(false);
          form.resetFields();
        }}
        width={900}
        destroyOnClose
      >
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <Tabs.TabPane tab="Basic Info" key="form">
            <Form form={form} layout="vertical" preserve={false}>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Template Code"
                    name="templateCode"
                    rules={[{ required: true, message: 'Please enter the template code' }]}
                  >
                    <Input placeholder="e.g. EMAIL_VERIFY_CODE" disabled={!!editingTemplate} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="Template Name"
                    name="templateName"
                    rules={[{ required: true, message: 'Please enter the template name' }]}
                  >
                    <Input placeholder="e.g. Email Verification Code" />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Notification Type"
                    name="notificationType"
                    rules={[{ required: true, message: 'Please select a notification type' }]}
                  >
                    <Select>
                      {availableTypes.map((type) => (
                        <Option key={type.value} value={type.value}>
                          {type.icon}
                          <span style={{ marginLeft: 8 }}>{type.label}</span>
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    label="Status"
                    name="isActive"
                    rules={[{ required: true, message: 'Please select a status' }]}
                  >
                    <Select>
                      <Option value={1}>Enabled</Option>
                      <Option value={0}>Disabled</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item
                label="Title"
                name="title"
                rules={[{ required: true, message: 'Please enter a title' }]}
                extra="Variables can be used in the format {{variableName}}"
              >
                <Input placeholder="e.g. Verification Code - {{systemName}}" />
              </Form.Item>

              <Form.Item
                label="Content"
                name="content"
                rules={[{ required: true, message: 'Please enter content' }]}
                extra="Variables can be used in the format {{variableName}}"
              >
                <TextArea rows={6} placeholder="e.g. Dear {{userName}}, your verification code is: {{verifyCode}}" />
              </Form.Item>

              <Form.Item
                label="Description"
                name="description"
              >
                <TextArea rows={2} placeholder="Please enter the template description" />
              </Form.Item>
            </Form>
          </Tabs.TabPane>

          <Tabs.TabPane tab="Available Variables" key="variables">
            <Alert
              message="Variable Info"
              description="Click a variable to insert it into the template automatically. Variable format: {{variableName}}"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />

            <Card size="small">
              <Space direction="vertical" style={{ width: '100%' }}>
                {COMMON_VARIABLES.map((variable) => (
                  <Card
                    size="small"
                    key={variable.name}
                    hoverable
                    onClick={() => {
                      const title = form.getFieldValue('title') || '';
                      const content = form.getFieldValue('content') || '';
                      form.setFieldsValue({
                        title: title + variable.name,
                        content: content + variable.name,
                      });
                    }}
                    style={{ cursor: 'pointer' }}
                  >
                    <Row>
                      <Col span={12}>
                        <Text code copyable={{ text: variable.name }}>
                          {variable.name}
                        </Text>
                      </Col>
                      <Col span={12}>
                        <Text type="secondary">{variable.description}</Text>
                      </Col>
                    </Row>
                  </Card>
                ))}
              </Space>
            </Card>
          </Tabs.TabPane>
        </Tabs>
      </Modal>

      {/* Preview modal */}
      <Modal
        title="Preview Template"
        open={isPreviewModalVisible}
        onCancel={() => setIsPreviewModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setIsPreviewModalVisible(false)}>
            Close
          </Button>,
        ]}
        width={700}
      >
        {previewTemplate && (
          <div>
            <Divider orientation="left">Variable Settings</Divider>
            <Card size="small" style={{ marginBottom: 16 }}>
              <Form layout="vertical">
                {previewTemplate.variables.map((variable) => (
                  <Form.Item key={variable} label={`{{${variable}}}`}>
                    <Input
                      value={previewData[variable]}
                      onChange={(e) =>
                        setPreviewData({ ...previewData, [variable]: e.target.value })
                      }
                      placeholder={`Please enter a value for ${variable}`}
                    />
                  </Form.Item>
                ))}
              </Form>
            </Card>

            <Divider orientation="left">Preview</Divider>
            <Card size="small">
              <Space direction="vertical" style={{ width: '100%' }}>
                <div>
                  <Text strong>Title:</Text>
                  <div style={{ marginTop: 8 }}>
                    <Title level={5}>
                      {renderPreview(previewTemplate.title, previewData)}
                    </Title>
                  </div>
                </div>
                <div>
                  <Text strong>Content:</Text>
                  <div style={{ marginTop: 8, whiteSpace: 'pre-wrap', background: '#f5f5f5', padding: 12 }}>
                    {renderPreview(previewTemplate.content, previewData)}
                  </div>
                </div>
              </Space>
            </Card>
          </div>
        )}
      </Modal>

      {/* Test send modal */}
      <Modal
        title="Send Test"
        open={isTestModalVisible}
        onOk={handleSendTest}
        onCancel={() => setIsTestModalVisible(false)}
        width={600}
      >
        {previewTemplate && (
          <Form form={testForm} layout="vertical" preserve={false}>
            <Alert
              message="Test Info"
              description="This sends a test notification using the current template and sample data. Please make sure the target address is correct."
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
            />

            <Form.Item
              label="Test Target"
              name="testTarget"
              rules={[{ required: true, message: 'Please enter a test target' }]}
              extra={previewTemplate.notificationType === 'EMAIL' ? 'Please enter an email address' : 'Please enter a phone number'}
            >
              <Input
                placeholder={
                  previewTemplate.notificationType === 'EMAIL'
                    ? 'test@example.com'
                    : '+86 13800000000'
                }
              />
            </Form.Item>

            <Divider>Preview Content</Divider>
            <Card size="small">
              <Space direction="vertical" style={{ width: '100%' }}>
                <div>
                  <Text strong>Title:</Text>
                  <div>{previewTemplate.title}</div>
                </div>
                <div>
                  <Text strong>Content:</Text>
                  <div style={{ whiteSpace: 'pre-wrap' }}>{previewTemplate.content}</div>
                </div>
              </Space>
            </Card>
          </Form>
        )}
      </Modal>
    </div>
  );
};

export default NotificationTemplatePage;
