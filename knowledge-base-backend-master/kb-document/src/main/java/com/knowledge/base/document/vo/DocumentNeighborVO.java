package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Document neighbor (adjacent) response VO
 *
 * <p>Used for previous/next navigation on the document detail page</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Document neighbor information")
public class DocumentNeighborVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Previous document ID")
    private Long prevId;

    @Schema(description = "Previous document title")
    private String prevTitle;

    @Schema(description = "Next document ID")
    private Long nextId;

    @Schema(description = "Next document title")
    private String nextTitle;
}
