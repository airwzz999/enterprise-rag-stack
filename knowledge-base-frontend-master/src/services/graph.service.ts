import { http } from './request';
import { GraphNode, GraphEdge, GraphData, GraphPathResult } from '@/types';

/**
 * Knowledge graph service
 */
export const graphService = {
  /**
   * Get graph data
   */
  getGraphData: async (): Promise<GraphData> => {
    return http.get('/graph/data');
  },

  /**
   * Search nodes
   */
  searchNodes: async (keyword: string): Promise<GraphNode[]> => {
    return http.get('/graph/search', { params: { keyword } });
  },

  /**
   * Get node details
   */
  getNodeDetail: async (nodeId: string): Promise<GraphNode> => {
    return http.get(`/graph/node/${nodeId}`);
  },

  /**
   * Get neighboring nodes of a node
   */
  getNodeNeighbors: async (nodeId: string, depth: number = 1): Promise<{ nodes: GraphNode[]; edges: GraphEdge[] }> => {
    return http.get(`/graph/node/${nodeId}/neighbors`, { params: { depth } });
  },

  /**
   * Get node relationships
   */
  getNodeRelationships: async (nodeId: string): Promise<GraphEdge[]> => {
    return http.get(`/graph/node/${nodeId}/relationships`);
  },

  /**
   * Path analysis
   */
  findPath: async (startNodeId: string, endNodeId: string): Promise<GraphPathResult> => {
    return http.get('/graph/path', { params: { startNodeId, endNodeId } });
  },

  /**
   * Get community detection results
   */
  detectCommunities: async (): Promise<{ id: string; nodes: string[] }[]> => {
    return http.get('/graph/communities');
  },

  /**
   * Rebuild the knowledge graph (triggers KAG to build the graph for all published documents)
   */
  rebuildGraph: async (): Promise<string> => {
    return http.post('/document/documents/graph/rebuild');
  },

  /**
   * Clean up orphaned/invalid nodes in the knowledge graph
   */
  cleanupGraph: async (): Promise<string> => {
    return http.post('/document/documents/graph/cleanup');
  },
};
