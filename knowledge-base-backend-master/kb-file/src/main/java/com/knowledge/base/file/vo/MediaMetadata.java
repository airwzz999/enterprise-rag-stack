package com.knowledge.base.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Media metadata
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Duration (seconds) */
    private Integer duration;

    /** Resolution, e.g. "1920x1080" */
    private String resolution;

    /** Bitrate (kbps) */
    private Integer bitrate;

    /** Video codec, e.g. "h264" */
    private String videoCodec;

    /** Audio codec, e.g. "aac" */
    private String audioCodec;
}
