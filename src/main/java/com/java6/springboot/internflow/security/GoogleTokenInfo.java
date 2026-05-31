package com.java6.springboot.internflow.security;

public record GoogleTokenInfo(
        String email,
        String name,
        String audience,
        boolean emailVerified
) {
}
