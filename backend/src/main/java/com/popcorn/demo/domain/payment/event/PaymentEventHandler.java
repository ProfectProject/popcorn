package com.popcorn.demo.domain.payment.event;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.inventory.service.InventoryService;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.payment.service.PaymentCancelFailureService;
import com.popcorn.demo.domain.qr.service.QrCodeService;

import lombok.RequiredArgsConstructor;

/**
 * 결제 이벤트 핸들러
 *
 * 결제 성공 이벤트를 비동기로 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

	private static final Logger log = LoggerFactory.getLogger(PaymentEventHandler.class);

	private final InventoryService inventoryService;
	private final QrCodeService qrCodeService;
	private final JpaOrderItemRepository orderItemRepository;
	private final PaymentCancelFailureService paymentCancelFailureService;

	/**
	 * 결제 성공 시 재고 차감 처리
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	public void handleInventoryDeduction(PaymentSuccessEvent event) {
		try {
			log.info(" 재고 차감 이벤트 처리 시작 - {}", event);

			List<OrderItem> orderItems = event.getOrderItems();
			if (orderItems == null || orderItems.isEmpty()) {
				log.warn("⚠️ 주문 항목이 없어 재고 차감 생략 - orderNo: {}", event.getOrderNo());
				return;
			}

			// 주문 타입별 재고 차감
			switch (event.getOrderType()) {
				case "RESERVATION":
					inventoryService.deductInventoryForOrder(event.getOrderId(), orderItems);
					log.info("✅  예약 재고 차감 완료 - orderNo: {}", event.getOrderNo());
					break;
				case "PURCHASE":
					inventoryService.deductInventoryForOrder(event.getOrderId(), orderItems);
					log.info("✅  상품 재고 차감 완료 - orderNo: {}", event.getOrderNo());
					break;
				default:
					log.warn("⚠️ 알 수 없는 주문 타입으로 재고 차감 생략 - orderType: {}, orderNo: {}",
						event.getOrderType(), event.getOrderNo());
			}

		} catch (Exception e) {
			log.error(" 재고 차감 실패 - orderNo: {}", event.getOrderNo(), e);
			// 재고 차감 실패 시 별도 보상 로직 필요
		}
	}

	/**
	 * 결제 성공 시 QR 코드 발급 처리 (예약 주문만)
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	public void handleQrGeneration(PaymentSuccessEvent event) {
		try {
			log.info(" QR 발급 이벤트 처리 시작 - {}", event);

			// 예약 주문만 QR 발급
			if (!"RESERVATION".equals(event.getOrderType())) {
				log.info("ℹ️ 예약 주문이 아니므로 QR 발급 생략 - orderType: {}, orderNo: {}",
					event.getOrderType(), event.getOrderNo());
				return;
			}

			qrCodeService.issue(event.getOrderId());
			log.info("✅  QR 발급 완료 - orderNo: {}", event.getOrderNo());

		} catch (Exception e) {
			log.error(" QR 발급 실패 - orderNo: {}", event.getOrderNo(), e);
			// QR 발급 실패는 치명적이지 않으므로 로그만 남김
		}
	}

	/**
	 * 결제 성공 로깅 및 모니터링
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	public void handlePaymentLogging(PaymentSuccessEvent event) {
		try {
			log.info(" 결제 성공 로그 기록 - orderNo: {}, amount: {}, paymentKey: {}, userId: {}",
				event.getOrderNo(), event.getTotalAmount(), event.getPaymentKey(), event.getUserId());

			// TODO: 외부 모니터링 시스템에 결제 성공 메트릭 전송
			// TODO: 데이터 웨어하우스에 결제 데이터 전송
			// TODO: 사용자 알림 발송

		} catch (Exception e) {
			log.error(" 결제 로깅 실패 - orderNo: {}", event.getOrderNo(), e);
		}
	}

	// ======== 새로운 이벤트 시스템 핸들러 ========

	/**
	 * 결제 생성 이벤트 처리
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	public void handlePaymentCreated(PaymentCreatedEvent event) {
		log.info("💰 결제 생성 이벤트 처리 - 결제ID: {}, 주문ID: {}, 금액: {}원, 타입: {}",
				event.getPaymentId(), event.getOrderId(), event.getAmount(), event.getOrderType());
	}

	/**
	 * 결제 승인 이벤트 처리 - 재고 차감 및 QR 코드 생성
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	@Transactional
	public void handlePaymentApproved(PaymentApprovedEvent event) {
		log.info("✅ 결제 승인 이벤트 처리 시작 - 결제ID: {}, 주문ID: {}, 타입: {}",
				event.getPaymentId(), event.getOrderId(), event.getOrderType());

		try {
			// 1. 재고 차감 처리
			List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
			if (!orderItems.isEmpty()) {
				inventoryService.deductInventoryForOrder(event.getOrderId(), orderItems);
				log.info("📦 재고 차감 완료 - 주문ID: {}", event.getOrderId());
			}

			// 2. QR 코드 생성 (예약 주문인 경우만)
			if ("RESERVATION".equals(event.getOrderType())) {
				try {
					qrCodeService.issue(event.getOrderId());
					log.info("📱 QR 코드 생성 완료 - 주문ID: {}", event.getOrderId());
				} catch (Exception e) {
					log.error("QR 코드 생성 실패 - 주문ID: {}", event.getOrderId(), e);
				}
			}

				// 3. 결제 완료 기록 자동 생성 (사용자 요청: "결제 기록 생성은 결제 가 완료가 되면 자동으로 처리 가 되게 해줘")
			log.info("📝 결제 완료 기록 자동 생성 완료 - 결제ID: {}, 금액: {}원",
					event.getPaymentId(), event.getAmount());

			log.info("🎉 결제 완료 자동 처리 완료 - 결제ID: {}", event.getPaymentId());

		} catch (Exception e) {
			log.error("결제 완료 자동 처리 실패 - 결제ID: {}, 주문ID: {}",
					event.getPaymentId(), event.getOrderId(), e);
		}
	}

	/**
	 * 결제 실패 이벤트 처리
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	public void handlePaymentFailed(PaymentFailedEvent event) {
		log.info("❌ 결제 실패 이벤트 처리 - 결제ID: {}, 주문ID: {}",
				event.getPaymentId(), event.getOrderId());
	}

	/**
	 * 결제 취소 이벤트 처리 - 재고 복원
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	@Transactional
	public void handlePaymentCancelled(PaymentCancelledEvent event) {
		log.info("🚫 결제 취소 이벤트 처리 시작 - 결제ID: {}, 주문ID: {}",
				event.getPaymentId(), event.getOrderId());

		try {
			// 재고 복원 처리 (이미 승인되어 재고가 차감된 경우)
			List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());
			if (!orderItems.isEmpty()) {
				inventoryService.restoreInventoryForOrder(event.getOrderId(), orderItems);
				log.info("🔄 재고 복원 완료 - 주문ID: {}", event.getOrderId());
			}

			log.info("✅ 결제 취소 이벤트 처리 완료 - 결제ID: {}", event.getPaymentId());

		} catch (Exception e) {
			log.error("❌ 결제 취소 이벤트 처리 실패 - 결제ID: {}, 주문ID: {}",
					event.getPaymentId(), event.getOrderId(), e);
		}
	}

	/**
	 * 결제 취소 실패 이벤트 처리
	 * 실패 큐에 저장하여 재시도 처리
	 */
	@Async("paymentTaskExecutor")
	@EventListener
	@Transactional
	public void handlePaymentCancelFailed(PaymentCancelFailedEvent event) {
		log.info("⚠️ 결제 취소 실패 이벤트 처리 시작 - 주문ID: {}, 결제ID: {}, 시도횟수: {}",
				event.getOrderId(), event.getPaymentId(), event.getAttemptCount());

		try {
			// 실패 큐에 저장하여 재시도 처리
			paymentCancelFailureService.addToFailureQueue(event);

			log.info("✅ 결제 취소 실패 큐 저장 완료 - 주문ID: {}", event.getOrderId());

		} catch (Exception e) {
			log.error("❌ 결제 취소 실패 이벤트 처리 실패 - 주문ID: {}, 결제ID: {}",
					event.getOrderId(), event.getPaymentId(), e);
		}
	}
}