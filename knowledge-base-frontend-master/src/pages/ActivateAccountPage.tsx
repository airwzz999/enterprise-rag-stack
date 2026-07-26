import React, { useEffect, useState } from 'react';
import { Typography, Spin, Result, Button } from 'antd';
import { Link, useSearchParams } from 'react-router-dom';
import { authService } from '@/services';

const { Text } = Typography;

/**
 * Account activation page
 *
 * <p>Reached via the activation link in the email (/activate?token=xxx),
 * verifies the email and activates the account, then redirects to the login page on success.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
const ActivateAccountPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState<string>('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('Invalid activation link: missing activation token');
      return;
    }

    let cancelled = false;

    authService
      .verifyEmail(token)
      .then((msg) => {
        if (cancelled) return;
        setStatus('success');
        setMessage(msg || 'Account activated successfully');
      })
      .catch((err) => {
        if (cancelled) return;
        setStatus('error');
        // The backend typically returns an error message wrapped in a Result object
        const errorMsg =
          err?.response?.data?.message ||
          err?.message ||
          'Activation failed, please try again later';
        setMessage(errorMsg);
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  if (status === 'loading') {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>
          <Text type="secondary">Verifying activation link...</Text>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <Result
        status="error"
        title="Activation Failed"
        subTitle={message}
        extra={[
          <Link to="/login" key="login">
            <Button type="primary">Back to Login</Button>
          </Link>,
        ]}
      />
    );
  }

  return (
    <Result
      status="success"
      title="Activation Successful"
      subTitle={message}
      extra={[
        <Link to="/login" key="login">
          <Button type="primary">Go to Login</Button>
        </Link>,
      ]}
    />
  );
};

export default ActivateAccountPage;
