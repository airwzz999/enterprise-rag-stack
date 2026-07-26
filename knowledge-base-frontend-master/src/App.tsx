import React, { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { ConfigProvider, theme, App as AntdApp } from 'antd';
import { setMessageApi } from '@/utils/message-holder';
import enUS from 'antd/locale/en_US';
import { router } from './router';
import { useAuthStore } from './stores';
import { tokenStorage } from '@/utils/token-storage';

import 'antd/dist/reset.css';
import './styles/global.css';

/**
 * Global message injector component
 * Obtains the message instance from AntdApp.useApp() and injects it into messageHolder
 * for use by non-component files
 */
const GlobalMessageHolder: React.FC = () => {
  const { message } = AntdApp.useApp();
  useEffect(() => {
    setMessageApi(message);
  }, [message]);
  return null;
};

const App: React.FC = () => {
  const { checkAuth, isAuthenticated } = useAuthStore();

  useEffect(() => {
    // Check the user's authentication status on page load (runs only once)
    const token = tokenStorage.getAccessToken() || localStorage.getItem('token');

    console.log('🔐 App init - checking login status:');
    console.log('  - Token from cookie:', tokenStorage.getAccessToken() ? '✅ present' : '❌ absent');
    console.log('  - Token from localStorage:', localStorage.getItem('token') ? '✅ present' : '❌ absent');
    console.log('  - isAuthenticated in store:', isAuthenticated);

    // Only call checkAuth when unauthenticated and a token is present, to avoid duplicate calls
    if (!isAuthenticated && token) {
      console.log('✅ Token found and not authenticated, calling checkAuth to verify...');
      checkAuth().catch(error => {
        console.error('❌ checkAuth failed:', error);
        tokenStorage.clearToken();
      });
    } else if (isAuthenticated) {
      console.log('✅ User already authenticated, skipping checkAuth call');
    } else {
      console.log('⚠️ No token found, user is not logged in');
    }
  }, []); // isAuthenticated intentionally omitted from deps to avoid duplicate calls

  return (
    <ConfigProvider
      locale={enUS}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#2563eb',
          colorSuccess: '#10b981',
          colorWarning: '#f59e0b',
          colorError: '#ef4444',
          colorInfo: '#3b82f6',
          colorBgBase: '#ffffff',
          colorBgContainer: '#ffffff',
          colorBorder: '#e2e8f0',
          colorBorderSecondary: '#f1f5f9',
          colorTextBase: '#0f172a',
          colorTextSecondary: '#475569',
          colorTextTertiary: '#94a3b8',
          borderRadius: 8,
          borderRadiusLG: 12,
          borderRadiusSM: 4,
          fontFamily: `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif`,
          fontSize: 14,
          fontSizeHeading1: 32,
          fontSizeHeading2: 24,
          fontSizeHeading3: 20,
          fontSizeHeading4: 16,
          fontSizeHeading5: 14,
        },
        components: {
          Layout: {
            headerBg: '#ffffff',
            headerHeight: 64,
            siderBg: '#ffffff',
          },
          Menu: {
            itemBorderRadius: 8,
            itemMarginInline: 8,
            itemPaddingInline: 12,
          },
          Card: {
            borderRadiusLG: 12,
          },
          Button: {
            borderRadius: 8,
            controlHeight: 36,
            controlHeightLG: 44,
            controlHeightSM: 28,
          },
          Input: {
            borderRadius: 8,
            controlHeight: 36,
            controlHeightLG: 44,
            controlHeightSM: 28,
          },
        },
      }}
    >
      <AntdApp>
        <GlobalMessageHolder />
        <RouterProvider router={router} />
      </AntdApp>
    </ConfigProvider>
  );
};

export default App;
