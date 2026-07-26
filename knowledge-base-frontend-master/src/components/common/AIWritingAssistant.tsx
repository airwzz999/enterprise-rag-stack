import React, { useState } from 'react';
import {
  Card,
  Button,
  Space,
  List,
  Typography,
  Input,
  Divider,
  Tag,
} from 'antd';
import { App } from 'antd';
import {
  RobotOutlined,
  ThunderboltOutlined,
  BulbOutlined,
  CheckOutlined,
} from '@ant-design/icons';

const { Text, Paragraph } = Typography;
const { TextArea } = Input;

interface AIWritingAssistantProps {
  content: string;
  onSelectSuggestion: (suggestion: string) => void;
  onGenerateContent: (content: string) => void;
}

export const AIWritingAssistant: React.FC<AIWritingAssistantProps> = ({
  content,
  onSelectSuggestion,
  onGenerateContent,
}) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [prompt, setPrompt] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [activeTab, setActiveTab] = useState<'suggest' | 'generate'>('suggest');

  const handleGetSuggestions = async () => {
    if (!content.trim()) {
      message.warning('Please enter some content first');
      return;
    }

    setLoading(true);
    try {
      // This should call the AI API to get suggestions
      // const result = await aiService.getWritingSuggestions(content);
      // Mock response
      setTimeout(() => {
        setSuggestions([
          'Add more examples to illustrate this concept',
          'Consider adding an overview paragraph at the beginning',
          'Consider using a chart to present the data',
          'Consider adding related links and references',
        ]);
        setLoading(false);
      }, 1000);
    } catch (error) {
      setLoading(false);
    }
  };

  const handleGenerateContent = async () => {
    if (!prompt.trim()) {
      message.warning('Please enter a generation prompt');
      return;
    }

    setLoading(true);
    try {
      // Call the AI to generate content
      // const result = await aiService.generateContent(prompt);
      message.success('Content generated successfully');
      onGenerateContent(`AI-generated content: ${prompt}`);
      setPrompt('');
      setLoading(false);
    } catch (error) {
      setLoading(false);
    }
  };

  const quickPrompts = [
    'Continue expanding this topic',
    'Add a code example',
    'Summarize the current content',
    'Improve the wording',
    'Add a note of caution',
  ];

  return (
    <Card
      title={
        <Space>
          <RobotOutlined style={{ color: '#1890ff' }} />
          <span>AI Writing Assistant</span>
        </Space>
      }
      size="small"
      style={{ height: '100%' }}
      styles={{ body: { padding: 16 } }}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <div>
          <Button
            type={activeTab === 'suggest' ? 'primary' : 'default'}
            icon={<BulbOutlined />}
            onClick={() => setActiveTab('suggest')}
            style={{ marginRight: 8 }}
          >
            Smart Suggestions
          </Button>
          <Button
            type={activeTab === 'generate' ? 'primary' : 'default'}
            icon={<ThunderboltOutlined />}
            onClick={() => setActiveTab('generate')}
          >
            Content Generation
          </Button>
        </div>

        <Divider style={{ margin: '8px 0' }} />

        {activeTab === 'suggest' && (
          <div>
            <Button
              type="primary"
              icon={<BulbOutlined />}
              onClick={handleGetSuggestions}
              loading={loading}
              block
            >
              Get Writing Suggestions
            </Button>

            {suggestions.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <Text strong>Suggestions:</Text>
                <List
                  size="small"
                  dataSource={suggestions}
                  renderItem={(item, index) => (
                    <List.Item
                      style={{ cursor: 'pointer', padding: '8px 0' }}
                      onClick={() => onSelectSuggestion(item)}
                    >
                      <Space>
                        <Tag color="blue">{index + 1}</Tag>
                        <Text>{item}</Text>
                      </Space>
                    </List.Item>
                  )}
                />
              </div>
            )}
          </div>
        )}

        {activeTab === 'generate' && (
          <div>
            <Paragraph style={{ fontSize: 12, color: '#666', marginBottom: 12 }}>
              Enter a prompt and the AI will generate content for you
            </Paragraph>

            <div style={{ marginBottom: 12 }}>
              <Text strong style={{ fontSize: 12 }}>
                Quick Prompts:
              </Text>
              <div style={{ marginTop: 8 }}>
                <Space wrap>
                  {quickPrompts.map((qp) => (
                    <Tag
                      key={qp}
                      style={{ cursor: 'pointer' }}
                      onClick={() => setPrompt(qp)}
                    >
                      {qp}
                    </Tag>
                  ))}
                </Space>
              </div>
            </div>

            <TextArea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="Enter a prompt describing the content you want to generate..."
              autoSize={{ minRows: 3, maxRows: 6 }}
              style={{ marginBottom: 8 }}
            />

            <Button
              type="primary"
              icon={<ThunderboltOutlined />}
              onClick={handleGenerateContent}
              loading={loading}
              block
            >
              Generate Content
            </Button>
          </div>
        )}

        <Divider style={{ margin: '8px 0' }} />

        <div>
          <Text strong style={{ fontSize: 12 }}>
            Writing Tips:
          </Text>
          <List
            size="small"
            dataSource={[
              'Use a clear heading structure',
              'Add code examples and charts',
              'Keep paragraphs concise',
              'Use lists to organize information',
            ]}
            renderItem={(item) => (
              <List.Item style={{ padding: '4px 0', fontSize: 12 }}>
                <CheckOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                <Text type="secondary">{item}</Text>
              </List.Item>
            )}
          />
        </div>
      </Space>
    </Card>
  );
};
