package com.knowledge.base.document.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.config.FeignMultipartSupportConfig;
import com.knowledge.base.document.config.InternalFeignConfig;
import com.knowledge.base.document.dto.FileUploadResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * File service Feign client
 *
 * <p>Used to call the file service's related endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
    name = "kb-file",
    url = "${kb-file.url:}",
    path = "/files",
    configuration = {FeignMultipartSupportConfig.class, InternalFeignConfig.class},
    fallbackFactory = FileServiceFallbackFactory.class
)
public interface FileServiceFeignClient {

    /**
     * Uploads a single file (sent via Feign to kb-file's RUSTFS storage)
     *
     * @param file        file (MultipartFile)
     * @param fileType    file type
     * @param accessLevel access level
     * @param teamId      team ID
     * @return file information
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<FileUploadResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "fileType", required = false) String fileType,
            @RequestParam(value = "accessLevel", required = false) Integer accessLevel,
            @RequestParam(value = "teamId", required = false) Long teamId);

    /**
     * Converts an image from a URL
     *
     * @param imageUrl image URL
     * @return conversion result
     */
    @PostMapping("/convert-url")
    Result<FileUploadResponse> convertImageUrl(@RequestParam("imageUrl") String imageUrl);
}
