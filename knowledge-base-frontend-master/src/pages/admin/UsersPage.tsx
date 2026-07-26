import React, { useState, useEffect } from 'react';
import {
  Table,
  Button,
  Space,
  Tag,
  Input,
  Select,
  Modal,
  Form,
  Card,
  Row,
  Col,
  Statistic,
} from 'antd';
import { App } from 'antd';
import {
  UserOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  TeamOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { User } from '@/types';
import { userService } from '@/services';
import UserAvatar from '@/components/common/UserAvatar';
import './AdminPages.css';

const { Search } = Input;
const { Option } = Select;

export const UsersPage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<User[]>([]);
  const [searchValue, setSearchValue] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchUsers();
  }, []);

  useEffect(() => {
    filterUsers();
  }, [users, searchValue, roleFilter]);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await userService.getUsers({ page: 1, pageSize: 100 });
      setUsers(data.list);
    } catch (error) {
      message.error('Failed to fetch user list');
    } finally {
      setLoading(false);
    }
  };

  const filterUsers = () => {
    let filtered = [...users];

    if (searchValue) {
      filtered = filtered.filter(
        (user) =>
          user.username.toLowerCase().includes(searchValue.toLowerCase()) ||
          user.email.toLowerCase().includes(searchValue.toLowerCase())
      );
    }

    if (roleFilter !== 'all') {
      filtered = filtered.filter((user) => user.role === roleFilter);
    }

    setFilteredUsers(filtered);
  };

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setIsModalVisible(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    form.setFieldsValue(user);
    setIsModalVisible(true);
  };

  const handleDelete = (userId: string) => {
    Modal.confirm({
      title: 'Confirm Deletion',
      content: 'Are you sure you want to delete this user?',
      okText: 'OK',
      cancelText: 'Cancel',
      onOk: async () => {
        try {
          // Call the delete API
          setUsers(users.filter((u) => u.id !== userId));
          message.success('Deleted successfully');
        } catch (error) {
          message.error('Delete failed');
        }
      },
    });
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();

      if (editingUser) {
        // Update user
        setUsers(
          users.map((u) =>
            u.id === editingUser.id ? { ...u, ...values } : u
          )
        );
        message.success('Updated successfully');
      } else {
        // Create user
        const newUser: User = {
          id: Date.now().toString(),
          ...values,
          createdAt: new Date().toISOString().split('T')[0],
          updatedAt: new Date().toISOString().split('T')[0],
        };
        setUsers([...users, newUser]);
        message.success('Created successfully');
      }

      setIsModalVisible(false);
      form.resetFields();
    } catch (error) {
      message.error('Operation failed');
    }
  };

  const columns: ColumnsType<User> = [
    {
      title: 'User',
      dataIndex: 'username',
      key: 'username',
      render: (text, record) => (
        <Space>
          <UserAvatar
            src={record.avatar}
            alt={text}
            style={{ width: 32, height: 32, borderRadius: '50%', objectFit: 'cover' }}
          />
          <div>
            <div>{text}</div>
            <div style={{ fontSize: 12, color: '#999' }}>{record.email}</div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Department',
      dataIndex: 'department',
      key: 'department',
    },
    {
      title: 'Position',
      dataIndex: 'position',
      key: 'position',
    },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={role === 'admin' ? 'red' : 'blue'}>
          {role === 'admin' ? 'Admin' : 'Regular User'}
        </Tag>
      ),
    },
    {
      title: 'Created At',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: (a, b) => new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime(),
    },
    {
      title: 'Actions',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(String(record.id))}
          >
            Delete
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="admin-users-page">
      <div className="page-header">
        <div>
          <h2>User Management</h2>
          <p>Manage system users and permissions</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          Add User
        </Button>
      </div>

      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Users"
              value={users.length}
              prefix={<UserOutlined />}
              valueStyle={{ color: '#2563eb' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Admins"
              value={users.filter((u) => u.role === 'admin').length}
              prefix={<SafetyOutlined />}
              valueStyle={{ color: '#ef4444' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="New This Month"
              value={2}
              valueStyle={{ color: '#10b981' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Active Users"
              value={users.filter((u) => u.role === 'user').length}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#f59e0b' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <div className="table-toolbar">
          <Space>
            <Search
              placeholder="Search by username or email"
              allowClear
              style={{ width: 300 }}
              onChange={(e) => setSearchValue(e.target.value)}
              prefix={<SearchOutlined />}
            />
            <Select
              style={{ width: 120 }}
              value={roleFilter}
              onChange={setRoleFilter}
            >
              <Option value="all">All Roles</Option>
              <Option value="admin">Admin</Option>
              <Option value="user">Regular User</Option>
            </Select>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={filteredUsers}
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
        title={editingUser ? 'Edit User' : 'Add User'}
        open={isModalVisible}
        onOk={handleModalOk}
        onCancel={() => {
          setIsModalVisible(false);
          form.resetFields();
        }}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="Username"
            name="username"
            rules={[{ required: true, message: 'Please enter a username' }]}
          >
            <Input placeholder="Please enter a username" />
          </Form.Item>

          <Form.Item
            label="Email"
            name="email"
            rules={[
              { required: true, message: 'Please enter an email' },
              { type: 'email', message: 'Please enter a valid email address' },
            ]}
          >
            <Input placeholder="Please enter an email" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              label="Password"
              name="password"
              rules={[
                { required: true, message: 'Please enter a password' },
                { min: 6, message: 'Password must be at least 6 characters' },
              ]}
            >
              <Input.Password placeholder="Please enter a password" />
            </Form.Item>
          )}

          <Form.Item
            label="Department"
            name="department"
          >
            <Input placeholder="Please enter the department" />
          </Form.Item>

          <Form.Item
            label="Position"
            name="position"
          >
            <Input placeholder="Please enter the position" />
          </Form.Item>

          <Form.Item
            label="Role"
            name="role"
            rules={[{ required: true, message: 'Please select a role' }]}
          >
            <Select>
              <Option value="user">Regular User</Option>
              <Option value="admin">Admin</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default UsersPage;
