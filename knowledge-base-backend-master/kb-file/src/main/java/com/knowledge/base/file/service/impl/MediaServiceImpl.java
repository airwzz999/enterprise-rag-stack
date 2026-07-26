package com.knowledge.base.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.file.config.FileStorageProperties;
import com.knowledge.base.file.entity.FileInfo;
import com.knowledge.base.file.mapper.FileMapper;
import com.knowledge.base.file.service.MediaService;
import com.knowledge.base.file.storage.FileStorage;
import com.knowledge.base.file.storage.FileStorageFactory;
import com.knowledge.base.file.vo.MediaMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * Media processing service implementation
 * Uses command-line invocations of FFmpeg/FFprobe to handle audio/video processing
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements MediaService {

    private final FileMapper fileMapper;
    private final FileStorageFactory storageFactory;
    private final FileStorageProperties storageProperties;

    @Override
    public MediaMetadata probeMediaInfo(Long fileId) {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        String ffprobePath = storageProperties.getFfmpeg().getFfprobePath();

        // Check whether ffprobe is available; if not, skip metadata extraction and log a warning only
        if (!new File(ffprobePath).exists()) {
            log.warn("ffprobe is unavailable (path: {}), skipping media metadata extraction: fileId={}", ffprobePath, fileId);
            return MediaMetadata.builder().build();
        }

        Path tempFile = null;

        try {
            // Download the file from RustFS to a temp directory
            tempFile = downloadToTemp(fileInfo);
            log.info("Extracting media metadata: fileId={}, tempPath={}", fileId, tempFile);

            // Invoke ffprobe
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    tempFile.toString()
            );

            Process process = pb.start();
            String output = readProcessOutput(process);
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);

            if (!completed || process.exitValue() != 0) {
                String errorOutput = readErrorOutput(process);
                log.error("ffprobe execution failed: exitCode={}, error={}", process.exitValue(), errorOutput);
                throw new BusinessException("Failed to extract media metadata");
            }

            return parseFfprobeOutput(output);

        } catch (IOException | InterruptedException e) {
            log.warn("Error extracting media metadata (does not affect the upload): fileId={}, error={}", fileId, e.getMessage());
            return MediaMetadata.builder().build();
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    public String transcodeToHls(Long fileId) {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        String ffmpegPath = storageProperties.getFfmpeg().getPath();

        // Check whether ffmpeg is available
        if (!new File(ffmpegPath).exists()) {
            log.warn("ffmpeg is unavailable (path: {}), skipping HLS transcoding: fileId={}", ffmpegPath, fileId);
            throw new BusinessException("HLS transcoding service is unavailable; please install ffmpeg and try again");
        }

        int segmentTime = storageProperties.getFfmpeg().getHlsSegmentTime();
        Path tempFile = null;
        Path hlsDir = null;

        try {
            // Download the original file from RustFS
            tempFile = downloadToTemp(fileInfo);
            log.info("Starting HLS transcoding: fileId={}, tempPath={}", fileId, tempFile);

            // Create the HLS temp directory
            hlsDir = Files.createTempDirectory("hls_" + fileId + "_");
            String hlsBasePath = fileInfo.getFilePath().replaceFirst("\\.[^.]+$", "");
            String hlsRelativePath = hlsBasePath + "/hls";

            // Transcode 360p
            Path hls360Dir = Paths.get(hlsDir.toString(), "360p");
            Files.createDirectories(hls360Dir);
            transcodeResolution(ffmpegPath, tempFile.toString(), hls360Dir, segmentTime,
                    640, 360, 28, 64);

            // Transcode 720p
            Path hls720Dir = Paths.get(hlsDir.toString(), "720p");
            Files.createDirectories(hls720Dir);
            transcodeResolution(ffmpegPath, tempFile.toString(), hls720Dir, segmentTime,
                    1280, 720, 23, 128);

            // Generate the master playlist
            String masterPlaylist = generateMasterPlaylist();
            Path masterFile = Paths.get(hlsDir.toString(), "master.m3u8");
            Files.writeString(masterFile, masterPlaylist);

            // Upload the HLS files to RustFS
            FileStorage storage = storageFactory.getStorage();
            uploadHlsFiles(storage, hlsDir, hlsRelativePath);

            // Update the HLS path in the DB
            fileInfo.setHlsPath(hlsRelativePath);
            fileMapper.updateById(fileInfo);

            log.info("HLS transcoding completed: fileId={}, hlsPath={}", fileId, hlsRelativePath);
            return hlsRelativePath;

        } catch (Exception e) {
            log.error("HLS transcoding failed: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new BusinessException("HLS transcoding failed: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
            deleteTempDir(hlsDir);
        }
    }

    @Override
    public String generateThumbnail(Long fileId) {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new BusinessException("File does not exist");
        }

        String ffmpegPath = storageProperties.getFfmpeg().getPath();

        // Check whether ffmpeg is available
        if (!new File(ffmpegPath).exists()) {
            log.warn("ffmpeg is unavailable (path: {}), skipping thumbnail generation: fileId={}", ffmpegPath, fileId);
            throw new BusinessException("Thumbnail service is unavailable; please install ffmpeg and try again");
        }

        int thumbnailTime = storageProperties.getFfmpeg().getThumbnailTime();
        Path tempFile = null;
        Path thumbnailFile = null;

        try {
            tempFile = downloadToTemp(fileInfo);
            thumbnailFile = Files.createTempFile("thumbnail_", ".jpg");
            log.info("Generating thumbnail: fileId={}, thumbnailPath={}", fileId, thumbnailFile);

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath,
                    "-i", tempFile.toString(),
                    "-ss", formatTime(thumbnailTime),
                    "-vframes", "1",
                    "-q:v", "2",
                    "-y",
                    thumbnailFile.toString()
            );

            Process process = pb.start();
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);

            if (!completed || process.exitValue() != 0) {
                String errorOutput = readErrorOutput(process);
                log.error("Thumbnail generation failed: exitCode={}, error={}", process.exitValue(), errorOutput);
                throw new BusinessException("Failed to generate thumbnail");
            }

            // Upload the thumbnail to RustFS
            String basePath = fileInfo.getFilePath().replaceFirst("\\.[^.]+$", "");
            String thumbnailRelativePath = basePath + "/thumbnail.jpg";

            FileStorage storage = storageFactory.getStorage();
            try (InputStream is = new FileInputStream(thumbnailFile.toFile())) {
                long fileSize = Files.size(thumbnailFile);
                storage.upload(is, thumbnailRelativePath, fileSize);
            }

            // Update the thumbnail path in the DB
            fileInfo.setThumbnailPath(thumbnailRelativePath);
            fileMapper.updateById(fileInfo);

            log.info("Thumbnail generation completed: fileId={}, thumbnailPath={}", fileId, thumbnailRelativePath);
            return thumbnailRelativePath;

        } catch (Exception e) {
            log.error("Thumbnail generation failed: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new BusinessException("Failed to generate thumbnail: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
            deleteTempFile(thumbnailFile);
        }
    }

    @Override
    public void updateTranscodeStatus(Long fileId, String status) {
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo != null) {
            fileInfo.setTranscodeStatus(status);
            fileMapper.updateById(fileInfo);
            log.info("Updated transcode status: fileId={}, status={}", fileId, status);
        }
    }

    // ==================== Private methods ====================

    /**
     * Download a file from RustFS to a temp directory
     */
    private Path downloadToTemp(FileInfo fileInfo) throws IOException {
        FileStorage storage = storageFactory.getStorage();
        try (InputStream is = storage.getInputStream(fileInfo.getFilePath())) {
            String extension = FileUtil.extName(fileInfo.getOriginalName());
            Path tempFile = Files.createTempFile("media_", "." + (extension.isEmpty() ? "tmp" : extension));
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }

    /**
     * Transcode HLS at the specified resolution
     */
    private void transcodeResolution(String ffmpegPath, String inputPath, Path outputDir,
                                      int segmentTime, int width, int height, int crf, int audioBitrate)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputPath,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", String.valueOf(crf),
                "-vf", "scale=" + width + ":" + height,
                "-c:a", "aac",
                "-b:a", audioBitrate + "k",
                "-hls_time", String.valueOf(segmentTime),
                "-hls_list_size", "0",
                "-hls_segment_filename", outputDir + "/%03d.ts",
                "-y",
                outputDir + "/index.m3u8"
        );

        log.info("Transcoding {}p: {}", height, String.join(" ", pb.command()));
        Process process = pb.start();
        boolean completed = process.waitFor(300, TimeUnit.SECONDS); // 5-minute timeout

        if (!completed || process.exitValue() != 0) {
            String errorOutput = readErrorOutput(process);
            throw new BusinessException("Failed to transcode " + height + "p: " + errorOutput);
        }
    }

    /**
     * Generate the master HLS playlist
     */
    private String generateMasterPlaylist() {
        return """
                #EXTM3U
                #EXT-X-STREAM-INF:BANDWIDTH=600000,RESOLUTION=640x360
                360p/index.m3u8
                #EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=1280x720
                720p/index.m3u8
                """;
    }

    /**
     * Upload all files under the HLS directory to RustFS
     */
    private void uploadHlsFiles(FileStorage storage, Path hlsDir, String hlsRelativePath)
            throws IOException {
        // Upload master.m3u8
        Path masterFile = hlsDir.resolve("master.m3u8");
        try (InputStream is = new FileInputStream(masterFile.toFile())) {
            storage.upload(is, hlsRelativePath + "/master.m3u8", Files.size(masterFile));
        }

        // Upload each resolution directory
        for (String quality : new String[]{"360p", "720p"}) {
            Path qualityDir = hlsDir.resolve(quality);
            if (!Files.isDirectory(qualityDir)) {
                continue;
            }
            try (var files = Files.list(qualityDir)) {
                files.forEach(file -> {
                    try (InputStream is = new FileInputStream(file.toFile())) {
                        String relativeKey = hlsRelativePath + "/" + quality + "/" + file.getFileName();
                        storage.upload(is, relativeKey, file.toFile().length());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload HLS segment: " + file.getFileName(), e);
                    }
                });
            }
        }
    }

    /**
     * Parse the ffprobe JSON output
     */
    private MediaMetadata parseFfprobeOutput(String jsonOutput) {
        try {
            JSONObject root = JSONUtil.parseObj(jsonOutput);
            JSONObject format = root.getJSONObject("format");
            JSONArray streams = root.getJSONArray("streams");

            Integer duration = format != null ? format.getDouble("duration").intValue() : null;
            Integer bitrate = format != null ? format.getInt("bit_rate") : null;
            // bit_rate is in bps; convert to kbps
            if (bitrate != null) {
                bitrate = bitrate / 1000;
            }

            String resolution = null;
            String videoCodec = null;
            String audioCodec = null;

            if (streams != null) {
                for (int i = 0; i < streams.size(); i++) {
                    JSONObject stream = streams.getJSONObject(i);
                    String codecType = stream.getStr("codec_type");
                    if ("video".equals(codecType)) {
                        Integer width = stream.getInt("width");
                        Integer height = stream.getInt("height");
                        if (width != null && height != null) {
                            resolution = width + "x" + height;
                        }
                        videoCodec = stream.getStr("codec_name");
                    } else if ("audio".equals(codecType)) {
                        audioCodec = stream.getStr("codec_name");
                    }
                }
            }

            return MediaMetadata.builder()
                    .duration(duration)
                    .resolution(resolution)
                    .bitrate(bitrate)
                    .videoCodec(videoCodec)
                    .audioCodec(audioCodec)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse ffprobe output: {}", e.getMessage(), e);
            return MediaMetadata.builder().build();
        }
    }

    /**
     * Read the process standard output
     */
    private String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Read the process error output
     */
    private String readErrorOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Format seconds as HH:MM:SS
     */
    private String formatTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * Delete a temp file
     */
    private void deleteTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path);
            }
        }
    }

    /**
     * Recursively delete a temp directory
     */
    private void deleteTempDir(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try {
                FileUtil.del(dir.toFile());
            } catch (Exception e) {
                log.warn("Failed to delete temp directory: {}", dir);
            }
        }
    }
}
