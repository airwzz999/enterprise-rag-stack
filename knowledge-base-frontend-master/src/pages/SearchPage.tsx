import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Select, Pagination, Tag, Card, Empty, Spin, Tooltip, Typography } from 'antd';
import {
  SearchOutlined,
  FileTextOutlined,
  UserOutlined,
  FolderOutlined,
  ClockCircleOutlined,
  FireOutlined,
  LoadingOutlined,
  CloseCircleOutlined,
  ThunderboltOutlined,
  DeleteOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { useSearchParams, useNavigate } from 'react-router-dom';
import type { EntityId, SearchResult } from '@/types';
import { searchService } from '@/services';
import { useAppStore } from '@/stores';
import './SearchPage.css';

const { Option } = Select;

const SearchPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { enableFullTextSearch } = useAppStore();

  // Full-text search has been disabled by the administrator
  if (!enableFullTextSearch) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
        <Card style={{ textAlign: 'center', maxWidth: 400 }}>
          <SearchOutlined style={{ fontSize: 48, color: '#94a3b8', marginBottom: 16 }} />
          <Typography.Title level={4}>Full-text search is disabled</Typography.Title>
          <Typography.Text type="secondary">The administrator has disabled full-text search in system settings. Please contact your administrator if you need this feature.</Typography.Text>
        </Card>
      </div>
    );
  }
  const inputRef = useRef<HTMLInputElement>(null);

  // Search state
  const [query, setQuery] = useState(searchParams.get('q') || '');
  const [searchMode, setSearchMode] = useState<'keyword' | 'hybrid'>('keyword');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(10);
  const [searched, setSearched] = useState(false);

  // Suggestions
  const [suggestions, setSuggestions] = useState<Array<{ text: string }>>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const suggestTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Hot searches & history
  const [hotSearches, setHotSearches] = useState<string[]>([]);
  const [searchHistory, setSearchHistory] = useState<Array<{ id: EntityId; keyword: string }>>([]);

  // Expanded chunks
  const [expandedChunks, setExpandedChunks] = useState<Set<string>>(new Set());

  // Filters
  const [filters, setFilters] = useState({
    type: 'all',
    sortBy: 'relevance',
  });

  // Load hot searches & history on mount
  useEffect(() => {
    loadHotSearches();
    loadHistory();
  }, []);

  // Auto-search on mount when keyword comes from URL
  useEffect(() => {
    const q = searchParams.get('q') || '';
    if (q.trim()) {
      performSearch(q);
    }
  }, []);

  // Auto-search when search mode toggles (if already searched)
  const isFirstRender = useRef(true);
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    if (query.trim()) {
      performSearch(query);
    }
  }, [searchMode]);

  // Search when query comes from URL (subsequent navigations)
  useEffect(() => {
    const q = searchParams.get('q') || '';
    if (q && q !== query) {
      setQuery(q);
      performSearch(q);
    }
  }, [searchParams]);

  // Debounced suggestions
  const fetchSuggestions = useCallback(async (keyword: string) => {
    if (keyword.trim().length < 1) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }
    try {
      const data = await searchService.suggestions(keyword);
      if (Array.isArray(data)) {
        setSuggestions(data.map((s: any) => ({ text: s.text || s })));
        setShowSuggestions(true);
      }
    } catch { /* ignore */ }
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    if (suggestTimer.current) clearTimeout(suggestTimer.current);
    suggestTimer.current = setTimeout(() => fetchSuggestions(value), 300);
  };

  const loadHotSearches = async () => {
    try {
      const data = await searchService.hotSearch();
      if (Array.isArray(data)) setHotSearches(data);
    } catch { /* ignore */ }
  };

  const loadHistory = async () => {
    try {
      const data = await searchService.history();
      if (Array.isArray(data)) {
        setSearchHistory(data.map((h: any) => ({ id: h.id, keyword: h.keyword })));
      }
    } catch { /* ignore */ }
  };

  const clearHistory = async () => {
    try {
      await searchService.clearHistory();
      setSearchHistory([]);
    } catch { /* ignore */ }
  };

  const performSearch = async (keyword: string, currentPage = 1) => {
    const q = keyword || query;
    if (!q.trim()) return;

    setLoading(true);
    setSearched(true);
    setShowSuggestions(false);
    setPage(currentPage);

    try {
      const response = await searchService.search({
        keyword: q,
        searchMode,
        topK: 10,
        enableRerank: true,
        page: currentPage,
        pageSize,
      });
      setResults(response.records || []);
      setTotal(response.total || 0);
      // Refresh search history after a successful search
      loadHistory();
      loadHotSearches();
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (value?: string) => {
    const q = value || query;
    if (!q.trim()) return;
    setQuery(q);
    navigate(`/search?q=${encodeURIComponent(q.trim())}`);
    performSearch(q.trim());
    inputRef.current?.blur();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
    if (e.key === 'Escape') setShowSuggestions(false);
  };

  const handlePageChange = (p: number) => {
    performSearch(query, p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleHotClick = (term: string) => {
    setQuery(term);
    navigate(`/search?q=${encodeURIComponent(term)}`);
    performSearch(term);
  };

  const toggleChunks = (resultId: string) => {
    setExpandedChunks(prev => {
      const next = new Set(prev);
      if (next.has(resultId)) next.delete(resultId);
      else next.add(resultId);
      return next;
    });
  };

  const getIconForType = (type: string) => {
    switch (type) {
      case 'document': return <FileTextOutlined />;
      case 'user': return <UserOutlined />;
      case 'category': return <FolderOutlined />;
      default: return <FileTextOutlined />;
    }
  };

  const getTypeColor = (type: string) => {
    switch (type) {
      case 'tech': return 'blue';
      case 'ai': case 'purple': return 'purple';
      case 'business': case 'green': return 'green';
      default: return 'default';
    }
  };

  const getScoreColor = (score: number): string => {
    if (score >= 0.8) return '#22c55e';
    if (score >= 0.6) return '#f59e0b';
    if (score >= 0.4) return '#f97316';
    return '#ef4444';
  };

  const highlightKeyword = (text: string): string => {
    if (!text || !query) return text;
    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return text.replace(regex, '<mark>$1</mark>');
  };

  const hotTerms = hotSearches.length > 0
    ? hotSearches
    : ['Spring Boot Tutorial', 'Microservices Architecture', 'AI Assistant User Guide', 'System Design Document'];

  return (
    <div className="search-page">
      <div className="search-container">
        {/* ---- Hero Header ---- */}
        <div className="search-header">
          <h1 className="search-title">Smart Search</h1>
          <p className="search-subtitle">
            AI-powered semantic search to quickly find the knowledge you need
          </p>
        </div>

        {/* ---- Search Box ---- */}
        <div className="search-box-wrapper">
          <div className="search-box-inner">
            <SearchOutlined className="search-icon" />
            <input
              ref={inputRef}
              type="text"
              className="search-input"
              placeholder="Search documents, users, tags..."
              value={query}
              onChange={handleInputChange}
              onKeyDown={handleKeyDown}
              onFocus={() => { if (suggestions.length > 0) setShowSuggestions(true); }}
              onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
            />
            {query && (
              <CloseCircleOutlined
                className="search-clear-btn"
                onClick={() => { setQuery(''); setSuggestions([]); setSearched(false); }}
              />
            )}
          </div>
          <button className="search-btn" onClick={() => handleSearch()}>
            {loading ? <LoadingOutlined spin /> : 'Search'}
          </button>

          {/* Auto-complete */}
          {showSuggestions && suggestions.length > 0 && (
            <div className="suggestions-dropdown">
              {suggestions.map((s, i) => (
                <div
                  key={i}
                  className="suggestion-item"
                  onMouseDown={() => {
                    setQuery(s.text);
                    setShowSuggestions(false);
                    handleSearch(s.text);
                  }}
                >
                  <SearchOutlined className="suggestion-icon" />
                  <span dangerouslySetInnerHTML={{ __html: highlightKeyword(s.text) }} />
                </div>
              ))}
            </div>
          )}
        </div>

        {/* ---- Search Mode Toggle ---- */}
        <div className="search-mode-toggle">
          <button
            className={`mode-btn ${searchMode === 'keyword' ? 'active' : ''}`}
            onClick={() => setSearchMode('keyword')}
          >
            <FileTextOutlined /> Keyword Search
          </button>
          <Tooltip title="BM25 + vector semantic retrieval + RRF fusion + LLM re-ranking, precise and intelligent">
            <button
              className={`mode-btn hybrid ${searchMode === 'hybrid' ? 'active' : ''}`}
              onClick={() => setSearchMode('hybrid')}
            >
              <ThunderboltOutlined /> Hybrid Smart Search
            </button>
          </Tooltip>
        </div>

        {/* ---- Loading ---- */}
        {loading && (
          <div className="loading-container">
            <Spin size="large" />
          </div>
        )}

        {/* ---- Results Section ---- */}
        {!loading && searched && (
          <div className="results-section">
            <div className="results-header">
              <span className="results-count">
                Found <strong>{total}</strong> result(s)
                <span className="mode-badge">
                  {searchMode === 'hybrid' ? 'Hybrid Smart' : 'Keyword'}
                </span>
              </span>
              <div className="sort-dropdown">
                <span className="sort-label">Sort by:</span>
                <Select
                  value={filters.sortBy}
                  onChange={(value) => setFilters(prev => ({ ...prev, sortBy: value }))}
                  className="sort-select"
                >
                  <Option value="relevance">Relevance</Option>
                  <Option value="time">Newest</Option>
                  <Option value="views">Views</Option>
                </Select>
              </div>
            </div>

            {results.length > 0 ? (
              <div className="results-list">
                {results.map((result) => (
                  <div
                    key={result.id}
                    className="search-result"
                    onClick={() => {
                      window.open(`/documents/${result.id}`, '_blank');
                    }}
                  >
                    <div className="result-header">
                      <div className="result-icon">
                        {getIconForType('document')}
                      </div>
                      <div className="result-title-wrapper">
                        <div className="result-badges">
                          <Tag className={`result-badge ${getTypeColor('tech')}`}>
                            Document
                          </Tag>
                          {result.score !== undefined && (
                            <span
                              className="result-score-badge"
                              style={{ color: getScoreColor(result.score) }}
                            >
                              {(result.score * 100).toFixed(0)}%
                            </span>
                          )}
                        </div>
                        <h3
                          className="result-title"
                          dangerouslySetInnerHTML={{ __html: result.title || 'Untitled document' }}
                        />

                        {/* Summary */}
                        {result.summary && (
                          <div
                            className="result-excerpt"
                            dangerouslySetInnerHTML={{
                              __html: result.summary.includes('<em>')
                                ? result.summary
                                : highlightKeyword(result.summary)
                            }}
                          />
                        )}

                        {/* Highlights (keyword mode) */}
                        {result.highlights && result.highlights.length > 0 && (
                          <div className="result-highlights">
                            {result.highlights.slice(0, 2).map((h, i) => (
                              <div
                                key={i}
                                className="result-excerpt"
                                dangerouslySetInnerHTML={{ __html: h }}
                              />
                            ))}
                          </div>
                        )}

                        {/* Score breakdown (hybrid mode) */}
                        {searchMode === 'hybrid' && (
                          <div className="score-breakdown">
                            {result.bm25Score !== undefined && (
                              <span className="score-chip bm25">BM25 {(result.bm25Score * 100).toFixed(0)}</span>
                            )}
                            {result.vectorScore !== undefined && (
                              <span className="score-chip vector">Vector {(result.vectorScore * 100).toFixed(0)}</span>
                            )}
                            {result.rerankScore !== undefined && (
                              <span className="score-chip rerank">Rerank {(result.rerankScore * 100).toFixed(0)}</span>
                            )}
                          </div>
                        )}

                        {/* Chunks (hybrid mode) */}
                        {searchMode === 'hybrid' && result.chunks && result.chunks.length > 0 && (
                          <div className="result-chunks" onClick={(e) => { e.stopPropagation(); toggleChunks(result.id); }}>
                            <div className="chunks-toggle">
                              Related Chunks ({result.chunks.length})
                              <span className={`toggle-arrow ${expandedChunks.has(result.id) ? 'expanded' : ''}`}>&#9654;</span>
                            </div>
                            {expandedChunks.has(result.id) && result.chunks.map((chunk, idx) => (
                              <div key={chunk.chunkId || idx} className="chunk-item">
                                {chunk.heading && <div className="chunk-heading">{chunk.heading}</div>}
                                <div
                                  className="chunk-content"
                                  dangerouslySetInnerHTML={{
                                    __html: (() => {
                                      const text = chunk.content.length > 300 ? chunk.content.slice(0, 300) + '...' : chunk.content;
                                      return text.includes('<em>') ? text : highlightKeyword(text);
                                    })()
                                  }}
                                />
                              </div>
                            ))}
                          </div>
                        )}

                        <div className="result-meta">
                          <span className="meta-item">
                            <ClockCircleOutlined />
                            {result.publishAt || 'No date'}
                          </span>
                          {result.creatorName && (
                            <span className="meta-item">
                              <UserOutlined />
                              {result.creatorName}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}

                {/* Pagination */}
                {total > pageSize && (
                  <div className="search-pagination">
                    <Pagination
                      current={page}
                      pageSize={pageSize}
                      total={total}
                      onChange={handlePageChange}
                      showSizeChanger={false}
                    />
                  </div>
                )}
              </div>
            ) : (
              <div className="no-results">
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={
                    <div>
                      <div className="no-results-title">No results found</div>
                      <div className="no-results-text">
                        Try different keywords, or switch to Hybrid Smart Search mode
                      </div>
                    </div>
                  }
                />
              </div>
            )}
          </div>
        )}

        {/* ---- Initial State: Hot Searches + History ---- */}
        {!loading && !searched && (
          <div className="search-suggestions">
            <div className="suggestions-grid">
              {/* Hot Searches */}
              <Card title={<span><FireOutlined style={{ color: '#f97316' }} /> Hot Searches</span>} className="suggestion-card">
                <div className="suggestion-card-body">
                  {hotTerms.map((term) => (
                    <button
                      key={term}
                      className="suggestion-link"
                      onClick={() => handleHotClick(term)}
                    >
                      {term}
                    </button>
                  ))}
                </div>
              </Card>

              {/* Search History */}
              <Card
                title={<span><HistoryOutlined /> Search History</span>}
                className="suggestion-card"
                extra={
                  searchHistory.length > 0 && (
                    <button className="clear-history-link" onClick={clearHistory}>
                      <DeleteOutlined /> Clear
                    </button>
                  )
                }
              >
                <div className="suggestion-card-body">
                  {searchHistory.length > 0 ? (
                    searchHistory.map((h) => (
                      <div
                        key={h.id}
                        className="history-row"
                        onClick={() => handleHotClick(h.keyword)}
                      >
                        <ClockCircleOutlined className="history-row-icon" />
                        {h.keyword}
                      </div>
                    ))
                  ) : (
                    <div className="suggestion-empty">No search history yet</div>
                  )}
                </div>
              </Card>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchPage;
