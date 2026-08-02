import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Form,
  Input,
  Switch,
  Button,
  Space,
  Select,
  Row,
  Col,
  Typography,
  Tag,
  Progress,
  Tabs,
  InputNumber,
  Spin,
  Result,
  Divider,
  Statistic,
  Popconfirm,
} from 'antd';
import { App } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  SettingOutlined,
  SecurityScanOutlined,
  CloudServerOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  BellOutlined,
  RobotOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  MailOutlined,
  SendOutlined,
  CloudUploadOutlined,
} from '@ant-design/icons';
import { settingsService } from '@/services';
import { useAppStore } from '@/stores';
import type { SystemSettings } from '@/types';
import dayjs from 'dayjs';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;

type SettingsTab = 'basic' | 'security' | 'storage' | 'notification' | 'ai' | 'status';

interface TabConfig {
  key: SettingsTab;
  label: string;
  icon: React.ReactNode;
}

const TABS: TabConfig[] = [
  { key: 'basic',         label: 'Basic Settings',        icon: <SettingOutlined /> },
  { key: 'security',      label: 'Security Settings',     icon: <SecurityScanOutlined /> },
  { key: 'storage',       label: 'Storage Settings',      icon: <CloudServerOutlined /> },
  { key: 'notification',  label: 'Notification Settings', icon: <BellOutlined /> },
  { key: 'ai',            label: 'AI Settings',           icon: <RobotOutlined /> },
  { key: 'status',        label: 'System Status',         icon: <DatabaseOutlined /> },
];

// eslint-disable-next-line @typescript-eslint/no-empty-interface
interface SettingsPageState {
  loading: boolean;
  saving: boolean;
  settings: SystemSettings | null;
  error: string | null;
  activeTab: SettingsTab;
}

export const SettingsPage: React.FC = () => {
  const { message } = App.useApp();

  const [state, setState] = useState<SettingsPageState>({
    loading: true,
    saving: false,
    settings: null,
    error: null,
    activeTab: 'basic',
  });

  const [basicForm]    = Form.useForm();
  const [securityForm] = Form.useForm();
  const [storageForm]  = Form.useForm();
  const [notifForm]    = Form.useForm();
  const [aiForm]       = Form.useForm();

  const enableEmail = useAppStore((s) => s.enableEmail);

  // ---- Data Fetching ----

  const fetchSettings = useCallback(async () => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    try {
      const data = await settingsService.getSettings();
      setState(prev => ({ ...prev, settings: data, loading: false }));

      // Populate all forms
      if (data.basic)    basicForm.setFieldsValue(data.basic);
      if (data.security) securityForm.setFieldsValue(data.security);
      if (data.storage)  storageForm.setFieldsValue(data.storage);
      if (data.notification) notifForm.setFieldsValue(data.notification);
      if (data.ai)       aiForm.setFieldsValue(data.ai);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load settings';
      setState(prev => ({ ...prev, loading: false, error: msg }));
      message.error(msg);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  // ---- Save Handlers ----

  const handleSave = useCallback(async (section: string, values: Record<string, unknown>) => {
    setState(prev => ({ ...prev, saving: true }));
    try {
      await settingsService.updateSettings(section, values);
      message.success('Settings saved');
      // Refresh to get latest server state
      await fetchSettings();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Save failed';
      message.error(msg);
    } finally {
      setState(prev => ({ ...prev, saving: false }));
    }
  }, [fetchSettings]);

  const handleSaveBasic    = () => { basicForm.validateFields().then(v => handleSave('basic', Object.fromEntries(Object.entries(v).filter(([k]) => k !== 'systemVersion')))); };
  const handleSaveSecurity = () => { securityForm.validateFields().then(v => handleSave('security', v)); };
  const handleSaveStorage  = () => { storageForm.validateFields().then(v => handleSave('storage', v)); };
  const handleSaveNotif    = () => { notifForm.validateFields().then(v => handleSave('notification', Object.fromEntries(Object.entries(v).filter(([k]) => k !== 'emailTestAddress')))); };
  const handleSaveAI       = () => { aiForm.validateFields().then(v => handleSave('ai', v)); };

  // ---- Status Actions ----

  const handleClearCache = async () => {
    try {
      const result = await settingsService.clearCache();
      message.success(result || 'Cache cleared');
    } catch {
      message.error('Failed to clear cache');
    }
  };

  const handleBackup = async () => {
    try {
      const result = await settingsService.createBackup();
      message.success(result || 'Backup created successfully');
    } catch {
      message.error('Failed to create backup');
    }
  };

  const handleTestEmail = async () => {
    try {
      const email = notifForm.getFieldValue('emailTestAddress');
      if (!email) {
        message.warning('Please configure a test email address in Notification Settings first');
        return;
      }
      await settingsService.testEmail(email);
      message.success('Test email sent');
    } catch {
      message.error('Failed to send test email');
    }
  };

  // ---- Tab Change ----

  const handleTabChange = (key: string) => {
    setState(prev => ({ ...prev, activeTab: key as SettingsTab }));
  };

  // ---- Render Helpers ----

  const renderSwitchItem = (
    title: string,
    description: string,
    name: string,
  ) => (
    <div style={SWITCH_ITEM_STYLE}>
      <div style={{ flex: 1, paddingRight: 24 }}>
        <div style={SWITCH_TITLE_STYLE}>{title}</div>
        <div style={SWITCH_DESC_STYLE}>{description}</div>
      </div>
      <Form.Item name={name} valuePropName="checked" noStyle>
        <Switch />
      </Form.Item>
    </div>
  );

  const renderSectionHeader = (
    icon: React.ReactNode,
    title: string,
    description: string,
    onSave: () => void,
  ) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
      <div>
        <Space>
          <span style={{ color: '#2563eb', fontSize: 20 }}>{icon}</span>
          <div>
            <Title level={4} style={{ margin: 0 }}>{title}</Title>
            <Text type="secondary" style={{ fontSize: 13 }}>{description}</Text>
          </div>
        </Space>
      </div>
      <Space>
        <Button icon={<ReloadOutlined />} onClick={fetchSettings} disabled={state.saving}>
          Reset
        </Button>
        <Button
          type="primary"
          icon={<SaveOutlined />}
          onClick={onSave}
          loading={state.saving}
        >
          Save Settings
        </Button>
      </Space>
    </div>
  );

  const renderSaveBar = (onSave: () => void) => (
    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, padding: '16px 0 0 0', borderTop: '1px solid #f1f5f9', marginTop: 24 }}>
      <Button icon={<ReloadOutlined />} onClick={fetchSettings} disabled={state.saving}>
        Reset
      </Button>
      <Button type="primary" icon={<SaveOutlined />} onClick={onSave} loading={state.saving}>
        Save Settings
      </Button>
    </div>
  );

  // ---- Main Render ----

  if (state.loading) {
    return (
      <div style={PAGE_STYLE}>
        <div style={{ padding: '80px 0', textAlign: 'center' }}>
          <Spin size="large" />
          <Paragraph type="secondary" style={{ marginTop: 16 }}>Loading system settings...</Paragraph>
        </div>
      </div>
    );
  }

  if (state.error && !state.settings) {
    return (
      <div style={PAGE_STYLE}>
        <Result
          status="error"
          title="Failed to load settings"
          subTitle={state.error}
          extra={
            <Button type="primary" icon={<ReloadOutlined />} onClick={fetchSettings}>
              Reload
            </Button>
          }
        />
      </div>
    );
  }

  const { settings } = state;

  return (
    <div style={PAGE_STYLE}>
      {/* Page Header */}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0, marginBottom: 8, fontSize: 28, fontWeight: 700 }}>
            System Settings
          </Title>
          <Text type="secondary">Configure the knowledge base system's basic settings and runtime parameters</Text>
        </div>
      </div>

      <Tabs
        activeKey={state.activeTab}
        onChange={handleTabChange}
        items={TABS.map(tab => ({
          key: tab.key,
          label: (
            <Space>
              {tab.icon}
              <span>{tab.label}</span>
            </Space>
          ),
          children: renderTabContent(tab.key),
        }))}
        tabBarStyle={{ marginBottom: 0 }}
      />
      <style>{TAB_CARD_STYLE}</style>
    </div>
  );

  function renderTabContent(tab: SettingsTab): React.ReactNode {
    switch (tab) {
      case 'basic':
        return renderBasicTab();
      case 'security':
        return renderSecurityTab();
      case 'storage':
        return renderStorageTab();
      case 'notification':
        return renderNotificationTab();
      case 'ai':
        return renderAITab();
      case 'status':
        return renderStatusTab();
      default:
        return null;
    }
  }

  // ===================== BASIC TAB =====================
  function renderBasicTab() {
    return (
      <Card style={CARD_STYLE} styles={{ body: { padding: '24px 32px' } }}>
        {renderSectionHeader(
          <SettingOutlined />,
          'Basic Settings',
          'Configure basic system information, language, and feature toggles',
          handleSaveBasic,
        )}
        <Form form={basicForm} layout="vertical">
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="System Name"
                name="systemName"
                rules={[{ required: true, message: 'Please enter the system name' }]}
              >
                <Input placeholder="The system name shown in the page title and navigation bar" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="System Description"
                name="systemDescription"
                rules={[{ required: true, message: 'Please enter the system description' }]}
              >
                <Input placeholder="A short description of the system" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Default Language"
                name="defaultLanguage"
                rules={[{ required: true, message: 'Please select a default language' }]}
              >
                <Select>
                  <Option value="zh-CN">Simplified Chinese</Option>
                  <Option value="en-US">English</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Timezone"
                name="timezone"
                rules={[{ required: true, message: 'Please select a timezone' }]}
              >
                <Select>
                  <Option value="Asia/Shanghai">Asia/Shanghai (UTC+8)</Option>
                  <Option value="Asia/Tokyo">Asia/Tokyo (UTC+9)</Option>
                  <Option value="America/New_York">America/New_York (UTC-5)</Option>
                  <Option value="Europe/London">Europe/London (UTC+0)</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="System Version"
                name="systemVersion"
              >
                <Input disabled />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left" style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Feature Toggles</Text>
          </Divider>

          {renderSwitchItem('User Registration', 'Whether new users can register accounts on their own', 'allowRegistration')}
          {renderSwitchItem('Document Review', 'Whether newly published documents require review', 'requireApproval')}
          {renderSwitchItem('Comments', 'Whether users can comment on documents', 'enableComments')}
          {renderSwitchItem('AI Assistant', 'Enable the AI Q&A assistant feature', 'enableAI')}
          {renderSwitchItem('AI Writing', 'Enable the AI writing assistance feature', 'enableAIWriting')}
          {renderSwitchItem('Full-Text Search', 'Enable full-text document search', 'enableFullTextSearch')}
        </Form>
        {renderSaveBar(handleSaveBasic)}
      </Card>
    );
  }

  // ===================== SECURITY TAB =====================
  function renderSecurityTab() {
    return (
      <Card style={CARD_STYLE} styles={{ body: { padding: '24px 32px' } }}>
        {renderSectionHeader(
          <SecurityScanOutlined />,
          'Security Settings',
          'Configure password policy, session management, and login security',
          handleSaveSecurity,
        )}
        <Form form={securityForm} layout="vertical">
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Password Policy"
                name="passwordPolicy"
                rules={[{ required: true, message: 'Please select a password policy' }]}
              >
                <Select>
                  <Option value="low">Low (6+ characters)</Option>
                  <Option value="medium">Medium (8+ chars, letters and numbers)</Option>
                  <Option value="high">High (12+ chars, special characters)</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Session Timeout (seconds)"
                name="sessionTimeout"
                rules={[{ required: true, message: 'Please enter the session timeout' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={300}
                  max={86400}
                  step={300}
                  addonAfter="sec"
                  placeholder="3600"
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Minimum Password Length"
                name="passwordMinLength"
                rules={[{ required: true, message: 'Please enter the minimum password length' }]}
              >
                <InputNumber style={{ width: '100%' }} min={4} max={32} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Max Login Attempts"
                name="loginMaxRetry"
                rules={[{ required: true, message: 'Please enter the maximum login attempts' }]}
              >
                <InputNumber style={{ width: '100%' }} min={1} max={20} />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left" style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Security Toggles</Text>
          </Divider>

          {renderSwitchItem('Two-Factor Authentication', 'Enable two-factor authentication (2FA) for users', 'enable2FA')}
          {renderSwitchItem('Login Restriction', 'Restrict login by IP address', 'ipRestriction')}
          {renderSwitchItem('Special Character Requirement', 'Passwords must contain special characters', 'requireSpecialChar')}
        </Form>
        {renderSaveBar(handleSaveSecurity)}
      </Card>
    );
  }

  // ===================== STORAGE TAB =====================
  function renderStorageTab() {
    const status = settings?.status;
    const usedPercent = status ? Math.round((status.usedStorage / status.totalStorage) * 100) : 0;

    return (
      <Card style={CARD_STYLE} styles={{ body: { padding: '24px 32px' } }}>
        {renderSectionHeader(
          <CloudServerOutlined />,
          'Storage Settings',
          'Configure file storage policy and upload limits',
          handleSaveStorage,
        )}

        {/* Storage Stats */}
        {status && (
          <Row gutter={16} style={{ marginBottom: 24 }}>
            <Col span={6}>
              <Card size="small" style={STAT_CARD_STYLE}>
                <Statistic
                  title="Total Storage"
                  value={formatBytes(status.totalStorage)}
                  valueStyle={{ fontSize: 16, fontWeight: 600 }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small" style={STAT_CARD_STYLE}>
                <Statistic
                  title="Used"
                  value={formatBytes(status.usedStorage)}
                  suffix={`(${usedPercent}%)`}
                  valueStyle={{ fontSize: 16, fontWeight: 600 }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small" style={STAT_CARD_STYLE}>
                <Statistic
                  title="Documents"
                  value={status.documentCount}
                  valueStyle={{ fontSize: 16, fontWeight: 600 }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small" style={STAT_CARD_STYLE}>
                <Statistic
                  title="Users"
                  value={status.userCount}
                  valueStyle={{ fontSize: 16, fontWeight: 600 }}
                />
              </Card>
            </Col>
          </Row>
        )}

        {status && (
          <div style={{ marginBottom: 24 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <Text type="secondary">Storage Usage</Text>
              <Text>{usedPercent}%</Text>
            </div>
            <Progress
              percent={usedPercent}
              strokeColor={usedPercent > 80 ? '#ef4444' : usedPercent > 60 ? '#f59e0b' : '#2563eb'}
            />
          </div>
        )}

        <Form form={storageForm} layout="vertical">
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Max File Size (MB)"
                name="maxFileSize"
                rules={[{ required: true, message: 'Please enter the maximum file size' }]}
                getValueFromEvent={(val: number) => val}
                getValueProps={(val: number) => ({ value: val ? Math.round(val / 1048576) : null })}
                normalize={(val: number) => (val ? val * 1048576 : undefined)}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={1}
                  max={1024}
                  addonAfter="MB"
                  placeholder="100"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Allowed File Types"
                name="allowedFileTypes"
                rules={[{ required: true, message: 'Please select file types' }]}
              >
                <Select mode="tags" placeholder="Enter or select file types">
                  <Option value="pdf">PDF</Option>
                  <Option value="doc">DOC</Option>
                  <Option value="docx">DOCX</Option>
                  <Option value="xlsx">XLSX</Option>
                  <Option value="pptx">PPTX</Option>
                  <Option value="txt">TXT</Option>
                  <Option value="md">Markdown</Option>
                  <Option value="jpg">JPG</Option>
                  <Option value="png">PNG</Option>
                  <Option value="gif">GIF</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Storage Endpoint"
                name="storageEndpoints"
              >
                <Input placeholder="http://localhost:8200" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Storage Bucket Name"
                name="storageBucket"
              >
                <Input placeholder="knowledge-docs" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
        {renderSaveBar(handleSaveStorage)}
      </Card>
    );
  }

  // ===================== NOTIFICATION TAB =====================
  function renderNotificationTab() {
    return (
      <Card style={CARD_STYLE} styles={{ body: { padding: '24px 32px' } }}>
        {renderSectionHeader(
          <BellOutlined />,
          'Notification Settings',
          'Configure email notifications and WebSocket push',
          handleSaveNotif,
        )}
        <Form form={notifForm} layout="vertical">
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="SMTP Server"
                name="emailHost"
              >
                <Input placeholder="smtp.example.com" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="SMTP Port"
                name="emailPort"
              >
                <InputNumber style={{ width: '100%' }} min={1} max={65535} placeholder="587" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Notification Retention (days)"
                name="notificationRetentionDays"
              >
                <InputNumber style={{ width: '100%' }} min={1} max={365} addonAfter="days" placeholder="90" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Test Email Address"
                name="emailTestAddress"
              >
                <Input placeholder="admin@example.com" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left" style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Notification Toggles</Text>
          </Divider>

          {renderSwitchItem('Email Notifications', 'Enable email notifications', 'emailEnabled')}
          {renderSwitchItem('WebSocket Push', 'Enable real-time message push', 'websocketEnabled')}

          {enableEmail && (
            <div style={{ marginTop: 16 }}>
              <Space>
                <Button icon={<SendOutlined />} onClick={handleTestEmail}>
                  Send Test Email
                </Button>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  Click to send a test email and verify the email configuration
                </Text>
              </Space>
            </div>
          )}
        </Form>
        {renderSaveBar(handleSaveNotif)}
      </Card>
    );
  }

  // ===================== AI TAB =====================
  function renderAITab() {
    return (
      <Card style={CARD_STYLE} styles={{ body: { padding: '24px 32px' } }}>
        {renderSectionHeader(
          <RobotOutlined />,
          'AI Settings',
          'Configure LLM and vector database connection parameters',
          handleSaveAI,
        )}
        <Form form={aiForm} layout="vertical">
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="LLM Model Name"
                name="aiModelName"
                rules={[{ required: true, message: 'Please enter the model name' }]}
              >
                <Input placeholder="qwen-max" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Embedding Model"
                name="embeddingModel"
                rules={[{ required: true, message: 'Please enter the embedding model name' }]}
              >
                <Input placeholder="text-embedding-v3" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={[24, 0]}>
            <Col span={12}>
              <Form.Item
                label="Milvus Host"
                name="milvusHost"
                rules={[{ required: true, message: 'Please enter the Milvus host address' }]}
              >
                <Input placeholder="localhost" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Milvus Port"
                name="milvusPort"
                rules={[{ required: true, message: 'Please enter the Milvus port' }]}
              >
                <InputNumber style={{ width: '100%' }} min={1} max={65535} placeholder="19530" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
        {renderSaveBar(handleSaveAI)}
      </Card>
    );
  }

  // ===================== STATUS TAB =====================
  function renderStatusTab() {
    const status = settings?.status;

    if (!status) {
      return (
        <Card style={CARD_STYLE}>
          <Result
            status="warning"
            title="Unable to fetch system status"
            extra={
              <Button icon={<ReloadOutlined />} onClick={fetchSettings}>
                Reload
              </Button>
            }
          />
        </Card>
      );
    }

    return (
      <Card style={CARD_STYLE} styles={{ body: { padding: '24px 32px' } }}>
        {renderSectionHeader(
          <DatabaseOutlined />,
          'System Status',
          'View system runtime status and perform maintenance operations',
          () => fetchSettings(),
        )}

        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col span={8}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <Statistic
                title="System Version"
                value={status.version}
                prefix={<Tag color="blue" style={{ marginRight: 0 }}>v</Tag>}
                valueStyle={{ fontSize: 18, fontWeight: 700 }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 8 }}>Run Status</div>
              <Space>
                {status.runStatus === 'running' ? (
                  <>
                    <CheckCircleOutlined style={{ color: '#10b981', fontSize: 16 }} />
                    <Tag color="success" style={{ margin: 0 }}>Running Normally</Tag>
                  </>
                ) : (
                  <>
                    <CloseCircleOutlined style={{ color: '#ef4444', fontSize: 16 }} />
                    <Tag color="error" style={{ margin: 0 }}>{status.runStatus}</Tag>
                  </>
                )}
              </Space>
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 8 }}>Database Status</div>
              <Space>
                {status.dbStatus === 'connected' ? (
                  <>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#10b981' }} />
                    <Tag color="success" style={{ margin: 0 }}>Connected</Tag>
                  </>
                ) : (
                  <>
                    <div style={{ width: 8, height: 8, borderRadius: '50%', background: '#ef4444' }} />
                    <Tag color="error" style={{ margin: 0 }}>Connection Error</Tag>
                  </>
                )}
              </Space>
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col span={6}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <Statistic
                title="System Start Time"
                value={status.startTime ? dayjs(status.startTime).format('MM-DD HH:mm') : '-'}
                valueStyle={{ fontSize: 14, fontWeight: 600 }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <Statistic
                title="Last Backup"
                value={status.lastBackupTime || 'None'}
                valueStyle={{ fontSize: 14, fontWeight: 600 }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <Statistic
                title="Total Documents"
                value={status.documentCount}
                valueStyle={{ fontSize: 14, fontWeight: 600 }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small" style={STAT_CARD_STYLE}>
              <Statistic
                title="Registered Users"
                value={status.userCount}
                valueStyle={{ fontSize: 14, fontWeight: 600 }}
              />
            </Card>
          </Col>
        </Row>

        <Divider orientation="left">
          <Text type="secondary" style={{ fontSize: 12 }}>Maintenance Operations</Text>
        </Divider>

        <Row gutter={[16, 16]}>
          <Col span={8}>
            <Card size="small" style={OP_CARD_STYLE}>
              <div style={OP_CARD_CONTENT_STYLE}>
                <div>
                  <Text strong>Clear Cache</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    Clear the Redis cache to free up memory
                  </Text>
                </div>
              </div>
              <Popconfirm
                title="Confirm Cache Clear"
                description="After clearing the cache, some data will need to reload. Continue?"
                onConfirm={handleClearCache}
                okText="OK"
                cancelText="Cancel"
              >
                <Button icon={<DeleteOutlined />} danger size="small">
                  Clear Cache
                </Button>
              </Popconfirm>
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small" style={OP_CARD_STYLE}>
              <div style={OP_CARD_CONTENT_STYLE}>
                <div>
                  <Text strong>Data Backup</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    Create a full backup of system data
                  </Text>
                </div>
              </div>
              <Popconfirm
                title="Confirm Backup Creation"
                description="Creating a backup may take a few minutes. Continue?"
                onConfirm={handleBackup}
                okText="OK"
                cancelText="Cancel"
              >
                <Button icon={<CloudUploadOutlined />} size="small">
                  Back Up Now
                </Button>
              </Popconfirm>
            </Card>
          </Col>
          {enableEmail && (
            <Col span={8}>
              <Card size="small" style={OP_CARD_STYLE}>
                <div style={OP_CARD_CONTENT_STYLE}>
                  <div>
                    <Text strong>Email Test</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      Test whether the email service is configured correctly
                    </Text>
                  </div>
                </div>
                <Button
                  icon={<MailOutlined />}
                  size="small"
                  onClick={handleTestEmail}
                >
                  Send Test Email
                </Button>
              </Card>
            </Col>
          )}
        </Row>
      </Card>
    );
  }
};

// ---- Utility ----

function formatBytes(bytes: number): string {
  if (bytes >= 1073741824) return (bytes / 1073741824).toFixed(1) + ' GB';
  if (bytes >= 1048576)    return (bytes / 1048576).toFixed(1) + ' MB';
  if (bytes >= 1024)       return (bytes / 1024).toFixed(1) + ' KB';
  return bytes + ' B';
}

// ---- Styles ----

const PAGE_STYLE: React.CSSProperties = {
  padding: '24px 24px 32px 24px',
  background: '#f8fafc',
  minHeight: '100vh',
};

const CARD_STYLE: React.CSSProperties = {
  borderRadius: 12,
  border: '1px solid #e2e8f0',
  marginTop: 16,
};

const STAT_CARD_STYLE: React.CSSProperties = {
  borderRadius: 10,
  border: '1px solid #f1f5f9',
};

const OP_CARD_STYLE: React.CSSProperties = {
  borderRadius: 10,
  border: '1px solid #f1f5f9',
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
};

const OP_CARD_CONTENT_STYLE: React.CSSProperties = {
  flex: 1,
  paddingRight: 16,
};

const SWITCH_ITEM_STYLE: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'start',
  padding: '16px 0',
  borderBottom: '1px solid #f1f5f9',
};

const SWITCH_TITLE_STYLE: React.CSSProperties = {
  fontSize: 15,
  fontWeight: 500,
  marginBottom: 4,
};

const SWITCH_DESC_STYLE: React.CSSProperties = {
  fontSize: 13,
  color: '#94a3b8',
};

const TAB_CARD_STYLE = `
  .ant-tabs-nav {
    margin-bottom: 0 !important;
  }
  .ant-tabs-nav::before {
    border-bottom: none !important;
  }
`;

export default SettingsPage;
