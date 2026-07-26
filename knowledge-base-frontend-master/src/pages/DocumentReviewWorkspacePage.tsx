import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Tag,
  Timeline,
  Typography,
} from 'antd';
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  HistoryOutlined,
  SendOutlined,
} from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import { normalizeMarkdown } from '../utils/markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import { documentService, reviewService } from '@/services';
import { useAuthStore } from '@/stores';
import type { Document, ReviewTask } from '@/types';

dayjs.locale('zh-cn');

const { Title, Paragraph, Text } = Typography;
const { TextArea } = Input;

const pageStyle: React.CSSProperties = {
  minHeight: '100vh',
  background: '#f5f7fb',
  padding: 24,
};

const cardStyle: React.CSSProperties = {
  borderRadius: 18,
  border: '1px solid #e6ebf2',
  boxShadow: '0 10px 32px rgba(15, 23, 42, 0.06)',
};

const markdownStyle = `
  .review-markdown {
    color: #1f2937;
    line-height: 1.8;
    font-size: 14px;
  }
  .review-markdown h1,
  .review-markdown h2,
  .review-markdown h3,
  .review-markdown h4 {
    color: #111827;
    margin-top: 1.2em;
    margin-bottom: 0.6em;
  }
  .review-markdown p,
  .review-markdown li {
    margin-bottom: 0.75em;
  }
  .review-markdown pre {
    background: #0f172a;
    color: #e2e8f0;
    padding: 16px;
    border-radius: 12px;
    overflow: auto;
  }
  .review-markdown code {
    background: rgba(15, 23, 42, 0.06);
    padding: 2px 6px;
    border-radius: 6px;
  }
  .review-markdown pre code {
    background: transparent;
    padding: 0;
  }
  .review-markdown blockquote {
    margin: 1em 0;
    padding: 12px 16px;
    border-left: 4px solid #93c5fd;
    background: #eff6ff;
    color: #1e3a8a;
    border-radius: 0 10px 10px 0;
  }
`;

const statusMeta: Record<ReviewTask['status'], { text: string; color: string; icon: React.ReactNode }> = {
  pending: { text: 'Pending Review', color: 'gold', icon: <ClockCircleOutlined /> },
  approved: { text: 'Approved', color: 'green', icon: <CheckCircleOutlined /> },
  rejected: { text: 'Rejected', color: 'red', icon: <CloseCircleOutlined /> },
};

const documentStatusText = (status: Document['status']) => {
  if (status === 'draft' || status === 0) return 'Draft';
  if (status === 'pending_review' || status === 2) return 'Pending Review';
  if (status === 'published' || status === 1) return 'Published';
  if (status === 'archived' || status === 3) return 'Archived';
  return 'Unknown';
};

const canReviewByRoles = (roles: string[]) =>
  roles.some((role) => {
    const upperRole = role.toUpperCase();
    return upperRole.includes('REVIEWER') || upperRole.includes('ADMIN');
  });

const toTimelineColor = (status: ReviewTask['status']) => {
  if (status === 'approved') return 'green';
  if (status === 'rejected') return 'red';
  return 'blue';
};

export const DocumentReviewWorkspacePage: React.FC = () => {
  const { documentId = '' } = useParams();
  const navigate = useNavigate();
  const { message } = App.useApp();
  const user = useAuthStore((state) => state.user);

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [document, setDocument] = useState<Document | null>(null);
  const [currentTask, setCurrentTask] = useState<ReviewTask | null>(null);
  const [history, setHistory] = useState<ReviewTask[]>([]);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectComment, setRejectComment] = useState('');

  const currentRoles = useMemo(() => user?.roles || (user?.role ? [user.role] : []), [user?.role, user?.roles]);
  const canReview = canReviewByRoles(currentRoles);
  const pendingTask = currentTask?.status === 'pending' ? currentTask : null;

  const loadData = useCallback(async () => {
    if (!documentId) {
      return;
    }
    setLoading(true);
    try {
      const [docRes, currentRes, historyRes] = await Promise.all([
        documentService.getDocument(documentId),
        reviewService.getCurrentReviewTask(documentId),
        reviewService.getReviewHistory(documentId),
      ]);
      setDocument(docRes);
      setCurrentTask(currentRes);
      setHistory(historyRes || []);
    } catch (error: any) {
      message.error(error?.message || 'Failed to load the review page');
    } finally {
      setLoading(false);
    }
  }, [documentId, message]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const submitReview = async (status: 'approved' | 'rejected', comment?: string) => {
    if (!pendingTask) {
      message.warning('This document has no pending review task');
      return;
    }
    setSubmitting(true);
    try {
      await reviewService.reviewDocument(pendingTask.id, { status, comment });
      message.success(status === 'approved' ? 'Review approved' : 'Document rejected');
      setRejectOpen(false);
      setRejectComment('');
      await loadData();
    } catch (error: any) {
      message.error(error?.message || 'Review failed, please try again');
    } finally {
      setSubmitting(false);
    }
  };

  const latestTaskMeta = currentTask ? statusMeta[currentTask.status] : null;

  return (
    <div style={pageStyle}>
      <style>{markdownStyle}</style>
      <div style={{ maxWidth: 1480, margin: '0 auto' }}>
        <Card style={{ ...cardStyle, marginBottom: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            <div>
              <Space size={12} wrap style={{ marginBottom: 10 }}>
                <Tag color="blue" style={{ borderRadius: 999, paddingInline: 10 }}>Document Review Page</Tag>
                {latestTaskMeta && (
                  <Tag color={latestTaskMeta.color} icon={latestTaskMeta.icon} style={{ borderRadius: 999, paddingInline: 10 }}>
                    {latestTaskMeta.text}
                  </Tag>
                )}
              </Space>
              <Title level={2} style={{ margin: 0, fontSize: 26 }}>
                {document?.title || currentTask?.documentTitle || 'Document Review'}
              </Title>
              <Paragraph style={{ margin: '10px 0 0', color: '#64748b' }}>
                Clicking a review notification opens this page directly, allowing reviewers to view the document content, review status, and history in one place.
              </Paragraph>
            </div>
            <Space wrap>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/admin/review')}>
                Back to Review Management
              </Button>
              <Button icon={<FileTextOutlined />} onClick={() => window.open(`/documents/${documentId}`, '_blank', 'noopener,noreferrer')}>
                View Document Details
              </Button>
              <Button onClick={() => window.close()}>
                Close Window
              </Button>
            </Space>
          </div>
        </Card>

        <Spin spinning={loading}>
          <Row gutter={[20, 20]} align="top">
            <Col xs={24} xl={16}>
              <Card title="Document Content" style={cardStyle}>
                {document ? (
                  <>
                    <Descriptions column={2} size="small" style={{ marginBottom: 20 }}>
                      <Descriptions.Item label="Author">{document.author?.username || document.authorName || currentTask?.documentAuthor?.username || 'Unknown'}</Descriptions.Item>
                      <Descriptions.Item label="Category">{currentTask?.categoryName || 'Uncategorized'}</Descriptions.Item>
                      <Descriptions.Item label="Document Status">{documentStatusText(document.status)}</Descriptions.Item>
                      <Descriptions.Item label="Last Updated">{document.updatedAt ? dayjs(document.updatedAt).format('YYYY-MM-DD HH:mm') : '-'}</Descriptions.Item>
                    </Descriptions>
                    {document.summary && (
                      <Alert
                        type="info"
                        showIcon
                        message="Summary"
                        description={document.summary}
                        style={{ marginBottom: 20, borderRadius: 12 }}
                      />
                    )}
                    <div className="review-markdown">
                      <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>
                        {normalizeMarkdown(document.content || 'No document content available')}
                      </ReactMarkdown>
                    </div>
                  </>
                ) : (
                  <Empty description="Failed to load document content" />
                )}
              </Card>
            </Col>

            <Col xs={24} xl={8}>
              <Space direction="vertical" size={20} style={{ width: '100%' }}>
                <Card title="Current Review Task" style={cardStyle}>
                  {currentTask ? (
                    <>
                      <Descriptions column={1} size="small">
                        <Descriptions.Item label="Review Round">Round {currentTask.reviewRound || 1}</Descriptions.Item>
                        <Descriptions.Item label="Submitted At">{dayjs(currentTask.createdAt).format('YYYY-MM-DD HH:mm:ss')}</Descriptions.Item>
                        <Descriptions.Item label="Reviewer">{currentTask.reviewer?.username || 'Unclaimed / Pending'}</Descriptions.Item>
                        <Descriptions.Item label="Review Comments">{currentTask.comment || 'None'}</Descriptions.Item>
                      </Descriptions>

                      <Divider />

                      {!canReview && (
                        <Alert
                          type="warning"
                          showIcon
                          style={{ marginBottom: 16, borderRadius: 12 }}
                          message="This account does not have review permission"
                          description="Please use a reviewer or administrator account to process this document."
                        />
                      )}

                      {pendingTask ? (
                        <Space direction="vertical" style={{ width: '100%' }} size={12}>
                          <Button
                            type="primary"
                            icon={<CheckCircleOutlined />}
                            size="large"
                            block
                            disabled={!canReview}
                            loading={submitting}
                            onClick={() => submitReview('approved')}
                          >
                            Approve
                          </Button>
                          <Button
                            danger
                            icon={<CloseCircleOutlined />}
                            size="large"
                            block
                            disabled={!canReview}
                            onClick={() => setRejectOpen(true)}
                          >
                            Reject Document
                          </Button>
                          <Text type="secondary">
                            If approved, the document will be published; if rejected, it will be returned to draft and the author will be notified.
                          </Text>
                        </Space>
                      ) : (
                        <Alert
                          type={currentTask.status === 'approved' ? 'success' : 'error'}
                          showIcon
                          style={{ borderRadius: 12 }}
                          message={currentTask.status === 'approved' ? 'This document has been approved' : 'This document has been rejected'}
                          description="This task has already been processed; no further review is needed."
                        />
                      )}
                    </>
                  ) : (
                    <Empty description="No review tasks" />
                  )}
                </Card>

                <Card title={<Space><HistoryOutlined />Review History</Space>} style={cardStyle}>
                  {history.length > 0 ? (
                    <Timeline
                      items={history.map((item) => ({
                        color: toTimelineColor(item.status),
                        children: (
                          <div>
                            <Space wrap style={{ marginBottom: 4 }}>
                              <Text strong>Round {item.reviewRound || 1}</Text>
                              <Tag color={statusMeta[item.status].color}>{statusMeta[item.status].text}</Tag>
                            </Space>
                            <div style={{ color: '#64748b', fontSize: 13 }}>
                              Submitted: {dayjs(item.createdAt).format('YYYY-MM-DD HH:mm:ss')}
                            </div>
                            {item.reviewedAt && (
                              <div style={{ color: '#64748b', fontSize: 13 }}>
                                Reviewed: {dayjs(item.reviewedAt).format('YYYY-MM-DD HH:mm:ss')}
                              </div>
                            )}
                            <div style={{ color: '#64748b', fontSize: 13 }}>
                              Reviewer: {item.reviewer?.username || 'Pending'}
                            </div>
                            {item.comment && (
                              <div style={{ marginTop: 6, color: '#1f2937' }}>
                                Comments: {item.comment}
                              </div>
                            )}
                          </div>
                        ),
                      }))}
                    />
                  ) : (
                    <Empty description="No review history" />
                  )}
                </Card>
              </Space>
            </Col>
          </Row>
        </Spin>
      </div>

      <Modal
        title="Reject Document"
        open={rejectOpen}
        onCancel={() => {
          if (!submitting) {
            setRejectOpen(false);
            setRejectComment('');
          }
        }}
        onOk={() => submitReview('rejected', rejectComment)}
        confirmLoading={submitting}
        okText="Confirm Rejection"
        cancelText="Cancel"
        okButtonProps={{ danger: true, icon: <SendOutlined /> }}
        destroyOnHidden
      >
        <TextArea
          rows={5}
          value={rejectComment}
          onChange={(event) => setRejectComment(event.target.value)}
          placeholder="Enter the reason for rejection to help the author revise and resubmit."
          maxLength={500}
          showCount
        />
      </Modal>
    </div>
  );
};

export default DocumentReviewWorkspacePage;
