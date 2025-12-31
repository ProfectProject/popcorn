package com.popcorn.demo.application.order.port.out;

import java.util.List;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;

/**

	* 주문 저장 포트 (Output Port)

	*

	* Clean Architecture에서 애플리케이션 계층이 인프라 계층에게 요구하는 계약입니다.

	* - 주문 저장 기능 정의

	* - 인프라 계층에서 구현 (Adapter)

	* - 데이터베이스와의 연동

	*/

public interface SaveOrderPort {



	/**

		* 주문을 저장합니다.

		*

		* @param order 저장할 주문 엔티티

		* @return 저장된 주문 엔티티 (ID 포함)

		*/

	Order save(Order order);



	/**
		* 주문 항목을 일괄 저장합니다.
		*
		* @param orderItems 저장할 주문 항목들
		*/

	void saveOrderItems(List<OrderItem> orderItems);

}
