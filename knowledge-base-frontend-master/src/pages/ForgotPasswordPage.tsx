import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Typography, Card, Result } from 'antd';
import { App } from 'antd';
import { MailOutlined, LockOutlined, SafetyOutlined, ArrowLeftOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '@/services';

const { Title, Text, Paragraph } = Typography;

export const ForgotPasswordPage: React.FC = () => {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [countdown, setCountdown] = useState(0);
  const [resetSuccess, setResetSuccess] = useState(false);

  // Verification code countdown
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  // Send the verification code
  const handleSendCode = async (values: { email: string }) => {
    setLoading(true);
    try {
      await authService.sendResetCode(values.email);
      setEmail(values.email);
      setCurrentStep(1);
      setCountdown(60); // 60-second countdown
      message.success('Verification code sent to your email');
    } catch (error) {
      // Error already handled by HTTP interceptor
    } finally {
      setLoading(false);
    }
  };

  // Verify the code and proceed to the next step
  const handleVerifyCode = async (values: { code: string }) => {
    setLoading(true);
    try {
      await authService.verifyResetCode(email, values.code);
      setCode(values.code);
      setCurrentStep(2);
      message.success('Verified successfully. Please set a new password.');
    } catch (error) {
      // Error already handled by HTTP interceptor
    } finally {
      setLoading(false);
    }
  };

  // Reset the password
  const handleResetPassword = async (values: { newPassword: string; confirmPassword: string }) => {
    setLoading(true);
    try {
      await authService.resetPassword({
        email,
        code,
        newPassword: values.newPassword,
      });
      setResetSuccess(true);
      message.success('Password reset successfully. Please sign in with your new password.');
    } catch (error) {
      // Error already handled by HTTP interceptor
    } finally {
      setLoading(false);
    }
  };

  // Resend the verification code
  const handleResendCode = async () => {
    if (countdown > 0) return;

    setLoading(true);
    try {
      await authService.sendResetCode(email);
      setCountdown(60);
      message.success('Verification code resent');
    } catch (error) {
      // Error already handled by HTTP interceptor
    } finally {
      setLoading(false);
    }
  };

  // Back to login
  const handleBackToLogin = () => {
    navigate('/login');
  };

  // Success page
  if (resetSuccess) {
    return (
      <Card style={styles.card}>
        <Result
          status="success"
          icon={<CheckCircleOutlined style={{ color: '#10b981', fontSize: 72 }} />}
          title={<span style={{ fontSize: 26, fontWeight: 700, color: '#1a1a1a' }}>Password reset successfully!</span>}
          subTitle={<span style={{ fontSize: 15, color: '#6b7280' }}>Your password has been reset. You can now sign in with your new password.</span>}
          extra={[
            <Button
              key="login"
              type="primary"
              size="large"
              onClick={handleBackToLogin}
              style={{
                height: 50,
                fontSize: 16,
                fontWeight: 600,
                background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
                border: 'none',
                borderRadius: 12,
                boxShadow: '0 4px 14px rgba(37, 99, 235, 0.25)',
              }}
            >
              Go to Login
            </Button>,
            <Button
              key="back"
              size="large"
              onClick={() => setResetSuccess(false)}
              style={{
                height: 50,
                fontSize: 16,
                fontWeight: 600,
                borderRadius: 12,
                border: '2px solid #e5e7eb',
              }}
            >
              Back to Home
            </Button>,
          ]}
        />
      </Card>
    );
  }

  return (
    <Card style={styles.card}>
        {/* Header */}
        <div style={styles.header}>
          <Title level={2} style={styles.title}>
            Forgot Password
          </Title>
          <Text type="secondary" style={styles.subtitle}>
            Reset your account password via email verification
          </Text>
        </div>

        {/* Step 1: Enter email */}
        {currentStep === 0 && (
          <Form
            name="send-code"
            onFinish={handleSendCode}
            layout="vertical"
            style={styles.form}
          >
            <div style={styles.emailInfo}>
              <Text type="secondary">Enter the email address you registered with, and we'll send a verification code to it.</Text>
            </div>

            <Form.Item
              name="email"
              label="Registered Email"
              rules={[
                { required: true, message: 'Please enter your email address' },
                { type: 'email', message: 'Please enter a valid email address' },
              ]}
            >
              <Input
                prefix={<MailOutlined />}
                placeholder="Enter your registered email"
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                size="large"
                block
                style={styles.button}
              >
                Send Verification Code
              </Button>
            </Form.Item>

            <div style={styles.footer}>
              <Text type="secondary">Remembered your password?</Text>
              <Link to="/login" style={styles.link}>Back to Login</Link>
            </div>
          </Form>
        )}

        {/* Step 2: Verify the code */}
        {currentStep === 1 && (
          <Form
            name="verify-code"
            onFinish={handleVerifyCode}
            layout="vertical"
            style={styles.form}
          >
            <div style={styles.emailInfo}>
              <Text type="secondary">Verification code sent to:</Text>
              <Text strong style={{ marginLeft: 8 }}>{email}</Text>
            </div>

            <Form.Item
              name="code"
              label="Verification Code"
              rules={[
                { required: true, message: 'Please enter the verification code' },
                { len: 6, message: 'The verification code is 6 digits' },
                { pattern: /^\d+$/, message: 'The verification code must contain only digits' },
              ]}
            >
              <Input
                prefix={<SafetyOutlined />}
                placeholder="Enter the 6-digit verification code"
                size="large"
                maxLength={6}
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                size="large"
                block
                style={styles.button}
              >
                Verify
              </Button>
            </Form.Item>

            <div style={styles.resendSection}>
              <Text type="secondary">Didn't receive a code?</Text>
              <Button
                type="link"
                onClick={handleResendCode}
                disabled={countdown > 0}
                style={styles.link}
              >
                {countdown > 0 ? `Retry in ${countdown}s` : 'Resend'}
              </Button>
            </div>

            <div style={styles.footer}>
              <Button
                type="link"
                icon={<ArrowLeftOutlined />}
                onClick={() => { setCurrentStep(0); setCode(''); }}
                style={styles.link}
              >
                Back to Previous Step
              </Button>
            </div>
          </Form>
        )}

        {/* Step 3: Set a new password */}
        {currentStep === 2 && (
          <Form
            name="reset-password"
            onFinish={handleResetPassword}
            layout="vertical"
            style={styles.form}
          >
            <div style={styles.emailInfo}>
              <Text type="secondary">Set your new password. It must be 6-20 characters and include both uppercase and lowercase letters plus digits.</Text>
            </div>

            <Form.Item
              name="newPassword"
              label="New Password"
              rules={[
                { required: true, message: 'Please enter a new password' },
                { min: 6, message: 'Password must be at least 6 characters' },
                { max: 20, message: 'Password must be at most 20 characters' },
                {
                  pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
                  message: 'Password must include uppercase and lowercase letters and digits',
                },
              ]}
              hasFeedback
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Enter a new password (6-20 characters, with uppercase, lowercase, and digits)"
                size="large"
              />
            </Form.Item>

            <Form.Item
              name="confirmPassword"
              label="Confirm Password"
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
              hasFeedback
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Re-enter your new password"
                size="large"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={loading}
                size="large"
                block
                style={styles.button}
              >
                Reset Password
              </Button>
            </Form.Item>

            <div style={styles.footer}>
              <Button
                type="link"
                icon={<ArrowLeftOutlined />}
                onClick={() => { setCurrentStep(0); setCode(''); }}
                style={styles.link}
              >
                Start Over
              </Button>
            </div>
          </Form>
        )}

        {/* Help section at the bottom */}
        <div style={styles.helpSection}>
          <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 12, fontWeight: 600 }}>
            Having trouble?
          </Paragraph>
          <ul style={styles.helpList}>
            <li>Make sure you're entering the email address you registered with</li>
            <li>The verification code is valid for 10 minutes</li>
            <li>If you don't receive an email after a while, please check your spam folder</li>
            <li>If you have any questions, please contact your system administrator</li>
          </ul>
        </div>
      </Card>
  );
};

const styles = {
  card: {
    width: '100%',
    maxWidth: 520,
    margin: '40px auto',
    background: '#ffffff',
    boxShadow: '0 20px 60px rgba(0, 0, 0, 0.08), 0 8px 20px rgba(0, 0, 0, 0.04)',
    borderRadius: '20px',
    border: '1px solid rgba(0, 0, 0, 0.04)',
  },
  header: {
    textAlign: 'center' as const,
    marginBottom: 24,
    paddingBottom: 20,
    borderBottom: '1px solid rgba(0, 0, 0, 0.06)',
  },
  title: {
    fontSize: 28,
    marginBottom: 10,
    color: '#1a1a1a',
    fontWeight: 700,
    letterSpacing: '-0.5px',
  },
  subtitle: {
    fontSize: 15,
    color: '#6b7280',
    fontWeight: 400,
  },
  form: {
    marginTop: 20,
  },
  button: {
    height: 50,
    fontSize: 16,
    fontWeight: 600,
    background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
    border: 'none',
    borderRadius: '12px',
    boxShadow: '0 4px 14px rgba(37, 99, 235, 0.25)',
    marginTop: 8,
  },
  emailInfo: {
    padding: '18px 20px',
    background: 'linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%)',
    borderRadius: '12px',
    marginBottom: 20,
    textAlign: 'left' as const,
    lineHeight: 1.7,
    border: '1px solid rgba(0, 0, 0, 0.04)',
    fontSize: 14,
    color: '#475569',
  },
  resendSection: {
    textAlign: 'center' as const,
    marginTop: 20,
  },
  footer: {
    textAlign: 'center' as const,
    marginTop: 24,
    paddingTop: 20,
    borderTop: '1px solid rgba(0, 0, 0, 0.06)',
  },
  link: {
    fontSize: 14,
    marginLeft: 8,
    color: '#2563eb',
    fontWeight: 500,
  },
  helpSection: {
    marginTop: 28,
    padding: '20px',
    background: 'linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%)',
    borderRadius: '12px',
    border: '1px solid rgba(0, 0, 0, 0.06)',
  },
  helpList: {
    margin: 0,
    paddingLeft: 20,
    fontSize: 14,
    color: '#6b7280',
    lineHeight: 1.9,
  },
};

export default ForgotPasswordPage;
