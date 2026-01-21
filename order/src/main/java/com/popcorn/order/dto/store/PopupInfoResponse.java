package com.popcorn.order.dto.store;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Store 서비스로부터 받는 팝업 정보 응답
 *
 * [Java 초보자를 위한 가이드]
 *
 * 이 클래스가 하는 일:
 * - Store 마이크로서비스에서 팝업 정보를 조회할 때 받는 데이터 구조
 * - Order 서비스에서 팝업 제목, 매장 정보를 표시하기 위해 사용
 *
 * 팝업이란?
 * - 특정 기간 동안 진행되는 임시 매장이나 이벤트
 * - 예: "팝콘 브랜드 A의 한정 팝업스토어", "체험형 팝콘 만들기 이벤트"
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupInfoResponse {

    /** 팝업 ID */
    private UUID popupId;

    /** 팝업 제목 (고객에게 보여지는 이름) */
    private String title;

    /** 팝업 설명 */
    private String description;

    /** 매장 ID (이 팝업이 진행되는 매장) */
    private UUID storeId;

    /** 매장 정보 (중첩된 객체) */
    private StoreInfo storeInfo;

    /** 팝업 시작일 */
    private LocalDateTime startDate;

    /** 팝업 종료일 */
    private LocalDateTime endDate;

    /** 팝업 상태 */
    private String status;

    /**
     * 중첩된 매장 정보
     *
     * [Java 초보자 설명]
     * static class를 사용하는 이유:
     * - 이 클래스는 PopupInfoResponse 안에서만 사용됨
     * - 네임스페이스를 깔끔하게 정리할 수 있음
     * - JSON 구조에서 중첩된 객체를 표현할 때 유용
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private UUID storeId;
        private String name;
        private String address1;
        private String address2;
        private String phoneNumber;

        /**
         * Order 서비스의 LocationDto로 변환
         */
        public com.popcorn.order.dto.response.MyOrderTimelineResponse.LocationDto toLocationDto() {
            return com.popcorn.order.dto.response.MyOrderTimelineResponse.LocationDto.builder()
                .name(this.name != null ? this.name : "매장명 없음")
                .address1(this.address1 != null ? this.address1 : "주소 없음")
                .address2(this.address2 != null ? this.address2 : "")
                .build();
        }
    }

    /**
     * 팝업 제목 반환 (null 안전)
     */
    public String getSafeTitle() {
        return this.title != null ? this.title : "팝업 제목 없음";
    }

    /**
     * 매장 정보를 LocationDto로 변환
     */
    public com.popcorn.order.dto.response.MyOrderTimelineResponse.LocationDto getLocationDto() {
        if (this.storeInfo != null) {
            return this.storeInfo.toLocationDto();
        }
        return com.popcorn.order.dto.response.MyOrderTimelineResponse.LocationDto.builder()
            .name("매장 정보 없음")
            .address1("")
            .address2("")
            .build();
    }
}