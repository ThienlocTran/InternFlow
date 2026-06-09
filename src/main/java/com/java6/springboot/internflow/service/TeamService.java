package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.AddTeamMemberRequest;
import com.java6.springboot.internflow.dto.request.TeamRequest;
import com.java6.springboot.internflow.dto.response.AdminShiftComplianceResponse;
import com.java6.springboot.internflow.dto.response.TeamResponse;
import com.java6.springboot.internflow.dto.response.ShiftPeerResponse;
import com.java6.springboot.internflow.dto.response.TeamMemberDetailResponse;
import com.java6.springboot.internflow.dto.response.TeamMemberFullDetailResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TeamService {

    TeamResponse createTeam(TeamRequest request);

    UserResponse addMember(UUID teamId, AddTeamMemberRequest request);

    List<UserResponse> getMembers(UUID teamId);

    List<ShiftPeerResponse> getLeaderShiftPeers(UUID leaderId, LocalDate date);

    AdminShiftComplianceResponse getLeaderShiftCompliance(UUID leaderId, LocalDate date, UUID shiftId);

    TeamMemberDetailResponse getMemberDetail(UUID leaderId, UUID memberId, LocalDate startDate, LocalDate endDate);

    TeamMemberFullDetailResponse getMemberFullDetail(UUID leaderId, UUID memberId, LocalDate date);
}
