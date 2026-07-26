package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Team member Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    /**
     * Query all members by team ID
     */
    List<TeamMember> selectByTeamId(@Param("teamId") Long teamId);

    /**
     * Query the teams a user belongs to by user ID
     */
    List<TeamMember> selectByUserId(@Param("userId") Long userId);

    /**
     * Batch remove team members
     */
    int deleteByTeamIdAndUserIds(@Param("teamId") Long teamId, @Param("userIds") List<Long> userIds);

    /**
     * Count members in a team
     */
    Long countByTeamId(@Param("teamId") Long teamId);
}
