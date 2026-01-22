package com.popcorn.demo.common.dto;

/**
 * 공통 응답 코드 인터페이스
 *
 * 모든 도메인의 응답 코드가 구현해야 하는 기본 메서드를 정의합니다.
 */
public interface ResponseCode {

    int getCode();

    int getHttpStatus();

    String getMessage();
}