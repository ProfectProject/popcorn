package com.popcorn.users.users.dto;

import java.util.UUID;

import com.popcorn.users.users.entity.UserAddress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressResponse {
    private UUID addrId;
    private Long userId;
    private String addrName;
    private String address1;
    private String address2;
    private String postalCode;
    private Boolean isDefault;

    // UserAddress 엔티티에서 UserAddressResponse로 변환
    public static UserAddressResponse from(UserAddress userAddress) {
        return UserAddressResponse.builder()
                .addrId(userAddress.getId())
                .userId(userAddress.getUser().getUserId())
                .addrName(userAddress.getAddrName())
                .address1(userAddress.getAddress1())
                .address2(userAddress.getAddress2())
                .postalCode(userAddress.getPostalCode())
                .isDefault(userAddress.getIsDefault())
                .build();
    }
}
