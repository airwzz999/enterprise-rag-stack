import React, { useState, useEffect } from 'react';
import {
  Tree,
  Button,
  Modal,
  Form,
  Input,
  InputNumber,
  Card,
  Space,
  Popconfirm,
  Row,
  Col,
  Typography,
  Tag,
  Tooltip,
  Divider,
  Select,
} from 'antd';
import { App } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  SearchOutlined,
  FolderAddOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import type { DataNode, TreeProps } from 'antd/es/tree';
import { CategoryTree } from '@/types';
import { categoryService } from '@/services';
import CategoryIcon from '@/components/common/CategoryIcon';
import { useAuthStore } from '@/stores';
import { PERMISSIONS, hasPermission } from '@/utils/permission';
import './AdminPages.css';

const { Title, Text } = Typography;
const { Search } = Input;

interface ExtendedDataNode extends DataNode {
  data?: CategoryTree;
  children?: ExtendedDataNode[];
}

export const CategoriesPage: React.FC = () => {
  const { message } = App.useApp();
  const user = useAuthStore((state) => state.user);
  const canManageCategories = hasPermission(user, PERMISSIONS.documentCategory);
  const [categories, setCategories] = useState<CategoryTree[]>([]);
  const [treeData, setTreeData] = useState<ExtendedDataNode[]>([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingCategory, setEditingCategory] = useState<CategoryTree | null>(null);
  const [parentCategory, setParentCategory] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    buildTreeData();
  }, [categories]);

  const fetchCategories = async () => {
    try {
      const data = await categoryService.getCategoryTree();
      setCategories(data);
    } catch (error) {
      message.error('Failed to fetch category list');
    }
  };

  const buildTreeData = () => {
    const buildNode = (parentId: string = '0'): ExtendedDataNode[] => {
      return categories
        .filter((cat) => cat.parentId === parentId)
        .sort((a, b) => (a.sortOrder ?? a.sort ?? 0) - (b.sortOrder ?? b.sort ?? 0))
        .map((cat) => ({
          key: cat.id,
          title: cat.name,
          children: buildNode(cat.id),
          data: cat,
        }));
    };

    setTreeData(buildNode());
  };

  const handleAdd = (parentId: string = '0') => {
    setEditingCategory(null);
    setParentCategory(parentId);
    form.resetFields();
    setIsModalVisible(true);
  };

  const handleEdit = (category: CategoryTree) => {
    setEditingCategory(category);
    setParentCategory(category.parentId || '0');
    form.setFieldsValue(category);
    setIsModalVisible(true);
  };

  const handleDelete = async (categoryId: string) => {
    try {
      // Check whether the category has child categories
      const hasChildren = categories.some((cat) => cat.parentId === categoryId);
      if (hasChildren) {
        message.warning('This category has subcategories and cannot be deleted');
        return;
      }

      // Call the delete API
      setCategories(categories.filter((cat) => cat.id !== categoryId));
      message.success('Deleted successfully');
    } catch (error) {
      message.error('Delete failed');
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();

      if (editingCategory) {
        // Update category
        setCategories(
          categories.map((cat) =>
            cat.id === editingCategory.id ? { ...cat, ...values } : cat
          )
        );
        message.success('Updated successfully');
      } else {
        // Create category
        const newCategory: CategoryTree = {
          id: Date.now().toString(),
          ...values,
          parentId: parentCategory,
          sort: categories.filter((cat) => cat.parentId === parentCategory).length + 1,
          documentCount: 0,
        };
        setCategories([...categories, newCategory]);
        message.success('Created successfully');
      }

      setIsModalVisible(false);
      form.resetFields();
    } catch (error) {
      message.error('Operation failed');
    }
  };

  const handleDrop: TreeProps['onDrop'] = (info) => {
    const dropKey = info.node.key as string;
    const dragKey = info.dragNode.key as string;
    const dropPos = info.node.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);

    // Handle drag-and-drop reordering logic
    const dragIndex = categories.findIndex((cat) => cat.id === dragKey);
    const dragItem = categories[dragIndex];

    const newCategories = [...categories];
    newCategories.splice(dragIndex, 1);

    if (dropPosition === 0) {
      // Drop inside the target node as a child
      dragItem.parentId = dropKey;
    } else {
      // Drop as a sibling of the target node
      dragItem.parentId = '0';
    }

    newCategories.splice(dragIndex, 0, dragItem);
    setCategories(newCategories);
  };

  const getTreeIcon = ({ expanded }: { expanded?: boolean }) => {
    return expanded ? <FolderOpenOutlined /> : <FolderOutlined />;
  };

  return (
    <div style={{ padding: '8px 8px', background: '#f8fafc', minHeight: '100vh' }}>
      {/* Page header */}
      <div style={{ marginBottom: 32, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={2} style={{ margin: 0, marginBottom: 8, fontSize: 28, fontWeight: 700 }}>
            Category Management
          </Title>
          <Text type="secondary">Manage document category structure and hierarchy</Text>
        </div>
        {canManageCategories && (
          <Space>
            <Button type="primary" icon={<FolderAddOutlined />} onClick={() => handleAdd()}>
              New Category
            </Button>
          </Space>
        )}
      </div>

      <Row gutter={24}>
        {/* Category tree (left) */}
        <Col span={10}>
          <Card
            title="Category Tree"
            extra={
              <Space>
                <Text type="secondary">{categories.length} categories total</Text>
              </Space>
            }
            style={{ borderRadius: 12, height: 'fit-content' }}
            styles={{ body: { padding: 16 } }}
          >
            <Search
              placeholder="Search categories"
              prefix={<SearchOutlined />}
              style={{ marginBottom: 16 }}
            />
            <div style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                <InfoCircleOutlined style={{ marginRight: 4 }} />
                Drag categories to reorder and adjust hierarchy
              </Text>
            </div>
            <Tree
              className="category-tree"
              treeData={treeData}
              icon={getTreeIcon}
              showLine
              draggable
              blockNode
              onDrop={handleDrop}
              selectedKeys={selectedCategory ? [selectedCategory] : []}
              onSelect={(keys) => setSelectedCategory(keys[0] as string)}
              expandedKeys={expandedKeys}
              onExpand={(keys) => setExpandedKeys(keys as string[])}
              titleRender={(nodeData: ExtendedDataNode) => {
                const category = nodeData.data as CategoryTree;
                return (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '4px 0',
                      width: '100%',
                    }}
                  >
                    <CategoryIcon icon={category.icon} variant="sidebar" />
                    <span style={{ flex: 1 }}>{category.name}</span>
                    <Tag color="blue" style={{ margin: 0 }}>
                      {category.documentCount ?? 0}
                    </Tag>
                    {canManageCategories && (
                      <Space className="category-actions" size="small">
                        <Tooltip title="Add subcategory">
                          <Button
                            type="text"
                            size="small"
                            icon={<PlusOutlined />}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleAdd(category.id);
                            }}
                          />
                        </Tooltip>
                        <Tooltip title="Edit">
                          <Button
                            type="text"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleEdit(category);
                            }}
                          />
                        </Tooltip>
                        <Popconfirm
                          title="Are you sure you want to delete this category?"
                          onConfirm={(e) => {
                            e?.stopPropagation();
                            handleDelete(category.id);
                          }}
                          onCancel={(e) => e?.stopPropagation()}
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
                    )}
                  </div>
                );
              }}
            />
          </Card>
        </Col>

        {/* Category details (right) */}
        <Col span={14}>
          <Card
            title={
              <Space>
                <FolderOutlined style={{ color: '#2563eb' }} />
                <Text strong>Category Details</Text>
              </Space>
            }
            style={{ borderRadius: 12 }}
          >
            {!selectedCategory ? (
              <div style={{ padding: '24px 0', textAlign: 'center' }}>
                <FolderOutlined style={{ fontSize: 64, color: '#e2e8f0', marginBottom: 16 }} />
                <Title level={4} type="secondary">
                  Select a category on the left to view details
                </Title>
                <Text type="secondary">Click an item in the category tree to view and edit its details</Text>
              </div>
            ) : (
              <div>
                {(() => {
                  const category = categories.find(c => c.id === selectedCategory);
                  if (!category) return null;
                  return (
                    <div>
                      <Row gutter={[16, 16]}>
                        <Col span={24}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
                            <CategoryIcon icon={category.icon || undefined} variant="avatar" size={56} />
                            <div>
                              <Title level={4} style={{ margin: 0 }}>{category.name}</Title>
                              <Text type="secondary">ID: {category.id}</Text>
                            </div>
                          </div>
                        </Col>
                        <Col span={12}>
                          <Form.Item label="Category Name" style={{ marginBottom: 12 }}>
                            <Input value={category.name} disabled />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item label="Sort Order" style={{ marginBottom: 12 }}>
                            <InputNumber value={category.sortOrder ?? category.sort ?? 1} disabled style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={24}>
                          <Form.Item label="Category Description" style={{ marginBottom: 12 }}>
                            <Input.TextArea value={category.description || 'No description yet'} rows={3} disabled />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item label="Document Count" style={{ marginBottom: 12 }}>
                            <InputNumber value={category.documentCount ?? 0} disabled style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item label="Level" style={{ marginBottom: 12 }}>
                            <InputNumber value={category.level ?? 1} disabled style={{ width: '100%' }} />
                          </Form.Item>
                        </Col>
                      </Row>
                      <Divider />
                      {canManageCategories && (
                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                          <Button
                            type="primary"
                            onClick={() => handleEdit(category)}
                          >
                            Edit Category
                          </Button>
                        </div>
                      )}
                    </div>
                  );
                })()}
              </div>
            )}

            <Divider />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Space>
                <Text type="secondary">
                  <InfoCircleOutlined style={{ marginRight: 8 }} />
                  Category changes affect document classification
                </Text>
              </Space>
            </div>
          </Card>
        </Col>
      </Row>

      <Modal
        title={editingCategory ? 'Edit Category' : 'New Category'}
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
            label="Category Name"
            name="name"
            rules={[{ required: true, message: 'Please enter the category name' }]}
          >
            <Input placeholder="Please enter the category name" />
          </Form.Item>

          <Form.Item
            label="Category Description"
            name="description"
          >
            <Input.TextArea
              placeholder="Please enter the category description"
              rows={4}
            />
          </Form.Item>

          <Form.Item
            label="Icon"
            name="icon"
            initialValue="tech"
          >
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              {([
                { key: 'tech', label: 'Tech' },
                { key: 'product', label: 'Product' },
                { key: 'business', label: 'Business' },
                { key: 'hr', label: 'HR' },
                { key: 'finance', label: 'Finance' },
                { key: 'marketing', label: 'Marketing' },
                { key: 'legal', label: 'Legal' },
                { key: 'training', label: 'Training' },
                { key: 'backend', label: 'Backend' },
                { key: 'frontend', label: 'Frontend' },
                { key: 'database', label: 'Database' },
                { key: 'devops', label: 'DevOps' },
                { key: 'architecture', label: 'Architecture' },
                { key: 'requirement', label: 'Requirements' },
                { key: 'design', label: 'UI Design' },
                { key: 'planning', label: 'Planning' },
                { key: 'competitive', label: 'Competitive' },
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
                      gap: 2,
                      padding: '6px 10px',
                      borderRadius: 10,
                      cursor: 'pointer',
                      border: `2px solid ${isSelected ? '#3b82f6' : 'var(--border-color)'}`,
                      background: isSelected ? 'rgba(59, 130, 246, 0.08)' : 'transparent',
                      transition: 'all 0.2s',
                    }}
                    title={label}
                  >
                    <CategoryIcon icon={key} variant="sidebar" />
                    <span style={{ fontSize: 10, color: 'var(--text-secondary)' }}>{label}</span>
                  </div>
                );
              })}
            </div>
          </Form.Item>

          <Form.Item
            label="Parent Category"
            name="parentId"
          >
            <Select
              placeholder="Select a parent category"
              allowClear
              options={categories.map(cat => ({
                label: cat.name,
                value: cat.id,
              }))}
            />
          </Form.Item>

          <Form.Item
            label="Sort Order"
            name="sort"
            initialValue={1}
          >
            <InputNumber min={1} max={999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <style>{`
        .category-tree .ant-tree-node-content-wrapper {
          border-radius: 8px;
          padding: 4px 8px;
          margin: 2px 0;
          transition: all 0.2s;
        }
        .category-tree .ant-tree-node-content-wrapper:hover {
          background-color: #f1f5f9;
        }
        .category-tree .ant-tree-node-content-wrapper.ant-tree-node-selected {
          background-color: rgba(37, 99, 235, 0.1);
        }
        .category-actions {
          opacity: 0;
          transition: opacity 0.2s;
        }
        .ant-tree-node-content-wrapper:hover .category-actions {
          opacity: 1;
        }
      `}</style>
    </div>
  );
};

export default CategoriesPage;
