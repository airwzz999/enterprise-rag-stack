import React, { useEffect, useMemo, useState } from 'react';
import {
  App,
  Button,
  Card,
  Col,
  ConfigProvider,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Segmented,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tree,
  Typography,
} from 'antd';
import {
  ApiOutlined,
  EditOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import type { ColumnsType } from 'antd/es/table';
import permissionService, { PermissionTreeNode, PermissionVO } from '@/services/permission.service';
import { useAuthStore } from '@/stores';
import { PERMISSIONS, hasPermission } from '@/utils/permission';

const { Title, Text } = Typography;

type PermissionFormType = 'menu' | 'button' | 'api';
type ResourceFilterType = 'all' | 'menu' | 'button' | 'api';

const normalizeId = (value?: string | number | bigint | null) => String(value ?? '');

const isMenuType = (type?: string) => type === '1' || type === 'menu';

const getPermissionTypeName = (type?: string): string => {
  const typeMap: Record<string, string> = {
    '1': 'Menu',
    '2': 'Button Permission',
    '3': 'API Permission',
    menu: 'Menu',
    button: 'Button Permission',
    api: 'API Permission',
  };
  return typeMap[type || ''] || 'Other';
};

const getPermissionTypeColor = (type?: string) => {
  if (type === '2' || type === 'button') {
    return 'orange';
  }
  if (type === '3' || type === 'api') {
    return 'purple';
  }
  return 'green';
};

const getPermissionIcon = (type?: string) => {
  if (type === '2' || type === 'button') {
    return <KeyOutlined style={{ color: '#f59e0b' }} />;
  }
  if (type === '3' || type === 'api') {
    return <ApiOutlined style={{ color: '#7c3aed' }} />;
  }
  return <FolderOutlined style={{ color: '#2563eb' }} />;
};

const extractMenuNodes = (nodes: PermissionTreeNode[] = []): PermissionTreeNode[] =>
  nodes.flatMap((node) => {
    if (!isMenuType(node.type)) {
      return [];
    }
    return [
      {
        ...node,
        children: extractMenuNodes(node.children || []),
      },
    ];
  });

const filterMenuNodes = (nodes: PermissionTreeNode[], keyword: string): PermissionTreeNode[] => {
  if (!keyword.trim()) {
    return nodes;
  }
  const normalizedKeyword = keyword.trim().toLowerCase();
  return nodes.reduce<PermissionTreeNode[]>((acc, node) => {
    const children = filterMenuNodes(node.children || [], keyword);
    const matched = [node.name, node.code]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(normalizedKeyword));

    if (matched || children.length > 0) {
      acc.push({
        ...node,
        children,
      });
    }
    return acc;
  }, []);
};

const buildMenuTreeData = (nodes: PermissionTreeNode[]): DataNode[] =>
  nodes.map((node) => ({
    key: normalizeId(node.id),
    title: (
      <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <FolderOutlined style={{ color: '#2563eb' }} />
        <span>{node.name}</span>
      </span>
    ),
    children: buildMenuTreeData(node.children || []),
  }));

const findNodeById = (nodes: PermissionTreeNode[], targetId?: string | number | null): PermissionTreeNode | null => {
  const normalizedTargetId = normalizeId(targetId);
  if (!normalizedTargetId) {
    return null;
  }

  for (const node of nodes) {
    if (normalizeId(node.id) === normalizedTargetId) {
      return node;
    }
    const child = findNodeById(node.children || [], targetId);
    if (child) {
      return child;
    }
  }
  return null;
};

const findNodePath = (nodes: PermissionTreeNode[], targetId?: string | number | null): PermissionTreeNode[] => {
  const normalizedTargetId = normalizeId(targetId);
  if (!normalizedTargetId) {
    return [];
  }

  for (const node of nodes) {
    if (normalizeId(node.id) === normalizedTargetId) {
      return [node];
    }
    const childPath = findNodePath(node.children || [], targetId);
    if (childPath.length > 0) {
      return [node, ...childPath];
    }
  }
  return [];
};

const collectMenuKeys = (nodes: PermissionTreeNode[]): string[] =>
  nodes.flatMap((node) => [normalizeId(node.id), ...collectMenuKeys(node.children || [])]);

const buildParentPermissionOptions = (
  nodes: PermissionTreeNode[],
  level = 0,
): Array<{ label: string; value: string }> =>
  nodes.flatMap((node) => {
    const current = {
      label: `${'　'.repeat(level)}${node.name}`,
      value: normalizeId(node.id),
    };
    return [current, ...buildParentPermissionOptions(node.children || [], level + 1)];
  });

const PermissionsPage: React.FC = () => {
  const { message } = App.useApp();
  const user = useAuthStore((state) => state.user);
  const canCreatePermission = hasPermission(user, PERMISSIONS.systemPermission)
    || hasPermission(user, PERMISSIONS.systemPermissionCreate);
  const canEditPermission = hasPermission(user, PERMISSIONS.systemPermission)
    || hasPermission(user, PERMISSIONS.systemPermissionEdit);
  const canDeletePermission = hasPermission(user, PERMISSIONS.systemPermission)
    || hasPermission(user, PERMISSIONS.systemPermissionDelete);
  const [loading, setLoading] = useState(false);
  const [treeLoading, setTreeLoading] = useState(false);
  const [permissionTree, setPermissionTree] = useState<PermissionTreeNode[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [selectedMenuId, setSelectedMenuId] = useState<string>('');
  const [currentMenu, setCurrentMenu] = useState<PermissionVO | null>(null);
  const [currentResources, setCurrentResources] = useState<PermissionVO[]>([]);
  const [menuSearchText, setMenuSearchText] = useState('');
  const [resourceSearchText, setResourceSearchText] = useState('');
  const [resourceFilterType, setResourceFilterType] = useState<ResourceFilterType>('all');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingPermission, setEditingPermission] = useState<PermissionVO | null>(null);
  const [previewPermission, setPreviewPermission] = useState<PermissionVO | null>(null);
  const [form] = Form.useForm();
  const permissionType = Form.useWatch<PermissionFormType>('type', form) || 'menu';

  const menuTree = useMemo(() => extractMenuNodes(permissionTree), [permissionTree]);
  const filteredMenuTree = useMemo(
    () => filterMenuNodes(menuTree, menuSearchText),
    [menuTree, menuSearchText]
  );
  const parentPermissionOptions = useMemo(
    () => [{ label: 'Top-level Menu', value: '0' }, ...buildParentPermissionOptions(menuTree)],
    [menuTree]
  );
  const selectedMenuPath = useMemo(
    () => findNodePath(menuTree, selectedMenuId),
    [menuTree, selectedMenuId]
  );
  const selectedMenuPathText = useMemo(() => {
    if (selectedMenuPath.length <= 1) {
      return '';
    }
    return selectedMenuPath.map((item) => item.name).join(' / ');
  }, [selectedMenuPath]);

  const filteredResources = useMemo(() => {
    const normalizedKeyword = resourceSearchText.trim().toLowerCase();
    return currentResources.filter((permission) => {
      const matchesKeyword =
        !normalizedKeyword ||
        [permission.name, permission.code, permission.menuUrl, permission.apiUrl]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(normalizedKeyword));

      if (!matchesKeyword) {
        return false;
      }

      if (resourceFilterType === 'all') {
        return true;
      }
      if (resourceFilterType === 'menu') {
        return isMenuType(permission.type);
      }
      return permission.type === resourceFilterType || permission.type === (resourceFilterType === 'button' ? '2' : '3');
    });
  }, [currentResources, resourceSearchText, resourceFilterType]);

  const childMenuCount = useMemo(
    () => currentResources.filter((permission) => isMenuType(permission.type)).length,
    [currentResources]
  );
  const childPointCount = useMemo(
    () => currentResources.filter((permission) => !isMenuType(permission.type)).length,
    [currentResources]
  );

  const syncPageData = async (preferredSelectedMenuId?: string | null) => {
    setLoading(true);
    setTreeLoading(true);
    try {
      const treeResult = await permissionService.getPermissionTree({ skipErrorToast: true });
      const nextTree = treeResult || [];
      const nextMenuTree = extractMenuNodes(nextTree);
      const fallbackMenuId = normalizeId(nextMenuTree[0]?.id);
      const targetMenuId = normalizeId(preferredSelectedMenuId || selectedMenuId);
      const nextSelectedMenuId = findNodeById(nextMenuTree, targetMenuId)
        ? targetMenuId
        : fallbackMenuId;

      setPermissionTree(nextTree);
      setSelectedMenuId(nextSelectedMenuId);
      setExpandedKeys(Array.from(new Set(collectMenuKeys(nextMenuTree))));
    } catch (error) {
      message.error('Failed to fetch permission data');
    } finally {
      setLoading(false);
      setTreeLoading(false);
    }
  };

  useEffect(() => {
    syncPageData();
  }, []);

  useEffect(() => {
    const loadCurrentMenuData = async () => {
      if (!selectedMenuId) {
        setCurrentMenu(null);
        setCurrentResources([]);
        return;
      }

      setLoading(true);
      try {
        const [menuDetail, childResources] = await Promise.all([
          permissionService.getPermissionById(selectedMenuId, { skipErrorToast: true }),
          permissionService.getPermissionsByParentId(selectedMenuId, { skipErrorToast: true }),
        ]);
        setCurrentMenu(menuDetail || null);
        setCurrentResources(childResources || []);
      } catch (error) {
        setCurrentMenu(null);
        setCurrentResources([]);
        message.error('Failed to fetch menu resources');
      } finally {
        setLoading(false);
      }
    };

    loadCurrentMenuData();
  }, [selectedMenuId, message]);

  const openPermissionModal = (type: PermissionFormType, parentId: string) => {
    setEditingPermission(null);
    form.resetFields();
    form.setFieldsValue({
      type,
      parentId,
      sortOrder: 0,
      status: 1,
    });
    setIsModalVisible(true);
  };

  const handleCreateTopMenu = () => openPermissionModal('menu', '0');

  const handleCreateChildMenu = () => openPermissionModal('menu', selectedMenuId || '0');

  const handleCreatePermissionPoint = () => {
    if (!selectedMenuId) {
      message.warning('Please select a menu in the tree on the left first');
      return;
    }
    openPermissionModal('button', selectedMenuId);
  };

  const handleEditCurrentMenu = () => {
    if (!currentMenu) {
      message.warning('Please select a menu in the tree on the left first');
      return;
    }
    handleEditPermission(currentMenu);
  };

  const handleEditPermission = (permission: PermissionVO) => {
    setEditingPermission(permission);
    form.setFieldsValue({
      name: permission.name,
      code: permission.code,
      type: permission.type,
      parentId: normalizeId(permission.parentId || 0),
      menuUrl: permission.menuUrl,
      apiUrl: permission.apiUrl,
      method: permission.method,
      description: permission.description,
      sortOrder: permission.sortOrder,
      status: permission.status,
    });
    setIsModalVisible(true);
  };

  const handleDeletePermission = async (permission: PermissionVO) => {
    try {
      await permissionService.deletePermission(normalizeId(permission.id));
      message.success(isMenuType(permission.type) ? 'Menu deleted successfully' : 'Permission deleted successfully');
      const nextSelectedId = normalizeId(permission.id) === selectedMenuId
        ? normalizeId(permission.parentId)
        : selectedMenuId;
      syncPageData(nextSelectedId === '0' ? null : nextSelectedId);
    } catch (error) {
      message.error(isMenuType(permission.type) ? 'Failed to delete menu' : 'Failed to delete permission');
    }
  };

  const handleSavePermission = async () => {
    try {
      const values = await form.validateFields();
      if (editingPermission) {
        await permissionService.updatePermission({
          ...values,
          id: editingPermission.id,
        });
        message.success('Updated successfully');
      } else {
        await permissionService.createPermission(values);
        message.success('Created successfully');
      }
      setIsModalVisible(false);
      syncPageData(selectedMenuId || null);
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(editingPermission ? 'Update failed' : 'Create failed');
    }
  };

  const handleSelectMenu = (keys: React.Key[]) => {
    const nextSelectedId = normalizeId(keys[0]);
    if (!nextSelectedId) {
      return;
    }
    setSelectedMenuId(nextSelectedId);
    setResourceFilterType('all');
    setResourceSearchText('');
  };

  const handleEnterMenu = (permission: PermissionVO) => {
    const nextMenuId = normalizeId(permission.id);
    const pathIds = findNodePath(menuTree, nextMenuId).map((item) => normalizeId(item.id));
    setExpandedKeys((prev) => Array.from(new Set([...prev, ...pathIds])));
    setSelectedMenuId(nextMenuId);
  };

  const getCreateModalTitle = (type: PermissionFormType) => {
    if (type === 'button') {
      return 'Add Button Permission';
    }
    if (type === 'api') {
      return 'Add API Permission';
    }
    return 'Add Menu';
  };

  const resourceColumns: ColumnsType<PermissionVO> = [
    {
      title: 'Resource Name',
      dataIndex: 'name',
      key: 'name',
      width: 196,
      ellipsis: true,
      render: (name: string, record) => (
        <Space align="start" size={10}>
          <span style={{ marginTop: 4 }}>{getPermissionIcon(record.type)}</span>
          <div style={{ minWidth: 0 }}>
            {isMenuType(record.type) ? (
              <Button
                type="link"
                style={{
                  padding: 0,
                  height: 'auto',
                  fontWeight: 600,
                  maxWidth: 130,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
                onClick={() => handleEnterMenu(record)}
              >
                {name}
              </Button>
            ) : (
              <Text strong ellipsis style={{ display: 'block', maxWidth: 130 }}>
                {name}
              </Text>
            )}
            <div
              style={{
                fontSize: 12,
                color: '#94a3b8',
                maxWidth: 130,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {record.code}
            </div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      width: 92,
      render: (type: string) => (
        <Tag color={getPermissionTypeColor(type)}>{getPermissionTypeName(type)}</Tag>
      ),
    },
    {
      title: 'Path / API',
      key: 'path',
      width: 150,
      ellipsis: true,
      render: (_: unknown, record) => record.menuUrl || record.apiUrl || '-',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 68,
      render: (status?: number) => (
        <Tag color={status === 0 ? 'default' : 'success'}>
          {status === 0 ? 'Disabled' : 'Enabled'}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 210,
      render: (_: unknown, record) => (
        <Space size={[4, 4]} wrap>
          {isMenuType(record.type) && (
            <Button type="text" size="small" onClick={() => handleEnterMenu(record)}>
              Enter
            </Button>
          )}
          <Button
            type="text"
            size="small"
            onClick={() => setPreviewPermission(record)}
          >
            View
          </Button>
          {canEditPermission && (
            <Button
              type="text"
              size="small"
              onClick={() => handleEditPermission(record)}
            >
              Edit
            </Button>
          )}
          {canDeletePermission && (
            <Popconfirm
              title={`Are you sure you want to delete "${record.name}"?`}
              onConfirm={() => handleDeletePermission(record)}
              okText="OK"
              cancelText="Cancel"
            >
              <Button type="text" size="small" danger>
                Delete
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  const menuTreeData = buildMenuTreeData(filteredMenuTree);
  const overviewCards = [
    { label: 'Current Menu', value: currentMenu?.name || '-', color: '#2563eb' },
    { label: 'Submenus', value: String(childMenuCount), color: '#0f766e' },
    { label: 'Permission Points', value: String(childPointCount), color: '#7c3aed' },
    { label: 'Total Resources', value: String(currentResources.length), color: '#f59e0b' },
  ];

  return (
    <div style={{ padding: '0 4px 12px 0', marginTop: -4, marginLeft: -8, background: '#f8fafc', minHeight: '100vh' }}>
      <div style={{ marginBottom: 6 }}>
        <Title level={2} style={{ margin: 0, marginBottom: 6, fontSize: 28, fontWeight: 700 }}>
          Permission Management
        </Title>
        <Text type="secondary">Navigate the menu tree and manage contextual resources to quickly configure menus, button permissions, and API permissions</Text>
      </div>

      <Row gutter={16} align="top">
        <Col span={6}>
          <Card
            title="Menu Tree"
            style={{ borderRadius: 16 }}
            styles={{ body: { padding: 14 } }}
          >
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Input
                placeholder="Search menu name or code"
                prefix={<SearchOutlined />}
                value={menuSearchText}
                onChange={(event) => setMenuSearchText(event.target.value)}
              />
              <div style={{ padding: '8px 12px', background: '#f8fafc', borderRadius: 12 }}>
                <Text type="secondary">{collectMenuKeys(menuTree).length} menu nodes loaded</Text>
              </div>
              <Spin spinning={treeLoading}>
                {menuTreeData.length > 0 ? (
                  <Tree
                    selectedKeys={selectedMenuId ? [selectedMenuId] : []}
                    expandedKeys={expandedKeys}
                    onExpand={(keys) =>
                      setExpandedKeys(
                        keys.filter((key): key is string => typeof key === 'string')
                      )
                    }
                    onSelect={handleSelectMenu}
                    treeData={menuTreeData}
                    style={{ minHeight: 560 }}
                  />
                ) : (
                  <Empty description="No menu data yet" />
                )}
              </Spin>
            </Space>
          </Card>
        </Col>

        <Col span={18}>
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Card
              style={{ borderRadius: 16 }}
              styles={{ body: { padding: 18 } }}
            >
              <Row gutter={[16, 16]} align="middle">
                <Col span={14}>
                  <Space direction="vertical" size={6}>
                    <Space align="center" size={8}>
                      <FolderOpenOutlined style={{ color: '#2563eb' }} />
                      <Text type="secondary">Current location</Text>
                      <Text strong>{selectedMenuPathText || currentMenu?.name || 'No menu selected'}</Text>
                    </Space>
                    <Text type="secondary">
                      {currentMenu
                        ? `Menu code: ${currentMenu.code}. Click a menu in the tree on the left or in the resource table to switch context.`
                        : 'Select a menu on the left first, then add, edit, or manage permission points.'}
                    </Text>
                  </Space>
                </Col>
                <Col span={10}>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, flexWrap: 'wrap' }}>
                    {canCreatePermission && (
                      <Button icon={<PlusOutlined />} onClick={handleCreateTopMenu}>
                        Add Menu
                      </Button>
                    )}
                    {canCreatePermission && (
                      <Button icon={<PlusOutlined />} onClick={handleCreateChildMenu}>
                        Add Submenu
                      </Button>
                    )}
                    {canCreatePermission && (
                      <Button type="primary" icon={<PlusOutlined />} onClick={handleCreatePermissionPoint}>
                        Add Permission Point
                      </Button>
                    )}
                    {canEditPermission && (
                      <Button icon={<EditOutlined />} onClick={handleEditCurrentMenu} disabled={!currentMenu}>
                        Edit Menu
                      </Button>
                    )}
                    <Button icon={<ReloadOutlined />} onClick={() => syncPageData(selectedMenuId || null)}>
                      Refresh
                    </Button>
                  </div>
                </Col>
              </Row>

              <Row gutter={12} style={{ marginTop: 18 }}>
                {overviewCards.map((item) => (
                  <Col key={item.label} span={6}>
                    <div
                      style={{
                        background: '#f8fafc',
                        borderRadius: 14,
                        padding: '14px 16px',
                        border: '1px solid #eef2f7',
                        minHeight: 88,
                      }}
                    >
                      <div style={{ color: '#64748b', fontSize: 13, marginBottom: 10 }}>{item.label}</div>
                      <div style={{ color: item.color, fontSize: 24, fontWeight: 700, lineHeight: 1.2 }}>
                        {item.value}
                      </div>
                    </div>
                  </Col>
                ))}
              </Row>

              {currentMenu && (
                <div
                  style={{
                    marginTop: 18,
                    padding: 16,
                    borderRadius: 14,
                    background: '#ffffff',
                    border: '1px solid #eef2f7',
                  }}
                >
                  <div style={{ marginBottom: 14, fontSize: 15, fontWeight: 600, color: '#0f172a' }}>
                    Current Menu Details
                  </div>
                  <Descriptions
                    bordered
                    size="middle"
                    column={3}
                    labelStyle={{
                      width: 120,
                      minWidth: 120,
                      color: '#64748b',
                      fontWeight: 500,
                      background: '#f8fafc',
                      whiteSpace: 'nowrap',
                    }}
                    contentStyle={{
                      color: '#0f172a',
                      whiteSpace: 'nowrap',
                    }}
                    items={[
                      {
                        key: 'name',
                        label: 'Menu Name',
                        children: <Text strong>{currentMenu.name}</Text>,
                      },
                      {
                        key: 'code',
                        label: 'Menu Code',
                        children: (
                          <span style={{ display: 'inline-block', whiteSpace: 'nowrap' }}>
                            <Tag color="blue" style={{ marginInlineEnd: 0 }}>{currentMenu.code}</Tag>
                          </span>
                        ),
                      },
                      {
                        key: 'status',
                        label: 'Status',
                        children: (
                          <Tag color={currentMenu.status === 0 ? 'default' : 'success'} style={{ marginInlineEnd: 0 }}>
                            {currentMenu.status === 0 ? 'Disabled' : 'Enabled'}
                          </Tag>
                        ),
                      },
                      {
                        key: 'path',
                        label: 'Menu Path',
                        children: (
                          <span style={{ display: 'inline-block', whiteSpace: 'nowrap' }}>
                            {currentMenu.menuUrl || '-'}
                          </span>
                        ),
                      },
                      {
                        key: 'parent',
                        label: 'Parent Menu',
                        children: (
                          <span style={{ display: 'inline-block', whiteSpace: 'nowrap' }}>
                            {selectedMenuPath.length > 1
                              ? selectedMenuPath[selectedMenuPath.length - 2]?.name || '-'
                              : 'Top-level Menu'}
                          </span>
                        ),
                      },
                      {
                        key: 'description',
                        label: 'Description',
                        span: 3,
                        children: (
                          <span style={{ display: 'inline-block', whiteSpace: 'nowrap' }}>
                            {currentMenu.description || 'None'}
                          </span>
                        ),
                      },
                    ]}
                  />
                </div>
              )}
            </Card>

            <Card
              title={currentMenu ? `${currentMenu.name} Sub-resources` : 'Resource List'}
              style={{ borderRadius: 16 }}
              styles={{ body: { padding: 20, overflow: 'hidden' } }}
              extra={
                <Space size={12} wrap>
                  <ConfigProvider
                    theme={{
                      components: {
                        Segmented: {
                          trackBg: '#f1f5f9',
                          itemSelectedBg: '#dbeafe',
                          itemSelectedColor: '#1d4ed8',
                          itemHoverBg: '#e2e8f0',
                          itemActiveBg: '#bfdbfe',
                        },
                      },
                    }}
                  >
                    <Segmented<ResourceFilterType>
                      value={resourceFilterType}
                      onChange={(value) => setResourceFilterType(value)}
                      options={[
                        { label: <span style={{ whiteSpace: 'nowrap' }}>All Resources</span>, value: 'all' },
                        { label: <span style={{ whiteSpace: 'nowrap' }}>Submenus</span>, value: 'menu' },
                        { label: <span style={{ whiteSpace: 'nowrap' }}>Button Permissions</span>, value: 'button' },
                        { label: <span style={{ whiteSpace: 'nowrap' }}>API Permissions</span>, value: 'api' },
                      ]}
                    />
                  </ConfigProvider>
                  <Input
                    placeholder="Search resources under the current menu"
                    prefix={<SearchOutlined />}
                    style={{ width: 240 }}
                    value={resourceSearchText}
                    onChange={(event) => setResourceSearchText(event.target.value)}
                  />
                </Space>
              }
            >
              <Spin spinning={loading}>
                <div style={{ width: '100%', maxWidth: '100%', overflow: 'hidden' }}>
                  <Table
                    tableLayout="fixed"
                    style={{ width: '100%' }}
                    columns={resourceColumns}
                    dataSource={filteredResources}
                    rowKey={(record) => normalizeId(record.id)}
                    pagination={{
                      pageSize: 10,
                      showSizeChanger: true,
                      showQuickJumper: true,
                      showTotal: (total) => `${total} resources total`,
                    }}
                    locale={{
                      emptyText: currentMenu ? 'No resources under this menu yet' : 'Please select a menu on the left first',
                    }}
                  />
                </div>
              </Spin>
            </Card>
          </Space>
        </Col>
      </Row>

      <Modal
        title={editingPermission ? 'Edit Resource' : getCreateModalTitle(permissionType)}
        open={isModalVisible}
        onOk={handleSavePermission}
        onCancel={() => {
          setIsModalVisible(false);
          setEditingPermission(null);
          form.resetFields();
        }}
        width={620}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="Resource Name"
            name="name"
            rules={[{ required: true, message: 'Please enter the resource name' }]}
          >
            <Input placeholder="Please enter the resource name" />
          </Form.Item>

          <Form.Item
            label="Permission Code"
            name="code"
            rules={[
              { required: true, message: 'Please enter the permission code' },
              { pattern: /^[a-z:_]+$/, message: 'The permission code may only contain lowercase letters, colons, and underscores' },
            ]}
          >
            <Input placeholder="e.g. system:user:view" disabled={!!editingPermission} />
          </Form.Item>

          <Form.Item
            label="Resource Type"
            name="type"
            rules={[{ required: true, message: 'Please select a resource type' }]}
          >
            <Select
              options={[
                { label: 'Menu', value: 'menu' },
                { label: 'Button Permission', value: 'button' },
                { label: 'API Permission', value: 'api' },
              ]}
            />
          </Form.Item>

          <Form.Item label="Parent Menu" name="parentId">
            <Select
              showSearch
              optionFilterProp="label"
              placeholder="Please select a parent menu"
              options={parentPermissionOptions.filter(
                (option) => option.value !== normalizeId(editingPermission?.id)
              )}
            />
          </Form.Item>

          {permissionType === 'menu' && (
            <Form.Item label="Menu Path" name="menuUrl">
              <Input placeholder="e.g. /admin/permissions" />
            </Form.Item>
          )}

          {permissionType !== 'menu' && (
            <Form.Item label="API Address" name="apiUrl">
              <Input placeholder={permissionType === 'api' ? 'e.g. /api/auth/permissions' : 'Optional: enter the associated API address'} />
            </Form.Item>
          )}

          {permissionType === 'api' && (
            <Form.Item
              label="Request Method"
              name="method"
              rules={[{ required: true, message: 'Please select a request method' }]}
            >
              <Select
                placeholder="Please select a request method"
                options={['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].map((method) => ({
                  label: method,
                  value: method,
                }))}
              />
            </Form.Item>
          )}

          <Form.Item label="Sort Order" name="sortOrder">
            <InputNumber
              style={{ width: '100%' }}
              min={0}
              precision={0}
              placeholder="Lower numbers appear first"
            />
          </Form.Item>

          <Form.Item label="Status" name="status" initialValue={1}>
            <Select
              options={[
                { label: 'Enabled', value: 1 },
                { label: 'Disabled', value: 0 },
              ]}
            />
          </Form.Item>

          <Form.Item label="Description" name="description">
            <Input.TextArea rows={3} placeholder="Please enter the resource description" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Resource Details"
        open={!!previewPermission}
        onCancel={() => setPreviewPermission(null)}
        footer={[
          <Button key="close" onClick={() => setPreviewPermission(null)}>
            Close
          </Button>,
        ]}
        width={560}
      >
        {previewPermission && (
          <Row gutter={[24, 16]}>
            <Col span={12}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Resource Name
              </Text>
              <Text strong>{previewPermission.name}</Text>
            </Col>
            <Col span={12}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Permission Code
              </Text>
              <Tag color="blue">{previewPermission.code}</Tag>
            </Col>
            <Col span={12}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Resource Type
              </Text>
              <Tag color={getPermissionTypeColor(previewPermission.type)}>
                {getPermissionTypeName(previewPermission.type)}
              </Tag>
            </Col>
            <Col span={12}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Status
              </Text>
              <Tag color={previewPermission.status === 0 ? 'default' : 'success'}>
                {previewPermission.status === 0 ? 'Disabled' : 'Enabled'}
              </Tag>
            </Col>
            <Col span={24}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Path / API
              </Text>
              <Text>{previewPermission.menuUrl || previewPermission.apiUrl || '-'}</Text>
            </Col>
            <Col span={12}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Request Method
              </Text>
              <Tag color="geekblue">{previewPermission.method || '-'}</Tag>
            </Col>
            <Col span={24}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                Description
              </Text>
              <Text>{previewPermission.description || '-'}</Text>
            </Col>
          </Row>
        )}
      </Modal>
    </div>
  );
};

export default PermissionsPage;
