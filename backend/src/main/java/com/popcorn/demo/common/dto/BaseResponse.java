package com.popcorn.demo.common.dto;

import com.popcorn.demo.common.context.RequestContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class BaseResponse<T> {
    private String code;
    private String message;
    private T data;
    private String traceId;
    private String path;
    private OffsetDateTime timestamp;

    /**
     * 공통 응답 포맷 생성 (요청 메타데이터 포함)
     */
    public static <T> BaseResponse<T> of(String code, String message, T data) {
        RequestContext.RequestMetadata metadata = RequestContext.get();
        String traceId = metadata != null ? metadata.traceId() : null;
        String path = metadata != null ? metadata.path() : null;
        return BaseResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .traceId(traceId)
                .path(path)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> from(ResponseCode responseCode, T data) {
        return of(responseCode.getCode(), responseCode.getMessage(), data);
    }

    public static <T> BaseResponse<T> success(T data) {
        return from(ResponseCode.SUCCESS, data);
    }

    public static BaseResponse<Void> error(ResponseCode responseCode) {
        return from(responseCode, null);
    }
}
