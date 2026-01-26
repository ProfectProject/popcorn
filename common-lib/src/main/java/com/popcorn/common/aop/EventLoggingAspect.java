package com.popcorn.common.aop;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EventLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(EventLoggingAspect.class);
    private static final int MAX_PAYLOAD_LENGTH = 500;
    private static final List<String> INCLUDED_PREFIXES = List.of("com.popcorn", "com.example");
    private static final ThreadLocal<Deque<Snapshot>> CONTEXT =
        ThreadLocal.withInitial(ArrayDeque::new);

    @Around("execution(* org.springframework.context.ApplicationEventPublisher.publishEvent(..))")
    public Object logPublishEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        Object event = extractEvent(joinPoint.getArgs());

        if (isLoggableEvent(event)) {
            log.info("Event published: {} payload={}", event.getClass().getName(), summarize(event));
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            if (isLoggableEvent(event)) {
                log.error("Event publish failed: {} error={}", event.getClass().getName(), e.toString());
            }
            throw e;
        }
    }

    @Around("@annotation(eventListener)")
    public Object logEventListener(ProceedingJoinPoint joinPoint, EventListener eventListener) throws Throwable {
        Object event = extractEvent(joinPoint.getArgs());
        String handler = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        boolean loggable = isLoggableEvent(event);

        if (loggable) {
            pushContext(event.getClass().getName(), handler);
            log.info("Event handled: {} handler={}", event.getClass().getName(), handler);
        }

        try {
            Object result = joinPoint.proceed();
            if (loggable) {
                log.info("Event handled complete: {} handler={} durationMs={} result={}",
                    event.getClass().getName(),
                    handler,
                    System.currentTimeMillis() - startTime,
                    summarizeResult(result));
            }
            return result;
        } catch (Exception e) {
            if (loggable) {
                log.error("Event handling failed: {} handler={} error={}",
                    event.getClass().getName(),
                    handler,
                    e.toString());
            }
            throw e;
        } finally {
            if (loggable) {
                popContext();
            }
        }
    }

    @Around("within(com.popcorn..*) && execution(* org.springframework.context.ApplicationListener+.onApplicationEvent(..))")
    public Object logApplicationListener(ProceedingJoinPoint joinPoint) throws Throwable {
        Object event = extractEvent(joinPoint.getArgs());
        String handler = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();
        boolean loggable = isLoggableEvent(event);

        if (loggable) {
            pushContext(event.getClass().getName(), handler);
            log.info("Event handled: {} handler={}", event.getClass().getName(), handler);
        }

        try {
            Object result = joinPoint.proceed();
            if (loggable) {
                log.info("Event handled complete: {} handler={} durationMs={} result={}",
                    event.getClass().getName(),
                    handler,
                    System.currentTimeMillis() - startTime,
                    summarizeResult(result));
            }
            return result;
        } catch (Exception e) {
            if (loggable) {
                log.error("Event handling failed: {} handler={} error={}",
                    event.getClass().getName(),
                    handler,
                    e.toString());
            }
            throw e;
        } finally {
            if (loggable) {
                popContext();
            }
        }
    }

    private static Object extractEvent(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }

    private static boolean isLoggableEvent(Object event) {
        if (event == null) {
            return false;
        }
        String packageName = event.getClass().getPackageName();
        for (String prefix : INCLUDED_PREFIXES) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String summarize(Object event) {
        String value = String.valueOf(event);
        if (value.length() <= MAX_PAYLOAD_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PAYLOAD_LENGTH) + "...(truncated)";
    }

    private static String summarizeResult(Object result) {
        if (result == null) {
            return "void";
        }
        return summarize(result);
    }

    private static void pushContext(String eventName, String handler) {
        Deque<Snapshot> stack = CONTEXT.get();
        stack.push(new Snapshot(eventName, handler));
        MDC.put("event.name", eventName);
        MDC.put("event.handler", handler);
    }

    private static void popContext() {
        Deque<Snapshot> stack = CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        Snapshot current = stack.peek();
        if (current == null) {
            MDC.remove("event.name");
            MDC.remove("event.handler");
            if (stack.isEmpty()) {
                CONTEXT.remove();
            }
        } else {
            MDC.put("event.name", current.eventName());
            MDC.put("event.handler", current.handler());
        }
    }

    private record Snapshot(String eventName, String handler) {
    }
}
