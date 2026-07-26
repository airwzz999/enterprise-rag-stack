package com.knowledge.base.document.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * File service Feign fallback factory
 *
 * <p>Returns null when the kb-file service is unavailable, letting the caller fall back to local storage</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class FileServiceFallbackFactory implements FallbackFactory<FileServiceFeignClient> {

    @Override
    public FileServiceFeignClient create(Throwable cause) {
        log.error("File service Feign call failed, triggering fallback: {}", cause.getMessage());
        return new FileServiceFeignClient() {
            @Override
            public Result<FileUploadResponse> uploadFile(MultipartFile file, String fileType,
                                                          Integer accessLevel, Long teamId) {
                log.warn("Feign fallback: file upload fallback, fileName={}", file.getOriginalFilename());
                return null;
            }

            @Override
            public Result<FileUploadResponse> convertImageUrl(String imageUrl) {
                log.warn("Feign fallback: URL conversion fallback, imageUrl={}", imageUrl);
                return null;
            }
        };
    }
}
