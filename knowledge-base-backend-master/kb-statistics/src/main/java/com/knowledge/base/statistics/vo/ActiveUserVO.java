package com.knowledge.base.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Active user VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Active users")
public class ActiveUserVO {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Real name")
    private String realName;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Documents created")
    private Long documentCount;

    @Schema(description = "Comment count")
    private Long commentCount;

    @Schema(description = "View count")
    private Long viewCount;

    @Schema(description = "Statistic value")
    private Long statisticsValue;
}
