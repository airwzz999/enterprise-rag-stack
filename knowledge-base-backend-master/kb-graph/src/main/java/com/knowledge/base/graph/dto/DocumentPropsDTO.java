package com.knowledge.base.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document node property parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPropsDTO {

    /** Document ID */
    private Long docId;

    /** Document title */
    private String title;

    /** Document summary */
    private String summary;

    /** Category ID */
    private Long categoryId;

    /** Author ID */
    private Long authorId;

    /** Author name */
    private String authorName;

    /** Document status */
    private Integer status;
}
