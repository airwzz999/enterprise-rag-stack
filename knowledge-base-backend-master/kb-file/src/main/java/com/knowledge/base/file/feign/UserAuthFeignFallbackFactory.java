package com.knowledge.base.file.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.file.dto.TokenValidateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * UserAuthFeignClient fallback factory
 *
 * <p>When the kb-user-auth service is unavailable, returns a default response with
 * valid=false rather than throwing, so the caller fails closed on an authoritative
 * "invalid" verdict instead of crashing.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class UserAuthFeignFallbackFactory implements FallbackFactory<UserAuthFeignClient> {

    @Override
    public UserAuthFeignClient create(Throwable cause) {
        log.error("UserAuthFeignClient call failed, activating fallback logic: {}", cause.getMessage());

        return (authorization, token) -> {
            log.warn("Token validation fallback: kb-user-auth service is unavailable, returning invalid token");
            return Result.success(TokenValidateVO.builder().valid(false).build());
        };
    }
}
