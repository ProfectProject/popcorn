package com.popcorn.common.dto

/**
 * Kotlin 버전 API 응답 DTO
 * Java의 BaseResponse를 Kotlin으로 변환한 버전
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null
) {
    companion object {
        /**
         * 성공 응답 생성 (데이터 포함)
         */
        fun <T> success(data: T, message: String = "성공적으로 처리되었습니다."): ApiResponse<T> {
            return ApiResponse(
                code = CommonResponseCode.SUCCESS.code,
                message = message,
                data = data
            )
        }

        /**
         * 성공 응답 생성 (데이터 없음)
         */
        fun success(message: String = "성공적으로 처리되었습니다."): ApiResponse<Nothing?> {
            return ApiResponse(
                code = CommonResponseCode.SUCCESS.code,
                message = message,
                data = null
            )
        }

        /**
         * 에러 응답 생성
         */
        fun error(message: String, code: Int = 500): ApiResponse<Nothing?> {
            return ApiResponse(
                code = code,
                message = message,
                data = null
            )
        }

        /**
         * ResponseCode 기반 응답 생성
         */
        fun <T> of(responseCode: ResponseCode, data: T? = null): ApiResponse<T?> {
            return ApiResponse(
                code = responseCode.code,
                message = responseCode.message,
                data = data
            )
        }

        /**
         * 에러 응답 생성 (상세 정보 포함)
         */
        fun errorWithDetail(message: String, detail: String, code: Int = 500): ApiResponse<Map<String, String>> {
            return ApiResponse(
                code = code,
                message = message,
                data = mapOf("detail" to detail)
            )
        }
    }

    /**
     * 응답이 성공인지 확인
     */
    fun isSuccess(): Boolean = code == CommonResponseCode.SUCCESS.code

    /**
     * 응답이 에러인지 확인
     */
    fun isError(): Boolean = !isSuccess()
}