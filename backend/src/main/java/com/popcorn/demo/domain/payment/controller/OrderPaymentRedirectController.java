package com.popcorn.demo.domain.payment.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;

import lombok.RequiredArgsConstructor;

@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderPaymentRedirectController {

	private final OrderRepository orderRepository;
	private final JpaPaymentRepository paymentRepository;
	private final TossPaymentsProperties tossPaymentsProperties;

	@GetMapping("/{orderId}/pay")
	public ResponseEntity<Void> redirectToPayment(@PathVariable UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(OrderNotFoundException::orderNotFound);
		Payment payment = paymentRepository.findByOrderId(orderId)
				.orElseThrow(PaymentException::paymentNotFound);
		if (payment.getDeletedAt() != null) {
			throw PaymentException.paymentNotFound();
		}

		String redirectUrl = UriComponentsBuilder.fromHttpUrl(tossPaymentsProperties.getCheckoutUrl())
				.queryParam("orderNo", order.getOrderNo())
				.queryParam("amount", payment.getAmount())
				.queryParam("customerKey", toCustomerKey(order.getCustomerId()))
				.queryParam("paymentId", payment.getId())
				.queryParam("successUrl", tossPaymentsProperties.getSuccessUrl())
				.queryParam("failUrl", tossPaymentsProperties.getFailUrl())
				.build(true)
				.toUriString();

		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
	}

	private String toCustomerKey(Long customerId) {
		if (customerId == null) {
			return "guest";
		}
		return customerId.toString();
	}
}
