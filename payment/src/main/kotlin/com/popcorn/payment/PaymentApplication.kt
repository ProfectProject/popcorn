package com.popcorn.payment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(scanBasePackages = ["com.popcorn"])
@EnableAsync
@EnableTransactionManagement
@ConfigurationPropertiesScan("com.popcorn.payment.config")
class PaymentApplication

fun main(args: Array<String>) {
    EnvLoader.load(listOf("payment/.env", ".env"))
    runApplication<PaymentApplication>(*args)
}
