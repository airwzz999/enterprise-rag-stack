package com.knowledge.base.userauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.userauth.dto.TeamCreateDTO;
import com.knowledge.base.userauth.dto.TeamQueryDTO;
import com.knowledge.base.userauth.dto.TeamUpdateDTO;
import com.knowledge.base.userauth.entity.Team;
import com.knowledge.base.userauth.vo.TeamMemberVO;
import com.knowledge.base.userauth.vo.TeamVO;

import java.util.List;

/**
 * Team Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface TeamService extends IService<Team> {

    /**
     * Create a team
     *
     * @param dto creation DTO
     * @return team ID
     */
    Long createTeam(TeamCreateDTO dto);

    /**
     * Update a team
     *
     * @param dto update DTO
     * @return whether successful
     */
    Boolean updateTeam(TeamUpdateDTO dto);

    /**
     * Delete a team
     *
     * @param teamId team ID
     * @return whether successful
     */
    Boolean deleteTeam(Long teamId);

    /**
     * Get team details
     *
     * @param teamId team ID
     * @return team VO
     */
    TeamVO getTeamDetail(Long teamId);

    /**
     * Paginated team query
     *
     * @param dto query DTO
     * @return paginated result
     */
    PageResult<TeamVO> pageTeams(TeamQueryDTO dto);

    /**
     * Get the team tree
     *
     * @param rootOnly whether to return only root teams (top-level teams)
     * @return team tree
     */
    List<TeamVO> getTeamTree(boolean rootOnly);

    /**
     * Add team members
     *
     * @param teamId  team ID
     * @param userIds user ID list
     * @return whether successful
     */
    Boolean addTeamMembers(Long teamId, List<Long> userIds);

    /**
     * Remove team members
     *
     * @param teamId team ID
     * @param userIds user ID list
     * @return whether successful
     */
    Boolean removeTeamMembers(Long teamId, List<Long> userIds);

    /**
     * Get team members
     *
     * @param teamId team ID
     * @return team member list (includes user information)
     */
    List<TeamMemberVO> getTeamMembers(Long teamId);
}
