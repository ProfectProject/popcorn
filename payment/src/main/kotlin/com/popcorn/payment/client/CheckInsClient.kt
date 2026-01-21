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
    private val resolvedBaseUrl: String

    init {
        resolvedBaseUrl = when {
            checkInsBaseUrl.contains(":8084") -> {
                log.warn("checkIns base URL이 order 서비스(8084)로 설정되어 있어 default http://localhost:8086을 사용합니다.")
                "http://localhost:8086"
            }
            else -> checkInsBaseUrl
        }
        log.info("checkIns base URL={}", resolvedBaseUrl)
    }
    suspend fun issueQr(orderId: UUID): BaseResponse<Map<String, Any?>> {
        log.info("QR 발급 요청 - orderId={}", orderId)

        return webClient
            .post()
            .uri("$resolvedBaseUrl/api/qr/v1/orders/$orderId")
            .retrieve()
            .awaitBody<BaseResponse<Map<String, Any?>>>()
    }
}
