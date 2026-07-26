package com.knowledge.base.document.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign multipart file upload configuration
 *
 * <p>Configures SpringFormEncoder to support uploading MultipartFile via the Feign client</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class FeignMultipartSupportConfig {

    private final ObjectFactory<HttpMessageConverters> messageConverters;

    public FeignMultipartSupportConfig(ObjectFactory<HttpMessageConverters> messageConverters) {
        this.messageConverters = messageConverters;
    }

    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }
}
