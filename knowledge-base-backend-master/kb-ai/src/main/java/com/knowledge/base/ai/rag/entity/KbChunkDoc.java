package com.knowledge.base.ai.rag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

/**
 * ES knowledge base document chunk mapping
 *
 * <p>Maps to the kb_chunk index, storing document chunks and their vector embeddings.
 * Supports hybrid retrieval combining BM25 full-text search and kNN vector search.</p>
 *
 * <p><b>Note</b>: the embedding field is written via the low-level ElasticsearchClient,
 * because Spring Data Elasticsearch 5.x's @Field has limited support for dense_vector.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "kb_chunk", createIndex = false)
public class KbChunkDoc {

    /** Unique chunk ID (UUID) */
    @Id
    @Field(name = "chunk_id", type = FieldType.Keyword)
    private String chunkId;

    /** Source document ID */
    @Field(name = "document_id", type = FieldType.Long)
    private Long documentId;

    /** Source document title */
    @Field(name = "document_title", type = FieldType.Text)
    private String documentTitle;

    /** Chunk text content */
    @Field(name = "content", type = FieldType.Text)
    private String content;

    /** Section heading this chunk belongs to */
    @Field(name = "heading", type = FieldType.Keyword)
    private String heading;

    /** Chunk index within the document (0-based) */
    @Field(name = "chunk_index", type = FieldType.Integer)
    private Integer chunkIndex;

    /** Total number of chunks for the document */
    @Field(name = "total_chunks", type = FieldType.Integer)
    private Integer totalChunks;

    /** Category ID */
    @Field(name = "category_id", type = FieldType.Long)
    private Long categoryId;

    /** Author ID */
    @Field(name = "author_id", type = FieldType.Long)
    private Long authorId;

    /** Team ID */
    @Field(name = "team_id", type = FieldType.Long)
    private Long teamId;

    /** Document status */
    @Field(name = "doc_status", type = FieldType.Integer)
    private Integer docStatus;

    /** Whether the document is public (0-private, 1-public) */
    @Field(name = "is_public", type = FieldType.Integer)
    private Integer isPublic;

    /** Document publish time */
    @Field(name = "publish_time", type = FieldType.Date)
    private String publishTime;

    /** Index time (storage only, not part of the search mapping) */
    private LocalDateTime indexedAt;

    // The embedding field (dense_vector) is handled via the low-level ES client
    // and is not declared with a @Field annotation here
}
