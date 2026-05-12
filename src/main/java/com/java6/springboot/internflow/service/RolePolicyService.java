package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.response.RolePolicyResponse;
import java.util.List;

public interface RolePolicyService {

    List<RolePolicyResponse> getPolicies();
}
