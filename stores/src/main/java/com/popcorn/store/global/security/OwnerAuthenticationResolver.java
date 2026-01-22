package com.popcorn.store.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.popcorn.common.filter.PassportPrincipal;
import com.popcorn.store.domain.store.exception.StoreException;
import com.popcorn.store.domain.users.entity.enums.UserRole;

public final class OwnerAuthenticationResolver {

    private OwnerAuthenticationResolver() {
        // Utility class
    }

    public static Long resolveOwnerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw StoreException.unauthenticated();
        }

        PassportPrincipal passport = extractPassport(authentication.getPrincipal());
        Long userId = extractUserId(authentication, passport);
        if (userId == null) {
            throw StoreException.userIdRequired();
        }

        String roleValue = resolveRoleValue(authentication, passport);
        if (roleValue == null || roleValue.isBlank()) {
            throw StoreException.invalidRole();
        }
        roleValue = normalizeRoleValue(roleValue);

        UserRole role;
        try {
            role = UserRole.valueOf(roleValue);
        } catch (Exception e) {
            throw StoreException.invalidRole();
        }

        if (role != UserRole.OWNER) {
            throw StoreException.notOwner();
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
                throw StoreException.invalidPrincipal();
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

    private static String normalizeRoleValue(String roleValue) {
        if (roleValue.startsWith("ROLE_")) {
            return roleValue.substring(5);
        }
        return roleValue;
    }
}
