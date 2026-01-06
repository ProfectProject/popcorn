package com.popcorn.demo.domain.order.entity;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**

	* 예약형 주문 항목의 세부 정보

	* - 조합(Composition) 패턴의 일부로 사용

	* - 예약 관련 비즈니스 로직만 담당 (SRP 원칙)

	* - OrderItem과 조합되어 예약형 주문 항목을 구성

	*/

@Getter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class ReservationDetail {



	// ========================= 예약 전용 필드 =========================



	/**

		* 세션 옵션 ID

		* - p_popup_schedules 테이블의 schedule_id 참조

		* - 예약하려는 세션의 특정 옵션 (시간대, 가격대 등)

		*/

	private UUID sessionOptionId;



	/**

		* 세션 ID (편의 메서드용)

		* - V0 스키마에서는 sessionOptionId와 동일한 schedule_id 사용

		*/

	private UUID sessionId;



	/**

		* 옵션 ID (편의 메서드용)

		* - V0 스키마에서는 옵션 개념이 없으므로 null 유지

		*/

	private UUID optionId;



	// ========================= 팩토리 메서드 =========================



	/**

		* 예약 세부정보 생성

		* @param sessionOptionId 세션 옵션 ID (필수)

		* @return ReservationDetail 인스턴스

		* @throws IllegalArgumentException sessionOptionId가 null인 경우

		*/

	public static ReservationDetail of(UUID sessionOptionId) {

		validateSessionOptionId(sessionOptionId);



		return ReservationDetail.builder()

				.sessionOptionId(sessionOptionId)

				.build();

	}



	/**

		* 예약 세부정보 생성 (세션 ID, 옵션 ID 포함)

		* @param sessionOptionId 세션 옵션 ID (필수)

		* @param sessionId 세션 ID

		* @param optionId 옵션 ID

		* @return ReservationDetail 인스턴스

		* @throws IllegalArgumentException sessionOptionId가 null인 경우

		*/

	public static ReservationDetail of(UUID sessionOptionId, UUID sessionId, UUID optionId) {

		validateSessionOptionId(sessionOptionId);



		return ReservationDetail.builder()

				.sessionOptionId(sessionOptionId)

				.sessionId(sessionId)

				.optionId(optionId)

				.build();

	}



	// ========================= 검증 메서드 =========================



	/**

		* 세션 옵션 ID 검증

		* @param sessionOptionId 검증할 세션 옵션 ID

		* @throws IllegalArgumentException sessionOptionId가 null인 경우

		*/

	private static void validateSessionOptionId(UUID sessionOptionId) {

		if (sessionOptionId == null) {

			throw new IllegalArgumentException("세션 옵션 ID는 필수입니다.");

		}

	}



	// ========================= 비즈니스 로직 메서드 =========================



	/**

		* 유효한 예약 세부정보인지 확인

		* @return sessionOptionId가 존재하면 true

		*/

	public boolean isValid() {

		return sessionOptionId != null;

	}



	/**

		* 특정 세션의 예약인지 확인

		* @param sessionId 확인할 세션 ID

		* @return 해당 세션의 예약이면 true

		*/

	public boolean belongsToSession(UUID sessionId) {

		return this.sessionId != null && this.sessionId.equals(sessionId);

	}



	/**

		* 특정 옵션의 예약인지 확인

		* @param optionId 확인할 옵션 ID

		* @return 해당 옵션의 예약이면 true

		*/

	public boolean hasOption(UUID optionId) {

		return this.optionId != null && this.optionId.equals(optionId);

	}

}
