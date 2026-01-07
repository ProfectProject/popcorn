package com.popcorn.demo.domain.payment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.payment.entity.Payment;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {

	boolean existsByOrderId(UUID orderId);

	Optional<Payment> findByOrderId(UUID orderId);
}
