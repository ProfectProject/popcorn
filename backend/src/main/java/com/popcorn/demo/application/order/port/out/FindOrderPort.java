package com.popcorn.demo.application.order.port.out;

import java.util.Optional;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

/**

	* 주문 조회 포트 (Output Port)

	*

	* Clean Architecture에서 애플리케이션 계층이 인프라 계층에게 요구하는 계약입니다.

	* - 주문 조회 기능 정의

	* - 인프라 계층에서 구현 (Adapter)

	* - 데이터베이스와의 연동

	*/

public interface FindOrderPort {



	/**

		* 멱등성 키로 주문을 조회합니다.

		*

		* @param idempotencyKey 멱등성 키

		* @return 조회된 주문 (Optional)

		*/

	Optional<Order> findByIdempotencyKey(String idempotencyKey);



	/**

		* ID로 주문을 조회합니다.

		*

		* @param orderId 주문 ID

		* @return 조회된 주문 (Optional)

		*/

	Optional<Order> findById(Long orderId);



	/**

		* 주문 요약 조회 (필요 컬럼만)

		*

		* @param orderId 주문 ID

		* @return 주문 요약 정보

		*/

	Optional<OrderSummaryView> findSummaryById(Long orderId);

}

