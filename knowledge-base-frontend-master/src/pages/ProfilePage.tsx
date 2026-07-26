import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  Form,
  Input,
  Button,
  Upload,
  Typography,
  Space,
  Divider,
  Row,
  Col,
  Statistic,
  Tag,
  Modal,
} from 'antd';
import { App } from 'antd';
import type { UploadFile } from 'antd';
import type { UploadChangeParam } from 'antd/es/upload';
import {
  UserOutlined,
  MailOutlined,
  LockOutlined,
  SaveOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { useAuthStore, useAppStore } from '@/stores';
import { authService } from '@/services';
import { userService } from '@/services/user.service';
import type { User } from '@/types';
import UserAvatar from '@/components/common/UserAvatar';

const { Title, Text } = Typography;

export const ProfilePage: React.FC = () => {
  const { message } = App.useApp();
  const { user, logout, updateUser } = useAuthStore();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState<string | undefined>(user?.avatar);
  const [stats, setStats] = useState<{ documentCount: number; viewCount: number; likeCount: number; commentCount: number } | null>(null);
  const [, setFileList] = useState<UploadFile[]>([]);
  const maxFileSize = useAppStore((s) => s.maxFileSize);
  const allowedFileTypes = useAppStore((s) => s.allowedFileTypes);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const result = await userService.getUserStats();
        if (result) {
          setStats(result as { documentCount: number; viewCount: number; likeCount: number; commentCount: number });
        }
      } catch {
        // A failure to load stats doesn't affect page usability
      }
    };
    if (user) {
      fetchStats();
    }
  }, [user]);

  const handleProfileUpdate = async (values: Record<string, unknown>) => {
    setLoading(true);
    try {
      // Build the UserDTO request payload
      const params = {
        id: user?.id,
        username: values.username,
        email: values.email,
        department: values.department,
        position: values.position,
        remark: values.remark,
        avatar: avatarUrl,
      };
      await authService.updateProfile(params);
      // Update the user info in the auth store
      if (user) {
        updateUser({ ...user, ...params } as User);
      }
      message.success('Profile updated successfully');
    } catch {
      message.error('Failed to save changes');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordChange = async (values: { currentPassword: string; newPassword: string }) => {
    setPasswordLoading(true);
    try {
      await authService.changePassword({
        oldPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      message.success('Password changed successfully. Please sign in again.');
      passwordForm.resetFields();
      // Log out after changing the password
      setTimeout(async () => {
        await logout();
        navigate('/login');
      }, 1500);
    } catch {
      // Error messages are handled centrally by the request interceptor
    } finally {
      setPasswordLoading(false);
    }
  };

  const handleAvatarChange = async (info: UploadChangeParam<UploadFile>) => {
    setFileList(info.fileList);
    if (info.file.status === 'done') {
      const res = info.file.response;
      if (res && (res.code === 200 || res.code === 0)) {
        const fileUrl = res.data?.fileUrl || res.url;
        setAvatarUrl(fileUrl);
        // After a successful upload, save the avatar URL to the user profile (required fields must be included)
        try {
          await authService.updateProfile({
            id: user?.id,
            username: user?.username,
            email: user?.email,
            avatar: fileUrl,
          });
          if (user) {
            updateUser({ ...user, avatar: fileUrl });
          }
        } catch {
          // The avatar file uploaded successfully; a failure to save it to the profile doesn't affect the message
        }
        message.success('Avatar uploaded successfully');
      } else {
        message.error(res?.message || 'Failed to upload avatar');
      }
    } else if (info.file.status === 'error') {
      message.error('Failed to upload avatar');
    }
  };

  const beforeUpload = (file: File) => {
    // Validate against the system-configured allowed file types
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    const allowedExts = allowedFileTypes.split(',').map(t => {
      const trimmed = t.trim().toLowerCase();
      return trimmed.startsWith('.') ? trimmed : '.' + trimmed;
    });
    if (!allowedExts.includes(ext)) {
      message.error(`Unsupported file type: ${ext}. Allowed types: ${allowedFileTypes}`);
      return false;
    }
    // Avatar is capped at 5MB and must not exceed the system-configured max file size
    const maxAvatarSize = Math.min(maxFileSize, 5 * 1024 * 1024);
    if (file.size > maxAvatarSize) {
      const maxMB = Math.round(maxAvatarSize / 1048576 * 10) / 10;
      message.error(`Image size cannot exceed ${maxMB}MB`);
      return false;
    }
    return true;
  };

  return (
    <div>
      <Title level={2} style={{ marginBottom: 24 }}>
        Profile
      </Title>

      <Row gutter={[24, 24]}>
        {/* Avatar and basic information */}
        <Col xs={24} lg={8}>
          <Card style={{ borderRadius: 12 }}>
            <div style={{ textAlign: 'center', marginBottom: 24 }}>
              <Upload
                name="file"
                listType="picture-circle"
                className="avatar-uploader"
                showUploadList={false}
                action="/api/file/files/upload"
                beforeUpload={beforeUpload}
                onChange={handleAvatarChange}
              >
                <div style={{ width: 120, height: 120, borderRadius: '50%', overflow: 'hidden', marginBottom: 16 }}>
                  <UserAvatar
                    src={avatarUrl}
                    alt="Avatar"
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                </div>
              </Upload>
              <div>
                <Title level={4} style={{ marginBottom: 4 }}>
                  {user?.username}
                </Title>
                <Text type="secondary">{user?.email}</Text>
              </div>
              <div style={{ marginTop: 16 }}>
                <Space>
                  <Tag color="blue">{user?.role === 'admin' ? 'Administrator' : 'Regular User'}</Tag>
                  <Tag color="green">Online</Tag>
                </Space>
              </div>
            </div>

            <Divider />

            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title="Documents"
                  value={stats?.documentCount ?? 0}
                  prefix={<UserOutlined />}
                  valueStyle={{ color: '#1890ff' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="Likes"
                  value={stats?.likeCount ?? 0}
                  prefix={<UserOutlined />}
                  valueStyle={{ color: '#52c41a' }}
                />
              </Col>
            </Row>
          </Card>
        </Col>

        {/* Personal information form */}
        <Col xs={24} lg={16}>
          <Card
            title="Basic Information"
            style={{ borderRadius: 12, marginBottom: 24 }}
          >
            <Form
              form={form}
              layout="vertical"
              initialValues={{
                username: user?.username,
                email: user?.email,
                department: user?.department,
                position: user?.position,
                remark: user?.remark,
              }}
              onFinish={handleProfileUpdate}
            >
              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Username"
                    name="username"
                    rules={[{ required: true, message: 'Please enter a username' }]}
                  >
                    <Input
                      prefix={<UserOutlined />}
                      placeholder="Enter a username"
                      size="large"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Email"
                    name="email"
                    rules={[
                      { required: true, message: 'Please enter an email address' },
                      { type: 'email', message: 'Please enter a valid email address' },
                    ]}
                  >
                    <Input
                      prefix={<MailOutlined />}
                      placeholder="Enter your email"
                      size="large"
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col xs={24} md={12}>
                  <Form.Item label="Department" name="department">
                    <Input placeholder="Enter your department" size="large" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item label="Position" name="position">
                    <Input
                      prefix={<UserOutlined />}
                      placeholder="Enter your position"
                      size="large"
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Form.Item label="Bio" name="remark">
                <Input.TextArea
                  rows={4}
                  placeholder="Tell us about yourself..."
                  showCount
                  maxLength={200}
                />
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SaveOutlined />}
                  loading={loading}
                  size="large"
                  style={{
                    background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                    border: 'none',
                  }}
                >
                  Save Changes
                </Button>
              </Form.Item>
            </Form>
          </Card>

          {/* Change password */}
          <Card title="Change Password" style={{ borderRadius: 12 }}>
            <Form
              form={passwordForm}
              layout="vertical"
              onFinish={handlePasswordChange}
            >
              <Form.Item
                label="Current Password"
                name="currentPassword"
                rules={[{ required: true, message: 'Please enter your current password' }]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="Enter your current password"
                  size="large"
                />
              </Form.Item>

              <Form.Item
                label="New Password"
                name="newPassword"
                rules={[
                  { required: true, message: 'Please enter a new password' },
                  { min: 6, message: 'Password must be at least 6 characters' },
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="Enter a new password"
                  size="large"
                />
              </Form.Item>

              <Form.Item
                label="Confirm New Password"
                name="confirmPassword"
                dependencies={['newPassword']}
                rules={[
                  { required: true, message: 'Please confirm your new password' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('newPassword') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('The two passwords do not match'));
                    },
                  }),
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="Confirm your new password"
                  size="large"
                />
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SaveOutlined />}
                  loading={passwordLoading}
                  size="large"
                  style={{
                    background: 'linear-gradient(135deg, #52c41a, #1890ff)',
                    border: 'none',
                  }}
                >
                  Change Password
                </Button>
              </Form.Item>
            </Form>
          </Card>

          {/* Sign out */}
          <Card style={{ borderRadius: 12, marginTop: 24 }}>
            <div style={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              gap: '16px',
            }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '14px' }}>
                Securely sign out of your account
              </span>
              <Button
                type="primary"
                icon={<LogoutOutlined />}
                size="large"
                style={{
                  background: 'linear-gradient(135deg, #6366f1, #4f46e5)',
                  border: 'none',
                  boxShadow: '0 2px 8px rgba(99, 102, 241, 0.25)',
                }}
                onClick={() => {
                  Modal.confirm({
                    title: 'Confirm Sign Out',
                    content: 'Are you sure you want to sign out?',
                    okText: 'Sign Out',
                    cancelText: 'Cancel',
                    onOk: async () => {
                      await logout();
                      message.success('Signed out successfully');
                      navigate('/login');
                    },
                  });
                }}
              >
                Sign Out
              </Button>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ProfilePage;
