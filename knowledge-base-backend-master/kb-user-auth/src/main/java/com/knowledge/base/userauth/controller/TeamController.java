package com.knowledge.base.userauth.controller;

import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.userauth.dto.TeamCreateDTO;
import com.knowledge.base.userauth.dto.TeamQueryDTO;
import com.knowledge.base.userauth.dto.TeamUpdateDTO;
import com.knowledge.base.userauth.service.TeamService;
import com.knowledge.base.userauth.vo.TeamMemberVO;
import com.knowledge.base.userauth.vo.TeamVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Team management controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/teams")
@RequiredArgsConstructor
@Tag(name = "Team Management", description = "Team management endpoints")
public class TeamController {

    private final TeamService teamService;

    /**
     * Create a team
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create team", description = "Create a new team")
    @OperationLog(module = "Team Management", operation = "Create Team", description = "Create a new team")
    public Result<Long> createTeam(@Valid @RequestBody TeamCreateDTO dto) {
        Long teamId = teamService.createTeam(dto);
        return Result.success(teamId);
    }

    /**
     * Update a team
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update team", description = "Update team information")
    @OperationLog(module = "Team Management", operation = "Update Team", description = "Update team information")
    public Result<Boolean> updateTeam(@Valid @RequestBody TeamUpdateDTO dto) {
        Boolean result = teamService.updateTeam(dto);
        return Result.success(result);
    }

    /**
     * Delete a team
     */
    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete team", description = "Delete the specified team")
    @OperationLog(module = "Team Management", operation = "Delete Team", description = "Delete a team")
    public Result<Boolean> deleteTeam(@PathVariable Long teamId) {
        Boolean result = teamService.deleteTeam(teamId);
        return Result.success(result);
    }

    /**
     * Get team details
     */
    @GetMapping("/{teamId}")
    @Operation(summary = "Get team details", description = "Get team details by ID")
    public Result<TeamVO> getTeamDetail(@PathVariable Long teamId) {
        TeamVO teamVO = teamService.getTeamDetail(teamId);
        return Result.success(teamVO);
    }

    /**
     * Paginated team query
     */
    @PostMapping("/page")
    @Operation(summary = "Paginated team query", description = "Query the team list with pagination")
    public Result<PageResult<TeamVO>> pageTeams(@RequestBody TeamQueryDTO dto) {
        PageResult<TeamVO> pageResult = teamService.pageTeams(dto);
        return Result.success(pageResult);
    }

    /**
     * Get the team tree
     */
    @GetMapping("/tree")
    @Operation(summary = "Get team tree", description = "Get the full team tree structure; when rootOnly=true only top-level teams are returned")
    public Result<List<TeamVO>> getTeamTree(@RequestParam(required = false, defaultValue = "false") boolean rootOnly) {
        List<TeamVO> teamTree = teamService.getTeamTree(rootOnly);
        return Result.success(teamTree);
    }

    /**
     * Add team members
     */
    @PostMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add team members", description = "Batch add team members")
    @OperationLog(module = "Team Management", operation = "Add Members", description = "Add team members")
    public Result<Boolean> addTeamMembers(
            @PathVariable Long teamId,
            @RequestBody List<Long> userIds) {
        Boolean result = teamService.addTeamMembers(teamId, userIds);
        return Result.success(result);
    }

    /**
     * Remove team members
     */
    @DeleteMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Remove team members", description = "Batch remove team members")
    @OperationLog(module = "Team Management", operation = "Remove Members", description = "Remove team members")
    public Result<Boolean> removeTeamMembers(
            @PathVariable Long teamId,
            @RequestBody List<Long> userIds) {
        Boolean result = teamService.removeTeamMembers(teamId, userIds);
        return Result.success(result);
    }

    /**
     * Get team members
     */
    @GetMapping("/{teamId}/members")
    @Operation(summary = "Get team members", description = "Get the team member list")
    public Result<List<TeamMemberVO>> getTeamMembers(@PathVariable Long teamId) {
        List<TeamMemberVO> members = teamService.getTeamMembers(teamId);
        return Result.success(members);
    }
}
