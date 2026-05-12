package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.response.RolePolicyResponse;
import com.java6.springboot.internflow.service.RolePolicyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/role-policies")
@RequiredArgsConstructor
public class RolePolicyController {

    private final RolePolicyService rolePolicyService;

    @GetMapping
    public ApiResponse<List<RolePolicyResponse>> getPolicies() {
        return ApiResponse.ok("Lay cau hinh role thanh cong", rolePolicyService.getPolicies());
    }
}
