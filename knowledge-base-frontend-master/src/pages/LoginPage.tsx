import React from 'react';
import { Form, Input, Button, Typography, Tabs, Spin, Select } from 'antd';
import { App } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, IdcardOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '@/stores';
import { foundationService, authService, teamService } from '@/services';
import type { RegisterResponse } from '@/types';

const { Text } = Typography;

/** Config key for the registration tab */
const REGISTRATION_CONFIG_KEY = 'user.registration.enabled';

/** Password policy config keys */
const PW_POLICY_KEY = 'system.passwordPolicy';
const PW_MIN_LENGTH_KEY = 'auth.password.min.length';
const PW_REQUIRE_SPECIAL_KEY = 'auth.password.require.special';

interface SecurityConfig {
  passwordPolicy: string;
  passwordMinLength: number;
  requireSpecialChar: boolean;
}

const DEFAULT_SECURITY_CONFIG: SecurityConfig = {
  passwordPolicy: 'medium',
  passwordMinLength: 8,
  requireSpecialChar: true,
};

/**
 * Generate hint text based on the password policy
 */
function getPasswordHint(config: SecurityConfig): string {
  const parts: string[] = [`At least ${config.passwordMinLength} characters`];

  switch (config.passwordPolicy) {
    case 'low':
      if (config.requireSpecialChar) parts.push('include a special character');
      break;
    case 'high':
      parts.push('include uppercase letters, lowercase letters, digits, and a special character');
      break;
    case 'medium':
    default:
      parts.push('include both letters and digits');
      if (config.requireSpecialChar) parts.push('include a special character');
      break;
  }

  return 'Password requirements: ' + parts.join(', ');
}

/**
 * Generate dynamic password validation rules based on the password policy
 */
function getPasswordRules(config: SecurityConfig): any[] {
  const rules: any[] = [
    { required: true, message: 'Please enter a password' },
    { min: config.passwordMinLength, message: `Password must be at least ${config.passwordMinLength} characters` },
  ];

  switch (config.passwordPolicy) {
    case 'low':
      if (config.requireSpecialChar) {
        rules.push({
          pattern: /[^a-zA-Z0-9]/,
          message: 'Password must contain at least one special character',
        });
      }
      break;
    case 'high':
      rules.push({
        pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/,
        message: 'Password must include uppercase, lowercase, digits, and a special character',
      });
      break;
    case 'medium':
    default:
      rules.push({
        pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
        message: 'Password must include both letters and digits',
      });
      if (config.requireSpecialChar) {
        rules.push({
          pattern: /[^a-zA-Z0-9]/,
          message: 'Password must contain at least one special character',
        });
      }
      break;
  }

  return rules;
}

export const LoginPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [loading, setLoading] = React.useState(false);
  const [allowRegistration, setAllowRegistration] = React.useState<boolean | null>(null);
  const [securityConfig, setSecurityConfig] = React.useState<SecurityConfig>(DEFAULT_SECURITY_CONFIG);
  const [registerForm] = Form.useForm();
  const [activeTab, setActiveTab] = React.useState('login');
  const [teams, setTeams] = React.useState<any[]>([]);
  const [teamsFetched, setTeamsFetched] = React.useState(false);

  // Read the "allow registration" toggle and security config from system settings
  React.useEffect(() => {
    let cancelled = false;
    foundationService.config
      .getPublic()
      .then((configs: Record<string, string>) => {
        if (cancelled) return;
        const raw = configs[REGISTRATION_CONFIG_KEY];
        setAllowRegistration(raw === 'true');

        // Read the password policy config
        const policy = configs[PW_POLICY_KEY] || 'medium';
        const minLengthRaw = configs[PW_MIN_LENGTH_KEY];
        const requireSpecialRaw = configs[PW_REQUIRE_SPECIAL_KEY];

        setSecurityConfig({
          passwordPolicy: policy,
          passwordMinLength: minLengthRaw ? parseInt(minLengthRaw, 10) || 8 : 8,
          requireSpecialChar: requireSpecialRaw === 'true',
        });
      })
      .catch(() => {
        if (!cancelled) setAllowRegistration(false);
      });
    return () => { cancelled = true; };
  }, []);

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    if (key === 'register') {
      registerForm.resetFields();
      // Fetch the team space list only the first time the register tab is opened
      if (!teamsFetched) {
        setTeamsFetched(true);
        teamService.getTeamTree(true).then(setTeams).catch((err) => {
          console.error('Failed to fetch team space list:', err);
        });
      }
    }
  };

  const handleLogin = async (values: any) => {
    setLoading(true);
    try {
      await login(values.username, values.password);
      message.success('Signed in successfully');
      navigate('/');
    } catch (error) {
      // Error already handled in store
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (values: any) => {
    setLoading(true);
    try {
      // Call the registration API
      const response: RegisterResponse = await authService.register({
        username: values.username,
        password: values.password,
        confirmPassword: values.confirmPassword,
        email: values.email,
        realName: values.realName,
        teamId: values.teamId,
        phone: values.phone || undefined,
      });

      // Email verification flow: show a message and switch to the login tab
      message.success(response.message || 'Registered successfully. Please check your email to activate your account.');
      registerForm.resetFields();
      setActiveTab('login');
    } catch (error) {
      // Error already handled by HTTP interceptor
    } finally {
      setLoading(false);
    }
  };

  // Loading state while reading the registration toggle config
  if (allowRegistration === null) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 0' }}>
        <Spin size="large" />
      </div>
    );
  }

  const passwordHint = getPasswordHint(securityConfig);
  const passwordRules = getPasswordRules(securityConfig);

  const tabItems = [
    {
      key: 'login',
      label: 'Sign In',
      children: (
        <Form
          name="login"
          onFinish={handleLogin}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: 'Please enter your username' }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Username"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Please enter your password' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
              style={{
                background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                border: 'none',
                height: 44,
                borderRadius: 8,
                fontWeight: 600,
              }}
            >
              Sign In
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Forgot your password? <Link to="/forgot-password">Reset it</Link>
            </Text>
          </div>
        </Form>
      ),
    },
  ];

  // Only show the registration tab if allowed by system configuration
  if (allowRegistration) {
    tabItems.push({
      key: 'register',
      label: 'Register',
      children: (
        <Form
          form={registerForm}
          name="register"
          onFinish={handleRegister}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            name="username"
            rules={[
              { required: true, message: 'Please enter a username' },
              { min: 4, max: 20, message: 'Username must be 4-20 characters' },
              { pattern: /^[a-zA-Z0-9_]+$/, message: 'Username can only contain letters, digits, and underscores' },
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Username"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="realName"
            rules={[
              { required: true, message: 'Please enter your full name' },
              { max: 50, message: 'Full name cannot exceed 50 characters' },
            ]}
          >
            <Input
              prefix={<IdcardOutlined />}
              placeholder="Full Name"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="email"
            rules={[
              { required: true, message: 'Please enter your email' },
              { type: 'email', message: 'Please enter a valid email address' },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="Email"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="teamId"
            label="Team Space"
            rules={[
              { required: true, message: 'Please select a team space' },
            ]}
          >
            <Select
              placeholder="Select a team space"
              size="large"
            >
              {teams.map((team: any) => (
                <Select.Option key={team.id} value={String(team.id)}>
                  {team.teamName || team.name}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="password"
            rules={passwordRules}
            extra={<Text type="secondary" style={{ fontSize: 11 }}>{passwordHint}</Text>}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password"
              size="large"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Please confirm your password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('The two passwords do not match'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Confirm Password"
              size="large"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
              style={{
                background: 'linear-gradient(135deg, #1890ff, #722ed1)',
                border: 'none',
                height: 44,
                borderRadius: 8,
                fontWeight: 600,
              }}
            >
              Register
            </Button>
          </Form.Item>
        </Form>
      ),
    });
  }

  return (
    <div>
      <Tabs
        activeKey={activeTab}
        onChange={handleTabChange}
        centered
        items={tabItems}
      />
    </div>
  );
};

export default LoginPage;
