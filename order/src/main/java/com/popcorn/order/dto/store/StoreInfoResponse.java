package com.popcorn.order.dto.store;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Store 서비스로부터 받는 매장 정보 응답
 *
 * [Java 초보자를 위한 가이드]
 *
 * 이 클래스가 하는 일:
 * - Store 마이크로서비스에서 매장 정보를 조회할 때 받는 데이터 구조
 * - Order 서비스에서 매장명, 주소 정보를 표시하기 위해 사용
 * - HTTP 통신으로 JSON을 받아서 Java 객체로 변환할 때 사용
 *
 * 사용 예시:
 * 이벤트 기반 팝업/매장 조회 → StoreInfoResponse 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreInfoResponse {

    /** 매장 ID */
    private UUID storeId;

    /** 매장명 */
    private String name;

    /** 매장 주소 (기본 주소) */
    private String address1;

    /** 매장 상세 주소 */
    private String address2;

    /** 매장 전화번호 */
    private String phoneNumber;

    /** 매장 운영 상태 */
    private String status;

    /**
     * Order 서비스의 LocationDto로 변환하는 편의 메소드
     *
     * [Java 초보자 설명]
     * 이런 변환 메소드를 만드는 이유:
     * 1. 서로 다른 마이크로서비스 간에 DTO 구조가 다를 수 있음
     * 2. 변환 로직을 한 곳에 모아두면 유지보수가 쉬움
     * 3. 컴파일 타임에 오류를 잡을 수 있음
     */
    public com.popcorn.order.dto.response.MyOrderTimelineResponse.LocationDto toLocationDto() {
        return com.popcorn.order.dto.response.MyOrderTimelineResponse.LocationDto.builder()
            .name(this.name != null ? this.name : "매장명 없음")
            .address1(this.address1 != null ? this.address1 : "주소 없음")
            .address2(this.address2 != null ? this.address2 : "")
            .build();
    }
}
