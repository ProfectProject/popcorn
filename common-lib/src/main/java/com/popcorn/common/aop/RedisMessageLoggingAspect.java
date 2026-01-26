package com.popcorn.common.aop;

import java.nio.charset.StandardCharsets;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RedisMessageLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageLoggingAspect.class);
    private static final int MAX_PAYLOAD_LENGTH = 500;

    @Around("within(com.popcorn..*) && execution(* org.springframework.data.redis.connection.MessageListener.onMessage(..))")
    public Object logRedisMessage(ProceedingJoinPoint joinPoint) throws Throwable {
        Message message = extractMessage(joinPoint.getArgs());
        String channel = message != null ? decode(message.getChannel()) : "unknown";
        String body = message != null ? summarize(decode(message.getBody())) : "null";

        log.info("Redis message received: channel={} payload={}", channel, body);

        try {
            Object result = joinPoint.proceed();
            log.info("Redis message handled: channel={} handler={}", channel, joinPoint.getSignature().toShortString());
            return result;
        } catch (Exception e) {
            log.error("Redis message handling failed: channel={} handler={} error={}",
                channel,
                joinPoint.getSignature().toShortString(),
                e.toString());
            throw e;
        }
    }

    private static Message extractMessage(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        if (args[0] instanceof Message message) {
            return message;
        }
        return null;
    }

    private static String decode(byte[] value) {
        if (value == null) {
            return "";
        }
        return new String(value, StandardCharsets.UTF_8);
    }

    private static String summarize(String value) {
        if (value.length() <= MAX_PAYLOAD_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
    }
}
