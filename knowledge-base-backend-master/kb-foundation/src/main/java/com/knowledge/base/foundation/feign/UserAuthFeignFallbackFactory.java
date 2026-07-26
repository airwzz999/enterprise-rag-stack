package com.knowledge.base.foundation.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.dto.TokenValidateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * UserAuthFeignClient fallback factory
 *
 * <p>When the kb-user-auth service is unavailable, returns a default response with
 * valid=false, ensuring the foundation service does not become entirely unavailable
 * due to a user service outage.</p>
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

        return new UserAuthFeignClient() {
            @Override
            public Result<TokenValidateVO> validateToken(String authorization, String token) {
                log.warn("Token validation fallback: kb-user-auth service is unavailable, returning invalid token");
                return Result.success(
                        TokenValidateVO.builder().valid(false).build());
            }

            @Override
            public Result<List<Long>> getUserIdsByRole(String roleCode) {
                log.warn("Role-based user query fallback: kb-user-auth service is unavailable, returning empty list");
                return Result.success(List.of());
            }
        };
    }
}
