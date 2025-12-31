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

    public record RequestMetadata(String traceId, String path) {
    }
}
