package com.popcorn.payment.config

import com.popcorn.payment.event.standard.StandardPaymentEventPublisher
import com.popcorn.payment.repository.PaymentRepository
import com.popcorn.payment.service.PaymentCommandCoroutineService
import com.popcorn.payment.service.PaymentOrderInfoService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentServiceConfig {

    @Bean
    @ConditionalOnMissingBean(PaymentCommandCoroutineService::class)
    fun paymentCommandCoroutineService(
        transactionManager: CoroutineTransactionManager,
        paymentRepository: PaymentRepository,
        standardPaymentEventPublisher: StandardPaymentEventPublisher,
        paymentOrderInfoService: PaymentOrderInfoService
    ): PaymentCommandCoroutineService {
        return PaymentCommandCoroutineService(
            transactionManager,
            paymentRepository,
            standardPaymentEventPublisher,
            paymentOrderInfoService
        )
    }
}
