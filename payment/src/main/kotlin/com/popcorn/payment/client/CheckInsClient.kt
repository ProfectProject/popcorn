package com.popcorn.payment.client

import com.popcorn.common.dto.BaseResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

@Component
class CheckInsClient(
    private val webClient: WebClient,
    @param:Value("\${microservices.checkins.base-url}")
    private val checkInsBaseUrl: String
) {

    private val log = LoggerFactory.getLogger(CheckInsClient::class.java)

    suspend fun issueQr(orderId: UUID): BaseResponse<Map<String, Any?>> {
        log.info("QR 발급 요청 - orderId={}", orderId)

        log.debug("checkIns 연결 주소: {}", checkInsBaseUrl)
        return webClient
            .post()
            .uri("$checkInsBaseUrl/v1/orders/$orderId")
            .retrieve()
            .awaitBody<BaseResponse<Map<String, Any?>>>()
    }
}
