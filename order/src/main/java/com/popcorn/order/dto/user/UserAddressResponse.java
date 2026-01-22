package com.popcorn.order.dto.user;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * User 서비스 주소 응답 DTO
 */
@Getter
@Builder
public class UserAddressResponse {
    private UUID addrId;
    private Long userId;
    private String addrName;
    private String address1;
    private String address2;
    private String postalCode;
    private Boolean isDefault;
}
