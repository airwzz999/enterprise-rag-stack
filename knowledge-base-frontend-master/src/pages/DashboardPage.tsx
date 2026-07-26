import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import dashboardService from '@/services/dashboard.service';
import statisticsService from '@/services/statistics.service';
import type { EntityId } from '@/types';
import { useAppStore, useAuthStore } from '@/stores';
import { PERMISSIONS, hasPermission } from '@/utils/permission';

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const systemName = useAppStore((s) => s.systemName);
  const enableAI = useAppStore((s) => s.enableAI);
  const canCreateDocument = hasPermission(user, PERMISSIONS.documentCreate);
  const [isListView, setIsListView] = useState(false);
  const [stats, setStats] = useState({
    totalDocuments: 0,
    aiSearchCount: 0,
    aiQaCount: 0,
    activeUserCount: 0,
  });
  const [loading, setLoading] = useState(true);
  const [latestDocuments, setLatestDocuments] = useState<any[]>([]);
  const [latestDocsLoading, setLatestDocsLoading] = useState(true);
  const [hotDocuments, setHotDocuments] = useState<any[]>([]);
  const [hotDocsLoading, setHotDocsLoading] = useState(true);

  // Fetch latest documents
  useEffect(() => {
    statisticsService.getLatestDocuments({ limit: 6 })
      .then((docs) => setLatestDocuments(docs || []))
      .catch(err => console.error('Failed to fetch latest documents:', err))
      .finally(() => setLatestDocsLoading(false));
  }, []);

  useEffect(() => {
    dashboardService.getStats()
      .then((res: any) => {
        // The response interceptor already unwraps data.data, so res is the DashboardVO
        const overview = res?.overview;
        if (overview) {
          setStats({
            totalDocuments: Number(overview.totalDocuments) || 0,
            aiSearchCount: Number(overview.aiSearchCount) || 0,
            aiQaCount: Number(overview.aiQaCount) || 0,
            activeUserCount: Number(overview.activeUserCount) || 0,
          });
        }
      })
      .catch((err: any) => {
        console.error('Failed to fetch dashboard data:', err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  // Fetch popular documents
  useEffect(() => {
    statisticsService.getPopularDocuments({ limit: 6 })
      .then((docs) => {
        setHotDocuments(docs || []);
      })
      .catch((err) => {
        console.error('Failed to fetch popular documents:', err);
      })
      .finally(() => {
        setHotDocsLoading(false);
      });
  }, []);

  const formatNumber = (num: number): string => {
    if (num >= 10000) {
      return (num / 10000).toFixed(1).replace(/\.0$/, '') + 'w';
    }
    return num.toLocaleString('zh-CN');
  };

  const cardGradients = [
    'linear-gradient(135deg, #dbeafe, #bfdbfe)',
    'linear-gradient(135deg, #ede9fe, #ddd6fe)',
    'linear-gradient(135deg, #d1fae5, #a7f3d0)',
    'linear-gradient(135deg, #ffedd5, #fed7aa)',
    'linear-gradient(135deg, #cffafe, #a5f3fc)',
    'linear-gradient(135deg, #fce7f3, #fbcfe8)',
  ];

  const categoryTypeMap: Record<string, string> = {
    'Technical': 'tech',
    'AI': 'ai',
    'Business': 'business',
    'Legal': 'business',
  };

  const techStack = [
    { name: 'Java 21', version: 'LTS Version', icon: 'Java', gradient: 'linear-gradient(135deg, #2563eb, #8b5cf6)' },
    { name: 'Spring Boot 3.2', version: 'Enterprise-grade Framework', icon: 'S', gradient: 'linear-gradient(135deg, #6DB33F, #4CAF50)' },
    { name: 'Redis 7.2', version: 'High-performance Cache', icon: 'R', gradient: 'linear-gradient(135deg, #FF6B6B, #EE5A24)' },
    { name: 'MySQL 8.0', version: 'Relational Database', icon: 'M', gradient: 'linear-gradient(135deg, #4479A1, #274C77)' },
    { name: 'PostgreSQL 16', version: 'Advanced Database', icon: 'PG', gradient: 'linear-gradient(135deg, #336791, #205E75)' },
    { name: 'Elasticsearch 8', version: 'Search Engine', icon: 'ES', gradient: 'linear-gradient(135deg, #F29111, #D66D18)' },
    { name: 'Claude 3.5 Opus', version: 'AI Model', icon: 'AI', gradient: 'linear-gradient(135deg, #CD6799, #A020F0)' },
    { name: 'React 18', version: 'Frontend Framework', icon: 'Re', gradient: 'linear-gradient(135deg, #61DAFB, #21A4C7)' },
  ];

  const handleSuggestionClick = (text: string) => {
    // Navigate to AI assistant with the suggestion text
    navigate('/ai', { state: { query: text } });
  };

  const handleStatCardClick = () => {
    navigate('/documents');
  };

  const handleDocumentClick = (id: EntityId) => {
    window.open(`/documents/${id}`, '_blank');
  };

  const renderDocCard = (doc: any, index: number, badge?: { label: string; className: string }) => {
    const docId = doc.documentId || doc.id;
    const authorName = doc.authorName || 'Unknown';
    const authorInitials = authorName.length > 0 ? authorName.substring(0, 2).toUpperCase() : '?';
    const categoryName = doc.categoryName || 'Document';
    const categoryType = categoryTypeMap[categoryName] || 'tech';
    const excerpt = doc.summary || doc.excerpt || '';
    const publishTime = doc.createdAt || '';
    return (
      <div
        key={docId}
        className={`document-card ${isListView ? 'list-view' : ''}`}
        onClick={() => handleDocumentClick(docId)}
      >
        <div className="doc-preview" style={{ background: cardGradients[index % cardGradients.length] }}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
          {badge && (
            <span className={`doc-trend-badge ${badge.className}`}>{badge.label}</span>
          )}
          <span className={`doc-badge ${categoryType}`}>{categoryName}</span>
        </div>
        <div className="doc-content">
          <div className="doc-content-main">
            <h3 className="doc-title">{doc.title}</h3>
            <p className="doc-excerpt">{excerpt}</p>
          </div>
          <div className="doc-meta">
            <div className="doc-author">
              <div className="doc-author-avatar">{authorInitials}</div>
              <span className="doc-author-name">{authorName}</span>
            </div>
            {publishTime && (
              <span className="doc-publish-time">{publishTime}</span>
            )}
            <div className="doc-stats">
              <span className="doc-stat">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                {formatNumber(doc.viewCount || 0)}
              </span>
              <span className="doc-stat">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="7 10 12 15 17 10"></polyline>
                  <line x1="12" y1="15" x2="12" y2="3"></line>
                </svg>
                {formatNumber(doc.favoriteCount || 0)}
              </span>
            </div>
          </div>
        </div>
      </div>
    );
  };

  const renderDocSection = (title: string, docs: any[], loading: boolean, badge?: { label: string; className: string }) => (
    <>
      <div className="section-header">
        <h2 className="section-title">{title}</h2>
        <div className="section-header-right">
          <span className="section-more" onClick={() => navigate('/documents')}>
            More <span className="section-more-arrow">&rsaquo;</span>
          </span>
          <div className="view-toggle">
            <div
              className={`view-btn ${!isListView ? 'active' : ''}`}
              onClick={() => setIsListView(false)}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
              </svg>
            </div>
            <div
              className={`view-btn ${isListView ? 'active' : ''}`}
              onClick={() => setIsListView(true)}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="8" y1="6" x2="21" y2="6"></line>
                <line x1="8" y1="12" x2="21" y2="12"></line>
                <line x1="8" y1="18" x2="21" y2="18"></line>
                <line x1="3" y1="6" x2="3.01" y2="6"></line>
                <line x1="3" y1="12" x2="3.01" y2="12"></line>
                <line x1="3" y1="18" x2="3.01" y2="18"></line>
              </svg>
            </div>
          </div>
        </div>
      </div>
      <div className={`documents-grid ${isListView ? 'list-view' : ''}`}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--text-secondary)' }}>Loading...</div>
        ) : docs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--text-secondary)' }}>No {title} yet</div>
        ) : (
          docs.map((doc, index) => renderDocCard(doc, index, badge))
        )}
      </div>
    </>
  );

  return (
    <>
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">{systemName}</h1>
          <p className="page-subtitle">Next-generation enterprise knowledge management platform powered by AI</p>
        </div>
        <div className="action-buttons">
          {canCreateDocument && (
            <button className="btn btn-secondary" onClick={() => navigate('/documents/import')}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                <polyline points="7 10 12 15 17 10"></polyline>
                <line x1="12" y1="15" x2="12" y2="3"></line>
              </svg>
              Import Document
            </button>
          )}
          {canCreateDocument && (
            <button className="btn btn-primary" onClick={() => navigate('/documents/new')}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              New Document
            </button>
          )}
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="stats-grid">
        <div className="stat-card" onClick={handleStatCardClick}>
          <div className="stat-header">
            <div className="stat-icon blue">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
            </div>
            <div className="stat-trend up">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              +12.5%
            </div>
          </div>
          <div className="stat-value">{loading ? '-' : formatNumber(stats.totalDocuments)}</div>
          <div className="stat-label">Total Documents</div>
        </div>

        <div className="stat-card" onClick={handleStatCardClick}>
          <div className="stat-header">
            <div className="stat-icon green">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </div>
            <div className="stat-trend up">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              Live
            </div>
          </div>
          <div className="stat-value">{loading ? '-' : formatNumber(stats.aiSearchCount)}</div>
          <div className="stat-label">AI Smart Search</div>
        </div>

        <div className="stat-card" onClick={handleStatCardClick}>
          <div className="stat-header">
            <div className="stat-icon purple">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <div className="stat-trend up">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              Live
            </div>
          </div>
          <div className="stat-value">{loading ? '-' : formatNumber(stats.aiQaCount)}</div>
          <div className="stat-label">AI Q&A Sessions</div>
        </div>

        <div className="stat-card" onClick={handleStatCardClick}>
          <div className="stat-header">
            <div className="stat-icon orange">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </div>
            <div className="stat-trend up">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
                <polyline points="17 6 23 6 23 12"></polyline>
              </svg>
              Last 30 days
            </div>
          </div>
          <div className="stat-value">{loading ? '-' : formatNumber(stats.activeUserCount)}</div>
          <div className="stat-label">Active Users</div>
        </div>
      </div>

      {/* AI Assistant Section */}
      {enableAI && (
      <div className="ai-assistant-section" onClick={() => navigate('/ai')}>
        <div className="ai-header">
          <div className="ai-avatar">🤖</div>
          <div className="ai-info">
            <h3>AI Knowledge Assistant</h3>
            <p>An intelligent knowledge management assistant powered by Claude 3.5 Opus, ready to provide professional knowledge services anytime</p>
          </div>
          <div className="ai-status">
            <span className="dot"></span>
            Online
          </div>
        </div>
        <div className="ai-input-area">
          <div className="ai-input">
            <textarea
              placeholder="Ask the AI assistant, for example:
- How can I optimize the performance of a Spring Boot application?
- Help me summarize best practices for microservices architecture
- Analyze the company's latest technology trend report..."
              readOnly
            />
            <div className="ai-suggestions">
              <span
                className="suggestion-chip"
                onClick={(e) => {
                  e.stopPropagation();
                  handleSuggestionClick('Smart document search');
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                Smart document search
              </span>
              <span
                className="suggestion-chip"
                onClick={(e) => {
                  e.stopPropagation();
                  handleSuggestionClick('Generate a data analysis report');
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="20" x2="18" y2="10"></line>
                  <line x1="12" y1="20" x2="12" y2="4"></line>
                  <line x1="6" y1="20" x2="6" y2="14"></line>
                </svg>
                Generate a data analysis report
              </span>
              <span
                className="suggestion-chip"
                onClick={(e) => {
                  e.stopPropagation();
                  handleSuggestionClick('Assist with document writing');
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
                Assist with document writing
              </span>
              <span
                className="suggestion-chip"
                onClick={(e) => {
                  e.stopPropagation();
                  handleSuggestionClick('Provide innovative suggestions');
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"></path>
                </svg>
                Provide innovative suggestions
              </span>
              <span
                className="suggestion-chip"
                onClick={(e) => {
                  e.stopPropagation();
                  handleSuggestionClick('Summarize key knowledge points');
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <circle cx="12" cy="12" r="6"></circle>
                  <circle cx="12" cy="12" r="2"></circle>
                </svg>
                Summarize key knowledge points
              </span>
            </div>
          </div>
          <button className="btn btn-primary">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="22" y1="2" x2="11" y2="13"></line>
              <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
            </svg>
            Send
          </button>
        </div>
      </div>
      )}

      {/* Latest documents */}
      {renderDocSection('Latest Documents', latestDocuments, latestDocsLoading, { label: 'NEW', className: 'new' })}

      {/* Popular documents */}
      {renderDocSection('Popular Documents', hotDocuments, hotDocsLoading, { label: 'HOT', className: 'hot' })}

      {/* Tech Stack Section */}
      <div className="tech-stack-section">
        <h2 className="section-title" style={{ textAlign: 'center', marginBottom: '8px' }}>
          Core Technology Stack
        </h2>
        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', marginBottom: '32px' }}>
          Built on industry-leading technology architecture, ensuring high performance, high availability, and scalability
        </p>

        <div className="tech-grid">
          {techStack.map((tech, index) => (
            <div key={index} className="tech-item">
              <div
                className="tech-icon"
                style={{
                  background: tech.gradient,
                }}
              >
                {tech.icon}
              </div>
              <div className="tech-name">{tech.name}</div>
              <div className="tech-version">{tech.version}</div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
};

export default DashboardPage;
