package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Dictionary data VO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to return
 * dictionary data information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dictionary data response")
public class DictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary data ID
     */
    @Schema(description = "Dictionary data ID")
    private Long id;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID")
    private Long dictId;

    /**
     * Dictionary code (redundant)
     */
    @Schema(description = "Dictionary code")
    private String dictCode;

    /**
     * Dictionary label
     */
    @Schema(description = "Dictionary label")
    private String dictLabel;

    /**
     * Dictionary value
     */
    @Schema(description = "Dictionary value")
    private String dictValue;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer dictSort;

    /**
     * CSS class name
     */
    @Schema(description = "CSS class name")
    private String cssClass;

    /**
     * List style
     */
    @Schema(description = "List style")
    private String listClass;

    /**
     * Is default: 0-no, 1-yes
     */
    @Schema(description = "Is default")
    private Integer isDefault;

    /**
     * Status: 0-disabled, 1-enabled
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Created time
     */
    @Schema(description = "Created time")
    private LocalDateTime createdAt;
}
