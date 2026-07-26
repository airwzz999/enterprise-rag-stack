package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Dictionary VO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to return
 * dictionary type information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dictionary type response")
public class DictVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID")
    private Long id;

    /**
     * Dictionary code
     */
    @Schema(description = "Dictionary code")
    private String dictCode;

    /**
     * Dictionary name
     */
    @Schema(description = "Dictionary name")
    private String dictName;

    /**
     * Dictionary type
     */
    @Schema(description = "Dictionary type")
    private String dictType;

    /**
     * Description
     */
    @Schema(description = "Description")
    private String description;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer sort;

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

    /**
     * Updated time
     */
    @Schema(description = "Updated time")
    private LocalDateTime updatedAt;
}
