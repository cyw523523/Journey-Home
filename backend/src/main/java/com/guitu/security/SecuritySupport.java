package com.guitu.security;

import com.guitu.domain.enums.UserRole;
import com.guitu.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecuritySupport {
    private SecuritySupport() {
    }

    public static SecurityPrincipal requireUser() {
        return currentUser().orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    public static Optional<SecurityPrincipal> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static boolean isAdmin() {
        return requireUser().role() == UserRole.ADMIN;
    }

    public static void requireOwnerOrAdmin(Long ownerId) {
        SecurityPrincipal principal = requireUser();
        if (!principal.id().equals(ownerId) && principal.role() != UserRole.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前账号无权限操作");
        }
    }
}
