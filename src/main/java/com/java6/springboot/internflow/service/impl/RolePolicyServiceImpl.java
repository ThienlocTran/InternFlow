package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.response.RolePolicyResponse;
import com.java6.springboot.internflow.repository.RolePolicyRepository;
import com.java6.springboot.internflow.service.RolePolicyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RolePolicyServiceImpl implements RolePolicyService {

    private final RolePolicyRepository rolePolicyRepository;

    @Override
    public List<RolePolicyResponse> getPolicies() {
        return rolePolicyRepository.findAll()
                .stream()
                .map(RolePolicyResponse::from)
                .toList();
    }
}
