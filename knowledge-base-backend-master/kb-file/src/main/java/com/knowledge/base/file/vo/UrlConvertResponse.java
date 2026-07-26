package com.knowledge.base.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URL conversion response
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlConvertResponse {

    /**
     * Original URL
     */
    private String originalUrl;

    /**
     * New URL
     */
    private String newUrl;

    /**
     * Whether the operation succeeded
     */
    private Boolean success;

    /**
     * Error message
     */
    private String errorMessage;
}
