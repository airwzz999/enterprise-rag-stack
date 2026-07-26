package com.knowledge.base.ai.dto.kag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document record (returned data from the kb-document Feign API)
 *
 * <p>Used to unify parsing of document data returned by Feign pagination/detail
 * calls in GraphBuildServiceImpl and ReindexConsumer.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRecordDTO {

    /** Document ID */
    private Long id;

    /** Document title */
    private String title;

    /** Document body */
    private String content;

    /** Category ID */
    private Long categoryId;

    /** Author ID */
    private Long authorId;

    /** Author name */
    private String authorName;

    /** Team ID */
    private Long teamId;

    /** Document status */
    private Integer status;

    /** Summary */
    private String summary;

    /** Publish time */
    private String publishTime;
}
