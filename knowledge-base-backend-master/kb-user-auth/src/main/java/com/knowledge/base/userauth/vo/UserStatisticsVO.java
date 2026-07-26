package com.knowledge.base.userauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * User statistics response VO
 *
 * <p>Returns a user's knowledge-base statistics (document count, views, likes, comments)</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User statistics")
public class UserStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Document count", example = "12")
    private Long documentCount;

    @Schema(description = "Total views", example = "345")
    private Long viewCount;

    @Schema(description = "Total likes received", example = "89")
    private Long likeCount;

    @Schema(description = "Total comments received", example = "23")
    private Long commentCount;
}
