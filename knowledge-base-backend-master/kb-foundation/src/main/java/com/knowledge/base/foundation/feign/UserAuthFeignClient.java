package com.knowledge.base.foundation.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.dto.TokenValidateVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * User authentication Feign client
 *
 * <p>Calls the authentication endpoints of the kb-user-auth service via Feign, reusing
 * the user service's login and authentication logic. This avoids re-implementing JWT
 * parsing, blacklist checks, role lookups, etc. in every microservice.</p>
 *
 * <p>Endpoints called:</p>
 * <ul>
 *   <li>POST /auth/validate — validates the JWT token and returns the user's identity and role information</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-user-auth",
        url = "${kb-user-auth.url:#{null}}",
        path = "/auth",
        fallbackFactory = UserAuthFeignFallbackFactory.class
)
public interface UserAuthFeignClient {

    /**
     * Validate a JWT token
     *
     * <p>Sends the token to kb-user-auth for validation. kb-user-auth is responsible for:</p>
     * <ol>
     *   <li>Parsing the JWT token</li>
     *   <li>Checking the token blacklist (whether it has been logged out)</li>
     *   <li>Looking up the user information</li>
     *   <li>Looking up the user's role list</li>
     * </ol>
     *
     * @param authorization the Authorization request header (Bearer &lt;token&gt;)
     * @param token the token parameter (alternative delivery method)
     * @return the token validation result, including valid, userId, username, roles, etc.
     */
    @PostMapping("/validate")
    Result<TokenValidateVO> validateToken(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "token", required = false) String token);

    /**
     * Query the list of user IDs by role code
     *
     * <p>Calls kb-user-auth's /auth/users/by-role endpoint (MyBatis-backed) to query
     * all user IDs that hold the specified role.</p>
     *
     * @param roleCode the role code, e.g. ROLE_REVIEWER
     * @return the list of user IDs
     */
    @GetMapping("/users/by-role")
    Result<List<Long>> getUserIdsByRole(@RequestParam("roleCode") String roleCode);
}
