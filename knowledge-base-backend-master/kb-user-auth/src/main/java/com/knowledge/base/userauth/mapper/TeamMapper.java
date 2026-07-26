package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.Team;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Team Mapper interface
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides team data access operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

    /**
     * Query child teams by parent team ID
     */
    List<Team> selectByParentId(@Param("parentId") Long parentId);

    /**
     * Query a team by its code
     */
    Team selectByTeamCode(@Param("teamCode") String teamCode);

    /**
     * Query teams by status
     */
    List<Team> selectByStatus(@Param("status") Integer status);

    /**
     * Query teams by level
     */
    List<Team> selectByLevel(@Param("level") Integer level);

    /**
     * Query all root teams
     */
    List<Team> selectRootTeams();

    /**
     * Query teams by leader ID
     */
    List<Team> selectByLeaderId(@Param("leaderId") Long leaderId);

    /**
     * Query teams by path prefix (returns all descendant teams)
     */
    List<Team> selectByPathPrefix(@Param("path") String path);

    /**
     * Query the team tree
     */
    List<Team> selectTeamTree();

    /**
     * Update member count
     */
    int updateMemberCount(@Param("teamId") Long teamId, @Param("count") Integer count);

    /**
     * Increment member count
     */
    int incrementMemberCount(@Param("teamId") Long teamId);

    /**
     * Decrement member count
     */
    int decrementMemberCount(@Param("teamId") Long teamId);

    /**
     * Update document count
     */
    int updateDocumentCount(@Param("teamId") Long teamId, @Param("count") Integer count);

    /**
     * Increment document count
     */
    int incrementDocumentCount(@Param("teamId") Long teamId);

    /**
     * Decrement document count
     */
    int decrementDocumentCount(@Param("teamId") Long teamId);

    /**
     * Check whether a team code already exists
     */
    Boolean checkTeamCodeExists(@Param("teamCode") String teamCode, @Param("id") Long id);

    /**
     * Count all teams
     */
    Long countAll();

    /**
     * Count enabled teams
     */
    Long countEnabled();
}
