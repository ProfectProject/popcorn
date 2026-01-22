package com.example.orderquery.global.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.example.orderquery.domain.summary.entity.OrderSummary;
import com.example.orderquery.domain.summary.exception.SummaryException;
import com.example.orderquery.domain.summary.repository.OrderSummaryRepository;
import com.example.orderquery.global.exception.OwnerAuthException;
import com.popcorn.common.dto.CommonResponseCode;

@Service
public class OwnerAuthService {

    private final OrderSummaryRepository orderSummaryRepository;

    public OwnerAuthService(OrderSummaryRepository orderSummaryRepository) {
        this.orderSummaryRepository = orderSummaryRepository;
    }

    public Long getCurrentOwnerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new OwnerAuthException(CommonResponseCode.FORBIDDEN, "unauthenticated");
        }

        Long userId = parseUserId(authentication);
        if (userId == null) {
            throw new OwnerAuthException(CommonResponseCode.FORBIDDEN, "invalid principal");
        }

        String roleValue = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && !auth.isBlank())
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .findFirst()
                .orElseThrow(() -> new OwnerAuthException(CommonResponseCode.FORBIDDEN, "invalid role"));

        if (!"OWNER".equals(roleValue)) {
            throw new OwnerAuthException(CommonResponseCode.FORBIDDEN, "owner role required");
        }

        return userId;
    }

    public void requireOwnedPopup(UUID storeId, UUID popupId, Long ownerId) {
        OrderSummary summary = orderSummaryRepository.findByStoreIdAndPopupId(storeId, popupId)
                .orElseThrow(() -> SummaryException.notFound(storeId, popupId));
        if (summary.getCreatedBy() == null || !summary.getCreatedBy().equals(ownerId)) {
            throw new OwnerAuthException(CommonResponseCode.FORBIDDEN, "not popup owner");
        }
    }

    private Long parseUserId(Authentication authentication) {
        String name = authentication.getName();
        if (name != null) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value;
        }
        return null;
    }
}
