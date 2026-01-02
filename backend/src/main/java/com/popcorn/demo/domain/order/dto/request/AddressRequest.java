package com.popcorn.demo.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

	@NotBlank(message = "기본 주소는 필수입니다.")
	@Size(max = 255, message = "기본 주소는 255자 이하로 입력해주세요.")
	private String address1;

	@Size(max = 255, message = "상세 주소는 255자 이하로 입력해주세요.")
	private String address2;

	@Size(max = 100, message = "수령인 이름은 100자 이하로 입력해주세요.")
	private String receiverName;

	@Size(max = 20, message = "연락처는 20자 이하로 입력해주세요.")
	private String phone;

	public boolean isValid() {
		return address1 != null && !address1.trim().isEmpty();
	}

	public String getFullAddress() {
		if (address1 == null || address1.trim().isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder(address1.trim());
		if (address2 != null && !address2.trim().isEmpty()) {
			sb.append(" ").append(address2.trim());
		}
		return sb.toString();
	}
}
