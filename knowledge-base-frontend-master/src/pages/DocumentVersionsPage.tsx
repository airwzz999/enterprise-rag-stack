import React, { useEffect, useState } from 'react';
import {
  Card,
  List,
  Typography,
  Tag,
  Button,
  Space,
  Modal,
  Result,
  Tooltip,
  Popconfirm,
  Spin,
  Descriptions,
} from 'antd';
import { App } from 'antd';
import {
  HistoryOutlined,
  UserOutlined,
  ClockCircleOutlined,
  RollbackOutlined,
  EyeOutlined,
  DiffOutlined,
} from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/common';
import UserAvatar from '@/components/common/UserAvatar';
import { useVersionStore } from '@/stores';
import { DocumentVersion } from '@/types';
import dayjs from 'dayjs';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeRaw from 'rehype-raw';

const { Text } = Typography;

export const DocumentVersionsPage: React.FC = () => {
  const { message } = App.useApp();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { versions, isLoading, isComparing, compareResult, fetchVersions, restoreVersion, compareVersions } =
    useVersionStore();

  const [selectedVersions, setSelectedVersions] = useState<string[]>([]);
  const [previewVersion, setPreviewVersion] = useState<DocumentVersion | null>(null);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [compareModalVisible, setCompareModalVisible] = useState(false);

  useEffect(() => {
    if (id) {
      fetchVersions(id);
    }
  }, [id]);

  const handleRestore = async (versionId: string) => {
    if (!id) return;

    try {
      await restoreVersion(id, versionId);
      message.success('Version restored successfully');
      // Refetch the version list
      await fetchVersions(id);
    } catch (error) {
      // Error handled
    }
  };

  const handlePreview = (version: DocumentVersion) => {
    setPreviewVersion(version);
    setPreviewVisible(true);
  };

  const handleCompare = () => {
    if (selectedVersions.length !== 2) {
      message.warning('Please select two versions to compare');
      return;
    }

    if (!id) return;

    setCompareModalVisible(true);
    compareVersions(id, selectedVersions[0], selectedVersions[1]);
  };

  const getVersionStatusTag = (version: DocumentVersion) => {
    if (version.isCurrent) {
      return <Tag color="green">Current Version</Tag>;
    }
    return <Tag>Historical Version</Tag>;
  };

  return (
    <div>
      <PageHeader
        title="Version History"
        subtitle="View and restore historical versions of the document"
        extra={
          <Space>
            <Button
              icon={<RollbackOutlined />}
              disabled={selectedVersions.length !== 2}
              onClick={handleCompare}
            >
              Compare Versions
            </Button>
            <Button onClick={() => navigate(`/documents/${id}`)}>Back to Document</Button>
          </Space>
        }
      />

      <Card style={{ borderRadius: 12 }}>
        <Spin spinning={isLoading}>
          {versions.length === 0 ? (
            <Result
              icon={<HistoryOutlined style={{ fontSize: 48, color: '#d9d9d9' }} />}
              title="No Version History"
              subTitle="This document has no version records yet"
            />
          ) : (
            <List
              dataSource={versions}
              renderItem={(version) => (
                <List.Item
                  key={version.id}
                  style={{
                    padding: '16px',
                    borderRadius: 8,
                    background: '#fff',
                    marginBottom: 8,
                    border: '1px solid #f0f0f0',
                    cursor: 'pointer',
                  }}
                  onClick={() => {
                    const selectedIndex = selectedVersions.indexOf(version.id);
                    if (selectedIndex > -1) {
                      setSelectedVersions(selectedVersions.filter((id) => id !== version.id));
                    } else if (selectedVersions.length < 2) {
                      setSelectedVersions([...selectedVersions, version.id]);
                    }
                  }}
                  actions={[
                    <Tooltip title="Preview this version">
                      <Button
                        type="text"
                        icon={<EyeOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          handlePreview(version);
                        }}
                      />
                    </Tooltip>,
                    !version.isCurrent && (
                      <Popconfirm
                        title="Are you sure you want to restore to this version?"
                        description="A new version will be created after restoring"
                        onConfirm={(e) => {
                          e?.stopPropagation();
                          handleRestore(version.id);
                        }}
                        okText="Confirm"
                        cancelText="Cancel"
                      >
                        <Button
                          type="text"
                          icon={<RollbackOutlined />}
                          onClick={(e) => e.stopPropagation()}
                          danger
                        >
                          Restore
                        </Button>
                      </Popconfirm>
                    ),
                  ]}
                >
                  <List.Item.Meta
                    avatar={
                      <div
                        style={{
                          position: 'relative',
                          display: 'inline-block',
                        }}
                      >
                        <UserAvatar
                          src={version.author.avatar}
                          style={{
                            width: 48,
                            height: 48,
                            borderRadius: '50%',
                            objectFit: 'cover',
                            border: selectedVersions.includes(version.id)
                              ? '2px solid #1890ff'
                              : '2px solid transparent',
                          }}
                        />
                        {selectedVersions.includes(version.id) && (
                          <div
                            style={{
                              position: 'absolute',
                              bottom: -4,
                              right: -4,
                              background: '#1890ff',
                              color: '#fff',
                              borderRadius: '50%',
                              width: 20,
                              height: 20,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontSize: 12,
                              fontWeight: 'bold',
                            }}
                          >
                            {selectedVersions.indexOf(version.id) + 1}
                          </div>
                        )}
                      </div>
                    }
                    title={
                      <Space>
                        <Text strong>v{version.version}</Text>
                        {getVersionStatusTag(version)}
                        <Text type="secondary">|</Text>
                        <Text>{version.title}</Text>
                      </Space>
                    }
                    description={
                      <div>
                        <Space size="large">
                          <Text type="secondary">
                            <ClockCircleOutlined /> {dayjs(version.createdAt).format('YYYY-MM-DD HH:mm')}
                          </Text>
                          <Text type="secondary">
                            <UserOutlined /> {version.author.username}
                          </Text>
                          {version.changeLog && (
                            <Text type="secondary">{version.changeLog}</Text>
                          )}
                        </Space>
                      </div>
                    }
                  />
                </List.Item>
              )}
            />
          )}
        </Spin>
      </Card>

      {/* Version preview modal */}
      <Modal
        title={`Version Preview - ${previewVersion?.version}`}
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={[
          <Button key="close" onClick={() => setPreviewVisible(false)}>
            Close
          </Button>,
          !previewVersion?.isCurrent && (
            <Popconfirm
              key="restore"
              title="Are you sure you want to restore to this version?"
              onConfirm={() => {
                if (previewVersion && id) {
                  handleRestore(previewVersion.id);
                  setPreviewVisible(false);
                }
              }}
              okText="Confirm"
              cancelText="Cancel"
            >
              <Button type="primary" icon={<RollbackOutlined />} danger>
                Restore This Version
              </Button>
            </Popconfirm>
          ),
        ]}
        width={800}
      >
        {previewVersion && (
          <div>
            <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Version">{previewVersion.version}</Descriptions.Item>
              <Descriptions.Item label="Created At">
                {dayjs(previewVersion.createdAt).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
              <Descriptions.Item label="Created By">{previewVersion.author.username}</Descriptions.Item>
              <Descriptions.Item label="Change Notes">
                {previewVersion.changeLog || '-'}
              </Descriptions.Item>
            </Descriptions>
            <Card title="Content Preview" size="small">
              <div style={{ maxHeight: 400, overflow: 'auto' }}>
                <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeRaw]}>
                  {previewVersion.content}
                </ReactMarkdown>
              </div>
            </Card>
          </div>
        )}
      </Modal>

      {/* Version comparison modal */}
      <Modal
        title={
          <Space>
            <DiffOutlined />
            <span>Version Comparison</span>
          </Space>
        }
        open={compareModalVisible}
        onCancel={() => setCompareModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setCompareModalVisible(false)}>
            Close
          </Button>,
        ]}
        width={1000}
      >
        {isComparing ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin size="large" />
          </div>
        ) : compareResult ? (
          <div>
            <Descriptions size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Old Version">
                v{compareResult.old.version} ({dayjs(compareResult.old.createdAt).format('YYYY-MM-DD')})
              </Descriptions.Item>
              <Descriptions.Item label="New Version">
                v{compareResult.new.version} ({dayjs(compareResult.new.createdAt).format('YYYY-MM-DD')})
              </Descriptions.Item>
            </Descriptions>
            <Card title="Diff Comparison" size="small">
              <div
                dangerouslySetInnerHTML={{ __html: compareResult.diff }}
                style={{
                  maxHeight: 400,
                  overflow: 'auto',
                  fontFamily: 'Monaco, Menlo, monospace',
                  fontSize: 13,
                }}
              />
            </Card>
          </div>
        ) : null}
      </Modal>
    </div>
  );
};

export default DocumentVersionsPage;
