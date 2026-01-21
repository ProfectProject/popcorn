package com.popcorn.payment.service

import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class QrIssuanceTracker {

    private enum class IssueStatus { IN_PROGRESS, COMPLETED }

    private val state = ConcurrentHashMap<UUID, IssueStatus>()

    /**
     * QR 발급을 새로 처리해도 되는지 판단합니다.
     * 이미 완료(COMPLETED)되었거나 진행 중이라면 false를 반환하고, 그렇지 않으면 true를 반환합니다.
     */
    fun tryStart(orderId: UUID): Boolean {
        val previous = state.putIfAbsent(orderId, IssueStatus.IN_PROGRESS)
        return previous == null
    }

    /**
     * 발급에 성공하면 완료 상태로 표시합니다.
     */
    fun markSuccess(orderId: UUID) {
        state[orderId] = IssueStatus.COMPLETED
    }

    /**
     * 발급이 실패하면 트래커에서 제거하여 재시도를 허용합니다.
     */
    fun markFailure(orderId: UUID) {
        state.remove(orderId, IssueStatus.IN_PROGRESS)
    }
}
