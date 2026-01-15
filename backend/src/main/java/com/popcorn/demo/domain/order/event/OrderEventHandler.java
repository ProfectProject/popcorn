package com.popcorn.demo.domain.order.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.popcorn.demo.domain.order.service.OrderNotificationService;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.domain.payment.service.TossPaymentService;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.payment.event.PaymentCancelFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 주문 이벤트 통합 핸들러
 *
 * 모든 주문 관련 이벤트를 처리하는 중앙화된 핸들러
 * - 이벤트별 비즈니스 로직 분리
 * - 실패 처리 및 재시도 로직
 * - 성능 모니터링
 */
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    private final OrderQueryService orderQueryService;
    private final OrderNotificationService orderNotificationService;
    private final OrderEventStore eventStore;
    private final OrderEventMetrics eventMetrics;
    private final TossPaymentService tossPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final JpaPaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    // ================ 주문 생성 이벤트 처리 ================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("orderEventExecutor")
    public void handleOrderCreated(OrderCreatedEvent event) {
        String eventId = event.getEventId().toString();
        log.info("🎉 주문 생성 이벤트 처리 시작 - {}", event.getDetailedDescription());

        try {
            // 이벤트 저장 (이벤트 소싱)
            eventStore.saveEvent(event);

            // 비즈니스 처리
            processOrderCreated(event);

            // 알림 발송
            orderNotificationService.notifyOrderCreated(event.getOrder());

            // 메트릭 업데이트
            eventMetrics.recordEventProcessed("order_created", event.getBusinessPriority());

            log.info("✅ 주문 생성 이벤트 처리 완료 - 주문ID: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ 주문 생성 이벤트 처리 실패 - 주문ID: {}, 이벤트ID: {}",
                    event.getOrderId(), eventId, e);
            eventMetrics.recordEventError("order_created", e.getClass().getSimpleName());
            throw e; // 재시도를 위해 예외 전파
        }
    }

    // ================ 주문 상태 변경 이벤트 처리 ================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("orderEventExecutor")
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("📊 주문 상태 변경 이벤트 처리 시작 - {}", event.getStatusChangeDescription());

        try {
            // 이벤트 저장
            eventStore.saveEvent(event);

            // 상태별 비즈니스 처리
            processOrderStatusChanged(event);

            // 필요한 경우 고객 알림
            if (event.requiresCustomerNotification()) {
                // TODO: 알림 서비스 메서드 구현 필요
                log.info("📬 고객 알림 필요 - 주문ID: {}, 상태: {}, 사유: {}",
                        event.getOrderId(), event.getToStatus(), event.getReason());
            }

            // 중요한 상태 변경인 경우 특별 처리
            if (event.isCriticalStatusChange()) {
                processCriticalStatusChange(event);
            }

            eventMetrics.recordEventProcessed("order_status_changed",
                event.isCriticalStatusChange() ? "HIGH" : "NORMAL");

            log.info("✅ 주문 상태 변경 이벤트 처리 완료 - 주문ID: {}, {} → {}",
                    event.getOrderId(), event.getFromStatus(), event.getToStatus());

        } catch (Exception e) {
            log.error("❌ 주문 상태 변경 이벤트 처리 실패 - 주문ID: {}, 상태변경: {} → {}",
                    event.getOrderId(), event.getFromStatus(), event.getToStatus(), e);
            eventMetrics.recordEventError("order_status_changed", e.getClass().getSimpleName());
            throw e;
        }
    }

    // ================ 주문 취소 이벤트 처리 ================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("orderEventExecutor")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("❌ 주문 취소 이벤트 처리 시작 - {}", event.getCancellationDescription());

        try {
            // 이벤트 저장
            eventStore.saveEvent(event);

            // 취소 처리
            processOrderCancelled(event);

            // 환불 처리가 필요한 경우
            if (event.isRefundRequired()) {
                initiateRefundProcess(event);
            }

            // 보상이 필요한 경우 (점주에게 보상 등)
            if (event.requiresCompensation()) {
                handleCancellationCompensation(event);
            }

            // 알림 발송
            // TODO: 취소 알림 서비스 메서드 구현 필요
            log.info("📢 주문 취소 알림 - 주문ID: {}, 사유: {}, 환불필요: {}",
                    event.getOrderId(), event.getCancellationReason(), event.isRefundRequired());

            eventMetrics.recordEventProcessed("order_cancelled", event.getCancellationSeverity());

            log.info("✅ 주문 취소 이벤트 처리 완료 - 주문ID: {}, 심각도: {}",
                    event.getOrderId(), event.getCancellationSeverity());

        } catch (Exception e) {
            log.error("❌ 주문 취소 이벤트 처리 실패 - 주문ID: {}",
                    event.getOrderId(), e);
            eventMetrics.recordEventError("order_cancelled", e.getClass().getSimpleName());
            throw e;
        }
    }

    // ================ 주문 완료 이벤트 처리 ================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("orderEventExecutor")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("🎊 주문 완료 이벤트 처리 시작 - {}", event.getCompletionDescription());

        try {
            // 이벤트 저장
            eventStore.saveEvent(event);

            // 완료 처리
            processOrderCompleted(event);

            // 포인트 적립 요청
            if (event.getAccrualPoints() > 0) {
                requestPointAccrual(event);
            }

            // 리뷰 요청 (우선순위에 따라)
            scheduleReviewRequest(event);

            // 서비스 품질 분석
            analyzeServiceQuality(event);

            // 완료 알림
            // TODO: 완료 알림 서비스 메서드 구현 필요
            log.info("🎉 주문 완료 알림 - 주문ID: {}, 금액: {}원, 포인트: {}점",
                    event.getOrderId(), event.getFinalAmount(), event.getAccrualPoints());

            eventMetrics.recordEventProcessed("order_completed", event.getServiceQuality());

            log.info("✅ 주문 완료 이벤트 처리 완료 - 주문ID: {}, 품질: {}",
                    event.getOrderId(), event.getServiceQuality());

        } catch (Exception e) {
            log.error("❌ 주문 완료 이벤트 처리 실패 - 주문ID: {}",
                    event.getOrderId(), e);
            eventMetrics.recordEventError("order_completed", e.getClass().getSimpleName());
            throw e;
        }
    }

    // ================ 결제 처리 이벤트 처리 ================

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("orderEventExecutor")
    public void handlePaymentProcessed(OrderPaymentProcessedEvent event) {
        log.info("💳 결제 처리 이벤트 처리 시작 - {}", event.getPaymentDescription());

        try {
            // 이벤트 저장
            eventStore.saveEvent(event);

            // 결제 후처리
            processPayment(event);

            // 정산 데이터 업데이트
            updateSettlementData(event);

            // 위험 거래 모니터링
            if ("HIGH".equals(event.getPaymentRisk())) {
                monitorHighRiskPayment(event);
            }

            // 결제 알림
            // TODO: 결제 알림 서비스 메서드 구현 필요
            log.info("💳 결제 처리 알림 - 주문ID: {}, 상태: {}, 금액: {}원",
                    event.getOrderId(), event.getPaymentStatus(), event.getPaidAmount());

            eventMetrics.recordEventProcessed("order_payment_processed", event.getPaymentRisk());

            log.info("✅ 결제 처리 이벤트 처리 완료 - 주문ID: {}, 상태: {}, 위험도: {}",
                    event.getOrderId(), event.getPaymentStatus(), event.getPaymentRisk());

        } catch (Exception e) {
            log.error("❌ 결제 처리 이벤트 처리 실패 - 주문ID: {}, 결제ID: {}",
                    event.getOrderId(), event.getPaymentId(), e);
            eventMetrics.recordEventError("order_payment_processed", e.getClass().getSimpleName());
            throw e;
        }
    }

    // ================ 비즈니스 처리 메서드들 ================

    private void processOrderCreated(OrderCreatedEvent event) {
        log.debug("🔧 주문 생성 후처리 - 주문ID: {}", event.getOrderId());
        // 재고 차감, 예약 확정 등 비즈니스 로직
    }

    private void processOrderStatusChanged(OrderStatusChangedEvent event) {
        log.debug("🔧 상태 변경 후처리 - 주문ID: {}, {} → {}",
                event.getOrderId(), event.getFromStatus(), event.getToStatus());
        // 상태별 후속 처리 로직
    }

    private void processCriticalStatusChange(OrderStatusChangedEvent event) {
        log.info("⚠️ 중요 상태 변경 특별 처리 - 주문ID: {}, {} → {}",
                event.getOrderId(), event.getFromStatus(), event.getToStatus());
        // 중요 상태 변경에 대한 특별 처리
    }

    private void processOrderCancelled(OrderCancelledEvent event) {
        log.debug("🔧 주문 취소 후처리 - 주문ID: {}", event.getOrderId());
        // 재고 복구, 예약 취소 등
    }

    private void initiateRefundProcess(OrderCancelledEvent event) {
        log.info("💰 환불 프로세스 시작 - 주문ID: {}, 환불금액: {}원",
                event.getOrderId(), event.getRefundAmount());

        try {
            // 토스 결제 취소 처리
            TossPaymentService.TossPaymentCancelResult result = tossPaymentService.cancelPayment(
                    event.getOrderId(),
                    event.getCancellationReason()
            );

            log.info("✅ 토스 결제 취소 성공 - 주문ID: {}, 결제ID: {}, 취소금액: {}원",
                    event.getOrderId(), result.getPaymentId(), result.getCancelAmount());
        } catch (Exception ex) {
            log.error("❌ 토스 결제 취소 실패 - 주문ID: {}", event.getOrderId(), ex);
            // 결제 취소 실패 시 실패 이벤트 발행
            handleRefundFailure(event, ex);
        }
    }

    private void handleRefundFailure(OrderCancelledEvent event, Exception ex) {
        log.warn("⚠️ 결제 취소 실패, 실패 큐에 저장 - 주문ID: {}, 오류: {}",
                event.getOrderId(), ex.getMessage());

        try {
            // 해당 주문의 결제 정보 조회
            var payments = paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(event.getOrderId());

            if (payments.isEmpty()) {
                log.warn("⚠️ 결제 정보 없음 - 주문ID: {}", event.getOrderId());
                return;
            }

            Payment payment = payments.get(0); // 가장 최근 결제
            if (payment.getStatus() != PaymentStatus.PAID) {
                log.warn("⚠️ 결제 상태가 PAID가 아님 - 주문ID: {}, 상태: {}",
                        event.getOrderId(), payment.getStatus());
                return;
            }

            // rawPayload에서 paymentKey 추출
            String paymentKey = extractPaymentKeyFromRawPayload(payment.getRawPayload());
            if (paymentKey == null) {
                log.warn("⚠️ paymentKey 추출 실패 - 주문ID: {}", event.getOrderId());
                return;
            }

            // 결제 취소 실패 이벤트 발행
            PaymentCancelFailedEvent failedEvent = new PaymentCancelFailedEvent(
                    this,
                    event.getOrderId(),
                    payment.getId(),
                    paymentKey,
                    event.getCancellationReason(),
                    ex.getMessage(),
                    payment.getAmount(),
                    LocalDateTime.now(),
                    1 // 첫 번째 시도 실패
            );

            eventPublisher.publishEvent(failedEvent);

            log.info("📨 결제 취소 실패 이벤트 발행 완료 - 주문ID: {}, 결제ID: {}",
                    event.getOrderId(), payment.getId());

        } catch (Exception e) {
            log.error("❌ 결제 취소 실패 이벤트 발행 중 오류 - 주문ID: {}", event.getOrderId(), e);
        }
    }

    private String extractPaymentKeyFromRawPayload(String rawPayload) {
        if (rawPayload == null) {
            return null;
        }
        try {
            var node = objectMapper.readTree(rawPayload);
            return node.get("paymentKey").asText();
        } catch (Exception ex) {
            log.warn("결제 원본 데이터에서 paymentKey 추출 실패", ex);
            return null;
        }
    }

    private void handleCancellationCompensation(OrderCancelledEvent event) {
        log.info("🎁 취소 보상 처리 - 주문ID: {}, 심각도: {}",
                event.getOrderId(), event.getCancellationSeverity());
        // 보상 처리 로직
    }

    private void processOrderCompleted(OrderCompletedEvent event) {
        log.debug("🔧 주문 완료 후처리 - 주문ID: {}", event.getOrderId());
        // 완료 후 정산, 통계 업데이트 등
    }

    private void requestPointAccrual(OrderCompletedEvent event) {
        log.info("⭐ 포인트 적립 요청 - 주문ID: {}, 포인트: {}점",
                event.getOrderId(), event.getAccrualPoints());
        // 포인트 적립 로직
    }

    private void scheduleReviewRequest(OrderCompletedEvent event) {
        log.debug("📝 리뷰 요청 스케줄링 - 주문ID: {}, 우선순위: {}",
                event.getOrderId(), event.getReviewRequestPriority());
        // 리뷰 요청 스케줄링 로직
    }

    private void analyzeServiceQuality(OrderCompletedEvent event) {
        log.debug("📊 서비스 품질 분석 - 주문ID: {}, 품질: {}",
                event.getOrderId(), event.getServiceQuality());
        // 서비스 품질 데이터 수집 및 분석
    }

    private void processPayment(OrderPaymentProcessedEvent event) {
        log.debug("🔧 결제 후처리 - 주문ID: {}", event.getOrderId());
        // 결제 후 비즈니스 로직
    }

    private void updateSettlementData(OrderPaymentProcessedEvent event) {
        log.debug("📊 정산 데이터 업데이트 - 주문ID: {}, 우선순위: {}",
                event.getOrderId(), event.getSettlementPriority());
        // 정산 데이터 업데이트 로직
    }

    private void monitorHighRiskPayment(OrderPaymentProcessedEvent event) {
        log.warn("🚨 고위험 결제 모니터링 - 주문ID: {}, 금액: {}원",
                event.getOrderId(), event.getPaidAmount());
        // 고위험 결제 모니터링 로직
    }
}