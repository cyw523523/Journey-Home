package com.guitu.security;

import com.guitu.domain.enums.UserRole;

public record SecurityPrincipal(Long id, String account, UserRole role) {
}
