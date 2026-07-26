package com.knowledge.base.file.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Transcode message body
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** File ID */
    private Long fileId;

    /** Target format, fixed to "hls" */
    private String targetFormat;
}
