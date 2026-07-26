import React, { useState, useEffect } from 'react';
import { useNavigate, useParams, Routes, Route, Navigate } from 'react-router-dom';
import { Layout, Menu, Card, Row, Col, Statistic, Typography, Space, Badge } from 'antd';
import {
  UserOutlined,
  TeamOutlined,
  FolderOutlined,
  CheckCircleOutlined,
  SettingOutlined,
  BarChartOutlined,
  SafetyOutlined,
  FileTextOutlined,
  BellOutlined,
} from '@ant-design/icons';
import { UsersPage } from './admin/UsersPage';
import { CategoriesPage } from './admin/CategoriesPage';
import { RolesPage } from './admin/RolesPage';
import { TeamsPage } from './admin/TeamsPage';
import { ReviewPage } from './admin/ReviewPage';
import { SettingsPage } from './admin/SettingsPage';
import { StatisticsPage } from './admin/StatisticsPage';
import './AdminCenterPage.css';

const { Sider, Content } = Layout;
const { Title, Text } = Typography;

interface AdminStats {
  totalUsers: number;
  totalDocuments: number;
  pendingApprovals: number;
  systemIssues: number;
  userGrowth: number;
  documentGrowth: number;
}

export const AdminCenterPage: React.FC = () => {
  const navigate = useNavigate();
  const { section } = useParams();
  const [stats, setStats] = useState<AdminStats>({
    totalUsers: 0,
    totalDocuments: 0,
    pendingApprovals: 0,
    systemIssues: 0,
    userGrowth: 0,
    documentGrowth: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      // Mock data
      setStats({
        totalUsers: 156,
        totalDocuments: 1243,
        pendingApprovals: 23,
        systemIssues: 3,
        userGrowth: 12.5,
        documentGrowth: 8.3,
      });
    } catch (error) {
      console.error('Failed to fetch stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const menuItems = [
    {
      key: 'overview',
      icon: <BarChartOutlined />,
      label: 'System Overview',
    },
    {
      key: 'users',
      icon: <UserOutlined />,
      label: 'User Management',
    },
    {
      key: 'roles',
      icon: <SafetyOutlined />,
      label: 'Role Management',
    },
    {
      key: 'teams',
      icon: <TeamOutlined />,
      label: 'Team Management',
    },
    {
      key: 'categories',
      icon: <FolderOutlined />,
      label: 'Category Management',
    },
    {
      key: 'review',
      icon: <CheckCircleOutlined />,
      label: 'Review Process',
      badge: stats.pendingApprovals,
    },
    {
      key: 'statistics',
      icon: <BarChartOutlined />,
      label: 'Data Statistics',
    },
    {
      key: 'settings',
      icon: <SettingOutlined />,
      label: 'System Settings',
    },
  ];

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(`/admin/${key}`);
  };

  const renderOverview = () => (
    <div className="admin-overview">
      <div className="page-header">
        <div>
          <Title level={2}>System Overview</Title>
          <Text type="secondary">System status and key metrics</Text>
        </div>
      </div>

      <Row gutter={[20, 20]} className="stats-grid">
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic
              title="Total Users"
              value={stats.totalUsers}
              prefix={<UserOutlined />}
              loading={loading}
              valueStyle={{ color: '#2563eb' }}
            />
            <div className="stat-trend positive">
              <span>↑ {stats.userGrowth}%</span>
              <Text type="secondary">vs. last month</Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic
              title="Total Documents"
              value={stats.totalDocuments}
              prefix={<FileTextOutlined />}
              loading={loading}
              valueStyle={{ color: '#10b981' }}
            />
            <div className="stat-trend positive">
              <span>↑ {stats.documentGrowth}%</span>
              <Text type="secondary">vs. last month</Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic
              title="Pending Review"
              value={stats.pendingApprovals}
              prefix={<CheckCircleOutlined />}
              loading={loading}
              valueStyle={{ color: '#f59e0b' }}
            />
            <div className="stat-trend">
              <Text type="secondary">Needs action</Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="stat-card">
            <Statistic
              title="System Alerts"
              value={stats.systemIssues}
              prefix={<BellOutlined />}
              loading={loading}
              valueStyle={{ color: '#ef4444' }}
            />
            <div className="stat-trend negative">
              <span>Needs attention</span>
            </div>
          </Card>
        </Col>
      </Row>

      <Row gutter={[20, 20]} style={{ marginTop: 24 }}>
        <Col xs={24}>
          <Card title="Quick Actions" className="actions-card">
            <Row gutter={[16, 16]}>
              <Col xs={12} sm={8} md={6}>
                <div className="action-item" onClick={() => navigate('/admin/users')}>
                  <UserOutlined />
                  <span>Add User</span>
                </div>
              </Col>
              <Col xs={12} sm={8} md={6}>
                <div className="action-item" onClick={() => navigate('/admin/categories')}>
                  <FolderOutlined />
                  <span>Create Category</span>
                </div>
              </Col>
              <Col xs={12} sm={8} md={6}>
                <div className="action-item" onClick={() => navigate('/admin/roles')}>
                  <SafetyOutlined />
                  <span>Configure Role</span>
                </div>
              </Col>
              <Col xs={12} sm={8} md={6}>
                <div className="action-item" onClick={() => navigate('/admin/approvals')}>
                  <CheckCircleOutlined />
                  <span>Review Document</span>
                </div>
              </Col>
            </Row>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="Recent Activity" className="activity-card">
            <div className="activity-list">
              <div className="activity-item">
                <div className="activity-icon user">
                  <UserOutlined />
                </div>
                <div className="activity-content">
                  <div className="activity-title">New user registered</div>
                  <div className="activity-time">Zhang San just registered</div>
                </div>
                <div className="activity-time">2 minutes ago</div>
              </div>
              <div className="activity-item">
                <div className="activity-icon document">
                  <FileTextOutlined />
                </div>
                <div className="activity-content">
                  <div className="activity-title">Document uploaded</div>
                  <div className="activity-time">Li Si uploaded "System Design Document"</div>
                </div>
                <div className="activity-time">15 minutes ago</div>
              </div>
              <div className="activity-item">
                <div className="activity-icon approval">
                  <CheckCircleOutlined />
                </div>
                <div className="activity-content">
                  <div className="activity-title">Document reviewed</div>
                  <div className="activity-time">Wang Wu approved 3 documents</div>
                </div>
                <div className="activity-time">1 hour ago</div>
              </div>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="System Status" className="status-card">
            <div className="status-list">
              <div className="status-item">
                <div className="status-label">Database Service</div>
                <div className="status-value">
                  <Badge status="success" text="Running normally" />
                </div>
              </div>
              <div className="status-item">
                <div className="status-label">Search Service</div>
                <div className="status-value">
                  <Badge status="success" text="Running normally" />
                </div>
              </div>
              <div className="status-item">
                <div className="status-label">AI Service</div>
                <div className="status-value">
                  <Badge status="processing" text="High load" />
                </div>
              </div>
              <div className="status-item">
                <div className="status-label">Storage Space</div>
                <div className="status-value">
                  <Badge status="warning" text="75% used" />
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );

  return (
    <Layout className="admin-center">
      <Sider
        width={280}
        theme="light"
        className="admin-sider"
        style={{
          borderRight: '1px solid #f0f0f0',
          overflow: 'auto',
          height: 'calc(100vh - 64px)',
        }}
      >
        <div className="admin-header">
          <Title level={4}>Admin Center</Title>
          <Text type="secondary">System Management</Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[section || 'overview']}
          items={menuItems.map((item) => ({
            ...item,
            label: item.badge ? (
              <Space>
                {item.label}
                <Badge count={item.badge} size="small" />
              </Space>
            ) : (
              item.label
            ),
          }))}
          onClick={handleMenuClick}
          className="admin-menu"
        />
      </Sider>
      <Content className="admin-content">
        <Routes>
          <Route index element={<>{renderOverview()}</>} />
          <Route path="overview" element={<>{renderOverview()}</>} />
          <Route path="users" element={<UsersPage />} />
          <Route path="roles" element={<RolesPage />} />
          <Route path="teams" element={<TeamsPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="review" element={<ReviewPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="statistics" element={<StatisticsPage />} />
          <Route path="*" element={<Navigate to="/admin/overview" replace />} />
        </Routes>
      </Content>
    </Layout>
  );
};

export default AdminCenterPage;
