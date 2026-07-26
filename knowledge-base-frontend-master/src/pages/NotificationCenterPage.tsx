import React, { useEffect, useState, useMemo, useCallback } from 'react';
import {
  Card,
  Typography,
  Tag,
  Button,
  Space,
  Badge,
  Popconfirm,
  Row,
  Col,
  Statistic,
  Segmented,
  Pagination,
  Drawer,
  Skeleton,
} from 'antd';
import {
  BellOutlined,
  CheckOutlined,
  DeleteOutlined,
  ClearOutlined,
  NotificationOutlined,
  CommentOutlined,
  FileTextOutlined,
  LikeOutlined,
  TeamOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  AppstoreOutlined,
  RightOutlined,
} from '@ant-design/icons';
import { PageHeader } from '@/components/common';
import { useNotificationStore } from '@/stores';
import { SystemNotification } from '@/types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/en';
import './NotificationCenterPage.css';

dayjs.extend(relativeTime);
dayjs.locale('en');

const { Text } = Typography;

const PAGE_SIZE = 20;

/* ────────── Type config map ────────── */

const TYPE_CONFIG: Record<
  string,
  { label: string; icon: React.ReactNode; color: string; tagColor: string }
> = {
  'review-approved': {
    label: 'Approved',
    icon: <CheckCircleOutlined />,
    color: '#059669',
    tagColor: 'green',
  },
  'review-rejected': {
    label: 'Rejected',
    icon: <CloseCircleOutlined />,
    color: '#dc2626',
    tagColor: 'red',
  },
  'review-submitted': {
    label: 'Pending Review',
    icon: <ClockCircleOutlined />,
    color: '#d97706',
    tagColor: 'gold',
  },
  system: {
    label: 'System',
    icon: <BellOutlined />,
    color: '#3b82f6',
    tagColor: 'blue',
  },
  comment: {
    label: 'Comment',
    icon: <CommentOutlined />,
    color: '#10b981',
    tagColor: 'green',
  },
  mention: {
    label: 'Mention',
    icon: <TeamOutlined />,
    color: '#f59e0b',
    tagColor: 'orange',
  },
  review: {
    label: 'Review',
    icon: <FileTextOutlined />,
    color: '#8b5cf6',
    tagColor: 'purple',
  },
  like: {
    label: 'Like',
    icon: <LikeOutlined />,
    color: '#ef4444',
    tagColor: 'red',
  },
};

/** Determine the review status subtype based on the notification title */
function resolveReviewKey(notif: { title?: string; type?: string }): string {
  if (notif.type !== 'review') return notif.type || '';
  if (notif.title?.includes('approved')) return 'review-approved';
  if (notif.title?.includes('rejected')) return 'review-rejected';
  return 'review-submitted';
}

/* ────────── Sidebar filter key type ────────── */

type SidebarFilterKey = 'all' | 'unread' | 'read' | SystemNotification['type'];

/* ────────── Component ────────── */

export const NotificationCenterPage: React.FC = () => {
  const {
    notifications,
    unreadCount,
    isLoading,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    clearAll,
  } = useNotificationStore();

  const [activeFilter, setActiveFilter] = useState<SidebarFilterKey>('all');
  const [selectedNotification, setSelectedNotification] =
    useState<SystemNotification | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  /* ── Derived data ── */

  const readCount = notifications.length - unreadCount;
  const uniqueTypeCount = 5; // system, comment, mention, review, like

  const typeCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    notifications.forEach((n) => {
      counts[n.type] = (counts[n.type] || 0) + 1;
    });
    return counts;
  }, [notifications]);

  /* ── Filtering ── */

  const filteredNotifications = useMemo(() => {
    if (activeFilter === 'all') return notifications;
    if (activeFilter === 'unread') return notifications.filter((n) => !n.read);
    if (activeFilter === 'read') return notifications.filter((n) => n.read);
    return notifications.filter((n) => n.type === activeFilter);
  }, [notifications, activeFilter]);

  /* ── Pagination ── */

  const paginatedNotifications = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredNotifications.slice(start, start + PAGE_SIZE);
  }, [filteredNotifications, currentPage]);

  // Reset to page 1 on filter change
  useEffect(() => {
    setCurrentPage(1);
  }, [activeFilter]);

  /* ── Handlers ── */

  const handleMarkAsRead = useCallback(
    async (id: string, e?: React.MouseEvent) => {
      e?.stopPropagation();
      try {
        await markAsRead(id);
      } catch {
        // handled by interceptor
      }
    },
    [markAsRead],
  );

  const handleMarkAllAsRead = useCallback(async () => {
    try {
      await markAllAsRead();
    } catch {
      // handled by interceptor
    }
  }, [markAllAsRead]);

  const handleDelete = useCallback(
    async (id: string, e?: React.MouseEvent) => {
      e?.stopPropagation();
      try {
        await deleteNotification(id);
        if (selectedNotification?.id === id) {
          setSelectedNotification(null);
          setDrawerOpen(false);
        }
      } catch {
        // handled by interceptor
      }
    },
    [deleteNotification, selectedNotification],
  );

  const handleClearAll = useCallback(async () => {
    try {
      await clearAll();
      setDrawerOpen(false);
    } catch {
      // handled by interceptor
    }
  }, [clearAll]);

  const handleCardClick = useCallback((notification: SystemNotification) => {
    setSelectedNotification(notification);
    setDrawerOpen(true);
  }, []);

  const handleDrawerClose = useCallback(() => {
    setDrawerOpen(false);
    setSelectedNotification(null);
  }, []);

  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  /* ── Sidebar config ── */

  const sidebarItems = useMemo(() => {
    const typeKeys: SystemNotification['type'][] = [
      'system',
      'comment',
      'mention',
      'review',
      'like',
    ];

    return {
      status: [
        { key: 'all' as const, label: 'All Notifications', count: notifications.length },
        { key: 'unread' as const, label: 'Unread', count: unreadCount, dot: true },
        { key: 'read' as const, label: 'Read', count: readCount },
      ],
      types: typeKeys.map((key) => ({
        key,
        label: TYPE_CONFIG[key].label,
        count: typeCounts[key] || 0,
      })),
    };
  }, [notifications.length, unreadCount, readCount, typeCounts]);

  /* ── Render helpers ── */

  const renderStatCard = (
    icon: React.ReactNode,
    bgColor: string,
    label: string,
    value: number,
  ) => (
    <Col xs={12} sm={12} md={6}>
      <Card variant="borderless">
        <Statistic
          title={label}
          value={isLoading ? undefined : value}
          prefix={
            isLoading ? undefined : (
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 36,
                  height: 36,
                  borderRadius: 10,
                  background: bgColor,
                  color: '#fff',
                  fontSize: 18,
                  marginRight: 8,
                }}
              >
                {icon}
              </span>
            )
          }
          formatter={
            isLoading
              ? () => (
                  <Skeleton.Button
                    active
                    size="small"
                    style={{ width: 40, height: 28 }}
                  />
                )
              : undefined
          }
        />
      </Card>
    </Col>
  );

  const renderSidebarItem = (
    item: (typeof sidebarItems.status)[0] | (typeof sidebarItems.types)[0],
    isUnreadBadge = false,
  ) => (
    <div
      key={item.key}
      className={`notification-sidebar-item${activeFilter === item.key ? ' active' : ''}${isUnreadBadge ? ' unread-badge' : ''}`}
      onClick={() => setActiveFilter(item.key)}
    >
      <span>{item.label}</span>
      <Badge count={item.count} overflowCount={999} />
    </div>
  );

  const renderEmptyState = () => {
    if (activeFilter === 'all') {
      return (
        <div className="notification-empty">
          <NotificationOutlined className="notification-empty-icon" />
          <div className="notification-empty-title">No notifications yet</div>
          <div className="notification-empty-desc">
            System notifications, comment replies, review results, and other messages will appear here
          </div>
        </div>
      );
    }
    if (activeFilter === 'unread') {
      return (
        <div className="notification-empty">
          <CheckCircleOutlined className="notification-empty-icon" style={{ color: '#10b981', opacity: 0.5 }} />
          <div className="notification-empty-title">All notifications have been read</div>
          <div className="notification-empty-desc">Great job, you've caught up on everything</div>
        </div>
      );
    }
    return (
      <div className="notification-empty">
        <NotificationOutlined className="notification-empty-icon" />
        <div className="notification-empty-title">No notifications in this category</div>
        <div className="notification-empty-desc">There are no matching notifications for this category</div>
      </div>
    );
  };

  const renderSkeleton = () => (
    <div>
      {[1, 2, 3, 4, 5].map((i) => (
        <div className="notification-skeleton-card" key={i}>
          <div className="notification-skeleton-avatar" />
          <div className="notification-skeleton-body">
            <div
              className="notification-skeleton-line"
              style={{ width: `${40 + Math.random() * 30}%` }}
            />
            <div className="notification-skeleton-line" />
          </div>
        </div>
      ))}
    </div>
  );

  /* ── Main render ── */

  return (
    <div className="notification-center">
      {/* ── Header ── */}
      <PageHeader
        title="Notification Center"
        subtitle="Manage your system notifications and message alerts"
        extra={
          <Space>
            {unreadCount > 0 && (
              <Button
                icon={<CheckOutlined />}
                onClick={handleMarkAllAsRead}
              >
                Mark All as Read ({unreadCount})
              </Button>
            )}
            {notifications.length > 0 && (
              <Popconfirm
                title="Are you sure you want to clear all notifications?"
                description="This action cannot be undone"
                onConfirm={handleClearAll}
                okText="Clear All"
                cancelText="Cancel"
                okButtonProps={{ danger: true }}
              >
                <Button icon={<ClearOutlined />} danger>
                  Clear Notifications
                </Button>
              </Popconfirm>
            )}
          </Space>
        }
      />

      {/* ── Stats Cards ── */}
      <Row gutter={[16, 16]} className="notification-stats-row">
        {renderStatCard(
          <BellOutlined />,
          '#3b82f6',
          'All Notifications',
          notifications.length,
        )}
        {renderStatCard(
          <ExclamationCircleOutlined />,
          '#f59e0b',
          'Unread',
          unreadCount,
        )}
        {renderStatCard(
          <CheckCircleOutlined />,
          '#10b981',
          'Read',
          readCount,
        )}
        {renderStatCard(
          <AppstoreOutlined />,
          '#8b5cf6',
          'Types',
          uniqueTypeCount,
        )}
      </Row>

      {/* ── Mobile filter tabs ── */}
      <div className="notification-mobile-tabs">
        <Segmented
          block
          value={activeFilter}
          onChange={(val) => setActiveFilter(val as SidebarFilterKey)}
          options={[
            { label: `All (${notifications.length})`, value: 'all' },
            { label: `Unread (${unreadCount})`, value: 'unread' },
            { label: 'System', value: 'system' },
            { label: 'Review', value: 'review' },
          ]}
        />
      </div>

      {/* ── Main two-panel ── */}
      <div className="notification-main-content">
        {/* Sidebar */}
        <aside className="notification-sidebar">
          <div className="notification-sidebar-group">
            <div className="notification-sidebar-group-title">Message Status</div>
            {sidebarItems.status.map((item, i) =>
              renderSidebarItem(item, i === 1),
            )}
          </div>
          <div className="notification-sidebar-group">
            <div className="notification-sidebar-group-title">Notification Type</div>
            {sidebarItems.types.map((item) => renderSidebarItem(item))}
          </div>
        </aside>

        {/* List Panel */}
        <main className="notification-list-panel">
          <Card variant="borderless" styles={{ body: { padding: 0 } }}>
            {isLoading ? (
              renderSkeleton()
            ) : paginatedNotifications.length === 0 ? (
              renderEmptyState()
            ) : (
              <>
                {paginatedNotifications.map((notification) => {
                  const resolvedType = resolveReviewKey(notification);
                  const config = TYPE_CONFIG[resolvedType] || TYPE_CONFIG[notification.type];
                  return (
                    <div
                      key={notification.id}
                      className={`notification-card type-${resolvedType} ${notification.read ? 'read' : 'unread'}`}
                      onClick={() => handleCardClick(notification)}
                    >
                      <div className="notification-card-dot" />
                      <div
                        className={`notification-card-icon ${resolvedType}`}
                      >
                        {config.icon}
                      </div>
                      <div className="notification-card-body">
                        <div className="notification-card-header">
                          <span className="notification-card-title">
                            {notification.title}
                          </span>
                          <Tag color={config.tagColor}>{config.label}</Tag>
                        </div>
                        <div className="notification-card-content">
                          {notification.content}
                        </div>
                        <div className="notification-card-footer">
                          <span className="notification-card-time">
                            {dayjs(notification.createdAt).fromNow()}
                          </span>
                          <div className="notification-card-actions">
                            {!notification.read && (
                              <Button
                                type="link"
                                size="small"
                                icon={<CheckOutlined />}
                                onClick={(e) =>
                                  handleMarkAsRead(notification.id, e)
                                }
                              >
                                Mark as Read
                              </Button>
                            )}
                            <Popconfirm
                              title="Are you sure you want to delete this notification?"
                              onConfirm={() => {
                                handleDelete(notification.id);
                              }}
                              okText="OK"
                              cancelText="Cancel"
                            >
                              <Button
                                type="link"
                                size="small"
                                danger
                                icon={<DeleteOutlined />}
                                onClick={(e) => e.stopPropagation()}
                              >
                                Delete
                              </Button>
                            </Popconfirm>
                          </div>
                        </div>
                      </div>
                      <RightOutlined
                        style={{ color: 'var(--text-muted)', fontSize: 12, marginLeft: 8, flexShrink: 0 }}
                      />
                    </div>
                  );
                })}

                {filteredNotifications.length > PAGE_SIZE && (
                  <div className="notification-pagination">
                    <Pagination
                      current={currentPage}
                      total={filteredNotifications.length}
                      pageSize={PAGE_SIZE}
                      onChange={handlePageChange}
                      showSizeChanger={false}
                      size="small"
                    />
                  </div>
                )}
              </>
            )}
          </Card>
        </main>
      </div>

      {/* ── Detail Drawer ── */}
      <Drawer
        title={null}
        open={drawerOpen}
        onClose={handleDrawerClose}
        width={480}
        placement="right"
        styles={{ body: { padding: '24px' } }}
        footer={
          selectedNotification ? (
            <Space>
              {!selectedNotification.read && (
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => {
                    handleMarkAsRead(selectedNotification.id);
                    setSelectedNotification({
                      ...selectedNotification,
                      read: true,
                    });
                  }}
                >
                  Mark as Read
                </Button>
              )}
              <Popconfirm
                title="Are you sure you want to delete this notification?"
                onConfirm={() => handleDelete(selectedNotification.id)}
                okText="OK"
                cancelText="Cancel"
              >
                <Button icon={<DeleteOutlined />} danger>
                  Delete
                </Button>
              </Popconfirm>
              <Button onClick={handleDrawerClose}>Close</Button>
            </Space>
          ) : undefined
        }
      >
        {selectedNotification && (
          <div>
            {(() => {
              const resolvedType = resolveReviewKey(selectedNotification);
              const config = TYPE_CONFIG[resolvedType] || TYPE_CONFIG[selectedNotification.type];
              return (
                <>
                  <div
                    className={`notification-drawer-icon ${resolvedType}`}
                  >
                    {config.icon}
                  </div>
                  <div className="notification-drawer-type-tag">
                    <Tag color={config.tagColor}>{config.label}</Tag>
                  </div>
                  <Text
                    strong
                    style={{ fontSize: 18, display: 'block', textAlign: 'center', marginBottom: 16 }}
                  >
                    {selectedNotification.title}
                  </Text>
                  <div className="notification-drawer-body">
                    {selectedNotification.content}
                  </div>
                  <div className="notification-drawer-meta">
                    <span>
                      {dayjs(selectedNotification.createdAt).format(
                        'YYYY-MM-DD HH:mm',
                      )}
                    </span>
                    {selectedNotification.read ? (
                      <Tag>Read</Tag>
                    ) : (
                      <Tag color="blue">Unread</Tag>
                    )}
                  </div>
                  {selectedNotification.link && (
                    <div className="notification-drawer-link">
                      <Button
                        type="primary"
                        block
                        onClick={() =>
                          window.open(selectedNotification.link, '_blank')
                        }
                      >
                        View Details
                      </Button>
                    </div>
                  )}
                </>
              );
            })()}
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default NotificationCenterPage;
