package com.knowledge.base.search.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

/**
 * Document index entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "kb_document", createIndex = false)
public class DocumentIndex {

    /**
     * Document ID
     */
    @Id
    private String id;

    /**
     * Document title
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    /**
     * Document summary
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String summary;

    /**
     * Document content
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String content;

    /**
     * Category ID
     */
    @Field(type = FieldType.Long)
    private Long categoryId;

    /**
     * Category name
     */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /**
     * Tag ID list
     */
    @Field(type = FieldType.Long)
    private List<Long> tagIds;

    /**
     * Tag name list
     */
    @Field(type = FieldType.Keyword)
    private List<String> tagNames;

    /**
     * Creator ID
     */
    @Field(type = FieldType.Long)
    private Long creatorId;

    /**
     * Creator name
     */
    @Field(type = FieldType.Keyword)
    private String creatorName;

    /**
     * Team ID
     */
    @Field(type = FieldType.Long)
    private Long teamId;

    /**
     * Team name
     */
    @Field(type = FieldType.Keyword)
    private String teamName;

    /**
     * Document status
     */
    @Field(type = FieldType.Integer)
    private Integer docStatus;

    /**
     * View count
     */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /**
     * Like count
     */
    @Field(type = FieldType.Integer)
    private Integer likeCount;

    /**
     * Comment count
     */
    @Field(type = FieldType.Integer)
    private Integer commentCount;

    /**
     * Whether public
     */
    @Field(type = FieldType.Boolean)
    private Boolean isPublic;

    /**
     * Publish time
     */
    @Field(type = FieldType.Keyword)
    private String publishAt;

    /**
     * Creation time
     */
    @Field(type = FieldType.Keyword)
    private String createdAt;

    /**
     * Update time
     */
    @Field(type = FieldType.Keyword)
    private String updatedAt;
}
