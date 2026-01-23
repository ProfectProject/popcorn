package com.popcorn.payment.client

import com.popcorn.common.dto.BaseResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.UUID

/**
 * CheckIns 서비스 클라이언트
 *
 * 🚨 현재 사용되지 않음 - 이벤트 기반 아키텍처로 변경됨
 * QR 코드 생성은 이제 QrCodeGenerationRequestedEvent를 통해 처리됩니다.
 * Order 서비스가 이 이벤트를 구독하여 QR 생성을 담당합니다.
 */
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
            .header("X-User-Id", "1")  // 시스템 사용자 ID
            .header("X-User-Role", "SYSTEM")  // 시스템 권한
            .header("X-User-Email", "system@popcorn.com")  // 시스템 이메일
            .retrieve()
            .awaitBody<BaseResponse<Map<String, Any?>>>()
    }
}
