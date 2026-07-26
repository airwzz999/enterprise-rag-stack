package com.knowledge.base.userauth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.ResultCode;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.common.utils.JwtTokenUtil;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.userauth.dto.UserDTO;
import com.knowledge.base.userauth.entity.Role;
import com.knowledge.base.userauth.entity.User;
import com.knowledge.base.userauth.entity.UserRole;
import com.knowledge.base.userauth.mapper.RoleMapper;
import com.knowledge.base.userauth.mapper.UserMapper;
import com.knowledge.base.userauth.mapper.UserRoleMapper;
import com.knowledge.base.userauth.dto.RegisterDTO;
import com.knowledge.base.userauth.service.EmailService;
import com.knowledge.base.userauth.service.SecurityConfigService;
import com.knowledge.base.userauth.service.TeamService;
import com.knowledge.base.userauth.service.UserService;
import com.knowledge.base.userauth.util.VerificationTokenUtil;
import com.knowledge.base.userauth.vo.LoginVO;
import com.knowledge.base.userauth.vo.RegisterVO;
import com.knowledge.base.userauth.vo.TokenValidateVO;
import com.knowledge.base.userauth.vo.UserStatisticsVO;
import com.knowledge.base.userauth.vo.UserVO;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * User Service implementation class
 *
 * <p>Designed following the Alibaba Java Development Guidelines; implements user-related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SecurityConfigService securityConfigService;

    @Resource
    private EmailService emailService;

    @Resource
    private VerificationTokenUtil verificationTokenUtil;

    @Resource
    private TeamService teamService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginVO login(String username, String password) {
        log.info("User login: username={}", username);

        // Check whether the account is locked
        if (securityConfigService.isAccountLocked(username)) {
            int maxRetry = securityConfigService.getLoginMaxRetry();
            throw new BusinessException("Account is locked, please try again in 15 minutes (maximum " + maxRetry + " failed attempts allowed)");
        }

        // Query the user
        User user = getByUsername(username);
        if (user == null) {
            recordLoginFailureAndThrow(username, "Incorrect username or password");
            return null; // unreachable
        }

        // Check the user status
        checkUserStatus(user);

        // Verify the password
        if (!BCrypt.checkpw(password, user.getPassword())) {
            recordLoginFailureAndThrow(username, "Incorrect username or password");
            return null; // unreachable
        }

        // Login succeeded: clear the failure record
        securityConfigService.clearLoginFailure(username);

        // Update the last login information
        updateLastLoginInfo(user.getId());

        // Read the session timeout from the security configuration
        long sessionTimeout = securityConfigService.getSessionTimeout();

        // Generate the JWT token (using the session timeout from security configuration)
        String accessToken = jwtTokenUtil.generateAccessToken(user.getId(), user.getUsername(), user.getAvatar(), sessionTimeout);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getId());
        List<String> roles = getUserRoleCodes(user.getId());
        List<String> permissions = getUserPermissions(user.getId());

        // Build the login response
        LoginVO.UserInfo userInfo = LoginVO.UserInfo.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .nickname(user.getRealName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .avatar(user.getAvatar())
            .roles(roles)
            .permissions(permissions)
            .build();

        return LoginVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(sessionTimeout)
            .userInfo(userInfo)
            .build();
    }

    /**
     * Record a login failure and throw an exception including the number of remaining attempts
     */
    private void recordLoginFailureAndThrow(String username, String defaultMsg) {
        int remaining = securityConfigService.recordLoginFailure(username);
        if (remaining <= 0) {
            int maxRetry = securityConfigService.getLoginMaxRetry();
            throw new BusinessException("Incorrect username or password; the account has been locked for 15 minutes (maximum " + maxRetry + " failed attempts allowed)");
        }
        throw new BusinessException("Incorrect username or password, " + remaining + " attempt(s) remaining");
    }

    /**
     * Check the user status, distinguishing between an unverified email and an admin-disabled account
     */
    private void checkUserStatus(User user) {
        if (user.getStatus() != null && user.getStatus() == 0) {
            if ((user.getEmailVerified() == null || user.getEmailVerified() == 0)
                    && StringUtils.hasText(user.getActivationToken())) {
                throw new BusinessException("Account is not activated; please check your email and click the activation link to activate your account");
            }
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
    }

    /**
     * Update the user's last login information
     */
    private void updateLastLoginInfo(Long userId) {
        try {
            jdbcTemplate.update(
                "UPDATE kb_user SET last_login_time = NOW() WHERE id = ?",
                userId
            );
        } catch (Exception e) {
            log.warn("Failed to update the user's last login time: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        log.info("User logout");

        if (!StringUtils.hasText(token)) {
            log.warn("Logout failed: token is blank");
            return;
        }

        // Strip the Bearer prefix
        String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        // Parse the token to get its expiration time
        Date expireTime = null;
        try {
            Claims claims = jwtTokenUtil.parseToken(rawToken);
            if (claims != null) {
                expireTime = claims.getExpiration();
            }
        } catch (Exception e) {
            log.warn("Failed to parse the token, using the default expiration time: {}", e.getMessage());
        }

        // If the token cannot be parsed, default to an expiration time 24 hours from now
        if (expireTime == null) {
            expireTime = new Date(System.currentTimeMillis() + 24 * 3600 * 1000L);
        }

        // SHA-256 hash the token for storage
        String tokenHash = DigestUtil.sha256Hex(rawToken);

        // Add the token to the blacklist (using INSERT IGNORE to avoid errors from duplicate inserts)
        try {
            jdbcTemplate.update(
                "INSERT IGNORE INTO tb_token_blacklist (id, token_hash, expire_time, created_at) VALUES (?, ?, ?, NOW())",
                SnowflakeIdGenerator.getInstance().nextId(), tokenHash, expireTime
            );
            log.info("Token added to the blacklist, expiration time: {}", expireTime);
        } catch (Exception e) {
            log.error("Failed to insert into the token blacklist: {}", e.getMessage());
        }
    }

    /**
     * Scheduled cleanup of expired blacklisted tokens (runs every hour)
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanExpiredTokens() {
        try {
            int count = jdbcTemplate.update(
                "DELETE FROM tb_token_blacklist WHERE expire_time < NOW()"
            );
            if (count > 0) {
                log.info("Cleaned up expired blacklisted tokens: {} record(s)", count);
            }
        } catch (Exception e) {
            log.error("Failed to clean up expired blacklisted tokens: {}", e.getMessage());
        }
    }

    @Override
    public boolean isTokenBlacklisted(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return false;
        }
        String tokenHash = DigestUtil.sha256Hex(rawToken);
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tb_token_blacklist WHERE token_hash = ? AND expire_time > NOW()",
                    Integer.class, tokenHash);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Error while querying the token blacklist: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO userDTO) {
        log.info("Create user: username={}, realName={}, department={}, position={}",
                userDTO.getUsername(), userDTO.getRealName(), userDTO.getDepartment(), userDTO.getPosition());

        // Check whether the username already exists
        User existUser = getByUsername(userDTO.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
        }

        // Check whether the email already exists
        if (StringUtils.hasText(userDTO.getEmail())) {
            existUser = userMapper.selectByEmail(userDTO.getEmail());
            if (existUser != null) {
                throw new BusinessException("Email is already in use");
            }
        }

        // Check whether the phone number already exists
        if (StringUtils.hasText(userDTO.getPhone())) {
            existUser = userMapper.selectByPhone(userDTO.getPhone());
            if (existUser != null) {
                throw new BusinessException("Phone number is already in use");
            }
        }

        // Build the user entity
        User user = new User();
        BeanUtil.copyProperties(userDTO, user);
        log.debug("After BeanUtil.copyProperties: realName={}, department={}, position={}",
                user.getRealName(), user.getDepartment(), user.getPosition());

        // Generate the ID
        user.setId(SnowflakeIdGenerator.getInstance().nextId());

        // Encrypt the password
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(BCrypt.hashpw(userDTO.getPassword()));
        }

        // Set default values
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        // Save the user
        int count = userMapper.insert(user);
        if (count <= 0) {
            throw new BusinessException("Failed to create user");
        }

        // Assign the default role (ROLE_USER / knowledge base member)
        assignDefaultRole(user.getId());

        log.info("User created successfully: id={}", user.getId());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(UserDTO userDTO) {
        log.info("Update user: userId={}, realName={}, department={}, position={}",
                userDTO.getId(), userDTO.getRealName(), userDTO.getDepartment(), userDTO.getPosition());

        if (userDTO.getId() == null) {
            throw new BusinessException("User ID must not be null");
        }

        // Check whether the user exists
        User existUser = userMapper.selectById(userDTO.getId());
        if (existUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // Check whether the username is used by another user
        if (StringUtils.hasText(userDTO.getUsername())
            && !userDTO.getUsername().equals(existUser.getUsername())) {
            User user = getByUsername(userDTO.getUsername());
            if (user != null && !user.getId().equals(userDTO.getId())) {
                throw new BusinessException("Username is already in use");
            }
        }

        // Build the update entity
        User user = new User();
        BeanUtil.copyProperties(userDTO, user);
        log.debug("After BeanUtil.copyProperties: realName={}, department={}, position={}",
                user.getRealName(), user.getDepartment(), user.getPosition());

        // Encrypt the new password if one was provided
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(BCrypt.hashpw(userDTO.getPassword()));
        }

        int count = userMapper.updateById(user);
        log.info("Update result: count={}", count);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long userId) {
        log.info("Delete user: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        int count = userMapper.deleteById(userId);
        return count > 0;
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        return buildUserVO(user, true);
    }

    @Override
    public IPage<UserVO> pageUsers(Long current, Long size, String keyword, String role, Integer status) {
        // Build the query conditions
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getRealName, keyword)
                .or()
                .like(User::getEmail, keyword)
                .or()
                .like(User::getPhone, keyword);
        }

        // Filter by status
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        List<Long> filteredUserIds = resolveUserIdsByRoleFilter(role);
        if (filteredUserIds != null) {
            if (filteredUserIds.isEmpty()) {
                return new Page<>(current, size);
            }
            wrapper.in(User::getId, filteredUserIds);
        }

        // Sort by creation time descending
        wrapper.orderByDesc(User::getCreatedAt);

        // Paginated query
        Page<User> page = new Page<>(current, size);
        IPage<User> userPage = userMapper.selectPage(page, wrapper);

        // Convert to VO
        return userPage.convert(user -> buildUserVO(user, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resetPassword(Long userId, String newPassword) {
        log.info("Reset user password: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException("New password must not be blank");
        }

        User user = new User();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(newPassword));

        int count = userMapper.updateById(user);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changePassword(String oldPassword, String newPassword) {
        log.info("Change password");

        // Get the current logged-in user from the context
        Long userId = UserContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // Verify the old password
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("The old password is incorrect");
        }

        // Update the password
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(newPassword));

        int count = userMapper.updateById(updateUser);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO registerDTO) {
        log.info("User registration: username={}", registerDTO.getUsername());

        // Check whether registration is open
        if (!isRegistrationAllowed()) {
            throw new BusinessException("Registration is currently closed");
        }

        // Verify the two password entries match
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("The two password entries do not match");
        }

        // Validate the password policy
        securityConfigService.validatePassword(registerDTO.getPassword());

        // Check whether the username already exists
        User existUser = getByUsername(registerDTO.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXIST);
        }

        // Check whether the email already exists
        if (org.springframework.util.StringUtils.hasText(registerDTO.getEmail())) {
            existUser = userMapper.selectByEmail(registerDTO.getEmail());
            if (existUser != null) {
                throw new BusinessException("This email is already in use");
            }
        }

        // Check whether the phone number already exists
        if (org.springframework.util.StringUtils.hasText(registerDTO.getPhone())) {
            existUser = userMapper.selectByPhone(registerDTO.getPhone());
            if (existUser != null) {
                throw new BusinessException("This phone number is already in use");
            }
        }

        // Create the user (email is required, so verification is needed for activation)
        User user = new User();
        user.setId(SnowflakeIdGenerator.getInstance().nextId());
        user.setUsername(registerDTO.getUsername());
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(0);
        user.setEmailVerified(0);
        String activationToken = verificationTokenUtil.generateToken();
        user.setActivationToken(activationToken);
        user.setActivationTokenExpiry(verificationTokenUtil.calculateExpiryTime());

        int count = userMapper.insert(user);
        if (count <= 0) {
            throw new BusinessException("Registration failed, please try again later");
        }

        // Assign the default role (ROLE_USER)
        assignDefaultRole(user.getId());

        // Send the activation email (roll back the registration data on failure)
        try {
            emailService.sendActivationEmail(registerDTO.getEmail(),
                    registerDTO.getUsername(), user.getActivationToken());
            log.info("Activation email sent: email={}, userId={}", registerDTO.getEmail(), user.getId());
        } catch (Exception e) {
            log.error("Failed to send the activation email: email={}, error={}", registerDTO.getEmail(), e.getMessage());
            throw new BusinessException("Failed to send the activation email; registration was not completed, please try again later");
        }

        // Team assignment runs after the registration transaction commits, to avoid an internal
        // exception rolling back the transaction
        Long userId = user.getId();
        Long teamId = registerDTO.getTeamId();
        if (teamId != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        teamService.addTeamMembers(teamId, List.of(userId));
                        log.info("User has joined the team: userId={}, teamId={}", userId, teamId);
                    } catch (Exception e) {
                        log.warn("Failed to add user to team (does not affect registration): userId={}, teamId={}, error={}",
                                userId, teamId, e.getMessage());
                    }
                }
            });
        }

        return RegisterVO.builder()
                .userId(userId)
                .emailVerificationRequired(true)
                .message("Registration successful! An activation email has been sent to " + registerDTO.getEmail() + ". Please click the link in the email within 24 hours to activate your account")
                .build();
    }

    /**
     * Check whether registration is open
     */
    private boolean isRegistrationAllowed() {
        String value = securityConfigService.getConfig("user.registration.enabled");
        return value == null || "true".equals(value);
    }

    /**
     * Assign the default role (ROLE_USER) to a new user
     */
    private void assignDefaultRole(Long userId) {
        try {
            // Query the ROLE_USER role
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                            .eq(Role::getRoleCode, "ROLE_USER")
                            .eq(Role::getDeleted, 0)
                            .last("LIMIT 1"));
            if (role != null) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(role.getId());
                userRoleMapper.insert(userRole);
                log.info("Default role assigned to user: userId={}, roleId={}", userId, role.getId());
            } else {
                log.warn("ROLE_USER role not found; skipping default role assignment");
            }
        } catch (Exception e) {
            log.error("Failed to assign the default role: {}", e.getMessage());
        }
    }

    @Override
    public UserVO getCurrentUserInfo() {
        log.info("Get current logged-in user's information");

        // Get the current logged-in user ID from the context
        Long userId = UserContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // Query the user information
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // Check the user status (distinguishing between unverified email and admin-disabled)
        checkUserStatus(user);

        // Convert to VO and return
        UserVO userVO = buildUserVO(user, true);

        log.info("Successfully retrieved user information: userId={}, username={}", userVO.getId(), userVO.getUsername());
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRoles(Long userId, List<Long> roleIds) {
        log.info("Assign roles to user: userId={}, roleIds={}", userId, roleIds);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        // Check whether the user exists
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // Delete the existing role associations
        jdbcTemplate.update("DELETE FROM kb_user_role WHERE user_id = ?", userId);

        // Batch insert the role associations
        if (roleIds != null && !roleIds.isEmpty()) {
            String sql = "INSERT INTO kb_user_role (id, user_id, role_id, created_at) VALUES (?, ?, ?, NOW())";
            for (Long roleId : roleIds) {
                // Check whether the role exists
                Role role = roleMapper.selectById(roleId);
                if (role == null) {
                    throw new BusinessException("Role does not exist: " + roleId);
                }
                jdbcTemplate.update(sql, SnowflakeIdGenerator.getInstance().nextId(), userId, roleId);
            }
        }

        log.info("Roles assigned successfully: userId={}", userId);
        return true;
    }

    @Override
    public List<Long> getUserRoles(Long userId) {
        log.info("Get user roles: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        return jdbcTemplate.queryForList(
                "SELECT role_id FROM kb_user_role WHERE user_id = ?",
                Long.class,
                userId
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignPermissions(Long userId, List<Long> permissionIds) {
        log.info("Assign permissions to user: userId={}, permissionCount={}", userId, permissionIds != null ? permissionIds.size() : 0);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        // Check whether the user exists
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // Delete the existing directly assigned permissions
        jdbcTemplate.update("DELETE FROM kb_user_permission WHERE user_id = ?", userId);

        // Batch insert the permission associations
        if (permissionIds != null && !permissionIds.isEmpty()) {
            String sql = "INSERT INTO kb_user_permission (id, user_id, permission_id, created_at) VALUES (?, ?, ?, NOW())";
            for (Long permissionId : permissionIds) {
                jdbcTemplate.update(sql, SnowflakeIdGenerator.getInstance().nextId(), userId, permissionId);
            }
        }

        log.info("Permissions assigned successfully: userId={}", userId);
        return true;
    }

    private static final List<String> ADMIN_OPERATION_PERMISSIONS = List.of(
            "document:list", "document:create", "document:edit", "document:delete",
            "document:review", "document:category", "document:category:query",
            "document:tag", "document:version",
            "system:user", "system:role", "system:permission",
            "system:permission:create", "system:permission:edit", "system:permission:delete",
            "system:team", "system:statistics", "system:settings"
    );

    private static final List<String> ADMIN_ROLES = List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN");

    public List<String> getUserPermissions(Long userId) {
        log.info("Get user permissions: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        // Query the user's directly assigned permissions
        List<String> directPermissions = jdbcTemplate.queryForList(
                "SELECT p.permission_code FROM kb_user_permission up " +
                        "JOIN kb_permission p ON up.permission_id = p.id " +
                        "WHERE up.user_id = ? AND p.status = 1",
                String.class,
                userId
        );

        // Query the permissions the user obtains through roles
        List<String> rolePermissions = jdbcTemplate.queryForList(
                "SELECT DISTINCT p.permission_code FROM kb_user_role ur " +
                        "JOIN kb_role_permission rp ON ur.role_id = rp.role_id " +
                        "JOIN kb_permission p ON rp.permission_id = p.id " +
                        "WHERE ur.user_id = ? AND p.status = 1",
                String.class,
                userId
        );

        // Merge permissions and deduplicate
        List<String> allPermissions = Stream.concat(
                directPermissions.stream(),
                rolePermissions.stream()
        ).distinct().collect(Collectors.toList());

        // Admins/super admins automatically get all operation permissions
        List<String> userRoles = getUserRoleCodes(userId);
        boolean isAdmin = userRoles.stream().anyMatch(ADMIN_ROLES::contains);
        if (isAdmin) {
            for (String perm : ADMIN_OPERATION_PERMISSIONS) {
                if (!allPermissions.contains(perm)) {
                    allPermissions.add(perm);
                }
            }
        }

        log.info("User permissions retrieved successfully: userId={}, permissionCount={}, isAdmin={}", userId, allPermissions.size(), isAdmin);
        return allPermissions;
    }

    private UserVO buildUserVO(User user, boolean includePermissions) {
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        List<String> roleCodes = getUserRoleCodes(user.getId());
        userVO.setRoles(roleCodes);
        userVO.setRole(resolvePrimaryRole(roleCodes));
        if (includePermissions) {
            userVO.setPermissions(getUserPermissions(user.getId()));
        } else {
            userVO.setPermissions(new ArrayList<>());
        }
        return userVO;
    }

    private List<String> getUserRoleCodes(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    private String resolvePrimaryRole(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return null;
        }
        return mapRoleCodeToFrontendRole(roleCodes.get(0));
    }

    private String mapRoleCodeToFrontendRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return roleCode;
        }
        return switch (roleCode) {
            case "ROLE_SUPER_ADMIN" -> "SUPER_ADMIN";
            case "ROLE_ADMIN" -> "KNOWLEDGE_ADMIN";
            case "ROLE_EDITOR" -> "EDITOR";
            case "ROLE_REVIEWER" -> "REVIEWER";
            case "ROLE_USER", "ROLE_GUEST" -> "VIEWER";
            default -> roleCode.startsWith("ROLE_") ? roleCode.substring(5) : roleCode;
        };
    }

    private List<Long> resolveUserIdsByRoleFilter(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        List<String> roleCodes = new ArrayList<>();
        roleCodes.add(role);
        if (!role.startsWith("ROLE_")) {
            roleCodes.add("ROLE_" + role);
        }
        switch (role) {
            case "SUPER_ADMIN" -> roleCodes.add("ROLE_SUPER_ADMIN");
            case "KNOWLEDGE_ADMIN" -> roleCodes.add("ROLE_ADMIN");
            case "EDITOR" -> roleCodes.add("ROLE_EDITOR");
            case "REVIEWER" -> roleCodes.add("ROLE_REVIEWER");
            case "VIEWER" -> {
                roleCodes.add("ROLE_USER");
                roleCodes.add("ROLE_GUEST");
            }
            default -> {
            }
        }
        String placeholders = roleCodes.stream().map(item -> "?").collect(Collectors.joining(","));
        String sql = "SELECT DISTINCT ur.user_id FROM kb_user_role ur " +
                "INNER JOIN kb_role r ON ur.role_id = r.id " +
                "WHERE r.deleted = 0 AND r.role_code IN (" + placeholders + ")";
        return jdbcTemplate.queryForList(sql, Long.class, roleCodes.toArray());
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException("Refresh token must not be blank");
        }

        // Parse the refresh token
        Claims claims = jwtTokenUtil.parseToken(refreshToken);
        if (claims == null) {
            throw new BusinessException("Refresh token is invalid or expired, please log in again");
        }

        Long userId = Long.parseLong(claims.getSubject());

        // Check whether the refresh token is blacklisted
        if (isTokenBlacklisted(refreshToken)) {
            throw new BusinessException("Refresh token has expired, please log in again");
        }

        // Verify the user exists and is in normal status
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }
        if (user.getStatus() == 0) {
            checkUserStatus(user);
        }

        // Read the session timeout from the security configuration
        long sessionTimeout = securityConfigService.getSessionTimeout();

        // Generate new access and refresh tokens (token rotation)
        String newAccessToken = jwtTokenUtil.generateAccessToken(user.getId(), user.getUsername(), user.getAvatar(), sessionTimeout);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(user.getId());

        // Get the user's roles and permissions
        List<String> roles = getUserRoleCodes(user.getId());
        List<String> permissions = getUserPermissions(user.getId());

        // Build the response
        LoginVO.UserInfo userInfo = LoginVO.UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .roles(roles)
                .permissions(permissions)
                .build();

        log.info("Token refreshed successfully: userId={}, username={}", user.getId(), user.getUsername());

        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(sessionTimeout)
                .userInfo(userInfo)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String verifyEmail(String token) {
        log.info("Email verification: token={}", token);

        if (!StringUtils.hasText(token)) {
            throw new BusinessException("Activation token must not be blank");
        }

        // Find the user by the token
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getActivationToken, token));

        if (user == null) {
            throw new BusinessException("Invalid activation link");
        }

        // Check whether the token has expired
        if (verificationTokenUtil.isTokenExpired(user.getActivationTokenExpiry())) {
            throw new BusinessException("The activation link has expired, please register again");
        }

        // Check whether the email is already verified
        if (user.getEmailVerified() != null && user.getEmailVerified() == 1) {
            return "Account is already activated, please log in directly";
        }

        // Activate the account
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmailVerified(1);
        updateUser.setStatus(1);
        updateUser.setActivationToken(null);
        updateUser.setActivationTokenExpiry(null);

        userMapper.updateById(updateUser);
        log.info("Email verified successfully: userId={}, email={}", user.getId(), user.getEmail());

        return "Account activated successfully, please log in";
    }

    // ==================== Password reset related methods ====================

    private static final String RESET_CODE_PREFIX = "password:reset:code:";
    private static final long RESET_CODE_EXPIRE_MINUTES = 10;
    private static final int RESET_CODE_LENGTH = 6;

    @Override
    public void sendResetCode(String email) {
        // Check whether the email is registered
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException("This email is not registered");
        }

        // Check whether it is within the cooldown period (resending is not allowed within 60 seconds)
        String redisKey = RESET_CODE_PREFIX + email;
        String existingCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (existingCode != null) {
            Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > (RESET_CODE_EXPIRE_MINUTES * 60 - 60)) {
                throw new BusinessException("A verification code has already been sent, please try again in " + ttl + " seconds");
            }
        }

        // Generate a 6-digit numeric verification code
        String code = generateResetCode();

        // Store it in Redis (valid for 10 minutes)
        stringRedisTemplate.opsForValue().set(redisKey, code, RESET_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // Send the verification code email
        try {
            emailService.sendResetCodeEmail(email, code);
            log.info("Password reset verification code sent: email={}", email);
        } catch (Exception e) {
            // Remove the code from Redis if sending failed
            stringRedisTemplate.delete(redisKey);
            log.error("Failed to send the password reset verification code email: email={}, error={}", email, e.getMessage());
            throw new BusinessException("Failed to send the email, please try again later");
        }
    }

    @Override
    public boolean verifyResetCode(String email, String code) {
        String redisKey = RESET_CODE_PREFIX + email;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new BusinessException("The verification code has expired, please request a new one");
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException("Incorrect verification code");
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String code, String newPassword) {
        // Verify the code first
        String redisKey = RESET_CODE_PREFIX + email;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new BusinessException("The verification code has expired, please request a new one");
        }

        if (!storedCode.equals(code)) {
            throw new BusinessException("Incorrect verification code");
        }

        // Validate the password policy
        securityConfigService.validatePassword(newPassword);

        // Find the user
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException("User does not exist");
        }

        // Update the password
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(updateUser);

        // Delete the verification code to prevent reuse
        stringRedisTemplate.delete(redisKey);

        log.info("Password reset successfully: email={}, userId={}", email, user.getId());
    }

    /**
     * Generate a 6-digit numeric verification code
     */
    private String generateResetCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    @Override
    public UserStatisticsVO getUserStatistics(Long userId) {
        log.info("Get user statistics: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be null");
        }

        // Check whether the user exists
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        Long docCount = userMapper.countDocumentsByAuthorId(userId);
        Long likeCount = userMapper.sumLikesByAuthorId(userId);
        Long viewCount = userMapper.sumViewsByAuthorId(userId);

        return UserStatisticsVO.builder()
                .documentCount(docCount != null ? docCount : 0L)
                .viewCount(viewCount != null ? viewCount : 0L)
                .likeCount(likeCount != null ? likeCount : 0L)
                .commentCount(0L)
                .build();
    }

    @Override
    public TokenValidateVO validateToken(String authorization, String token) {
        // Prefer extracting the token from the request header, otherwise use the parameter
        String jwtToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            jwtToken = authorization.substring(7);
        } else if (token != null && !token.isEmpty()) {
            jwtToken = token;
        }

        if (jwtToken == null || jwtToken.isEmpty()) {
            return TokenValidateVO.builder().valid(false).build();
        }

        try {
            Long userId = jwtTokenUtil.getUserIdFromToken(jwtToken);
            if (userId == null) {
                return TokenValidateVO.builder().valid(false).build();
            }

            if (isTokenBlacklisted(jwtToken)) {
                log.warn("Token validation failed: token has been logged out, userId={}", userId);
                return TokenValidateVO.builder().valid(false).build();
            }

            UserVO user = getUserById(userId);
            if (user == null || user.getStatus() != null && user.getStatus() != 1) {
                return TokenValidateVO.builder().valid(false).build();
            }

            List<String> roles = roleMapper.selectRoleCodesByUserId(userId);

            return TokenValidateVO.builder()
                    .userId(userId)
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .email(user.getEmail())
                    .status(user.getStatus())
                    .roles(roles)
                    .valid(true)
                    .build();
        } catch (Exception e) {
            log.warn("Token validation error: {}", e.getMessage());
            return TokenValidateVO.builder().valid(false).build();
        }
    }

    @Override
    public List<Long> getUserIdsByRoleCode(String roleCode) {
        try {
            return roleMapper.selectUserIdsByRoleCode(roleCode);
        } catch (Exception e) {
            log.error("Failed to query users by role: roleCode={}, error={}", roleCode, e.getMessage());
            return List.of();
        }
    }
}
