package com.popcorn.demo.domain.order.service;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.jpa.OrderQueryRepository;

import lombok.RequiredArgsConstructor;

/**
 * 주문 권한 검증 서비스
 *
 * 주문 관련 권한과 접근 제어를 담당:
 * - 사용자별 주문 접근 권한 검증
 * - 역할 기반 접근 제어 (RBAC)
 * - 주문 상태별 권한 검증
 */
@Service
@RequiredArgsConstructor
public class OrderAuthorizationService {

	private static final Logger log = LoggerFactory.getLogger(OrderAuthorizationService.class);

	private final OrderQueryRepository orderQueryRepository;

	/**
	 * 주문 조회 권한 검증
	 */
	public void validateOrderAccess(UUID orderId, Long userId, String role) {
		if (userId == null || role == null || role.isBlank()) {
			log.warn("❌ 권한 정보 부족 - 사용자: {}, 역할: {}", userId, role);
			throw OrderException.forbidden();
		}

		String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

		switch (normalizedRole) {
			case "ADMIN" -> validateAdminAccess(orderId);
			case "OWNER" -> validateOwnerAccess(orderId, userId);
			case "MANAGER" -> validateManagerAccess(orderId, userId);
			case "CUSTOMER", "USER" -> validateCustomerAccess(orderId, userId);
			default -> {
				log.warn("❌ 알 수 없는 역할: {}", normalizedRole);
				throw OrderException.forbidden();
			}
		}
	}

	/**
	 * 주문 상태 변경 권한 검증
	 */
	public void validateStatusChangePermission(UUID orderId, Long userId, String role, OrderStatus fromStatus, OrderStatus toStatus) {
		validateOrderAccess(orderId, userId, role);

		String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

		boolean allowed = switch (normalizedRole) {
			case "ADMIN" -> true; // 관리자는 모든 상태 변경 가능
			case "OWNER", "MANAGER" -> isOwnerAllowedStatusChange(fromStatus, toStatus);
			case "CUSTOMER", "USER" -> isCustomerAllowedStatusChange(fromStatus, toStatus);
			default -> false;
		};

		if (!allowed) {
			log.warn("❌ 상태 변경 권한 없음 - 역할: {}, 상태변경: {} → {}",
					normalizedRole, fromStatus, toStatus);
			throw OrderException.forbidden();
		}
	}

	/**
	 * 주문 취소 권한 검증
	 */
	public void validateOrderCancellationPermission(Order order, Long userId, String role) {
		validateOrderAccess(order.getId(), userId, role);

		// 취소 불가 상태 검증
		if (order.getStatus() == OrderStatus.COMPLETED ||
			order.getStatus() == OrderStatus.CANCELLED ||
			order.getStatus() == OrderStatus.REFUNDED) {
			log.warn("❌ 취소 불가 상태 - 주문: {}, 현재상태: {}", order.getId(), order.getStatus());
			throw OrderException.orderCannotBeCancelled();
		}

		String normalizedRole = role.trim().toUpperCase(Locale.ROOT);

		// 고객은 REQUESTED 상태에서만 취소 가능
		if ("CUSTOMER".equals(normalizedRole) || "USER".equals(normalizedRole)) {
			if (order.getStatus() != OrderStatus.REQUESTED) {
				log.warn("❌ 고객 취소 불가 상태 - 주문: {}, 상태: {}", order.getId(), order.getStatus());
				throw OrderException.customerCannotCancelOrder();
			}
		}
	}

	// ================ 내부 검증 메서드들 ================

	private void validateAdminAccess(UUID orderId) {
		log.debug("🔓 관리자 접근 허용 - 주문: {}", orderId);
		// 관리자는 모든 주문에 접근 가능
	}

	private void validateOwnerAccess(UUID orderId, Long userId) {
		// TODO: 실제 점주 권한 검증 로직 구현
		// 임시로 기본 검증만 수행
		if (userId <= 0) {
			log.warn("❌ 점주 접근 거부 - 주문: {}, 사용자: {}", orderId, userId);
			throw OrderException.forbidden();
		}
		log.debug("🔓 점주 접근 허용 - 주문: {}, 사용자: {}", orderId, userId);
	}

	private void validateManagerAccess(UUID orderId, Long userId) {
		// TODO: 실제 매니저 권한 검증 로직 구현
		// 임시로 기본 검증만 수행
		if (userId <= 0) {
			log.warn("❌ 매니저 접근 거부 - 주문: {}, 사용자: {}", orderId, userId);
			throw OrderException.forbidden();
		}
		log.debug("🔓 매니저 접근 허용 - 주문: {}, 사용자: {}", orderId, userId);
	}

	private void validateCustomerAccess(UUID orderId, Long userId) {
		// TODO: 실제 고객 권한 검증 로직 구현
		// 임시로 기본 검증만 수행
		if (userId <= 0) {
			log.warn("❌ 고객 접근 거부 - 주문: {}, 사용자: {}", orderId, userId);
			throw OrderException.forbidden();
		}
		log.debug("🔓 고객 접근 허용 - 주문: {}, 사용자: {}", orderId, userId);
	}

	private boolean isOwnerAllowedStatusChange(OrderStatus from, OrderStatus to) {
		// 점주/매니저가 할 수 있는 상태 변경
		return switch (to) {
			case OWNER_ACCEPTED, OWNER_REJECTED, CONFIRMED, PREPARING, READY, COMPLETED, CANCELLED -> true;
			default -> false;
		};
	}

	private boolean isCustomerAllowedStatusChange(OrderStatus from, OrderStatus to) {
		// 고객이 할 수 있는 상태 변경 (취소만 가능)
		return to == OrderStatus.CANCELLED && from == OrderStatus.REQUESTED;
	}
}