import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Checkbox,
  Card,
  Row,
  Col,
  Typography,
} from 'antd';
import { App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SafetyOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { roleService } from '@/services';
import permissionService, { PermissionVO } from '@/services/permission.service';
import type { Role } from '@/types';

interface RoleFormData {
  name: string;
  code: string;
  description?: string;
  permissions: string[];
}

const normalizeRole = (role: Role): Role => ({
  ...role,
  userCount: Number(role.userCount) || 0,
  status: role.status !== undefined ? Number(role.status) : role.status,
});

export const RolesPage: React.FC = () => {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<PermissionVO[]>([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [searchText, setSearchText] = useState('');
  const [form] = Form.useForm<RoleFormData>();

  useEffect(() => {
    fetchPageData();
  }, []);

  const permissionNameMap = useMemo(
    () =>
      permissions.reduce<Record<string, string>>((acc, permission) => {
        acc[String(permission.id)] = permission.name;
        return acc;
      }, {}),
    [permissions]
  );

  const permissionOptions = useMemo(
    () =>
      permissions.map((permission) => ({
        label: `${permission.name} (${permission.code})`,
        value: String(permission.id),
      })),
    [permissions]
  );

  const fetchPageData = async () => {
    setLoading(true);
    try {
      const [roleList, permissionList] = await Promise.all([
        roleService.getRoles({ skipErrorToast: true }),
        permissionService.getAllPermissions(),
      ]);
      setRoles((roleList || []).map(normalizeRole));
      setPermissions(permissionList || []);
    } catch (error) {
      message.error('Failed to fetch role data');
    } finally {
      setLoading(false);
    }
  };

  const filteredRoles = useMemo(() => {
    const keyword = searchText.trim().toLowerCase();
    if (!keyword) {
      return roles;
    }
    return roles.filter((role) =>
      [role.name, role.code, role.description]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(keyword))
    );
  }, [roles, searchText]);

  const activeRoleCount = useMemo(
    () => roles.filter((role) => role.status !== 0).length,
    [roles]
  );

  const assignedUserCount = useMemo(
    () => roles.reduce((sum, role) => sum + (Number(role.userCount) || 0), 0),
    [roles]
  );

  const permissionBindingCount = useMemo(
    () => roles.reduce((sum, role) => sum + (role.permissions?.length || 0), 0),
    [roles]
  );

  const getLinkedStatCardStyle = (clickable: boolean): React.CSSProperties => ({
    cursor: clickable ? 'pointer' : 'default',
    transition: 'all 0.2s ease',
    borderRadius: 12,
  });

  const statCardBodyStyle: React.CSSProperties = {
    minHeight: 120,
    display: 'flex',
    alignItems: 'stretch',
  };

  const statItemStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    minHeight: 88,
    width: '100%',
  };

  const handleAdd = () => {
    setEditingRole(null);
    form.resetFields();
    form.setFieldsValue({ permissions: [] });
    setIsModalVisible(true);
  };

  const handleEdit = async (role: Role) => {
    setEditingRole(role);
    setSaving(true);
    try {
      const permissionIds = await roleService.getRolePermissions(String(role.id), {
        skipErrorToast: true,
      });
      form.setFieldsValue({
        name: role.name,
        code: role.code,
        description: role.description,
        permissions: permissionIds || [],
      });
      setIsModalVisible(true);
    } catch (error) {
      message.error('Failed to fetch role permissions');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = (role: Role) => {
    if ((Number(role.userCount) || 0) > 0) {
      message.warning('This role is assigned to users. Please unassign users before deleting it');
      return;
    }
    Modal.confirm({
      title: 'Confirm Deletion',
      content: `Are you sure you want to delete the role "${role.name}"?`,
      okText: 'OK',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          await roleService.deleteRole(String(role.id));
          message.success('Deleted successfully');
          fetchPageData();
        } catch (error) {
          message.error('Delete failed');
        }
      },
    });
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);

      if (editingRole) {
        await roleService.updateRole(String(editingRole.id), values);
        await roleService.assignPermissions(String(editingRole.id), values.permissions || []);
        message.success('Role updated successfully');
      } else {
        const roleId = await roleService.createRole(values);
        await roleService.assignPermissions(String(roleId), values.permissions || []);
        message.success('Role created successfully');
      }

      setIsModalVisible(false);
      form.resetFields();
      fetchPageData();
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(editingRole ? 'Failed to update role' : 'Failed to create role');
    } finally {
      setSaving(false);
    }
  };

  const columns: ColumnsType<Role> = [
    {
      title: 'Role Name',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          <SafetyOutlined style={{ color: '#2563eb' }} />
          <div>
            <div>{text}</div>
            <div style={{ fontSize: 12, color: '#999' }}>{record.code}</div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      render: (value?: string) => value || '-',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status?: number) => (
        <Tag color={status === 0 ? 'default' : 'success'}>
          {status === 0 ? 'Disabled' : 'Enabled'}
        </Tag>
      ),
    },
    {
      title: 'Permissions',
      dataIndex: 'permissions',
      key: 'permissions',
      render: (permissionIds: string[] = []) => (
        <Space size="small" wrap>
          {permissionIds.slice(0, 3).map((permissionId) => (
            <Tag key={permissionId} color="blue">
              {permissionNameMap[permissionId] || permissionId}
            </Tag>
          ))}
          {permissionIds.length > 3 && <Tag>+{permissionIds.length - 3}</Tag>}
        </Space>
      ),
    },
    {
      title: 'Users',
      dataIndex: 'userCount',
      key: 'userCount',
      render: (count?: number | string) => <Tag color="green">{Number(count) || 0}</Tag>,
    },
    {
      title: 'Actions',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            Edit
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            disabled={(Number(record.userCount) || 0) > 0}
            onClick={() => handleDelete(record)}
          >
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="admin-roles-page">
      <div className="page-header">
        <div>
          <h2>Role Management</h2>
          <p>Manage system roles, statuses, and permission bindings</p>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchPageData}>
            Refresh
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            Add Role
          </Button>
        </Space>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card styles={{ body: statCardBodyStyle }}>
            <div className="stat-item" style={statItemStyle}>
              <div className="stat-value">{roles.length}</div>
              <div className="stat-label">Total Roles</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            hoverable
            style={getLinkedStatCardStyle(true)}
            styles={{ body: statCardBodyStyle }}
            onClick={() => navigate('/admin/users')}
          >
            <div className="stat-item" style={statItemStyle}>
              <div className="stat-value">{assignedUserCount}</div>
              <div className="stat-label">Assigned Users</div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Click to view User Management
              </Typography.Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card styles={{ body: statCardBodyStyle }}>
            <div className="stat-item" style={statItemStyle}>
              <div className="stat-value">{activeRoleCount}</div>
              <div className="stat-label">Enabled Roles</div>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card
            hoverable
            style={getLinkedStatCardStyle(true)}
            styles={{ body: statCardBodyStyle }}
            onClick={() => navigate('/admin/permissions')}
          >
            <div className="stat-item" style={statItemStyle}>
              <div className="stat-value">{permissionBindingCount}</div>
              <div className="stat-label">Permission Bindings</div>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                Click to view Permission Management
              </Typography.Text>
            </div>
          </Card>
        </Col>
      </Row>

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, marginBottom: 16, flexWrap: 'wrap' }}>
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="Search by role name, code, or description"
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
            style={{ width: 320 }}
          />
          <Typography.Text type="secondary">
            Roles assigned to users cannot be deleted directly
          </Typography.Text>
        </div>
        <Table
          columns={columns}
          dataSource={filteredRoles}
          rowKey="id"
          loading={loading}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `${total} total`,
          }}
        />
      </Card>

      <Modal
        title={editingRole ? 'Edit Role' : 'Add Role'}
        open={isModalVisible}
        onOk={handleModalOk}
        onCancel={() => {
          setIsModalVisible(false);
          setEditingRole(null);
          form.resetFields();
        }}
        confirmLoading={saving}
        width={720}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="Role Name"
            name="name"
            rules={[{ required: true, message: 'Please enter the role name' }]}
          >
            <Input placeholder="Please enter the role name" />
          </Form.Item>

          <Form.Item
            label="Role Code"
            name="code"
            rules={[
              { required: true, message: 'Please enter the role code' },
              { pattern: /^ROLE_[A-Z0-9_]+$/, message: 'Role code must start with ROLE_ and use uppercase letters' },
            ]}
          >
            <Input placeholder="Please enter the role code, e.g. ROLE_ADMIN" />
          </Form.Item>

          <Form.Item label="Description" name="description">
            <Input.TextArea placeholder="Please enter the role description" rows={3} />
          </Form.Item>

          <Form.Item
            label="Permissions"
            name="permissions"
            rules={[{ required: true, message: 'Please select permissions' }]}
          >
            <Checkbox.Group style={{ width: '100%' }}>
              <Row gutter={[8, 8]}>
                {permissionOptions.map((option) => (
                  <Col xs={24} sm={12} key={option.value}>
                    <Checkbox value={option.value}>{option.label}</Checkbox>
                  </Col>
                ))}
              </Row>
            </Checkbox.Group>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default RolesPage;
