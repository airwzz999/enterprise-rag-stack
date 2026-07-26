import React, { useState, useEffect } from 'react';
import {
  Card,
  List,
  Button,
  Space,
  Input,
  Select,
  Modal,
  Form,
  Tag,
  Popconfirm,
  Row,
  Col,
  Typography,
  Empty,
} from 'antd';
import { App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  BookOutlined,
  FormatPainterOutlined,
  DragOutlined,
} from '@ant-design/icons';
import { foundationService } from '@/services';
import type { Dict, DictData } from '@/services/foundation.service';
import type { EntityId } from '@/types';
import dayjs from 'dayjs';

const { Search } = Input;
const { Option } = Select;
const { Text } = Typography;

export const DictionaryManagePage: React.FC = () => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [dicts, setDicts] = useState<Dict[]>([]);
  const [selectedDict, setSelectedDict] = useState<Dict | null>(null);
  const [dictDataList, setDictDataList] = useState<DictData[]>([]);
  const [dictSearchValue, setDictSearchValue] = useState('');
  const [dataSearchValue, setDataSearchValue] = useState('');
  const [isDictModalVisible, setIsDictModalVisible] = useState(false);
  const [isDataModalVisible, setIsDataModalVisible] = useState(false);
  const [editingDict, setEditingDict] = useState<Dict | null>(null);
  const [editingData, setEditingData] = useState<DictData | null>(null);
  const [dictForm] = Form.useForm();
  const [dataForm] = Form.useForm();

  useEffect(() => {
    fetchDicts();
  }, []);

  const fetchDicts = async () => {
    setLoading(true);
    try {
      const response = await foundationService.dict.list({
        current: 1,
        size: 100,
      });
      setDicts(response.list);
    } catch (error) {
      message.error('Failed to fetch dictionary types');
    } finally {
      setLoading(false);
    }
  };

  const fetchDictData = async (dict: Dict) => {
    try {
      const data = await foundationService.dict.getData(dict.dictCode);
      setDictDataList(data);
      setSelectedDict(dict);
    } catch (error) {
      message.error('Failed to fetch dictionary data');
    }
  };

  const handleAddDict = () => {
    setEditingDict(null);
    dictForm.resetFields();
    dictForm.setFieldsValue({
      status: 1,
      sort: 0,
    });
    setIsDictModalVisible(true);
  };

  const handleEditDict = (dict: Dict) => {
    setEditingDict(dict);
    dictForm.setFieldsValue({
      dictCode: dict.dictCode,
      dictName: dict.dictName,
      dictType: dict.dictType,
      description: dict.description,
      sort: dict.sort,
      status: dict.status,
    });
    setIsDictModalVisible(true);
  };

  const handleDeleteDict = async (dictCode: string) => {
    try {
      await foundationService.dict.delete(dictCode);
      message.success('Deleted successfully');
      if (selectedDict?.dictCode === dictCode) {
        setSelectedDict(null);
        setDictDataList([]);
      }
      fetchDicts();
    } catch (error) {
      message.error('Delete failed');
    }
  };

  const handleDictModalOk = async () => {
    try {
      const values = await dictForm.validateFields();

      if (editingDict) {
        await foundationService.dict.update(editingDict.dictCode, values);
        message.success('Updated successfully');
      } else {
        await foundationService.dict.create(values);
        message.success('Created successfully');
      }

      setIsDictModalVisible(false);
      dictForm.resetFields();
      fetchDicts();
    } catch (error) {
      message.error('Operation failed');
    }
  };

  const handleAddData = () => {
    if (!selectedDict) {
      message.warning('Please select a dictionary type first');
      return;
    }
    setEditingData(null);
    dataForm.resetFields();
    dataForm.setFieldsValue({
      dictSort: dictDataList.length > 0 ? Math.max(...dictDataList.map(d => d.dictSort)) + 1 : 0,
      status: 1,
      isDefault: 0,
    });
    setIsDataModalVisible(true);
  };

  const handleEditData = (data: DictData) => {
    setEditingData(data);
    dataForm.setFieldsValue({
      dictLabel: data.dictLabel,
      dictValue: data.dictValue,
      dictSort: data.dictSort,
      cssClass: data.cssClass,
      listClass: data.listClass,
      isDefault: data.isDefault,
      status: data.status,
    });
    setIsDataModalVisible(true);
  };

  const handleDeleteData = async (id: EntityId) => {
    if (!selectedDict) return;

    try {
      await foundationService.dict.deleteData(selectedDict.dictCode, id);
      message.success('Deleted successfully');
      fetchDictData(selectedDict);
    } catch (error) {
      message.error('Delete failed');
    }
  };

  const handleDataModalOk = async () => {
    if (!selectedDict) return;

    try {
      const values = await dataForm.validateFields();

      if (editingData) {
        await foundationService.dict.updateData(selectedDict.dictCode, {
          ...values,
          id: editingData.id,
        });
        message.success('Updated successfully');
      } else {
        await foundationService.dict.addData(selectedDict.dictCode, values);
        message.success('Added successfully');
      }

      setIsDataModalVisible(false);
      dataForm.resetFields();
      fetchDictData(selectedDict);
    } catch (error) {
      message.error('Operation failed');
    }
  };

  const filteredDicts = dicts.filter(
    (dict) =>
      dict.dictName.toLowerCase().includes(dictSearchValue.toLowerCase()) ||
      dict.dictCode.toLowerCase().includes(dictSearchValue.toLowerCase()) ||
      dict.dictType.toLowerCase().includes(dictSearchValue.toLowerCase())
  );

  const filteredData = dictDataList.filter(
    (data) =>
      data.dictLabel.toLowerCase().includes(dataSearchValue.toLowerCase()) ||
      data.dictValue.toLowerCase().includes(dataSearchValue.toLowerCase())
  );

  const getListClassColor = (listClass?: string) => {
    if (!listClass) return 'default';
    const colorMap: Record<string, string> = {
      default: 'default',
      primary: 'blue',
      success: 'green',
      info: 'cyan',
      warning: 'orange',
      danger: 'red',
    };
    return colorMap[listClass] || 'default';
  };

  return (
    <div>
      <Card
        title={
          <Space>
            <BookOutlined />
            <span>Dictionary Management</span>
          </Space>
        }
        style={{ borderRadius: 12 }}
      >
        <Row gutter={16}>
          {/* Left: dictionary type list */}
          <Col xs={24} lg={8}>
            <Card
              size="small"
              title="Dictionary Types"
              extra={
                <Space>
                  <Button
                    size="small"
                    icon={<ReloadOutlined />}
                    onClick={fetchDicts}
                  >
                    Refresh
                  </Button>
                  <Button
                    size="small"
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={handleAddDict}
                  >
                    Add
                  </Button>
                </Space>
              }
            >
              <Search
                placeholder="Search dictionary types"
                allowClear
                style={{ marginBottom: 16 }}
                onChange={(e) => setDictSearchValue(e.target.value)}
                prefix={<SearchOutlined />}
              />

              <List
                loading={loading}
                dataSource={filteredDicts}
                renderItem={(dict) => (
                  <List.Item
                    key={dict.id}
                    style={{
                      padding: '12px',
                      borderRadius: 8,
                      cursor: 'pointer',
                      background:
                        selectedDict?.id === dict.id ? '#e6f7ff' : 'transparent',
                      border:
                        selectedDict?.id === dict.id
                          ? '1px solid #1890ff'
                          : '1px solid transparent',
                      marginBottom: 8,
                    }}
                    onClick={() => fetchDictData(dict)}
                  >
                    <List.Item.Meta
                      avatar={<FormatPainterOutlined style={{ fontSize: 20 }} />}
                      title={
                        <Space>
                          <Text strong>{dict.dictName}</Text>
                          <Tag color={dict.status === 1 ? 'green' : 'red'}>
                            {dict.status === 1 ? 'Enabled' : 'Disabled'}
                          </Tag>
                        </Space>
                      }
                      description={
                        <div>
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {dict.dictCode}
                          </Text>
                          <br />
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {dict.dictType} · Sort: {dict.sort}
                          </Text>
                        </div>
                      }
                    />
                    <Space>
                      <Button
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleEditDict(dict);
                        }}
                      />
                      <Popconfirm
                        title="Confirm Deletion"
                        description="Are you sure you want to delete this dictionary type?"
                        onConfirm={(e) => {
                          e?.stopPropagation();
                          handleDeleteDict(dict.dictCode);
                        }}
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
                    </Space>
                  </List.Item>
                )}
              />
            </Card>
          </Col>

          {/* Right: dictionary data list */}
          <Col xs={24} lg={16}>
            <Card
              size="small"
              title={
                <Space>
                  <span>Dictionary Data</span>
                  {selectedDict && (
                    <Tag color="blue">{selectedDict.dictName}</Tag>
                  )}
                </Space>
              }
              extra={
                <Space>
                  <Button
                    size="small"
                    icon={<ReloadOutlined />}
                    disabled={!selectedDict}
                    onClick={() => selectedDict && fetchDictData(selectedDict)}
                  >
                    Refresh
                  </Button>
                  <Button
                    size="small"
                    type="primary"
                    icon={<PlusOutlined />}
                    disabled={!selectedDict}
                    onClick={handleAddData}
                  >
                    Add
                  </Button>
                </Space>
              }
            >
              {!selectedDict ? (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Please select a dictionary type on the left"
                  style={{ padding: '60px 0' }}
                />
              ) : (
                <>
                  <Search
                    placeholder="Search dictionary data"
                    allowClear
                    style={{ marginBottom: 16 }}
                    onChange={(e) => setDataSearchValue(e.target.value)}
                    prefix={<SearchOutlined />}
                  />

                  <List
                    loading={loading}
                    dataSource={filteredData}
                    renderItem={(data) => (
                      <List.Item
                        key={data.id}
                        style={{
                          padding: '12px',
                          borderRadius: 8,
                          background: '#fafafa',
                          marginBottom: 8,
                          border: '1px solid #f0f0f0',
                        }}
                      >
                        <List.Item.Meta
                          avatar={
                            <Space direction="vertical" style={{ textAlign: 'center' }}>
                              <DragOutlined style={{ color: '#999', cursor: 'move' }} />
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {data.dictSort}
                              </Text>
                            </Space>
                          }
                          title={
                            <Space>
                              <Text strong>{data.dictLabel}</Text>
                              <Tag color={getListClassColor(data.listClass)}>
                                {data.dictValue}
                              </Tag>
                              {data.isDefault === 1 && (
                                <Tag color="orange">Default</Tag>
                              )}
                              <Tag color={data.status === 1 ? 'green' : 'red'}>
                                {data.status === 1 ? 'Enabled' : 'Disabled'}
                              </Tag>
                            </Space>
                          }
                          description={
                            <Space>
                              {data.cssClass && (
                                <Text code style={{ fontSize: 12 }}>
                                  CSS: {data.cssClass}
                                </Text>
                              )}
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                Created {dayjs(data.createdAt).format('YYYY-MM-DD')}
                              </Text>
                            </Space>
                          }
                        />
                        <Space>
                          <Button
                            type="link"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => handleEditData(data)}
                          >
                            Edit
                          </Button>
                          <Popconfirm
                            title="Confirm Deletion"
                            description="Are you sure you want to delete this dictionary data?"
                            onConfirm={() => handleDeleteData(data.id)}
                            okText="OK"
                            cancelText="Cancel"
                          >
                            <Button
                              type="link"
                              size="small"
                              danger
                              icon={<DeleteOutlined />}
                            >
                              Delete
                            </Button>
                          </Popconfirm>
                        </Space>
                      </List.Item>
                    )}
                  />
                </>
              )}
            </Card>
          </Col>
        </Row>
      </Card>

      {/* Add/Edit Dictionary Type modal */}
      <Modal
        title={editingDict ? 'Edit Dictionary Type' : 'Add Dictionary Type'}
        open={isDictModalVisible}
        onOk={handleDictModalOk}
        onCancel={() => {
          setIsDictModalVisible(false);
          dictForm.resetFields();
        }}
        width={600}
        destroyOnClose
      >
        <Form form={dictForm} layout="vertical" preserve={false}>
          <Form.Item
            label="Dictionary Name"
            name="dictName"
            rules={[{ required: true, message: 'Please enter the dictionary name' }]}
          >
            <Input placeholder="e.g. User Status" />
          </Form.Item>

          <Form.Item
            label="Dictionary Code"
            name="dictCode"
            rules={[{ required: true, message: 'Please enter the dictionary code' }]}
          >
            <Input placeholder="e.g. user_status" disabled={!!editingDict} />
          </Form.Item>

          <Form.Item
            label="Dictionary Type"
            name="dictType"
            rules={[{ required: true, message: 'Please enter the dictionary type' }]}
          >
            <Input placeholder="e.g. sys_user_status" />
          </Form.Item>

          <Form.Item
            label="Description"
            name="description"
          >
            <Input.TextArea rows={3} placeholder="Please enter the dictionary description" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="Sort Order"
                name="sort"
                rules={[{ required: true, message: 'Please enter the sort order' }]}
              >
                <Input type="number" placeholder="0" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Status"
                name="status"
                rules={[{ required: true, message: 'Please select a status' }]}
              >
                <Select>
                  <Option value={1}>Enabled</Option>
                  <Option value={0}>Disabled</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* Add/Edit Dictionary Data modal */}
      <Modal
        title={editingData ? 'Edit Dictionary Data' : 'Add Dictionary Data'}
        open={isDataModalVisible}
        onOk={handleDataModalOk}
        onCancel={() => {
          setIsDataModalVisible(false);
          dataForm.resetFields();
        }}
        width={600}
        destroyOnClose
      >
        <Form form={dataForm} layout="vertical" preserve={false}>
          <Form.Item
            label="Data Label"
            name="dictLabel"
            rules={[{ required: true, message: 'Please enter the data label' }]}
          >
            <Input placeholder="e.g. Normal" />
          </Form.Item>

          <Form.Item
            label="Data Value"
            name="dictValue"
            rules={[{ required: true, message: 'Please enter the data value' }]}
          >
            <Input placeholder="e.g. 1" />
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="Sort Order"
                name="dictSort"
                rules={[{ required: true, message: 'Please enter the sort order' }]}
              >
                <Input type="number" placeholder="0" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="Default"
                name="isDefault"
                rules={[{ required: true, message: 'Please select whether this is the default' }]}
              >
                <Select>
                  <Option value={0}>No</Option>
                  <Option value={1}>Yes</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="CSS Class"
                name="cssClass"
              >
                <Input placeholder="e.g. text-success" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="List Style"
                name="listClass"
              >
                <Select placeholder="Please select">
                  <Option value="default">Default</Option>
                  <Option value="primary">Primary</Option>
                  <Option value="success">Success</Option>
                  <Option value="info">Info</Option>
                  <Option value="warning">Warning</Option>
                  <Option value="danger">Danger</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            label="Status"
            name="status"
            rules={[{ required: true, message: 'Please select a status' }]}
          >
            <Select>
              <Option value={1}>Enabled</Option>
              <Option value={0}>Disabled</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DictionaryManagePage;
