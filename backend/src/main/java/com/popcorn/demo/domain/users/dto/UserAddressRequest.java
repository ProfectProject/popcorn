package com.popcorn.demo.domain.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressRequest {
    private String addrName;
    private String address1;
    private String address2;
    private String postalCode;
    private Boolean isDefault;
}
