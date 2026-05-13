package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.UserProfileRequest;
import com.java6.springboot.internflow.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createProfile(UserProfileRequest request);

    UserResponse updateProfile(UUID id, UserProfileRequest request);

    UserResponse getById(UUID id);

    List<UserResponse> getAll();
}
