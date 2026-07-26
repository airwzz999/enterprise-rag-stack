import React, { useEffect, useState, useMemo, useCallback } from 'react';
import {
  Card, Button, Space, Tag, Typography, Row, Col,
  Table, Input, message, Modal, Tooltip, Popconfirm,
  DatePicker,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckOutlined, CloseOutlined, EyeOutlined, SearchOutlined,
  ClockCircleOutlined, CheckCircleOutlined, ExclamationCircleOutlined,
  ReloadOutlined, UserOutlined, FolderOutlined,
} from '@ant-design/icons';
import { reviewService } from '@/services';
import { useAuthStore } from '@/stores';
import { ReviewTask } from '@/types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import { PERMISSIONS, hasPermission } from '@/utils/permission';

dayjs.extend(relativeTime);

const { Text } = Typography;
const { TextArea } = Input;
const { RangePicker } = DatePicker;

// ────────── Design Tokens ──────────
const C = {
  bg: '#f6f7f9',
  surface: '#ffffff',
  border: '#e8ebf0',
  text: '#111827',
  textSec: '#566073',
  textTer: '#8c94a3',
  pending: '#f59e0b',
  pendingBg: 'rgba(245,158,11,0.08)',
  approved: '#10b981',
  approvedBg: 'rgba(16,185,129,0.08)',
  rejected: '#ef4444',
  rejectedBg: 'rgba(239,68,68,0.08)',
  shadow: '0 1px 3px rgba(0,0,0,0.06)',
  radius: 12,
};

const statusMeta: Record<string, { label: string; color: string; bg: string; icon: React.ReactNode }> = {
  pending: { label: 'Pending', color: C.pending, bg: C.pendingBg, icon: <ClockCircleOutlined /> },
  approved: { label: 'Approved', color: C.approved, bg: C.approvedBg, icon: <CheckCircleOutlined /> },
  rejected: { label: 'Rejected', color: C.rejected, bg: C.rejectedBg, icon: <ExclamationCircleOutlined /> },
};

// ────────── Component ──────────
export const ReviewPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const canReview = hasPermission(user, PERMISSIONS.documentReview);
  const [tasks, setTasks] = useState<ReviewTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ pending: 0, approved: 0, rejected: 0 });
  const [activeTab, setActiveTab] = useState('pending');
  const [searchText, setSearchText] = useState('');

  // Reject modal
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectTask, setRejectTask] = useState<ReviewTask | null>(null);
  const [rejectComment, setRejectComment] = useState('');
  const [submittingIds, setSubmittingIds] = useState<Set<string>>(new Set());

  // ── Filters ──
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null] | null>(null);
  const [authorFilter, setAuthorFilter] = useState<string | undefined>(undefined);
  const [categoryFilter, setCategoryFilter] = useState<string | undefined>(undefined);

  const loadData = useCallback(async (status: string) => {
    setLoading(true);
    try {
      const [taskRes, statsRes] = await Promise.all([
        reviewService.getReviewTasks({ status, page: 1, pageSize: 100 }),
        reviewService.getReviewStats(),
      ]);
      setTasks(taskRes.list || []);
      setStats(statsRes || { pending: 0, approved: 0, rejected: 0 });
    } catch {
      setTasks([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData('pending'); }, [loadData]);

  // ── Approve ──
  const handleApprove = async (task: ReviewTask) => {
    setSubmittingIds((prev) => new Set(prev).add(task.id));
    try {
      await reviewService.reviewDocument(task.id, { status: 'approved' });
      message.success('Approved successfully');
      loadData(activeTab);
    } catch {
      message.error('Operation failed, please try again');
    } finally {
      setSubmittingIds((prev) => {
        const next = new Set(prev);
        next.delete(task.id);
        return next;
      });
    }
  };

  // ── Reject Flow ──
  const openRejectModal = (task: ReviewTask) => {
    setRejectTask(task);
    setRejectComment('');
    setRejectModalOpen(true);
  };

  const handleRejectConfirm = async () => {
    if (!rejectTask) return;
    if (!rejectComment.trim()) {
      message.warning('Please provide a review comment when rejecting');
      return;
    }
    setSubmittingIds((prev) => new Set(prev).add(rejectTask.id));
    try {
      await reviewService.reviewDocument(rejectTask.id, { status: 'rejected', comment: rejectComment });
      message.success('Rejected');
      setRejectModalOpen(false);
      setRejectTask(null);
      loadData(activeTab);
    } catch {
      message.error('Operation failed, please try again');
    } finally {
      setSubmittingIds((prev) => {
        const next = new Set(prev);
        next.delete(rejectTask.id);
        return next;
      });
    }
  };

  // ── Batch Approve ──
  const handleBatchApprove = async () => {
    const pendingTasks = tasks.filter((t) => t.status === 'pending');
    if (!pendingTasks.length) return message.warning('No pending tasks to review');
    try {
      await reviewService.batchReview(pendingTasks.map((t) => t.id), { status: 'approved' });
      message.success(`Approved ${pendingTasks.length} tasks in bulk`);
      loadData(activeTab);
    } catch {
      message.error('Batch operation failed');
    }
  };

  // ── Filter ──
  const filteredTasks = useMemo(() => {
    return tasks.filter((t) => {
      // Keyword search
      if (searchText) {
        const keyword = searchText.toLowerCase();
        const matchTitle = t.documentTitle.toLowerCase().includes(keyword);
        const matchAuthor = (t.documentAuthor?.username || '').toLowerCase().includes(keyword);
        if (!matchTitle && !matchAuthor) return false;
      }
      // Date range
      if (dateRange && dateRange[0] && dateRange[1]) {
        const taskDate = dayjs(t.createdAt);
        if (taskDate.isBefore(dateRange[0], 'day') || taskDate.isAfter(dateRange[1], 'day')) return false;
      }
      // Author (text match)
      if (authorFilter && !(t.documentAuthor?.username || '').toLowerCase().includes(authorFilter.toLowerCase())) return false;
      // Category (text match)
      if (categoryFilter && !(t.categoryName || '').toLowerCase().includes(categoryFilter.toLowerCase())) return false;
      return true;
    });
  }, [tasks, searchText, dateRange, authorFilter, categoryFilter]);

  // ── Table Columns ──
  const columns: ColumnsType<ReviewTask> = [
    {
      title: '#',
      key: 'index',
      width: 64,
      align: 'center',
      render: (_: unknown, __: ReviewTask, index: number) => index + 1,
    },
    {
      title: 'Document Title',
      dataIndex: 'documentTitle',
      key: 'title',
      width: 220,
      ellipsis: { showTitle: false },
      render: (title: string, record) => (
        <Tooltip title={title}>
          <a
            onClick={() => window.open(`/documents/${record.documentId}`, '_blank')}
            style={{ fontWeight: 500, color: C.text }}
          >
            {title}
          </a>
        </Tooltip>
      ),
    },
    {
      title: 'Author',
      dataIndex: ['documentAuthor', 'username'],
      key: 'author',
      width: 88,
      render: (name: string) => (
        <Text style={{ color: C.textSec, fontSize: 13 }}>
          <UserOutlined style={{ marginRight: 4 }} />{name || 'Unknown'}
        </Text>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 84,
      render: (status: string) => {
        const meta = statusMeta[status] || statusMeta.pending;
        return (
          <Tag style={{ borderRadius: 12, border: 'none', background: meta.bg, color: meta.color, fontSize: 12, padding: '2px 10px' }}>
            {meta.icon} {meta.label}
          </Tag>
        );
      },
    },
    {
      title: 'Category',
      dataIndex: 'categoryName',
      key: 'category',
      width: 88,
      render: (name: string | undefined) => name ? (
        <Text style={{ fontSize: 13, color: C.textSec }}>
          <FolderOutlined style={{ marginRight: 4 }} />{name}
        </Text>
      ) : (
        <Text style={{ color: C.textTer, fontSize: 13 }}>-</Text>
      ),
    },
    {
      title: 'Round',
      dataIndex: 'reviewRound',
      key: 'round',
      width: 56,
      align: 'center',
      render: (round: number) => round ?? 1,
    },
    {
      title: 'Submitted At',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 112,
      sorter: (a, b) => dayjs(a.createdAt).unix() - dayjs(b.createdAt).unix(),
      defaultSortOrder: 'descend',
      render: (t: string) => (
        <Tooltip title={dayjs(t).format('YYYY-MM-DD HH:mm:ss')}>
          <Text style={{ fontSize: 13, color: C.textTer }}>{dayjs(t).fromNow()}</Text>
        </Tooltip>
      ),
    },
    {
      title: 'Reviewer',
      dataIndex: ['reviewer', 'username'],
      key: 'reviewer',
      width: 88,
      render: (name: string) => name ? (
        <Text style={{ color: C.textSec, fontSize: 13 }}>{name}</Text>
      ) : (
        <Text style={{ color: C.textTer, fontSize: 13 }}>-</Text>
      ),
    },
    {
      title: 'Reviewed At',
      dataIndex: 'reviewedAt',
      key: 'reviewedAt',
      width: 112,
      render: (t: string | undefined) => t ? (
        <Tooltip title={dayjs(t).format('YYYY-MM-DD HH:mm:ss')}>
          <Text style={{ fontSize: 13, color: C.textTer }}>{dayjs(t).fromNow()}</Text>
        </Tooltip>
      ) : (
        <Text style={{ color: C.textTer, fontSize: 13 }}>-</Text>
      ),
    },
    {
      title: 'Review Comment',
      dataIndex: 'comment',
      key: 'comment',
      width: 120,
      ellipsis: { showTitle: false },
      render: (comment: string | undefined) => comment ? (
        <Tooltip title={comment}>
          <Text style={{ fontSize: 13, color: C.textSec }}>{comment}</Text>
        </Tooltip>
      ) : (
        <Text style={{ color: C.textTer, fontSize: 13 }}>-</Text>
      ),
    },
    {
      title: 'Actions',
      key: 'action',
      width: 150,
      render: (_: any, record: ReviewTask) => (
        <Space size={[4, 4]} wrap>
          <Button
            size="small"
            type="link"
            icon={<EyeOutlined />}
            onClick={() => window.open(`/documents/${record.documentId}`, '_blank')}
          >
            Preview
          </Button>
          {canReview && record.status === 'pending' && (
            <>
              <Popconfirm
                title="Confirm approval of this document?"
                onConfirm={() => handleApprove(record)}
                okText="Confirm"
                cancelText="Cancel"
              >
                <Button size="small" type="link" icon={<CheckOutlined />} style={{ color: C.approved }} loading={submittingIds.has(record.id)}>
                  Approve
                </Button>
              </Popconfirm>
              <Button
                size="small"
                type="link"
                danger
                icon={<CloseOutlined />}
                onClick={() => openRejectModal(record)}
              >
                Reject
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const segments = [
    { key: 'pending', label: 'Pending', color: C.pending, bg: C.pendingBg, dot: '#f59e0b' },
    { key: 'approved', label: 'Approved', color: C.approved, bg: C.approvedBg, dot: '#10b981' },
    { key: 'rejected', label: 'Rejected', color: C.rejected, bg: C.rejectedBg, dot: '#ef4444' },
  ];

  return (
    <div style={{ padding: '16px 20px 20px', background: C.bg, minHeight: '100vh' }}>
      {/* ── Header ── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h1 style={{ fontSize: 22, fontWeight: 700, color: C.text, margin: 0 }}>Review Management</h1>
          <Text style={{ fontSize: 13, color: C.textTer }}>Manage the document review workflow and control content quality</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => loadData(activeTab)} style={{ borderRadius: 8 }}>
            Refresh
          </Button>
          {canReview && activeTab === 'pending' && stats.pending > 0 && (
            <Popconfirm
              title={`Confirm bulk approval of ${tasks.filter((t) => t.status === 'pending').length} pending documents?`}
              onConfirm={handleBatchApprove}
              okText="Confirm"
              cancelText="Cancel"
            >
              <Button type="primary" icon={<CheckOutlined />} style={{ borderRadius: 8 }}>
                Approve All
              </Button>
            </Popconfirm>
          )}
        </Space>
      </div>

      {/* ── Stats ── */}
      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        {[
          { k: 'pending', label: 'Pending', color: C.pending, icon: <ClockCircleOutlined /> },
          { k: 'approved', label: 'Approved', color: C.approved, icon: <CheckCircleOutlined /> },
          { k: 'rejected', label: 'Rejected', color: C.rejected, icon: <CloseOutlined /> },
        ].map((s) => (
          <Col xs={8} sm={8} md={8} key={s.k}>
            <Card
              bodyStyle={{ padding: '14px 18px' }}
              style={{
                borderRadius: C.radius,
                border: activeTab === s.k ? `2px solid ${s.color}` : `1px solid ${C.border}`,
                boxShadow: C.shadow,
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              hoverable
              onClick={() => {
                setActiveTab(s.k);
                loadData(s.k);
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ fontSize: 26, fontWeight: 700, color: C.text }}>
                    {(stats as any)[s.k]}
                  </div>
                  <Text style={{ fontSize: 13, color: C.textTer }}>{s.label}</Text>
                </div>
                <div style={{
                  width: 40, height: 40, borderRadius: 10,
                  background: `${s.color}18`,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: s.color, fontSize: 18,
                }}>
                  {s.icon}
                </div>
              </div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* ── Filter Bar ── */}
      <Card bodyStyle={{ padding: '10px 16px 12px' }} style={{ borderRadius: C.radius, border: `1px solid ${C.border}`, boxShadow: C.shadow, marginBottom: 12 }}>
        {/* Row 1: Status segmented filter */}
        <div style={{
          display: 'inline-flex', background: '#f1f5f9', borderRadius: 10, padding: 3,
          border: `1px solid ${C.border}`, marginBottom: 10,
        }}>
          {segments.map((seg) => {
            const active = activeTab === seg.key;
            const count = stats[seg.key as keyof typeof stats];
            return (
              <button
                key={seg.key}
                onClick={() => { setActiveTab(seg.key); loadData(seg.key); }}
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: 6,
                  padding: '6px 16px', borderRadius: 8, border: 'none',
                  cursor: 'pointer', fontSize: 13, fontWeight: active ? 600 : 400,
                  color: active ? '#fff' : C.textSec,
                  background: active ? seg.color : 'transparent',
                  transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                  outline: 'none', lineHeight: '20px',
                  fontFamily: 'inherit',
                  boxShadow: active ? '0 1px 3px rgba(0,0,0,0.15)' : 'none',
                }}
                onMouseEnter={(e) => {
                  if (!active) (e.currentTarget as HTMLElement).style.background = '#e2e8f0';
                }}
                onMouseLeave={(e) => {
                  if (!active) (e.currentTarget as HTMLElement).style.background = 'transparent';
                }}
              >
                <span style={{
                  width: 6, height: 6, borderRadius: '50%',
                  background: active ? 'rgba(255,255,255,0.8)' : seg.dot,
                  flexShrink: 0,
                }} />
                {seg.label}
                {count > 0 && (
                  <span style={{
                    minWidth: 18, height: 18, lineHeight: '18px', textAlign: 'center',
                    borderRadius: 9, fontSize: 11, fontWeight: 600,
                    padding: '0 5px',
                    background: active ? 'rgba(255,255,255,0.25)' : seg.bg,
                    color: active ? '#fff' : seg.color,
                  }}>
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Row 2: Search + date + author + category */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
          <Input
            prefix={<SearchOutlined style={{ color: C.textTer }} />}
            placeholder="Search by title or author..."
            allowClear
            style={{ width: 240, borderRadius: 8 }}
            onChange={(e) => setSearchText(e.target.value)}
          />

          <div style={{ width: 1, height: 22, background: C.border }} />

          <RangePicker
            value={dateRange as any}
            onChange={(dates) => setDateRange(dates as [dayjs.Dayjs | null, dayjs.Dayjs | null] | null)}
            placeholder={['Start date', 'End date']}
            size="middle"
            style={{ borderRadius: 8, width: 240 }}
            allowClear
          />

          <Input
            value={authorFilter}
            onChange={(e) => setAuthorFilter(e.target.value || undefined)}
            placeholder="Submitter"
            allowClear
            size="middle"
            style={{ borderRadius: 8, width: 120 }}
          />

          <Input
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value || undefined)}
            placeholder="Document category"
            allowClear
            size="middle"
            style={{ borderRadius: 8, width: 120 }}
          />
        </div>
      </Card>

      {/* ── Table ── */}
      <Card bodyStyle={{ padding: 0 }} style={{ borderRadius: C.radius, border: `1px solid ${C.border}`, boxShadow: C.shadow }}>
        <Table<ReviewTask>
          rowKey="id"
          columns={columns}
          dataSource={filteredTasks}
          loading={loading}
          pagination={{
            pageSize: 20,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            showTotal: (total) => `${total} total`,
            size: 'default',
          }}
          size="middle"
          locale={{
            emptyText: (
              <div style={{ padding: '40px 0' }}>
                <Text style={{ color: C.textTer, fontSize: 14 }}>No data yet</Text>
              </div>
            ),
          }}
        />
      </Card>

      {/* ── Reject Modal ── */}
      <Modal
        title="Reject Document"
        open={rejectModalOpen}
        onOk={handleRejectConfirm}
        onCancel={() => {
          setRejectModalOpen(false);
          setRejectTask(null);
        }}
        confirmLoading={submittingIds.has(rejectTask?.id || '')}
        okText="Confirm Rejection"
        cancelText="Cancel"
        okButtonProps={{ danger: true }}
        destroyOnClose
      >
        <div style={{ marginBottom: 8 }}>
          <Text strong>Document:</Text>
          <Text>{rejectTask?.documentTitle}</Text>
        </div>
        <TextArea
          rows={4}
          value={rejectComment}
          onChange={(e) => setRejectComment(e.target.value)}
          placeholder="Please enter the reason for rejection (required)..."
          style={{ borderRadius: 8 }}
        />
      </Modal>
    </div>
  );
};

export default ReviewPage;
