package com.popcorn.demo.domain.order.entity;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**

	* 굿즈형 주문 항목의 세부 정보

	* - 조합(Composition) 패턴의 일부로 사용

	* - 굿즈 구매 관련 비즈니스 로직만 담당 (SRP 원칙)

	* - OrderItem과 조합되어 굿즈형 주문 항목을 구성

	*/

@Getter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class MerchDetail {



	// ========================= 굿즈 전용 필드 =========================



	/**

		* 상품 변형 ID

		* - p_merch_variants 테이블의 ID 참조

		* - 구매하려는 굿즈의 특정 변형 (사이즈, 색상, 옵션 등)

		*/

	private UUID merchVariantId;



	/**

		* SKU (Stock Keeping Unit)

		* - 상품 식별 코드

		* - merchVariantId를 통해 조회된 SKU 값

		*/

	private String sku;



	/**

		* 상품명

		* - merchVariantId를 통해 조회된 상품명

		*/

	private String productName;



	/**

		* 옵션명

		* - 변형 옵션명 (예: "XL 사이즈", "빨간색" 등)

		*/

	private String variantName;



	// ========================= 팩토리 메서드 =========================



	/**

		* 굿즈 세부정보 생성

		* @param merchVariantId 상품 변형 ID (필수)

		* @return MerchDetail 인스턴스

		* @throws IllegalArgumentException merchVariantId가 null인 경우

		*/

	public static MerchDetail of(UUID merchVariantId) {

		validateMerchVariantId(merchVariantId);



		return MerchDetail.builder()

				.merchVariantId(merchVariantId)

				.build();

	}



	/**

		* 굿즈 세부정보 생성 (상세 정보 포함)

		* @param merchVariantId 상품 변형 ID (필수)

		* @param sku 상품 식별 코드

		* @param productName 상품명

		* @param variantName 변형 옵션명

		* @return MerchDetail 인스턴스

		* @throws IllegalArgumentException merchVariantId가 null인 경우

		*/

	public static MerchDetail of(UUID merchVariantId, String sku, String productName, String variantName) {

		validateMerchVariantId(merchVariantId);



		return MerchDetail.builder()

				.merchVariantId(merchVariantId)

				.sku(sku)

				.productName(productName)

				.variantName(variantName)

				.build();

	}



	// ========================= 검증 메서드 =========================



	/**

		* 상품 변형 ID 검증

		* @param merchVariantId 검증할 상품 변형 ID

		* @throws IllegalArgumentException merchVariantId가 null인 경우

		*/

	private static void validateMerchVariantId(UUID merchVariantId) {

		if (merchVariantId == null) {

			throw new IllegalArgumentException("상품 변형 ID는 필수입니다.");

		}

	}



	// ========================= 비즈니스 로직 메서드 =========================



	/**

		* 유효한 굿즈 세부정보인지 확인

		* @return merchVariantId가 존재하면 true

		*/

	public boolean isValid() {

		return merchVariantId != null;

	}



	/**

		* 특정 상품 변형인지 확인

		* @param merchVariantId 확인할 상품 변형 ID

		* @return 해당 상품 변형이면 true

		*/

	public boolean isSameVariant(UUID merchVariantId) {

		return this.merchVariantId != null && this.merchVariantId.equals(merchVariantId);

	}



	/**

		* SKU로 상품 식별

		* @param sku 확인할 SKU

		* @return 해당 SKU면 true

		*/

	public boolean hasSku(String sku) {

		return this.sku != null && this.sku.equals(sku);

	}



	/**

		* 상품 정보 표시용 문자열 생성

		* @return "상품명 - 변형옵션" 형태의 문자열

		*/

	public String getDisplayName() {

		if (productName == null && variantName == null) {

			return "상품 ID: " + merchVariantId;

		}



		if (variantName == null) {

			return productName;

		}



		if (productName == null) {

			return variantName;

		}



		return productName + " - " + variantName;

	}



	/**

		* 재고 추적용 식별자 반환

		* @return SKU 또는 상품 변형 ID

		*/

	public String getStockIdentifier() {

		return sku != null ? sku : "VARIANT_" + merchVariantId;

	}

}
