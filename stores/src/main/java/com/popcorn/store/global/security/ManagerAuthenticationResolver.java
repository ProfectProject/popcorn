package com.popcorn.store.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.popcorn.common.filter.PassportPrincipal;
import com.popcorn.store.domain.popup.exception.manager.ManagerPopupException;
import com.popcorn.store.domain.users.entity.enums.UserRole;

public final class ManagerAuthenticationResolver {

    private ManagerAuthenticationResolver() {
        // utility
    }

    public static Long resolveManagerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw ManagerPopupException.unauthenticated();
        }

        PassportPrincipal passport = extractPassport(authentication.getPrincipal());
        Long userId = extractUserId(authentication, passport);
        if (userId == null) {
            throw ManagerPopupException.userIdRequired();
        }

        String role = resolveRoleValue(authentication, passport);
        if (role == null || role.isBlank()) {
            throw ManagerPopupException.invalidRole();
        }
        role = normalizeRoleValue(role);

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw ManagerPopupException.invalidRole();
        }

        if (userRole != UserRole.MANAGER) {
            throw ManagerPopupException.notManager();
        }

        return userId;
    }

    private static PassportPrincipal extractPassport(Object principal) {
        if (principal instanceof PassportPrincipal passport) {
            return passport;
        }
        return null;
    }

    private static Long extractUserId(Authentication authentication, PassportPrincipal passport) {
        if (passport != null && passport.userId() != null) {
            return passport.userId();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Number number) {
            return number.longValue();
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ex) {
                throw ManagerPopupException.invalidPrincipal();
            }
        }

        return null;
    }

    private static String resolveRoleValue(Authentication authentication, PassportPrincipal passport) {
        if (passport != null && passport.role() != null && !passport.role().isBlank()) {
            return passport.role();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && !auth.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String normalizeRoleValue(String role) {
        if (role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return role;
    }
}
