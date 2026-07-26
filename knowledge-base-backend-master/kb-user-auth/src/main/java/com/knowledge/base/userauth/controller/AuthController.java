package com.knowledge.base.userauth.controller;

import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.userauth.dto.LoginDTO;
import com.knowledge.base.userauth.dto.RegisterDTO;
import com.knowledge.base.userauth.dto.ResetPasswordDTO;
import com.knowledge.base.userauth.dto.SendResetCodeDTO;
import com.knowledge.base.userauth.dto.VerifyResetCodeDTO;
import com.knowledge.base.userauth.service.UserService;
import com.knowledge.base.userauth.vo.LoginVO;
import com.knowledge.base.userauth.vo.RegisterVO;
import com.knowledge.base.userauth.vo.TokenValidateVO;
import com.knowledge.base.userauth.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Auth controller
 *
 * <p>Handles HTTP request receipt and parameter validation; business logic is delegated to {@link UserService}.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth Management", description = "User authentication endpoints")
public class AuthController {

    @Resource
    private UserService userService;

    /**
     * Token validation (for Feign calls from other microservices)
     *
     * @param authorization Authorization request header (Bearer token)
     * @param token         token from URL parameter (fallback)
     * @return token validation result
     */
    @PostMapping("/validate")
    @Operation(summary = "Token validation", description = "For Feign calls from other microservices; validates the JWT token and returns user identity and role information")
    public Result<TokenValidateVO> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String token) {
        return Result.success(userService.validateToken(authorization, token));
    }

    /**
     * Get the current logged-in user's information
     *
     * @return current user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Get detailed information about the currently logged-in user")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = userService.getCurrentUserInfo();
        return Result.success(userVO);
    }

    /**
     * User login
     *
     * @param loginDTO login request parameters
     * @return login response information
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Log in using a username and password")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("Login request: username={}", loginDTO.getUsername());
        LoginVO loginVO = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return Result.success(loginVO);
    }

    /**
     * User registration
     *
     * @param registerDTO registration request parameters
     * @return registration response (includes email verification status)
     */
    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new account; if an email is provided, verification is required to activate it")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.info("Registration request: username={}", registerDTO.getUsername());
        RegisterVO registerVO = userService.register(registerDTO);
        return Result.success(registerVO);
    }

    /**
     * Verify email to activate the account
     *
     * @param token activation token
     * @return activation result message
     */
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify the email and activate the account via the link sent in the activation email")
    public Result<String> verifyEmail(@RequestParam String token) {
        log.info("Email verification request: token={}", token);
        String message = userService.verifyEmail(token);
        return Result.success(message);
    }

    /**
     * User logout
     *
     * @param token access token
     * @return response result
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Log the user out")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        log.info("User logout");
        userService.logout(token);
        return Result.success();
    }

    /**
     * Refresh token
     *
     * @param refreshToken refresh token
     * @return new access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Obtain a new access token using the refresh token")
    public Result<LoginVO> refresh(@RequestParam String refreshToken) {
        log.info("Refresh token request");
        LoginVO loginVO = userService.refreshToken(refreshToken);
        return Result.success(loginVO);
    }

    /**
     * Query user IDs by role code (for Feign calls from microservices such as kb-foundation)
     *
     * @param roleCode role code, e.g. ROLE_REVIEWER
     * @return user ID list
     */
    @GetMapping("/users/by-role")
    @Operation(summary = "Query user IDs by role", description = "For Feign calls from other microservices; queries all user IDs that hold the given role")
    public Result<List<Long>> getUserIdsByRole(@RequestParam String roleCode) {
        return Result.success(userService.getUserIdsByRoleCode(roleCode));
    }

    /**
     * Query reviewer user IDs (without the Result wrapper, for Feign calls only)
     *
     * @return reviewer user ID list
     */
    @GetMapping("/reviewer-ids")
    @Operation(summary = "Query reviewer IDs", description = "For Feign calls from the kb-foundation microservice; returns the list of reviewer user IDs")
    public List<Long> getReviewerIds() {
        List<Long> userIds = userService.getUserIdsByRoleCode("ROLE_REVIEWER");
        log.info("Query reviewer IDs: count={}", userIds.size());
        return userIds;
    }

    // ==================== Password reset endpoints ====================

    /**
     * Send a password reset verification code
     *
     * @param dto request (includes the registered email)
     * @return send result
     */
    @PostMapping("/password/reset/send-code")
    @Operation(summary = "Send password reset code", description = "Send a 6-digit verification code to the registered email, valid for 10 minutes")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeDTO dto) {
        log.info("Send password reset code: email={}", dto.getEmail());
        userService.sendResetCode(dto.getEmail());
        return Result.success();
    }

    /**
     * Verify the password reset code
     *
     * @param dto request (includes email and verification code)
     * @return verification result
     */
    @PostMapping("/password/reset/verify-code")
    @Operation(summary = "Verify password reset code", description = "Verify whether the code matches the given email")
    public Result<Void> verifyResetCode(@Valid @RequestBody VerifyResetCodeDTO dto) {
        log.info("Verify password reset code: email={}", dto.getEmail());
        userService.verifyResetCode(dto.getEmail(), dto.getCode());
        return Result.success();
    }

    /**
     * Reset password
     *
     * @param dto request (includes email, verification code, and new password)
     * @return reset result
     */
    @PostMapping("/password/reset")
    @Operation(summary = "Reset password", description = "Reset the user's password using a verification code")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        log.info("Reset password: email={}", dto.getEmail());
        userService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
        return Result.success();
    }
}
