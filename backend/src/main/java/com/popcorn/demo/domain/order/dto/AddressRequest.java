package com.popcorn.demo.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**

	* 주문 배송지 정보 DTO

	* 구매형 주문(PURCHASE)에서 필수로 사용되며,

	* 예약형 주문(RESERVATION)에서는 선택적으로 사용됩니다.

	*/

@Getter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class AddressRequest {



	/**

		* 기본 주소 (필수)

		* 구매형 주문에서는 반드시 입력되어야 합니다.

		*/

	@NotBlank(message = "기본 주소는 필수입니다.")

	@Size(max = 255, message = "기본 주소는 255자 이하로 입력해주세요.")

	private String address1;



	/**

		* 상세 주소 (선택)

		* 아파트 동호수, 건물명 등 상세 정보

		*/

	@Size(max = 255, message = "상세 주소는 255자 이하로 입력해주세요.")

	private String address2;



	/**

		* 수령인 이름 (선택)

		* 주문자와 다른 경우 입력

		*/

	@Size(max = 100, message = "수령인 이름은 100자 이하로 입력해주세요.")

	private String receiverName;



	/**

		* 연락처 (선택)

		* 배송 관련 연락용

		*/

	@Size(max = 20, message = "연락처는 20자 이하로 입력해주세요.")

	private String phone;



	/**

		* 주소 정보가 유효한지 검증

		* @return 기본 주소가 있으면 true

		*/

	public boolean isValid() {

		return address1 != null && !address1.trim().isEmpty();

	}



	/**

		* 전체 주소를 문자열로 반환

		* @return "기본주소 상세주소" 형태

		*/

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

