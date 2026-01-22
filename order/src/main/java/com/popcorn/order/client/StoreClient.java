package com.popcorn.order.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.popcorn.common.dto.BaseResponse;
import com.popcorn.order.dto.store.PopupInfoResponse;
import com.popcorn.order.dto.store.StoreInfoResponse;
import com.popcorn.order.dto.store.GoodsPriceResponse;
import com.popcorn.order.dto.store.SessionPriceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;

/**
 * Store 마이크로서비스와 통신하는 HTTP Client
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${microservices.store.base-url}")
    private String storeBaseUrl;

    /**
     * 굿즈 재고 예약 (주문 생성 시 호출)
     *
     * @param popupId 팝업 ID
     * @param goodsVariantId 굿즈 변형 ID
     * @param quantity 예약 수량
     * @throws IllegalStateException 재고 부족 또는 예약 실패 시
     */
    public void reserveGoods(UUID popupId, UUID goodsVariantId, Integer quantity) {
        log.info("재고 예약 요청 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);

        BaseResponse<Object> response = webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(storeBaseUrl + "/api/stores/v1/popups/{popupId}/goods/{goodsId}/reservation")
                        .queryParam("quantity", quantity)
                        .build(popupId, goodsVariantId))
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                .block();

        if (response == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "스토어 응답이 비어 있습니다.";
            throw new IllegalStateException("재고 예약 실패: " + message);
        }

        log.info("재고 예약 성공 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);
    }

    /**
     * 굿즈 예약 취소 (주문 취소 시 호출)
     *
     * @param popupId 팝업 ID
     * @param goodsVariantId 굿즈 변형 ID
     * @param quantity 취소 수량
     * @throws IllegalStateException 예약 취소 실패 시
     */
    public void cancelGoodsReservation(UUID popupId, UUID goodsVariantId, Integer quantity) {
        log.info("재고 예약 취소 요청 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);

        BaseResponse<Object> response = webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(storeBaseUrl + "/api/stores/v1/popups/{popupId}/goods/{goodsId}/reservation/cancel")
                        .queryParam("quantity", quantity)
                        .build(popupId, goodsVariantId))
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                .block();

        if (response == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "스토어 응답이 비어 있습니다.";
            throw new IllegalStateException("재고 예약 취소 실패: " + message);
        }

        log.info("재고 예약 취소 성공 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);
    }

    /**
     * 굿즈 예약 실패 처리 (시스템 오류 시 호출)
     *
     * @param popupId 팝업 ID
     * @param goodsVariantId 굿즈 변형 ID
     * @param quantity 실패 수량
     * @throws IllegalStateException 예약 실패 처리 실패 시
     */
    public void failGoodsReservation(UUID popupId, UUID goodsVariantId, Integer quantity) {
        log.info("재고 예약 실패 처리 요청 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);

        BaseResponse<Object> response = webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(storeBaseUrl + "/api/stores/v1/popups/{popupId}/goods/{goodsId}/reservation/fail")
                        .queryParam("quantity", quantity)
                        .build(popupId, goodsVariantId))
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                .block();

        if (response == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "스토어 응답이 비어 있습니다.";
            throw new IllegalStateException("재고 예약 실패 처리 실패: " + message);
        }

        log.info("재고 예약 실패 처리 성공 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);
    }

    /**
     * 굿즈 재고 차감 확정 (결제 완료 시 호출)
     *
     * @param popupId 팝업 ID
     * @param goodsVariantId 굿즈 변형 ID
     * @param quantity 확정 수량
     * @throws IllegalStateException 재고 차감 확정 실패 시
     */
    public void completeGoodsReservation(UUID popupId, UUID goodsVariantId, Integer quantity) {
        log.info("재고 차감 확정 요청 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);

        BaseResponse<Object> response = webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(storeBaseUrl + "/api/stores/v1/popups/{popupId}/goods/{goodsId}/reservation/complete")
                        .queryParam("quantity", quantity)
                        .build(popupId, goodsVariantId))
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                .block();

        if (response == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "스토어 응답이 비어 있습니다.";
            throw new IllegalStateException("재고 차감 확정 실패: " + message);
        }

        log.info("재고 차감 확정 성공 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);
    }

    /**
     * 매장 정보 조회 (Redis 캐시 적용)
     *
     * [Java 초보자를 위한 가이드]
     *
     * 이 메소드가 하는 일:
     * 1. Store 마이크로서비스에 HTTP 요청을 보내서 매장 정보를 조회
     * 2. JSON 응답을 Java 객체(StoreInfoResponse)로 변환
     * 3. 에러가 발생하면 기본값을 반환 (서비스 장애 시 복구력 제공)
     *
     * 캐시 적용:
     * - 매장 정보는 자주 변경되지 않으므로 30분간 캐시
     * - storeId를 키로 사용하여 같은 매장 정보는 캐시에서 즉시 반환
     * - Store 서비스 부하 감소 및 응답 시간 단축
     *
     * @param storeId 조회할 매장의 ID
     * @return 매장 정보 (이름, 주소, 전화번호 등)
     */
    @Cacheable(value = "store-info", key = "#storeId")
    public StoreInfoResponse getStoreInfo(UUID storeId) {
        if (storeId == null) {
            log.warn("매장 ID가 null입니다. 기본값을 반환합니다.");
            return createDefaultStoreInfo();
        }

        log.info("매장 정보 조회 요청 - storeId: {}", storeId);

        try {
            BaseResponse<StoreInfoResponse> response = webClientBuilder.build()
                .get()
                .uri(storeBaseUrl + "/api/stores/v1/{storeId}", storeId)
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<StoreInfoResponse>>() {})
                .block();

            if (response != null && response.getCode() == 200 && response.getData() != null) {
                log.info("매장 정보 조회 성공 - storeId: {}, name: {}", storeId, response.getData().getName());
                return response.getData();
            } else {
                log.warn("매장 정보 조회 실패 - storeId: {}, 응답: {}", storeId, response);
                return createDefaultStoreInfo();
            }

        } catch (Exception e) {
            log.error("매장 정보 조회 중 오류 발생 - storeId: {}, 에러: {}", storeId, e.getMessage(), e);
            return createDefaultStoreInfo();
        }
    }

    /**
     * 팝업 정보 조회 (매장 정보 포함, Redis 캐시 적용)
     *
     * [Java 초보자 설명]
     * 팝업 정보를 조회할 때 매장 정보도 함께 가져오는 이유:
     * - 팝업이 진행되는 매장의 위치를 고객에게 알려주기 위해
     * - 한 번의 API 호출로 필요한 모든 정보를 가져와서 성능 향상
     *
     * 캐시 적용:
     * - 팝업 정보는 가끔 변경되므로 15분간 캐시
     * - popupId를 키로 사용하여 같은 팝업 정보는 캐시에서 즉시 반환
     * - Order 서비스에서 팝업 정보는 매우 자주 조회되므로 성능 향상 효과 큼
     *
     * @param popupId 조회할 팝업의 ID
     * @return 팝업 정보 (제목, 설명, 매장 정보 포함)
     */
    @Cacheable(value = "popup-info", key = "#popupId")
    public PopupInfoResponse getPopupInfo(UUID popupId) {
        if (popupId == null) {
            log.warn("팝업 ID가 null입니다. 기본값을 반환합니다.");
            return createDefaultPopupInfo();
        }

        log.info("팝업 정보 조회 요청 - popupId: {}", popupId);

        try {
            BaseResponse<PopupInfoResponse> response = webClientBuilder.build()
                .get()
                .uri(storeBaseUrl + "/api/stores/v1/popups/{popupId}", popupId)
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<PopupInfoResponse>>() {})
                .block();

            if (response != null && response.getCode() == 200 && response.getData() != null) {
                log.info("팝업 정보 조회 성공 - popupId: {}, title: {}", popupId, response.getData().getTitle());
                return response.getData();
            } else {
                log.warn("팝업 정보 조회 실패 - popupId: {}, 응답: {}", popupId, response);
                return createDefaultPopupInfo();
            }

        } catch (Exception e) {
            log.error("팝업 정보 조회 중 오류 발생 - popupId: {}, 에러: {}", popupId, e.getMessage(), e);
            return createDefaultPopupInfo();
        }
    }

    /**
     * 굿즈 변형 가격 조회
     *
     * @param goodsVariantId 굿즈 변형 ID
     * @return 굿즈 가격 정보 (가격, 재고, 상태 포함)
     * @throws IllegalArgumentException 굿즈를 찾을 수 없는 경우
     */
    @Cacheable(value = "goods-price", key = "#goodsVariantId")
    public GoodsPriceResponse getGoodsVariantPrice(UUID goodsVariantId) {
        if (goodsVariantId == null) {
            log.warn("굿즈 변형 ID가 null입니다. 기본값을 반환합니다.");
            return createDefaultGoodsPrice(goodsVariantId);
        }

        log.info("굿즈 가격 조회 요청 - goodsVariantId: {}", goodsVariantId);

        try {
            BaseResponse<GoodsPriceResponse> response = webClientBuilder.build()
                .get()
                .uri(storeBaseUrl + "/api/stores/v1/goods/{goodsVariantId}/price", goodsVariantId)
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<GoodsPriceResponse>>() {})
                .block();

            if (response != null && response.getCode() == 200 && response.getData() != null) {
                log.info("굿즈 가격 조회 성공 - goodsVariantId: {}, price: {}원",
                        goodsVariantId, response.getData().getPrice());
                return response.getData();
            } else {
                log.warn("굿즈 가격 조회 실패 - goodsVariantId: {}, 응답: {}", goodsVariantId, response);
                return createDefaultGoodsPrice(goodsVariantId);
            }

        } catch (Exception e) {
            log.error("굿즈 가격 조회 중 오류 발생 - goodsVariantId: {}, 에러: {}", goodsVariantId, e.getMessage(), e);
            return createDefaultGoodsPrice(goodsVariantId);
        }
    }

    /**
     * 세션 가격 조회
     *
     * @param sessionId 세션 ID
     * @return 세션 가격 정보 (가격, 좌석 수, 상태 포함)
     * @throws IllegalArgumentException 세션을 찾을 수 없는 경우
     */
    @Cacheable(value = "session-price", key = "#sessionId")
    public SessionPriceResponse getSessionPrice(UUID sessionId) {
        if (sessionId == null) {
            log.warn("세션 ID가 null입니다. 기본값을 반환합니다.");
            return createDefaultSessionPrice(sessionId);
        }

        log.info("세션 가격 조회 요청 - sessionId: {}", sessionId);

        try {
            BaseResponse<SessionPriceResponse> response = webClientBuilder.build()
                .get()
                .uri(storeBaseUrl + "/api/stores/v1/sessions/{sessionId}/price", sessionId)
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<SessionPriceResponse>>() {})
                .block();

            if (response != null && response.getCode() == 200 && response.getData() != null) {
                log.info("세션 가격 조회 성공 - sessionId: {}, price: {}원",
                        sessionId, response.getData().getPrice());
                return response.getData();
            } else {
                log.warn("세션 가격 조회 실패 - sessionId: {}, 응답: {}", sessionId, response);
                return createDefaultSessionPrice(sessionId);
            }

        } catch (Exception e) {
            log.error("세션 가격 조회 중 오류 발생 - sessionId: {}, 에러: {}", sessionId, e.getMessage(), e);
            return createDefaultSessionPrice(sessionId);
        }
    }

    /**
     * 기본 굿즈 가격 정보 생성 (Store 서비스 장애 시 사용)
     */
    private GoodsPriceResponse createDefaultGoodsPrice(UUID goodsVariantId) {
        log.warn("Store 서비스 장애로 굿즈 기본 가격 반환 - goodsVariantId: {}", goodsVariantId);
        return GoodsPriceResponse.builder()
            .goodsVariantId(goodsVariantId)
            .productName("상품 정보를 불러올 수 없습니다")
            .price(25000) // 기본 가격
            .originalPrice(25000)
            .discountRate(0)
            .stockQuantity(0)
            .status("UNKNOWN")
            .currency("KRW")
            .build();
    }

    /**
     * 기본 세션 가격 정보 생성 (Store 서비스 장애 시 사용)
     */
    private SessionPriceResponse createDefaultSessionPrice(UUID sessionId) {
        log.warn("Store 서비스 장애로 세션 기본 가격 반환 - sessionId: {}", sessionId);
        return SessionPriceResponse.builder()
            .sessionId(sessionId)
            .popupId(null)
            .sessionName("세션 정보를 불러올 수 없습니다")
            .price(15000) // 기본 가격
            .originalPrice(15000)
            .discountRate(0)
            .availableSeats(0)
            .totalSeats(0)
            .status("UNKNOWN")
            .sessionStartTime(null)
            .sessionEndTime(null)
            .currency("KRW")
            .build();
    }

    /**
     * 기본 매장 정보 생성 (Store 서비스 장애 시 사용)
     *
     * [Java 초보자 설명]
     * 마이크로서비스에서 중요한 개념: Fallback (폴백)
     * - 다른 서비스가 장애를 일으켜도 현재 서비스는 계속 동작해야 함
     * - 완벽한 정보가 아니더라도 기본값을 제공해서 사용자 경험 유지
     */
    private StoreInfoResponse createDefaultStoreInfo() {
        return StoreInfoResponse.builder()
            .name("매장 정보를 불러올 수 없습니다")
            .address1("주소 정보 없음")
            .address2("")
            .phoneNumber("연락처 정보 없음")
            .status("UNKNOWN")
            .build();
    }

    /**
     * 기본 팝업 정보 생성 (Store 서비스 장애 시 사용)
     */
    private PopupInfoResponse createDefaultPopupInfo() {
        return PopupInfoResponse.builder()
            .title("팝업 정보를 불러올 수 없습니다")
            .description("")
            .storeInfo(PopupInfoResponse.StoreInfo.builder()
                .name("매장 정보 없음")
                .address1("")
                .address2("")
                .build())
            .status("UNKNOWN")
            .build();
    }

    private String resolveAuthHeader() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null || attrs.getRequest() == null) {
                return null;
            }
            return attrs.getRequest().getHeader("Authorization");
        } catch (Exception e) {
            return null;
        }
    }
}
