import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Row,
  Col,
  Statistic,
  Button,
  Spin,
  Table,
  Space,
  Typography,
} from 'antd';
import {
  FileTextOutlined,
  UserOutlined,
  EyeOutlined,
  HeartOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  TrophyOutlined,
  RiseOutlined,
} from '@ant-design/icons';
import { statisticsService } from '@/services';
import dayjs from 'dayjs';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import UserAvatar from '@/components/common/UserAvatar';

const { Title, Text } = Typography;

const CHART_COLORS = ['#2563eb', '#10b981', '#8b5cf6', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899', '#84cc16'];

export const StatisticsPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState<any>(null);
  const [documentTrend, setDocumentTrend] = useState<any[]>([]);
  const [userActivity, setUserActivity] = useState<any[]>([]);
  const [categoryDistribution, setCategoryDistribution] = useState<any[]>([]);
  const [popularDocuments, setPopularDocuments] = useState<any[]>([]);
  const [activeUsers, setActiveUsers] = useState<any[]>([]);
  const [trendPeriod, setTrendPeriod] = useState<'week' | 'month' | 'year'>('month');

  const fetchAllData = useCallback(async () => {
    setLoading(true);
    try {
      const [overviewRes, trendRes, activityRes, categoryRes, docsRes, usersRes] = await Promise.all([
        statisticsService.getSystemStatistics(),
        statisticsService.getDocumentTrend({ period: trendPeriod }),
        statisticsService.getUserActivity({ period: trendPeriod }),
        statisticsService.getCategoryDistribution(),
        statisticsService.getPopularDocuments({ limit: 10, period: 'month' }),
        statisticsService.getActiveUsers({ limit: 10 }),
      ]);
      setOverview(overviewRes);
      setDocumentTrend(trendRes || []);
      setUserActivity(activityRes || []);
      setCategoryDistribution(categoryRes || []);
      setPopularDocuments(docsRes || []);
      setActiveUsers(usersRes || []);
    } catch (error) {
      // Error handled
    } finally {
      setLoading(false);
    }
  }, [trendPeriod]);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  if (loading) {
    return (
      <div style={{ padding: '16px 16px 32px 16px', background: '#f8fafc', minHeight: '100vh' }}>
        <div style={{ textAlign: 'center', padding: '100px 0' }}>
          <Spin size="large" />
        </div>
      </div>
    );
  }

  const docColumns = [
    {
      title: 'Rank',
      key: 'rank',
      width: 56,
      render: (_: any, __: any, index: number) => {
        let rankColor = '#94a3b8';
        let rankBg = '#f1f5f9';
        if (index === 0) {
          rankColor = '#fff';
          rankBg = 'linear-gradient(135deg, #ffd700, #ffb347)';
        } else if (index === 1) {
          rankColor = '#fff';
          rankBg = 'linear-gradient(135deg, #c0c0c0, #a8a8a8)';
        } else if (index === 2) {
          rankColor = '#fff';
          rankBg = 'linear-gradient(135deg, #cd7f32, #b87333)';
        }
        return (
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: rankBg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
              color: rankColor,
              fontSize: 14,
            }}
          >
            {index + 1}
          </div>
        );
      },
    },
    {
      title: 'Document Title',
      dataIndex: 'title',
      key: 'title',
      ellipsis: true,
      render: (text: string) => <Text strong>{text || 'Unknown Document'}</Text>,
    },
    {
      title: 'Views',
      dataIndex: 'viewCount',
      key: 'viewCount',
      width: 100,
      render: (views: number) => (
        <Text style={{ fontSize: 15, fontWeight: 600, color: '#2563eb' }}>
          {(views ?? 0).toLocaleString()}
        </Text>
      ),
    },
  ];

  const userColumns = [
    {
      title: 'Rank',
      key: 'rank',
      render: (_: any, __: any, index: number) => {
        let rankColor = '#94a3b8';
        let rankBg = '#f1f5f9';
        if (index === 0) {
          rankColor = '#fff';
          rankBg = 'linear-gradient(135deg, #ffd700, #ffb347)';
        } else if (index === 1) {
          rankColor = '#fff';
          rankBg = 'linear-gradient(135deg, #c0c0c0, #a8a8a8)';
        } else if (index === 2) {
          rankColor = '#fff';
          rankBg = 'linear-gradient(135deg, #cd7f32, #b87333)';
        }
        return (
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: rankBg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
              color: rankColor,
              fontSize: 14,
            }}
          >
            {index + 1}
          </div>
        );
      },
    },
    {
      title: 'User',
      dataIndex: 'username',
      key: 'username',
      render: (name: string, record: any) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <UserAvatar
            src={record.avatar}
            alt={name || 'Unknown User'}
            style={{ width: 32, height: 32, borderRadius: '50%', objectFit: 'cover' }}
          />
          <div>
            <div>
              <Text strong>{name || 'Unknown User'}</Text>
            </div>
          </div>
        </div>
      ),
    },
    {
      title: 'Activity',
      dataIndex: 'statisticsValue',
      key: 'statisticsValue',
      render: (value: number) => (
        <Space>
          <Text style={{ fontSize: 18, fontWeight: 700, color: '#2563eb' }}>
            {value ?? 0}
          </Text>
        </Space>
      ),
    },
  ];

  // Convert date trend data into chart format
  const formatTrendData = (data: any[]) => {
    return data.map((item: any) => ({
      date: item.date ? dayjs(item.date).format('MM-DD') : item.date,
      count: Number(item.count) || 0,
    }));
  };

  // Convert category data into pie chart format
  const formatCategoryData = (data: any[]) => {
    return data.map((item: any, index: number) => ({
      name: item.categoryName || `Category ${item.categoryId || index}`,
      value: Number(item.documentCount) || 0,
      color: CHART_COLORS[index % CHART_COLORS.length],
    }));
  };

  // Convert user activity data into ranked bar chart format
  const formatActivityData = (data: any[]) => {
    return data
      .slice(0, 10)
      .map((item: any) => ({
        name: item.username || item.userName || `User ${item.userId}`,
        score: Math.round(Number(item.activityScore) || Number(item.score) || 0),
      }));
  };

  const safeNum = (val: any): number => Number(val) || 0;

  return (
    <div style={{ padding: '16px 16px 32px 16px', background: '#f8fafc', minHeight: '100vh' }}>
      {/* Page header */}
      <div style={{ marginBottom: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0, marginBottom: 8, fontSize: 28, fontWeight: 700 }}>
            Statistics & Analytics
          </Title>
          <Text type="secondary">Usage statistics and analytics reports for the knowledge base</Text>
        </div>
        <Space>
          <Button.Group>
            <Button
              type={trendPeriod === 'week' ? 'primary' : 'default'}
              onClick={() => setTrendPeriod('week')}
            >
              This Week
            </Button>
            <Button
              type={trendPeriod === 'month' ? 'primary' : 'default'}
              onClick={() => setTrendPeriod('month')}
            >
              This Month
            </Button>
            <Button
              type={trendPeriod === 'year' ? 'primary' : 'default'}
              onClick={() => setTrendPeriod('year')}
            >
              This Year
            </Button>
          </Button.Group>
        </Space>
      </div>

      {/* Stat cards */}
      <Row gutter={20} style={{ marginBottom: 32 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card
            style={{
              borderRadius: 16,
              border: '1px solid #e2e8f0',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
              borderTop: '3px solid #2563eb',
            }}
          >
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                background: 'rgba(37, 99, 235, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 16,
              }}
            >
              <FileTextOutlined style={{ fontSize: 24, color: '#2563eb' }} />
            </div>
            <Statistic
              title="Total Documents"
              value={safeNum(overview?.totalDocuments)}
              valueStyle={{ fontSize: 32, fontWeight: 700, color: '#0f172a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            style={{
              borderRadius: 16,
              border: '1px solid #e2e8f0',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
              borderTop: '3px solid #10b981',
            }}
          >
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                background: 'rgba(16, 185, 129, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 16,
              }}
            >
              <UserOutlined style={{ fontSize: 24, color: '#10b981' }} />
            </div>
            <Statistic
              title="Active Users"
              value={safeNum(overview?.activeUserCount || overview?.totalUsers)}
              valueStyle={{ fontSize: 32, fontWeight: 700, color: '#0f172a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            style={{
              borderRadius: 16,
              border: '1px solid #e2e8f0',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
              borderTop: '3px solid #8b5cf6',
            }}
          >
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                background: 'rgba(139, 92, 246, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 16,
              }}
            >
              <EyeOutlined style={{ fontSize: 24, color: '#8b5cf6' }} />
            </div>
            <Statistic
              title="Total Views"
              value={safeNum(overview?.totalViews)}
              valueStyle={{ fontSize: 32, fontWeight: 700, color: '#0f172a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            style={{
              borderRadius: 16,
              border: '1px solid #e2e8f0',
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
              borderTop: '3px solid #f59e0b',
            }}
          >
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: 12,
                background: 'rgba(245, 158, 11, 0.1)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 16,
              }}
            >
              <HeartOutlined style={{ fontSize: 24, color: '#f59e0b' }} />
            </div>
            <Statistic
              title="Total Likes"
              value={safeNum(overview?.totalLikes)}
              valueStyle={{ fontSize: 32, fontWeight: 700, color: '#0f172a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Chart section */}
      <Row gutter={24} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card
            title={
              <Space>
                <RiseOutlined style={{ color: '#2563eb' }} />
                <Text strong>Document Publishing Trend</Text>
              </Space>
            }
            extra={
              <Button size="small" onClick={fetchAllData}>Refresh</Button>
            }
            style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}
          >
            <ResponsiveContainer width="100%" height={380}>
              <AreaChart data={formatTrendData(documentTrend)}>
                <defs>
                  <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="date" stroke="#64748b" />
                <YAxis stroke="#64748b" allowDecimals={false} />
                <Tooltip />
                <Legend />
                <Area
                  type="monotone"
                  dataKey="count"
                  name="New Documents"
                  stroke="#2563eb"
                  fillOpacity={1}
                  fill="url(#colorCount)"
                  strokeWidth={3}
                />
              </AreaChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      <Row gutter={24} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <TrophyOutlined style={{ color: '#2563eb' }} />
                <Text strong>Document Distribution by Category</Text>
              </Space>
            }
            style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}
          >
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={formatCategoryData(categoryDistribution)}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }: any) => `${name} ${(percent * 100).toFixed(0)}%`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {formatCategoryData(categoryDistribution).map((entry: any, index: number) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <UserOutlined style={{ color: '#2563eb' }} />
                <Text strong>User Activity Ranking</Text>
              </Space>
            }
            style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}
          >
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={formatActivityData(userActivity)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis type="number" stroke="#64748b" allowDecimals={false} />
                <YAxis type="category" dataKey="name" stroke="#64748b" width={80} />
                <Tooltip />
                <Bar dataKey="score" name="Activity Score" fill="#10b981" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      {/* Popularity rankings */}
      <Row gutter={24}>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <FileTextOutlined style={{ color: '#2563eb' }} />
                <Text strong>Popular Documents Ranking</Text>
              </Space>
            }
            style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}
          >
            <Table
              columns={docColumns}
              dataSource={popularDocuments}
              rowKey="documentId"
              pagination={false}
              size="middle"
              style={{ fontSize: 14 }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <TrophyOutlined style={{ color: '#2563eb' }} />
                <Text strong>Active Users Ranking</Text>
              </Space>
            }
            style={{ borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}
          >
            <Table
              columns={userColumns}
              dataSource={activeUsers}
              rowKey="userId"
              pagination={false}
              size="middle"
              style={{ fontSize: 14 }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

// Calculate date range based on period
const getDateRange = (period: 'week' | 'month' | 'year'): { startDate: string; endDate: string } => {
  const endDate = dayjs().format('YYYY-MM-DD');
  const dayMap: Record<string, number> = { week: 6, month: 29, year: 364 };
  const startDate = dayjs().subtract(dayMap[period], 'days').format('YYYY-MM-DD');
  return { startDate, endDate };
};

export default StatisticsPage;
