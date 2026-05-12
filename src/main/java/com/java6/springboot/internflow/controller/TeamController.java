package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.AddTeamMemberRequest;
import com.java6.springboot.internflow.dto.request.TeamRequest;
import com.java6.springboot.internflow.dto.response.TeamResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.service.TeamService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ApiResponse<TeamResponse> createTeam(@RequestBody TeamRequest request) {
        return ApiResponse.ok("Tao nhom thanh cong", teamService.createTeam(request));
    }

    @PostMapping("/{teamId}/members")
    public ApiResponse<UserResponse> addMember(
            @PathVariable UUID teamId,
            @RequestBody AddTeamMemberRequest request
    ) {
        return ApiResponse.ok("Them thanh vien thanh cong", teamService.addMember(teamId, request));
    }

    @GetMapping("/{teamId}/members")
    public ApiResponse<List<UserResponse>> getMembers(@PathVariable UUID teamId) {
        return ApiResponse.ok("Lay thanh vien nhom thanh cong", teamService.getMembers(teamId));
    }
}
