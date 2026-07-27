package com.knowledge.base.search.filter;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.search.dto.TokenValidateVO;
import com.knowledge.base.search.feign.UserAuthFeignClient;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JWT authentication filter (dedicated to the search service)
 *
 * <p>Delegates JWT token validation to the kb-user-auth microservice via Feign, rather
 * than trusting the gateway-injected X-User-Id header alone. Previously this service
 * had no filter at all (Spring Security config was anyRequest().permitAll()) and every
 * controller read X-User-Id directly - a request that reached kb-search without going
 * through the gateway (or one that simply forged the header) was authenticated as
 * whatever user ID it claimed, with zero verification.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private UserAuthFeignClient userAuthFeignClient;

    @Value("${security.jwt.exclude-paths}")
    private String[] excludePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String bearerToken = request.getHeader("Authorization");
        boolean authenticated = false;
        boolean tokenPresented = bearerToken != null && bearerToken.startsWith("Bearer ");
        boolean tokenRejected = false;

        if (tokenPresented) {
            try {
                Result<TokenValidateVO> result = userAuthFeignClient.validateToken(bearerToken, null);
                TokenValidateVO validateVO = result != null ? result.getData() : null;

                if (validateVO != null && Boolean.TRUE.equals(validateVO.getValid())) {
                    log.debug("Token validation succeeded: userId={}, roles={}, uri={}",
                            validateVO.getUserId(), validateVO.getRoles(), requestUri);

                    setSecurityContext(validateVO, bearerToken, request);
                    UserContextUtil.setUserId(validateVO.getUserId());
                    if (validateVO.getUsername() != null) {
                        UserContextUtil.setUsername(validateVO.getUsername());
                    }
                    if (validateVO.getAvatar() != null) {
                        UserContextUtil.setAvatar(validateVO.getAvatar());
                    }
                    authenticated = true;
                } else {
                    log.debug("Token validation failed: token is invalid or expired, uri={}", requestUri);
                    tokenRejected = true;
                }
            } catch (Exception e) {
                log.error("Feign call to kb-user-auth to validate the token threw an exception: uri={}, error={}",
                        requestUri, e.getMessage());
                tokenRejected = true;
            }
        }

        // Fallback: trust the X-User-Id header set by the gateway, but only when the
        // client presented no bearer token at all. A presented token that kb-user-auth
        // explicitly rejected or that errored during validation must NOT fall through
        // to the header - kb-user-auth's verdict is authoritative.
        if (!authenticated && !tokenRejected) {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdHeader);
                    log.debug("Authenticating via the X-User-Id header: userId={}, uri={}", userId, requestUri);
                    UserContextUtil.setUserId(userId);
                    setDefaultSecurityContext(userId, bearerToken, request);
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-User-Id header format: {}", userIdHeader);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextUtil.clear();
        }
    }

    private void setSecurityContext(TokenValidateVO validateVO, String token, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<String> roles = validateVO.getRoles();
        if (roles != null) {
            for (String role : roles) {
                String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(validateVO.getUserId(), token, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setDefaultSecurityContext(Long userId, String token, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, token,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        for (String pattern : excludePaths) {
            if (pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }
}
