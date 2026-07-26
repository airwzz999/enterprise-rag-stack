package com.knowledge.base.userauth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.userauth.dto.TeamCreateDTO;
import com.knowledge.base.userauth.dto.TeamQueryDTO;
import com.knowledge.base.userauth.dto.TeamUpdateDTO;
import com.knowledge.base.userauth.entity.Team;
import com.knowledge.base.userauth.entity.TeamMember;
import com.knowledge.base.userauth.mapper.TeamMapper;
import com.knowledge.base.userauth.mapper.TeamMemberMapper;
import com.knowledge.base.userauth.service.TeamService;
import com.knowledge.base.userauth.vo.TeamMemberVO;
import com.knowledge.base.userauth.vo.TeamVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Team Service implementation
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private TeamMemberMapper teamMemberMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource(name = "caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** Caffeine local cache name */
    private static final String CAFFEINE_CACHE_NAME = "sidebar-teams";
    /** Redis cache key prefix */
    private static final String REDIS_TEAM_KEY_PREFIX = "sidebar:teams:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:teams", allEntries = true)
    public Long createTeam(TeamCreateDTO dto) {
        // Check whether the team code already exists
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Team::getTeamCode, dto.getTeamCode());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("Team code already exists");
        }

        // Build the team entity
        Team team = new Team();
        team.setId(SnowflakeIdGenerator.getInstance().nextId());
        team.setTeamName(dto.getTeamName());
        team.setTeamCode(dto.getTeamCode());
        team.setDescription(dto.getDescription());
        team.setIcon(dto.getIcon());

        // Handle the hierarchy relationship
        if (dto.getParentId() != null) {
            Team parentTeam = baseMapper.selectById(dto.getParentId());
            if (parentTeam == null) {
                throw new BusinessException("Parent team does not exist");
            }
            team.setParentId(dto.getParentId());
            team.setLevel(parentTeam.getLevel() + 1);
            team.setPath(parentTeam.getPath() + "/" + team.getId());
        } else {
            team.setLevel(1);
            team.setPath("/" + team.getId());
        }

        team.setLeaderId(dto.getLeaderId());
        team.setStatus(1);
        team.setMemberCount(0);
        team.setDocCount(0);

        baseMapper.insert(team);
        evictTeamCache();
        log.info("Team created successfully, team ID: {}", team.getId());
        return team.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:teams", allEntries = true)
    public Boolean updateTeam(TeamUpdateDTO dto) {
        Team team = baseMapper.selectById(dto.getId());
        if (team == null) {
            throw new BusinessException("Team does not exist");
        }

        // Check whether the team code is already taken
        if (StrUtil.isNotBlank(dto.getTeamCode()) && !dto.getTeamCode().equals(team.getTeamCode())) {
            LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Team::getTeamCode, dto.getTeamCode());
            queryWrapper.ne(Team::getId, dto.getId());
            if (baseMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("Team code is already taken");
            }
        }

        Team updateEntity = new Team();
        updateEntity.setId(dto.getId());
        if (StrUtil.isNotBlank(dto.getTeamName())) {
            updateEntity.setTeamName(dto.getTeamName());
        }
        if (StrUtil.isNotBlank(dto.getTeamCode())) {
            updateEntity.setTeamCode(dto.getTeamCode());
        }
        if (StrUtil.isNotBlank(dto.getDescription())) {
            updateEntity.setDescription(dto.getDescription());
        }
        if (StrUtil.isNotBlank(dto.getIcon())) {
            updateEntity.setIcon(dto.getIcon());
        }
        if (dto.getLeaderId() != null) {
            updateEntity.setLeaderId(dto.getLeaderId());
        }
        if (dto.getStatus() != null) {
            updateEntity.setStatus(dto.getStatus());
        }

        int result = baseMapper.updateById(updateEntity);
        evictTeamCache();
        log.info("Team updated successfully, team ID: {}", dto.getId());
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "sidebar:teams", allEntries = true)
    public Boolean deleteTeam(Long teamId) {
        // Check for child teams
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Team::getParentId, teamId);
        long childCount = baseMapper.selectCount(queryWrapper);
        if (childCount > 0) {
            throw new BusinessException("This team has child teams and cannot be deleted");
        }

        // Clean up team member associations
        LambdaQueryWrapper<TeamMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, teamId);
        teamMemberMapper.delete(memberWrapper);

        // Delete the team
        int result = baseMapper.deleteById(teamId);
        evictTeamCache();
        log.info("Team deleted successfully, team ID: {}", teamId);
        return result > 0;
    }

    @Override
    public TeamVO getTeamDetail(Long teamId) {
        Team team = baseMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("Team does not exist");
        }
        return convertToVO(team);
    }

    @Override
    public PageResult<TeamVO> pageTeams(TeamQueryDTO dto) {
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(dto.getTeamName())) {
            queryWrapper.like(Team::getTeamName, dto.getTeamName());
        }
        if (StrUtil.isNotBlank(dto.getTeamCode())) {
            queryWrapper.eq(Team::getTeamCode, dto.getTeamCode());
        }
        if (dto.getStatus() != null) {
            queryWrapper.eq(Team::getStatus, dto.getStatus());
        }

        // Sort by level and creation time
        queryWrapper.orderByAsc(Team::getLevel).orderByDesc(Team::getCreatedAt);

        Page<Team> page = new Page<>(dto.getCurrent(), dto.getSize());
        Page<Team> resultPage = baseMapper.selectPage(page, queryWrapper);

        List<TeamVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getTotal(),
                voList
        );
    }

    @Override
    public List<TeamVO> getTeamTree(boolean rootOnly) {
        String cacheKey = rootOnly ? "roots" : "tree";
        String redisKey = REDIS_TEAM_KEY_PREFIX + cacheKey;

        // 1. Check the Caffeine local cache (L1)
        Cache caffeineCache = caffeineCacheManager.getCache(CAFFEINE_CACHE_NAME);
        if (caffeineCache != null) {
            Cache.ValueWrapper wrapper = caffeineCache.get(cacheKey);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                List<TeamVO> cached = (List<TeamVO>) wrapper.get();
                if (cached != null && !cached.isEmpty()) {
                    log.debug("Team space tree hit the Caffeine local cache: key={}", cacheKey);
                    return cached;
                }
            }
        }

        // 2. Check the Redis cache (L2)
        try {
            @SuppressWarnings("unchecked")
            List<TeamVO> redisCached = (List<TeamVO>) redisTemplate.opsForValue().get(redisKey);
            if (redisCached != null && !redisCached.isEmpty()) {
                log.debug("Team space tree hit the Redis cache: key={}", redisKey);
                // Write back to the Caffeine local cache
                if (caffeineCache != null) {
                    caffeineCache.put(cacheKey, redisCached);
                }
                return redisCached;
            }
        } catch (Exception e) {
            log.warn("Failed to read the team space tree from the Redis cache: {}", e.getMessage());
        }

        // 3. Cache miss, query the database
        log.debug("Team space tree cache miss, querying the database");
        List<TeamVO> result = queryTeamTree(rootOnly);

        // 4. Write to both cache layers
        if (caffeineCache != null) {
            caffeineCache.put(cacheKey, result);
        }
        try {
            redisTemplate.opsForValue().set(redisKey, result, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to write the team space tree to the Redis cache: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Query the team space tree from the database (the original DB query logic of {@link #getTeamTree})
     */
    private List<TeamVO> queryTeamTree(boolean rootOnly) {
        LambdaQueryWrapper<Team> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Team::getStatus, 1);
        queryWrapper.orderByAsc(Team::getLevel).orderByAsc(Team::getCreatedAt);

        List<Team> allTeams = baseMapper.selectList(queryWrapper);

        if (rootOnly) {
            return allTeams.stream()
                    .filter(team -> team.getParentId() == null || team.getParentId() == 0L)
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        }

        return buildTeamTree(allTeams, null);
    }

    /**
     * Clear both cache layers (L1 + L2) for the team space tree
     *
     * <p>Called after team creation, update, or deletion to keep sidebar menu data current.</p>
     */
    private void evictTeamCache() {
        // Clear the Caffeine local cache (L1)
        Cache caffeineCache = caffeineCacheManager.getCache(CAFFEINE_CACHE_NAME);
        if (caffeineCache != null) {
            caffeineCache.evict("roots");
            caffeineCache.evict("tree");
            log.debug("Cleared the Caffeine team space tree cache");
        }
        // Clear the Redis cache (L2)
        try {
            redisTemplate.delete(REDIS_TEAM_KEY_PREFIX + "roots");
            redisTemplate.delete(REDIS_TEAM_KEY_PREFIX + "tree");
            log.debug("Cleared the Redis team space tree cache");
        } catch (Exception e) {
            log.warn("Failed to clear the Redis team space tree cache: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addTeamMembers(Long teamId, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return false;
        }

        Team team = baseMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("Team does not exist");
        }

        int addedCount = 0;
        for (Long userId : userIds) {
            // Check whether the user is already a member
            LambdaQueryWrapper<TeamMember> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(TeamMember::getTeamId, teamId)
                        .eq(TeamMember::getUserId, userId);
            if (teamMemberMapper.selectCount(existWrapper) > 0) {
                continue;
            }

            TeamMember member = new TeamMember();
            member.setId(SnowflakeIdGenerator.getInstance().nextId());
            member.setTeamId(teamId);
            member.setUserId(userId);
            member.setMemberRole("member");
            member.setJoinTime(LocalDateTime.now());
            teamMemberMapper.insert(member);
            addedCount++;
        }

        // Recalculate the member count
        Long actualCount = teamMemberMapper.countByTeamId(teamId);
        Team updateTeam = new Team();
        updateTeam.setId(teamId);
        updateTeam.setMemberCount(actualCount.intValue());
        baseMapper.updateById(updateTeam);

        log.info("Team members added successfully, team ID: {}, newly added: {}, current total: {}", teamId, addedCount, actualCount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeTeamMembers(Long teamId, List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return false;
        }

        Team team = baseMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("Team does not exist");
        }

        // Delete the member association records
        teamMemberMapper.deleteByTeamIdAndUserIds(teamId, userIds);

        // Recalculate the member count
        Long actualCount = teamMemberMapper.countByTeamId(teamId);
        Team updateTeam = new Team();
        updateTeam.setId(teamId);
        updateTeam.setMemberCount(actualCount.intValue());
        baseMapper.updateById(updateTeam);

        log.info("Team members removed successfully, team ID: {}, removed count: {}, current total: {}", teamId, userIds.size(), actualCount);
        return true;
    }

    @Override
    public List<TeamMemberVO> getTeamMembers(Long teamId) {
        Team team = baseMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException("Team does not exist");
        }

        List<TeamMember> members = teamMemberMapper.selectByTeamId(teamId);
        if (CollUtil.isEmpty(members)) {
            return new ArrayList<>();
        }

        // Batch query user information
        List<Long> userIds = members.stream()
                .map(TeamMember::getUserId)
                .collect(Collectors.toList());

        String userSql = "SELECT id, username, real_name, avatar FROM kb_user WHERE id IN ("
                + userIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                + ") AND deleted = 0";
        List<Map<String, Object>> userRows = jdbcTemplate.queryForList(userSql);

        Map<Long, Map<String, Object>> userMap = userRows.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("id")).longValue(),
                        row -> row
                ));

        return members.stream().map(member -> {
            Map<String, Object> userInfo = userMap.get(member.getUserId());
            String username = userInfo != null ? (String) userInfo.get("username") : "Unknown user";
            String realName = userInfo != null ? (String) userInfo.get("real_name") : null;
            String avatar = userInfo != null ? (String) userInfo.get("avatar") : null;

            return TeamMemberVO.builder()
                    .userId(member.getUserId())
                    .username(username)
                    .realName(realName)
                    .avatar(avatar)
                    .role(member.getMemberRole())
                    .joinedAt(member.getJoinTime())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Build the team tree
     */
    private List<TeamVO> buildTeamTree(List<Team> allTeams, Long parentId) {
        List<TeamVO> tree = new ArrayList<>();

        for (Team team : allTeams) {
            if ((parentId == null && team.getParentId() == null) ||
                    (parentId != null && parentId.equals(team.getParentId()))) {
                TeamVO vo = convertToVO(team);
                vo.setChildren(buildTeamTree(allTeams, team.getId()));
                tree.add(vo);
            }
        }

        return tree;
    }

    /**
     * Convert to VO
     */
    private TeamVO convertToVO(Team team) {
        TeamVO vo = new TeamVO();
        BeanUtil.copyProperties(team, vo);
        vo.setCreatedAt(team.getCreatedAt());

        // Set the parent team name
        if (team.getParentId() != null) {
            Team parentTeam = baseMapper.selectById(team.getParentId());
            if (parentTeam != null) {
                vo.setParentName(parentTeam.getTeamName());
            }
        }

        // Set the leader name
        if (team.getLeaderId() != null) {
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT username FROM kb_user WHERE id = ? AND deleted = 0", team.getLeaderId()
                );
                if (CollUtil.isNotEmpty(rows)) {
                    vo.setLeaderName((String) rows.get(0).get("username"));
                }
            } catch (Exception e) {
                log.warn("Failed to get team leader information: leaderId={}", team.getLeaderId(), e);
            }
        }

        return vo;
    }
}
