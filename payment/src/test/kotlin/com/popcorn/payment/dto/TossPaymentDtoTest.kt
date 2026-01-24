package com.popcorn.payment.dto

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TossPaymentDtoTest {

    @Test
    fun `TossPaymentConfirmRequest 생성 테스트`() {
        val request = TossPaymentConfirmRequest(
            paymentKey = "payment_key_123",
            orderId = "ORDER-123",
            amount = 10000
        )

        assertEquals("payment_key_123", request.paymentKey)
        assertEquals("ORDER-123", request.orderId)
        assertEquals(10000, request.amount)
    }

    @Test
    fun `TossPaymentConfirmResponse 생성 테스트`() {
        val response = TossPaymentConfirmResponse(
            paymentKey = "payment_key_456",
            orderId = "ORDER-456",
            totalAmount = 15000,
            status = "DONE",
            method = "CARD",
            approvedAt = "2024-01-01T10:00:00"
        )

        assertEquals("payment_key_456", response.paymentKey)
        assertEquals("ORDER-456", response.orderId)
        assertEquals(15000, response.totalAmount)
        assertEquals("DONE", response.status)
        assertEquals("CARD", response.method)
        assertEquals("2024-01-01T10:00:00", response.approvedAt)
    }

    @Test
    fun `TossPaymentCancelRequest 생성 테스트`() {
        val request = TossPaymentCancelRequest(
            cancelReason = "고객 요청",
            cancelAmount = 5000
        )

        assertEquals("고객 요청", request.cancelReason)
        assertEquals(5000, request.cancelAmount)
    }

    @Test
    fun `TossPaymentCancelResponse 생성 테스트`() {
        val response = TossPaymentCancelResponse(
            paymentKey = "payment_key_cancel",
            orderId = "ORDER-CANCEL",
            status = "CANCELLED",
            totalAmount = 20000,
            balanceAmount = 15000,
            suppliedAmount = 18182,
            vat = 1818,
            taxFreeAmount = 0,
            cancels = emptyList(),
            method = "CARD",
            approvedAt = "2024-01-01T11:00:00"
        )

        assertEquals("payment_key_cancel", response.paymentKey)
        assertEquals("ORDER-CANCEL", response.orderId)
        assertEquals("CANCELLED", response.status)
        assertEquals(20000, response.totalAmount)
        assertEquals(15000, response.balanceAmount)
        assertEquals("CARD", response.method)
    }

    @Test
    fun `RefundReceiveAccount 생성 테스트`() {
        val account = RefundReceiveAccount(
            bank = "NH농협은행",
            accountNumber = "123456789012",
            holderName = "홍길동"
        )

        assertEquals("NH농협은행", account.bank)
        assertEquals("123456789012", account.accountNumber)
        assertEquals("홍길동", account.holderName)
    }

    @Test
    fun `CancelDetail 생성 테스트`() {
        val cancelDetail = CancelDetail(
            cancelAmount = 10000,
            cancelReason = "단순 변심",
            taxFreeAmount = 0,
            taxExemptionAmount = 0,
            refundableAmount = 10000,
            easyPayDiscountAmount = 0,
            canceledAt = "2024-01-01T12:00:00",
            transactionKey = "transaction_123",
            receiptKey = "receipt_456"
        )

        assertEquals(10000, cancelDetail.cancelAmount)
        assertEquals("단순 변심", cancelDetail.cancelReason)
        assertEquals(0, cancelDetail.taxFreeAmount)
        assertEquals(10000, cancelDetail.refundableAmount)
        assertEquals("2024-01-01T12:00:00", cancelDetail.canceledAt)
    }

    @Test
    fun `CardDetail 생성 테스트`() {
        val cardDetail = CardDetail(
            amount = 25000,
            issuerCode = "신한카드",
            acquirerCode = "신한카드",
            number = "433012******1234",
            installmentPlanMonths = 0,
            approveNo = "00000000",
            useCardPoint = false,
            cardType = "신용",
            ownerType = "개인",
            acquireStatus = "READY",
            isInterestFree = true,
            interestPayer = "BUYER"
        )

        assertEquals(25000, cardDetail.amount)
        assertEquals("신한카드", cardDetail.issuerCode)
        assertEquals("433012******1234", cardDetail.number)
        assertEquals(0, cardDetail.installmentPlanMonths)
        assertEquals(false, cardDetail.useCardPoint)
        assertEquals("신용", cardDetail.cardType)
        assertEquals(true, cardDetail.isInterestFree)
    }

    @Test
    fun `VirtualAccountDetail 생성 테스트`() {
        val virtualAccount = VirtualAccountDetail(
            accountType = "NORMAL",
            accountNumber = "20240001234567",
            bankCode = "국민",
            customerName = "홍길동",
            dueDate = "2024-01-31T23:59:59",
            refundStatus = "NONE",
            expired = false,
            settlementStatus = "INCOMPLETED"
        )

        assertEquals("NORMAL", virtualAccount.accountType)
        assertEquals("20240001234567", virtualAccount.accountNumber)
        assertEquals("국민", virtualAccount.bankCode)
        assertEquals("홍길동", virtualAccount.customerName)
        assertEquals(false, virtualAccount.expired)
    }

    @Test
    fun `TransferDetail 생성 테스트`() {
        val transferDetail = TransferDetail(
            bankCode = "산업은행",
            settlementStatus = "COMPLETED"
        )

        assertEquals("산업은행", transferDetail.bankCode)
        assertEquals("COMPLETED", transferDetail.settlementStatus)
    }

    @Test
    fun `MobilePhoneDetail 생성 테스트`() {
        val mobilePhoneDetail = MobilePhoneDetail(
            customerMobilePhone = "01012345678",
            settlementStatus = "COMPLETED",
            receiptUrl = "https://receipt.example.com/12345"
        )

        assertEquals("01012345678", mobilePhoneDetail.customerMobilePhone)
        assertEquals("COMPLETED", mobilePhoneDetail.settlementStatus)
        assertEquals("https://receipt.example.com/12345", mobilePhoneDetail.receiptUrl)
    }

    @Test
    fun `GiftCertificateDetail 생성 테스트`() {
        val giftCertificate = GiftCertificateDetail(
            approveNo = "0123456789",
            settlementStatus = "COMPLETED"
        )

        assertEquals("0123456789", giftCertificate.approveNo)
        assertEquals("COMPLETED", giftCertificate.settlementStatus)
    }

    @Test
    fun `EasyPayDetail 생성 테스트`() {
        val easyPayDetail = EasyPayDetail(
            provider = "토스페이",
            amount = 30000,
            discountAmount = 3000
        )

        assertEquals("토스페이", easyPayDetail.provider)
        assertEquals(30000, easyPayDetail.amount)
        assertEquals(3000, easyPayDetail.discountAmount)
    }

    @Test
    fun `CashReceiptDetail 생성 테스트`() {
        val cashReceiptDetail = CashReceiptDetail(
            type = "소득공제",
            receiptKey = "9fb9c23b38254b39ab6b",
            issueNumber = "12-123456789",
            receiptUrl = "https://receipt.example.com",
            amount = 40000,
            taxFreeAmount = 0
        )

        assertEquals("소득공제", cashReceiptDetail.type)
        assertEquals("9fb9c23b38254b39ab6b", cashReceiptDetail.receiptKey)
        assertEquals("12-123456789", cashReceiptDetail.issueNumber)
        assertEquals(40000, cashReceiptDetail.amount)
    }

    @Test
    fun `DiscountDetail 생성 테스트`() {
        val discountDetail = DiscountDetail(
            amount = 5000
        )

        assertEquals(5000, discountDetail.amount)
    }

    @Test
    fun `FailureDetail 생성 테스트`() {
        val failureDetail = FailureDetail(
            code = "PAY_PROCESS_CANCELED",
            message = "사용자에 의해 결제가 취소되었습니다."
        )

        assertEquals("PAY_PROCESS_CANCELED", failureDetail.code)
        assertEquals("사용자에 의해 결제가 취소되었습니다.", failureDetail.message)
    }

    @Test
    fun `DTO copy 메서드 테스트`() {
        val originalRequest = TossPaymentConfirmRequest("key1", "order1", 1000)
        val copiedRequest = originalRequest.copy(amount = 2000)
        assertEquals("key1", copiedRequest.paymentKey)
        assertEquals("order1", copiedRequest.orderId)
        assertEquals(2000, copiedRequest.amount)

        val originalResponse = TossPaymentConfirmResponse("key2", "order2", 3000, "DONE", "CARD")
        val copiedResponse = originalResponse.copy(totalAmount = 4000)
        assertEquals("key2", copiedResponse.paymentKey)
        assertEquals(4000, copiedResponse.totalAmount)
    }

    @Test
    fun `DTO toString 메서드 테스트`() {
        val request = TossPaymentConfirmRequest("test_key", "test_order", 5000)
        val toString = request.toString()

        assertNotNull(toString)
        assert(toString.contains("test_key"))
        assert(toString.contains("test_order"))
        assert(toString.contains("5000"))
    }

    @Test
    fun `DTO equals 메서드 테스트`() {
        val request1 = TossPaymentConfirmRequest("key", "order", 1000)
        val request2 = TossPaymentConfirmRequest("key", "order", 1000)
        val request3 = TossPaymentConfirmRequest("key", "order", 2000)

        assertEquals(request1, request2)
        assert(request1 != request3)
    }

    @Test
    fun `DTO hashCode 메서드 테스트`() {
        val request1 = TossPaymentConfirmRequest("key", "order", 1000)
        val request2 = TossPaymentConfirmRequest("key", "order", 1000)

        assertEquals(request1.hashCode(), request2.hashCode())
    }

    @Test
    fun `nullable 필드 처리 테스트`() {
        val response = TossPaymentConfirmResponse(
            paymentKey = "null_test",
            orderId = "NULL-ORDER",
            totalAmount = 1000,
            status = "DONE",
            method = "CARD",
            requestedAt = null,
            approvedAt = null
        )

        assertEquals("null_test", response.paymentKey)
        assertEquals(null, response.requestedAt)
        assertEquals(null, response.approvedAt)
    }

    @Test
    fun `빈 리스트 처리 테스트`() {
        val response = TossPaymentCancelResponse(
            paymentKey = "empty_list_test",
            orderId = "EMPTY-ORDER",
            status = "CANCELLED",
            totalAmount = 1000,
            balanceAmount = 0,
            suppliedAmount = 909,
            vat = 91,
            taxFreeAmount = 0,
            cancels = emptyList(),
            method = "CARD"
        )

        assertEquals(0, response.cancels.size)
        assertEquals("CANCELLED", response.status)
    }

    @Test
    fun `대용량 금액 처리 테스트`() {
        val largeAmount = 999_999_999
        val request = TossPaymentConfirmRequest("large_key", "LARGE-ORDER", largeAmount)

        assertEquals(largeAmount, request.amount)
    }

    @Test
    fun `특수 문자 처리 테스트`() {
        val specialChars = "ORDER-특수문자@#$%^&*()"
        val request = TossPaymentConfirmRequest("special_key", specialChars, 1000)

        assertEquals(specialChars, request.orderId)
    }

    @Test
    fun `optional 필드 기본값 테스트`() {
        val cancelRequest = TossPaymentCancelRequest(
            cancelReason = "테스트 취소"
            // cancelAmount, refundReceiveAccount는 default null
        )

        assertEquals("테스트 취소", cancelRequest.cancelReason)
        assertEquals(null, cancelRequest.cancelAmount)
        assertEquals(null, cancelRequest.refundReceiveAccount)
    }
}