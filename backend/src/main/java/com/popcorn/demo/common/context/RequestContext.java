package com.popcorn.demo.common.context;

public final class RequestContext {
    private static final ThreadLocal<RequestMetadata> CONTEXT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(RequestMetadata metadata) {
        CONTEXT.set(metadata);
    }

    public static RequestMetadata get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 요청 단위 식별 정보 (필수 최소 정보만 보관)
     */
    public record RequestMetadata(String traceId, String path) {
    }
}
