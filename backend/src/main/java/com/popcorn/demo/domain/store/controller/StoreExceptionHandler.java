package com.popcorn.demo.domain.store.controller;

/**
 * TODO: Phase 4 - 예외 처리 구현
 * [ ] @RestControllerAdvice(basePackages = "com.popcorn.demo.domain.store") 어노테이션 추가
 * [ ] 예외 핸들러 메서드 구현:
 *     - @ExceptionHandler(StoreNotFoundException.class) -> 404
 *     - @ExceptionHandler(StoreAccessDeniedException.class) -> 403  
 *     - @ExceptionHandler(DuplicateStoreNameException.class) -> 409
 * [ ] 표준화된 JSON 에러 응답 구조:
 *     {
 *       "error": "ERROR_CODE",
 *       "message": "에러 메시지",
 *       "timestamp": "2024-01-01T00:00:00"
 *     }
 */
public class StoreExceptionHandler {
}
