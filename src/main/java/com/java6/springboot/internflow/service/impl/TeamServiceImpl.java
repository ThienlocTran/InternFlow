package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.AddTeamMemberRequest;
import com.java6.springboot.internflow.dto.request.TeamRequest;
import com.java6.springboot.internflow.dto.response.TeamResponse;
import com.java6.springboot.internflow.dto.response.UserResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Team;
import com.java6.springboot.internflow.entity.TeamMember;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.TeamMemberRepository;
import com.java6.springboot.internflow.repository.TeamRepository;
import com.java6.springboot.internflow.service.TeamService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new BusinessException("Ten nhom la bat buoc");
        }
        if (request.leaderId() == null) {
            throw new BusinessException("Nhom truong la bat buoc");
        }
        if (teamRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Ten nhom da ton tai");
        }

        AppUser leader = findUser(request.leaderId());
        if (leader.getRole() != UserRole.TEAM_LEADER) {
            throw new BusinessException("Leader phai co role TEAM_LEADER");
        }

        Team team = Team.builder()
                .name(request.name().trim())
                .leader(leader)
                .build();

        Team savedTeam = teamRepository.save(team);
        if (!teamMemberRepository.existsByUserId(leader.getId())) {
            teamMemberRepository.save(TeamMember.builder()
                    .team(savedTeam)
                    .user(leader)
                    .build());
        }

        return TeamResponse.from(savedTeam);
    }

    @Override
    @Transactional
    public UserResponse addMember(UUID teamId, AddTeamMemberRequest request) {
        if (request == null || request.userId() == null) {
            throw new BusinessException("User id la bat buoc");
        }
        Team team = findTeam(teamId);
        AppUser user = findUser(request.userId());
        if (teamMemberRepository.existsByUserId(user.getId())) {
            throw new BusinessException("Sinh vien da thuoc mot nhom");
        }

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(user)
                .build());
        return UserResponse.from(user);
    }

    @Override
    public List<UserResponse> getMembers(UUID teamId) {
        findTeam(teamId);
        return teamMemberRepository.findByTeamId(teamId)
                .stream()
                .map(TeamMember::getUser)
                .map(UserResponse::from)
                .toList();
    }

    private Team findTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay nhom"));
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }
}
