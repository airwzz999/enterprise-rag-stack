package com.knowledge.base.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Category document count statistics DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDocCountDTO {

    /**
     * Category ID
     */
    private Long categoryId;

    /**
     * Document count
     */
    private Integer count;
}
