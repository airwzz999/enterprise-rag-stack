package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User activity VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User activity")
public class UserActivityVO {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Documents created")
    private Long documentCount;

    @Schema(description = "Comment count")
    private Long commentCount;

    @Schema(description = "View count")
    private Long viewCount;

    @Schema(description = "Activity score")
    private Double activityScore;
}
