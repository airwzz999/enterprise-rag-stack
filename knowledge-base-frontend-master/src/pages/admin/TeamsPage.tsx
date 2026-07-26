import React, { useEffect, useState, useCallback } from 'react';
import {
  Card,
  Button,
  Space,
  Tag,
  Avatar,
  Modal,
  Form,
  Input,
  Select,
  Popconfirm,
  List,
  Typography,
  Row,
  Col,
  Divider,
  Tooltip,
  Pagination,
} from 'antd';
import { App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  TeamOutlined,
  SearchOutlined,
  CrownOutlined,
  UserAddOutlined,
} from '@ant-design/icons';
import { teamService } from '@/services';
import { userService } from '@/services';
import { Team, TeamMember } from '@/types';
import UserAvatar from '@/components/common/UserAvatar';
import TeamIcon from '@/components/common/TeamIcon';
import dayjs from 'dayjs';

const { Text, Title } = Typography;
const { Option } = Select;
const { Search } = Input;

export const TeamsPage: React.FC = () => {
  const { message } = App.useApp();
  const [users, setUsers] = useState<any[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingTeam, setEditingTeam] = useState<Team | null>(null);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [searchText, setSearchText] = useState('');
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  // Pagination state
  const [pagination, setPagination] = useState({ current: 1, size: 10, total: 0 });

  // Add member modal
  const [addMemberVisible, setAddMemberVisible] = useState(false);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);

  useEffect(() => {
    fetchTeams();
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const data = await userService.getUsers({ page: 1, pageSize: 1000 });
      setUsers(data.list || []);
    } catch (error) {
      console.error('Failed to fetch users:', error);
    }
  };

  const fetchTeams = useCallback(async (page = 1, pageSize = 10) => {
    setLoading(true);
    try {
      const result = await teamService.getTeams({ current: page, size: pageSize });
      setTeams(result.records || []);
      setPagination({
        current: result.current,
        size: result.size,
        total: result.total,
      });
    } catch (error) {
      console.error('Failed to fetch teams:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchTeamMembers = async (teamId: string) => {
    try {
      const members = await teamService.getTeamMembers(teamId);
      setTeamMembers(members || []);
    } catch (error) {
      console.error('Failed to fetch team members:', error);
    }
  };

  const handleCreate = () => {
    setEditingTeam(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (team: Team) => {
    setEditingTeam(team);
    form.setFieldsValue({
      teamName: team.teamName,
      teamCode: team.teamCode,
      description: team.description,
      icon: team.icon,
      leaderId: team.leaderId,
      status: team.status ?? 1,
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: string) => {
    try {
      await teamService.deleteTeam(id);
      message.success('Deleted successfully');
      // If the deleted team is the currently selected one, clear the selection
      if (selectedTeam?.id === id) {
        setSelectedTeam(null);
        setTeamMembers([]);
      }
      fetchTeams(pagination.current, pagination.size);
    } catch (error) {
      console.error('Failed to delete team:', error);
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      if (editingTeam) {
        await teamService.updateTeam({
          id: editingTeam.id,
          ...values,
        });
        message.success('Updated successfully');
      } else {
        await teamService.createTeam({
          teamName: values.teamName,
          teamCode: values.teamCode,
          description: values.description,
          icon: values.icon,
          leaderId: values.leaderId,
          parentId: values.parentId,
        });
        message.success('Created successfully');
      }
      setModalVisible(false);
      form.resetFields();
      fetchTeams(pagination.current, pagination.size);
      // Refresh the details of the selected team
      if (editingTeam && selectedTeam?.id === editingTeam.id) {
        const updated = await teamService.getTeam(editingTeam.id);
        setSelectedTeam(updated);
      }
    } catch (error) {
      console.error('Failed to submit team:', error);
    }
  };

  const handleShowMembers = (team: Team) => {
    setSelectedTeam(team);
    fetchTeamMembers(team.id);
  };

  const handleRemoveMember = async (userId: string) => {
    if (!selectedTeam) return;
    try {
      await teamService.removeMembers(selectedTeam.id, [userId]);
      message.success('Removed successfully');
      fetchTeamMembers(selectedTeam.id);
      fetchTeams(pagination.current, pagination.size);
    } catch (error) {
      console.error('Failed to remove member:', error);
    }
  };

  const handleAddMembers = async () => {
    if (!selectedTeam || selectedUserIds.length === 0) return;
    try {
      await teamService.addMembers(selectedTeam.id, selectedUserIds);
      message.success(`Added ${selectedUserIds.length} member(s) successfully`);
      setAddMemberVisible(false);
      setSelectedUserIds([]);
      fetchTeamMembers(selectedTeam.id);
      fetchTeams(pagination.current, pagination.size);
    } catch (error) {
      console.error('Failed to add members:', error);
    }
  };

  // Filter out non-member users
  const nonMemberUsers = users.filter(
    (u) => !teamMembers.some((m) => m.userId === u.id || String(m.userId) === String(u.id))
  );

  // Filter teams by search text
  const filteredTeams = teams.filter((team) =>
    team.teamName?.toLowerCase().includes(searchText.toLowerCase())
  );

  return (
    <div style={{ padding: '32px 24px', background: '#f8fafc', minHeight: '100vh' }}>
      {/* Page header */}
      <div style={{ marginBottom: 32, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0, marginBottom: 8, fontSize: 28, fontWeight: 700 }}>
            Team Space Management
          </Title>
          <Text type="secondary">Manage the organizational structure and team collaboration spaces</Text>
        </div>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            New Team
          </Button>
        </Space>
      </div>

      <Row gutter={24}>
        {/* Team list (left) */}
        <Col span={8}>
          <Card
            title="Team List"
            extra={
              <Space>
                <Text type="secondary">{pagination.total} teams total</Text>
              </Space>
            }
            style={{ borderRadius: 12, height: 'fit-content' }}
            styles={{ body: { padding: 16 } }}
          >
            <Search
              placeholder="Search teams"
              prefix={<SearchOutlined />}
              style={{ marginBottom: 16 }}
              onChange={(e) => setSearchText(e.target.value)}
            />
            <Space direction="vertical" style={{ width: '100%' }} size="middle">
              {filteredTeams.map((team) => (
                <div
                  key={team.id}
                  onClick={() => handleShowMembers(team)}
                  style={{
                    padding: '16px',
                    borderRadius: 12,
                    border: `1px solid ${selectedTeam?.id === team.id ? '#2563eb' : 'transparent'}`,
                    background: selectedTeam?.id === team.id ? 'rgba(37, 99, 235, 0.1)' : 'transparent',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                  }}
                  onMouseEnter={(e) => {
                    if (selectedTeam?.id !== team.id) {
                      e.currentTarget.style.background = '#f1f5f9';
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (selectedTeam?.id !== team.id) {
                      e.currentTarget.style.background = 'transparent';
                    }
                  }}
                >
                  {team.icon ? (
                    <TeamIcon icon={team.icon} variant="avatar" size={40} />
                  ) : (
                    <Avatar
                      size={40}
                      style={{
                        background: `linear-gradient(135deg, #2563eb, #8b5cf6)`,
                        fontWeight: 600,
                      }}
                    >
                      <TeamOutlined />
                    </Avatar>
                  )}
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <Text strong>{team.teamName}</Text>
                      <Tag color={team.status === 1 ? 'success' : 'default'} style={{ fontSize: 11, margin: 0 }}>
                        {team.status === 1 ? 'Active' : 'Disabled'}
                      </Tag>
                    </div>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {team.memberCount || 0} member(s) · {team.description || 'No description yet'}
                    </Text>
                  </div>
                  <Space className="team-actions">
                    <Tooltip title="Edit">
                      <Button
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleEdit(team);
                        }}
                      />
                    </Tooltip>
                    <Tooltip title="Delete">
                      <Popconfirm
                        title="Are you sure you want to delete this team?"
                        description="Deleting it will remove all team members and associated data"
                        onConfirm={() => handleDelete(team.id)}
                        onCancel={(e) => e?.stopPropagation()}
                        okText="OK"
                        cancelText="Cancel"
                      >
                        <Button
                          type="text"
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={(e) => e.stopPropagation()}
                        />
                      </Popconfirm>
                    </Tooltip>
                  </Space>
                </div>
              ))}
            </Space>
            <div style={{ marginTop: 16, textAlign: 'center' }}>
              <Pagination
                current={pagination.current}
                pageSize={pagination.size}
                total={pagination.total}
                size="small"
                onChange={(page, pageSize) => fetchTeams(page, pageSize)}
                showSizeChanger
                showTotal={(total) => `${total} teams total`}
              />
            </div>
          </Card>
        </Col>

        {/* Team details (right) */}
        <Col span={16}>
          <Card
            title={
              <Space>
                {selectedTeam?.icon ? (
                  <TeamIcon icon={selectedTeam.icon} variant="avatar" size={32} />
                ) : (
                  <Avatar
                    size={32}
                    style={{
                      background: `linear-gradient(135deg, #2563eb, #8b5cf6)`,
                    }}
                  >
                    <TeamOutlined />
                  </Avatar>
                )}
                <div>
                  <Text strong style={{ fontSize: 16 }}>
                    {selectedTeam?.teamName || 'Select a team'}
                  </Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {selectedTeam?.description || 'Select a team on the left to view details'}
                  </Text>
                </div>
              </Space>
            }
            extra={
              selectedTeam && (
                <Space>
                  <Button
                    size="small"
                    icon={<EditOutlined />}
                    onClick={() => handleEdit(selectedTeam)}
                  >
                    Edit
                  </Button>
                  <Popconfirm
                    title="Are you sure you want to delete this team?"
                    onConfirm={() => handleDelete(selectedTeam.id)}
                    okText="OK"
                    cancelText="Cancel"
                  >
                    <Button size="small" danger icon={<DeleteOutlined />}>
                      Delete
                    </Button>
                  </Popconfirm>
                </Space>
              )
            }
            style={{ borderRadius: 12 }}
          >
            {selectedTeam ? (
              <div>
                <Divider orientation="left">Team Info</Divider>
                <Row gutter={16}>
                  <Col span={12}>
                    <div style={{ marginBottom: 16 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        Team Leader
                      </Text>
                      <br />
                      <Space>
                        <UserAvatar
                          src={selectedTeam.leader?.avatar}
                          alt={selectedTeam.leaderName || selectedTeam.leader?.username || ''}
                          style={{ width: '24px', height: '24px', borderRadius: '50%', objectFit: 'cover' }}
                        />
                        <Text>{selectedTeam.leaderName || selectedTeam.leader?.username || selectedTeam.leaderId || 'Not set'}</Text>
                      </Space>
                    </div>
                  </Col>
                  <Col span={12}>
                    <div style={{ marginBottom: 16 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        Created At
                      </Text>
                      <br />
                      <Text>{selectedTeam.createdAt ? dayjs(selectedTeam.createdAt).format('YYYY-MM-DD') : '-'}</Text>
                    </div>
                  </Col>
                  {selectedTeam.teamCode && (
                    <Col span={12}>
                      <div style={{ marginBottom: 16 }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>Team Code</Text>
                        <br />
                        <Text code>{selectedTeam.teamCode}</Text>
                      </div>
                    </Col>
                  )}
                  {selectedTeam.docCount !== undefined && (
                    <Col span={12}>
                      <div style={{ marginBottom: 16 }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>Document Count</Text>
                        <br />
                        <Text>{selectedTeam.docCount}</Text>
                      </div>
                    </Col>
                  )}
                </Row>

                <Divider orientation="left">Team Members ({teamMembers.length})</Divider>
                <div style={{ marginBottom: 12, textAlign: 'right' }}>
                  <Button
                    type="primary"
                    size="small"
                    icon={<UserAddOutlined />}
                    onClick={() => {
                      setSelectedUserIds([]);
                      setAddMemberVisible(true);
                    }}
                  >
                    Add Member
                  </Button>
                </div>
                <div style={{ maxHeight: 400, overflowY: 'auto' }}>
                  {teamMembers.length > 0 ? (
                    <List
                      dataSource={teamMembers}
                      renderItem={(member) => (
                        <List.Item
                          style={{
                            padding: '12px 0',
                            borderBottom: '1px solid #f1f5f9',
                          }}
                          actions={[
                            <Popconfirm
                              key="remove"
                              title="Are you sure you want to remove this member?"
                              onConfirm={() => handleRemoveMember(member.userId)}
                              okText="OK"
                              cancelText="Cancel"
                            >
                              <Button type="text" danger size="small">
                                Remove
                              </Button>
                            </Popconfirm>,
                          ]}
                        >
                          <List.Item.Meta
                            avatar={
                              <UserAvatar
                                src={member.avatar}
                                alt={member.username || ''}
                                style={{ width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover' }}
                              />
                            }
                            title={
                              <Space>
                                <Text>{member.username}</Text>
                                {member.realName && (
                                  <Text type="secondary" style={{ fontSize: 12 }}>({member.realName})</Text>
                                )}
                                {member.role === 'leader' && (
                                  <Tag color="gold" icon={<CrownOutlined />} style={{ fontSize: 11 }}>
                                    Leader
                                  </Tag>
                                )}
                              </Space>
                            }
                            description={`Joined: ${member.joinedAt ? dayjs(member.joinedAt).format('YYYY-MM-DD') : '-'}`}
                          />
                        </List.Item>
                      )}
                    />
                  ) : (
                    <div style={{ padding: '24px 0', textAlign: 'center' }}>
                      <Text type="secondary">No members yet. Click the button above to add one</Text>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div style={{ padding: '48px 0', textAlign: 'center' }}>
                <TeamOutlined style={{ fontSize: 64, color: '#e2e8f0', marginBottom: 16 }} />
                <Title level={4} type="secondary">
                  Select a team on the left to view details
                </Title>
                <Text type="secondary">Click an item in the team list to view and edit its details</Text>
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* Create/Edit Team modal */}
      <Modal
        title={editingTeam ? 'Edit Team' : 'New Team'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
        width={600}
        confirmLoading={loading}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item
            label="Team Name"
            name="teamName"
            rules={[{ required: true, message: 'Please enter the team name' }]}
          >
            <Input placeholder="Please enter the team name" />
          </Form.Item>

          {!editingTeam && (
            <Form.Item
              label="Team Code"
              name="teamCode"
              rules={[{ required: true, message: 'Please enter the team code' }]}
              extra="The code cannot be changed after the team is created"
            >
              <Input placeholder="Please enter the team code (unique identifier)" />
            </Form.Item>
          )}

          <Form.Item label="Description" name="description">
            <Input.TextArea rows={3} placeholder="Please enter the team description" />
          </Form.Item>

          <Form.Item label="Icon" name="icon" initialValue="tech">
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              {([
                { key: 'tech', label: 'Tech' },
                { key: 'product', label: 'Product' },
                { key: 'ops', label: 'Operations' },
                { key: 'admin', label: 'Admin' },
                { key: 'backend', label: 'Backend' },
                { key: 'frontend', label: 'Frontend' },
                { key: 'qa', label: 'QA' },
              ] as const).map(({ key, label }) => {
                const currentIcon = form.getFieldValue('icon');
                const isSelected = currentIcon === key;
                return (
                  <div
                    key={key}
                    onClick={() => form.setFieldValue('icon', key)}
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 4,
                      padding: '8px 12px',
                      borderRadius: 10,
                      cursor: 'pointer',
                      border: `2px solid ${isSelected ? '#3b82f6' : 'var(--border-color)'}`,
                      background: isSelected ? 'rgba(59, 130, 246, 0.08)' : 'transparent',
                      transition: 'all 0.2s',
                    }}
                  >
                    <TeamIcon icon={key} variant="avatar" size={36} />
                    <span style={{ fontSize: 11, color: 'var(--text-secondary)' }}>{label}</span>
                  </div>
                );
              })}
            </div>
          </Form.Item>

          <Form.Item
            label="Team Leader"
            name="leaderId"
            rules={[{ required: true, message: 'Please select a team leader' }]}
          >
            <Select placeholder="Please select a team leader" showSearch optionFilterProp="children">
              {users.map((user: any) => (
                <Option key={user.id} value={String(user.id)}>
                  {user.username} {user.realName ? `(${user.realName})` : ''}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="Team Status"
            name="status"
            initialValue={1}
          >
            <Select>
              <Option value={1}>Active</Option>
              <Option value={0}>Disabled</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Add Member modal */}
      <Modal
        title="Add Team Member"
        open={addMemberVisible}
        onCancel={() => {
          setAddMemberVisible(false);
          setSelectedUserIds([]);
        }}
        onOk={handleAddMembers}
        okText="Add"
        cancelText="Cancel"
        okButtonProps={{ disabled: selectedUserIds.length === 0 }}
      >
        <Select
          mode="multiple"
          placeholder="Search and select users to add"
          style={{ width: '100%' }}
          value={selectedUserIds}
          onChange={setSelectedUserIds}
          filterOption={(input, option) => {
            const label = option?.label ?? option?.children;
            return String(label ?? '').toLowerCase().includes(input.toLowerCase());
          }}
        >
          {nonMemberUsers.map((user: any) => (
            <Option key={user.id} value={String(user.id)}>
              {user.username} {user.realName ? `(${user.realName})` : ''}
            </Option>
          ))}
        </Select>
        <div style={{ marginTop: 8 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {nonMemberUsers.length} non-member user(s) available
          </Text>
        </div>
      </Modal>

      <style>{`
        .team-actions {
          opacity: 0;
          transition: opacity 0.2s;
        }
        div:hover > .team-actions {
          opacity: 1;
        }
      `}</style>
    </div>
  );
};

export default TeamsPage;
