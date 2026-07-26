import React, { useState, useEffect, useCallback } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Dropdown, Badge, App, Select } from 'antd';
import {
  DownOutlined, BellOutlined, CheckCircleOutlined, CloseCircleOutlined,
  InfoCircleOutlined, MessageOutlined, LikeOutlined,
  FieldTimeOutlined, ClockCircleOutlined, FileTextOutlined,
} from '@ant-design/icons';
import { useAppStore, useAuthStore, useNotificationStore, useCategoryStore, useTeamStore } from '@/stores';
import { webSocketService } from '@/services/websocket.service';
import type { WsNotificationPayload } from '@/services/websocket.service';
import type { SystemNotification } from '@/types';
import NotificationToast from '@/components/common/NotificationToast';
import UserAvatar from '@/components/common/UserAvatar';
import TeamIcon from '@/components/common/TeamIcon';
import CategoryIcon from '@/components/common/CategoryIcon';
import '@/components/common/NotificationToast.css';
import { resolveNotificationTarget } from '@/utils/notification-link';
import { PERMISSIONS, getPrimaryRoleLabel, hasAdminAccess, hasAnyPermission, hasPermission } from '@/utils/permission';

/** Notification type → icon + color */
const NOTIF_ICON_MAP: Record<string, { icon: React.ReactNode; color: string; bg: string }> = {
  'review-approved':  { icon: <CheckCircleOutlined />, color: '#059669', bg: '#ecfdf5' },
  'review-rejected':  { icon: <CloseCircleOutlined />,  color: '#dc2626', bg: '#fef2f2' },
  'review-submitted': { icon: <ClockCircleOutlined />,   color: '#d97706', bg: '#fffbeb' },
  system:             { icon: <InfoCircleOutlined />,    color: '#2563eb', bg: '#eff6ff' },
  comment:            { icon: <MessageOutlined />,        color: '#7c3aed', bg: '#f5f3ff' },
  mention:            { icon: <FieldTimeOutlined />,      color: '#ea580c', bg: '#fff7ed' },
  like:               { icon: <LikeOutlined />,            color: '#dc2626', bg: '#fef2f2' },
};
const NOTIF_ICON_DEFAULT = { icon: <FileTextOutlined />, color: '#6b7280', bg: '#f9fafb' };

/** Determine the review status subtype based on the notification title/content */
function resolveReviewKey(notif: { title?: string; type?: string }): string {
  if (notif.type !== 'review') return notif.type || '';
  // NOTE: matches against the backend-generated title text, now translated to English (see ReviewNotificationListener)
  if (notif.title?.includes('approved')) return 'review-approved';
  if (notif.title?.includes('rejected')) return 'review-rejected';
  return 'review-submitted';
}

const MainLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, checkAuth, token } = useAuthStore();
  const { unreadCount, addNotification, fetchUnreadCount } = useNotificationStore();
  const { categoryTree, fetchCategoryTree } = useCategoryStore();
  const { teamTree, selectedTeam, setSelectedTeam, fetchTeamTree } = useTeamStore();
  const { notification } = App.useApp();
  const [showAdminMenu, setShowAdminMenu] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const currentRoles = user?.roles || (user?.role ? [user.role] : []);
  const canViewFiles = hasPermission(user, PERMISSIONS.documentList);
  const canManageCategories = hasAnyPermission(user, [PERMISSIONS.documentCategory, PERMISSIONS.documentCategoryQuery]);
  const canReviewDocuments = hasPermission(user, PERMISSIONS.documentReview);
  const canManagePermissions = hasPermission(user, PERMISSIONS.systemPermission);
  const canManageUsers = hasPermission(user, PERMISSIONS.systemUser);
  const canManageRoles = hasPermission(user, PERMISSIONS.systemRole);
  const canManageTeams = hasPermission(user, PERMISSIONS.systemTeam);
  const canViewStatistics = hasPermission(user, PERMISSIONS.systemStatistics);
  const canManageSettings = hasPermission(user, PERMISSIONS.systemSettings);

  // Determine whether the user is a reviewer (based on role)
  const isReviewer = canReviewDocuments || currentRoles.some((role) => role.toUpperCase().includes('REVIEWER'));

  const openNotificationTarget = useCallback((notif: SystemNotification) => {
    const target = resolveNotificationTarget(notif);
    if (!target.url) {
      return;
    }
    if (target.openInNewTab) {
      window.open(target.url, '_blank', 'noopener,noreferrer');
      return;
    }
    navigate(target.url);
  }, [navigate]);

  // Notification toast helper function
  const showNotificationToast = useCallback((notif: SystemNotification) => {
    const iconKey = resolveReviewKey(notif);
    const cfg = NOTIF_ICON_MAP[iconKey] || NOTIF_ICON_DEFAULT;
    const key = `notif-toast-${notif.id}`;
    notification.open({
      key,
      message: notif.title,
      description: (
        <NotificationToast
          notification={notif}
          onNavigate={() => {
            notification.destroy(key);
            openNotificationTarget(notif);
          }}
        />
      ),
      icon: (
        <span style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 36, height: 36,
          borderRadius: 10,
          background: cfg.bg,
          color: cfg.color,
          fontSize: 18,
        }}>
          {cfg.icon}
        </span>
      ),
      className: 'custom-notif-toast',
      placement: 'topRight',
      duration: 3,
      onClick: () => {
        notification.destroy(key);
        openNotificationTarget(notif);
      },
    });
  }, [notification, openNotificationTarget]);

  // WebSocket notification → store callback + real-time toast
  const handleWsNotification = useCallback((payload: WsNotificationPayload) => {
    const notif: SystemNotification = {
      id: `${payload.notificationType}-${payload.documentId}-${Date.now()}`,
      type: (payload.notificationType as SystemNotification['type']) || 'system',
      title: payload.title,
      content: payload.content,
      link: resolveNotificationTarget({
        type: payload.notificationType,
        link: payload.link,
        documentId: payload.documentId,
      }).url,
      documentId: payload.documentId ? String(payload.documentId) : undefined,
      read: false,
      createdAt: payload.timestamp || new Date().toISOString(),
    };
    addNotification(notif);
    showNotificationToast(notif);
  }, [addNotification, showNotificationToast]);

  // Reviewer broadcast callback
  const handleReviewerNotification = useCallback((payload: WsNotificationPayload) => {
    const notif: SystemNotification = {
      id: `review-${payload.documentId}-${Date.now()}`,
      type: 'review',
      title: payload.title,
      content: payload.content,
      link: resolveNotificationTarget({
        type: 'review',
        link: payload.link,
        documentId: payload.documentId,
      }).url,
      documentId: payload.documentId ? String(payload.documentId) : undefined,
      read: false,
      createdAt: payload.timestamp || new Date().toISOString(),
    };
    addNotification(notif);
    showNotificationToast(notif);
  }, [addNotification, showNotificationToast]);

  useEffect(() => {
    checkAuth();
  }, []);

  // Load global app config (system name, etc.)
  const systemName = useAppStore((s) => s.systemName);
  const enableAI = useAppStore((s) => s.enableAI);
  const enableAIWriting = useAppStore((s) => s.enableAIWriting);
  const enableFullTextSearch = useAppStore((s) => s.enableFullTextSearch);
  const enableWebSocket = useAppStore((s) => s.enableWebSocket);
  const fetchAppConfig = useAppStore((s) => s.fetchAppConfig);
  useEffect(() => {
    fetchAppConfig();
  }, []);

  // Load sidebar category tree and team space data
  useEffect(() => {
    fetchCategoryTree().catch(() => {});
    fetchTeamTree().catch(() => {});
  }, []);

  // WebSocket connection lifecycle
  useEffect(() => {
    if (!enableWebSocket || !user?.id || !token) return;

    // Fetch the initial unread count
    fetchUnreadCount().catch(() => {
      // Fail silently; doesn't affect the main flow
    });

    // Register callbacks
    const unsubNotification = webSocketService.onNotification(handleWsNotification);
    const unsubReviewer = webSocketService.onReviewerNotification(handleReviewerNotification);

    // Establish the connection (with JWT token)
    const authHeader = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    webSocketService.connect(authHeader, isReviewer);

    return () => {
      unsubNotification();
      unsubReviewer();
      webSocketService.disconnect();
    };
  }, [user?.id, token, isReviewer, handleWsNotification, handleReviewerNotification, fetchUnreadCount]);

  useEffect(() => {
    setShowAdminMenu(hasAdminAccess(user));
  }, [user]);

  const handleNavClick = (e: React.MouseEvent, path: string) => {
    e.preventDefault();
    navigate(path);
  };

  const handleSidebarClick = (e: React.MouseEvent, path: string) => {
    e.preventDefault();
    navigate(path);
  };

  return (
    <>
      {/* Enterprise-Grade Navigation Bar */}
      <nav className="navbar">
        <div className="navbar-left">
          {/* Mobile Menu Button */}
          <button
            className="mobile-menu-button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              {mobileMenuOpen ? (
                <>
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </>
              ) : (
                <>
                  <line x1="3" y1="12" x2="21" y2="12"></line>
                  <line x1="3" y1="6" x2="21" y2="6"></line>
                  <line x1="3" y1="18" x2="21" y2="18"></line>
                </>
              )}
            </svg>
          </button>

          <a href="/" className="logo" onClick={(e) => handleNavClick(e, '/')}>
            <div className="logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
            </div>
            <span>{systemName}</span>
          </a>
          <div className={`nav-links ${mobileMenuOpen ? 'mobile-open' : ''}`}>
            <a href="/" className={`nav-link ${location.pathname === '/' ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                <polyline points="9 22 9 12 15 12 15 22"></polyline>
              </svg>
              Home
            </a>
            <a href="/documents" className={`nav-link ${location.pathname.startsWith('/documents') ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/documents')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
              </svg>
              Document Center
            </a>
            {canViewFiles && (
              <a href="/files" className={`nav-link ${location.pathname === '/files' ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/files')}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                  <polyline points="13 2 13 9 20 9"></polyline>
                </svg>
                File Management
              </a>
            )}
            <a href="/knowledge-graph" className={`nav-link ${location.pathname === '/knowledge-graph' ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/knowledge-graph')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10"></circle>
                <circle cx="12" cy="12" r="4"></circle>
                <line x1="21.17" y1="8" x2="12" y2="8"></line>
                <line x1="3.95" y1="6.06" x2="8.54" y2="14"></line>
                <line x1="10.88" y1="21.94" x2="15.46" y2="14"></line>
              </svg>
              Knowledge Graph
            </a>
            <a href="/search" className={`nav-link ${location.pathname === '/search' ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/search')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              Search
            </a>
            {enableAI && (
            <a href="/ai" className={`nav-link ${location.pathname === '/ai' ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/ai')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
              AI Assistant
            </a>
            )}
            {enableAIWriting && (
            <a href="/ai-writing" className={`nav-link ${location.pathname === '/ai-writing' ? 'active' : ''}`} onClick={(e) => handleNavClick(e, '/ai-writing')}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 20h9"></path>
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
              </svg>
              AI Writing
            </a>
            )}
            {showAdminMenu && (
              <div className="nav-item admin-menu">
                <button
                  type="button"
                  className={`nav-link dropdown-toggle ${location.pathname.startsWith('/admin') ? 'active' : ''}`}
                  onClick={(e) => {
                    e.preventDefault();
                  }}
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M12 1v6m0 6v6M4.22 4.22l4.24 4.24m5.08 5.08l4.24 4.24M1 12h6m6 0h6M4.22 19.78l4.24-4.24m5.08-5.08l4.24-4.24"></path>
                  </svg>
                  System Management
                  <svg className="dropdown-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="6 9 12 15 18 9"></polyline>
                  </svg>
                </button>
                <div className="dropdown-menu">
                  {hasAdminAccess(user) && (
                    <a href="/admin" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <rect x="3" y="3" width="7" height="7"></rect>
                        <rect x="14" y="3" width="7" height="7"></rect>
                        <rect x="14" y="14" width="7" height="7"></rect>
                        <rect x="3" y="14" width="7" height="7"></rect>
                      </svg>
                      Admin Center
                    </a>
                  )}
                  {canManagePermissions && (
                    <a href="/admin/permissions" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/permissions')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                      </svg>
                      Permission Management
                    </a>
                  )}
                  {canManageCategories && (
                    <a href="/admin/categories" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/categories')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                      </svg>
                      Category Management
                    </a>
                  )}
                  {canManageTeams && (
                    <a href="/admin/teams" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/teams')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9" cy="7" r="4"></circle>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                      </svg>
                      Team Space Management
                    </a>
                  )}
                  {canManageUsers && (
                    <a href="/admin/users" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/users')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                      </svg>
                      User Management
                    </a>
                  )}
                  {canManageRoles && (
                    <a href="/admin/roles" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/roles')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M12 2l7 4v6c0 5-3.5 8-7 10-3.5-2-7-5-7-10V6l7-4z"></path>
                        <path d="M9.5 12l1.5 1.5L14.5 10"></path>
                      </svg>
                      Role Management
                    </a>
                  )}
                  {canReviewDocuments && (
                    <a href="/admin/review" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/review')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                        <polyline points="14 2 14 8 20 8"></polyline>
                        <line x1="16" y1="13" x2="8" y2="13"></line>
                        <line x1="16" y1="17" x2="8" y2="17"></line>
                        <polyline points="10 9 9 9 8 9"></polyline>
                      </svg>
                      Review Management
                    </a>
                  )}
                  {canViewStatistics && (
                    <a href="/admin/statistics" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/statistics')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="18" y1="20" x2="18" y2="10"></line>
                        <line x1="12" y1="20" x2="12" y2="4"></line>
                        <line x1="6" y1="20" x2="6" y2="14"></line>
                      </svg>
                      Statistics
                    </a>
                  )}
                  {canManageSettings && (
                    <a href="/admin/settings" className="dropdown-item" onClick={(e) => handleNavClick(e, '/admin/settings')}>
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="12" cy="12" r="3"></circle>
                        <path d="M12 1v6m0 6v6M4.22 4.22l4.24 4.24m5.08 5.08l4.24 4.24M1 12h6m6 0h6M4.22 19.78l4.24-4.24m5.08-5.08l4.24-4.24"></path>
                      </svg>
                      System Settings
                    </a>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
        <div className="navbar-right">
          {/* Mobile Search Button */}
          <button
            className="mobile-search-button"
            onClick={() => navigate('/search')}
            aria-label="Search"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </button>

          {/* Search box */}
          {enableFullTextSearch && (
          <div className="search-box">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              type="text"
              placeholder="Search knowledge base, documents, Q&A..."
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  const keyword = (e.target as HTMLInputElement).value.trim();
                  if (keyword) {
                    navigate(`/search?q=${encodeURIComponent(keyword)}`);
                  }
                }
              }}
            />
          </div>
          )}

{/* Team space selector - temporarily disabled */}
          {/* <Select
            value={selectedTeam ? String(selectedTeam.id) : undefined}
            placeholder="Select a team space"
            allowClear
            style={{ minWidth: 150, maxWidth: 200 }}
            size="middle"
            onChange={(value) => {
              const team = teamTree.find((t) => String(t.id) === value);
              if (team) {
                setSelectedTeam(team);
                navigate(`/documents?team=${team.id}`);
              }
            }}
            onClear={() => {
              setSelectedTeam(null);
              navigate('/documents');
            }}
            options={teamTree.map((team) => ({
              value: String(team.id),
              label: team.teamName || team.name,
            }))}
          /> */}

          {/* Notification bell */}
          <Badge count={unreadCount} size="small" overflowCount={99} offset={[-2, 2]}>
            <BellOutlined
              onClick={() => navigate('/notifications')}
              style={{
                fontSize: '20px',
                color: 'var(--text-secondary, #475569)',
                cursor: 'pointer',
                padding: '8px 4px 8px 8px',
                borderRadius: '8px',
                transition: 'background-color 0.2s, color 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--bg-tertiary, #f1f5f9)';
                e.currentTarget.style.color = '#2563eb';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
                e.currentTarget.style.color = 'var(--text-secondary, #475569)';
              }}
            />
          </Badge>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'profile',
                  label: 'Profile',
                  onClick: () => navigate('/profile'),
                },
                {
                  type: 'divider',
                },
                {
                  key: 'logout',
                  label: 'Log Out',
                  onClick: async () => {
                    await useAuthStore.getState().logout();
                    navigate('/login');
                  },
                },
              ],
              style: { minWidth: 120 },
            }}
            placement="bottomRight"
            trigger={['click']}
          >
            <div
              className="user-menu"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '8px 16px',
                borderRadius: '8px',
                minWidth: '180px',
                maxWidth: '220px',
                cursor: 'pointer',
                transition: 'background-color 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = 'var(--bg-tertiary)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
              }}
            >
              <UserAvatar
                src={user?.avatar}
                alt={user?.username || 'User'}
                style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: '50%',
                  objectFit: 'cover',
                  flexShrink: 0,
                }}
              />
              <div
                className="user-info-container"
                style={{
                  flex: 1,
                  minWidth: 0,
                  textAlign: 'left'
                }}
              >
                <div
                  className="user-name"
                  style={{
                    fontSize: '14px',
                    fontWeight: 600,
                    color: 'var(--text-primary)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    lineHeight: 1.2
                  }}
                >
                  {user?.username || 'John Doe'}
                </div>
                <div
                  className="user-role"
                  style={{
                    fontSize: '12px',
                    color: 'var(--text-muted)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    lineHeight: 1.2
                  }}
                >
                  {getPrimaryRoleLabel(user)}
                </div>
              </div>
              <DownOutlined style={{ fontSize: '10px', color: 'var(--text-muted)' }} />
            </div>
          </Dropdown>
        </div>
      </nav>

      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div
          className={`mobile-menu-overlay ${mobileMenuOpen ? 'show' : ''}`}
          onClick={() => setMobileMenuOpen(false)}
        ></div>
      )}

      {/* Main Container */}
      <div className="main-container">
        {/* Sidebar */}
        <aside className="sidebar">
          <div className="sidebar-section">
            <div className="sidebar-title">Knowledge Space</div>
            <ul className="sidebar-menu">
              <li className={`sidebar-item ${location.pathname === '/' ? 'active' : ''}`}>
                <a href="/" className="sidebar-link" onClick={(e) => handleSidebarClick(e, '/')}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="3" width="7" height="7"></rect>
                    <rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect>
                    <rect x="3" y="14" width="7" height="7"></rect>
                  </svg>
                  All Documents
                  <span className="badge">1,234</span>
                </a>
              </li>
              <li className={`sidebar-item ${location.pathname === '/my-documents' ? 'active' : ''}`}>
                <a href="/my-documents" className="sidebar-link" onClick={(e) => handleSidebarClick(e, '/my-documents')}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                    <line x1="3" y1="3" x2="8" y2="3"></line>
                    <line x1="3" y1="5" x2="8" y2="5"></line>
                    <line x1="3" y1="7" x2="8" y2="7"></line>
                  </svg>
                  My Documents
                </a>
              </li>
              <li className={`sidebar-item ${location.pathname === '/drafts' ? 'active' : ''}`}>
                <a href="/drafts" className="sidebar-link" onClick={(e) => handleSidebarClick(e, '/drafts')}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                  </svg>
                  Drafts
                </a>
              </li>
              <li className={`sidebar-item ${location.pathname === '/favorites' ? 'active' : ''}`}>
                <a href="/favorites" className="sidebar-link" onClick={(e) => handleSidebarClick(e, '/favorites')}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                  </svg>
                  My Favorites
                  <span className="badge">56</span>
                </a>
              </li>
              <li className={`sidebar-item ${location.pathname === '/recent-access' ? 'active' : ''}`}>
                <a href="/recent-access" className="sidebar-link" onClick={(e) => handleSidebarClick(e, '/recent-access')}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                  </svg>
                  Recently Accessed
                </a>
              </li>
              {canReviewDocuments && (
                <li className={`sidebar-item ${location.pathname === '/admin/review' ? 'active' : ''}`}>
                  <a href="/admin/review" className="sidebar-link" onClick={(e) => handleSidebarClick(e, '/admin/review')}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
                    </svg>
                    Pending Review
                  </a>
                </li>
              )}
            </ul>
          </div>

          <div className="sidebar-section">
            <div className="sidebar-title">Categories</div>
            <ul className="sidebar-menu">
              {categoryTree.map((cat) => {
                const active = location.pathname === '/documents' && location.search.includes(`category=${cat.id}`);
                return (
                  <li key={cat.id} className={`sidebar-item${active ? ' active' : ''}`}>
                    <a href={`/documents?category=${cat.id}`} className="sidebar-link" onClick={(e) => handleSidebarClick(e, `/documents?category=${cat.id}`)}>
                      <CategoryIcon icon={cat.icon} variant="sidebar" />
                      {cat.name}
                      {cat.documentCount != null && (
                        <span style={{ marginLeft: 'auto', color: 'var(--text-muted)', fontSize: '12px' }}>{cat.documentCount}</span>
                      )}
                    </a>
                  </li>
                );
              })}
              {categoryTree.length === 0 && (
                <li className="sidebar-item" style={{ color: 'var(--text-muted)', fontSize: '13px', padding: '8px 16px' }}>
                  No categories yet
                </li>
              )}
            </ul>
          </div>

          <div className="sidebar-section">
            <div className="sidebar-title">Team Spaces</div>
            <ul className="sidebar-menu">
              {teamTree.map((team) => {
                const active = location.pathname === '/documents' && location.search.includes(`team=${team.id}`);
                const name = team.teamName || team.name || '';
                return (
                  <li key={team.id} className={`sidebar-item${active ? ' active' : ''}`}>
                    <a href={`/documents?team=${team.id}`} className="sidebar-link" onClick={(e) => { useTeamStore.getState().setSelectedTeam(team); handleSidebarClick(e, `/documents?team=${team.id}`); }}>
                      <TeamIcon icon={team.icon} variant="sidebar" />
                      {name}
                    </a>
                  </li>
                );
              })}
              {teamTree.length === 0 && (
                <li className="sidebar-item" style={{ color: 'var(--text-muted)', fontSize: '13px', padding: '8px 16px' }}>
                  No teams yet
                </li>
              )}
            </ul>
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="content">
          <Outlet />
        </main>
      </div>
    </>
  );
};

export default MainLayout;
