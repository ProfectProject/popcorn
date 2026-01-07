# API 명세서

## 작성 방법

1. **API Endpoints** → 우측 상단 **새로 만들기** → **New API**
2. 필수 필드 입력

| 페이지 제목       | API 명칭(설명)                                  |
|--------------|---------------------------------------------|
| Domain       | API 대상 도메인                                  |
| Method       | HTTP 요청 메소드 (GET, POST, PUT, PATCH, DELETE) |
| Endpoint URL | HTTP 요청 URL                                 |
| Status       | 진행 상황                                       |

3. 선택 필드 입력

| Field              | 설명                             |
|--------------------|--------------------------------|
| Query              | 쿼리스트링 사용 여부                    |
| Parameters (페이지 내) | 쿼리 파라미터 목록                     |
| Request Type       | 요청 본문 DTO 또는 타입 (없을 경우 없다고 표시) |
| Response Type      | 응답 본문 DTO 또는 타입 (없을 경우 없다고 표시) |
| Authentication     | 인증 여부(Bearer)                  |
| Authorization      | 권한 수준 (사용자, 관리자 ..)            |

---

## 0) Common

### Headers

| Key             | Value            | Required | Note                        |
|-----------------|------------------|----------|-----------------------------|
| Authorization   | Bearer `<token>` | Y        | 로그인 필요                      |
| Content-Type    | application/json | Y        |                             |
| Idempotency-Key | `<uuid>`         | N        | 생성/결제 중복 방지 권장 (POST/PATCH) |

### Roles

| Role           | 권한 요약                                |
|----------------|--------------------------------------|
| CUSTOMER       | 예약/주문 생성, 내 예약/주문 조회, 취소 요청만 가능      |
| OWNER          | 내 행사 예약/내 가게 주문 조회, 운영 상태 변경 가능      |
| MANAGER        | 할당된 store/popup 범위 내 조회/운영 변경/체크인 가능 |
| ADMIN (MASTER) | 전체 조회, 예외/강제 상태 변경, 감사 로그 조회         |

### Status

#### ReservationStatus

- `REQUESTED` → `PAYMENT_PENDING` → `RESERVED` → `CHECKED_IN` → `CANCELED`

#### OrderStatus

- `REQUESTED` → `OWNER_ACCEPTED` / `OWNER_REJECTED` → `PAYMENT_PENDING` → `PAID` → `COMPLETED` → `CANCELED`

#### PaymentStatus

- `READY` → `PAID` / `FAILED` / `CANCELED`

### Error Response (Common)

| Field   | Type   | Note   |
|---------|--------|--------|
| code    | string | 에러 코드  |
| message | string | 에러 메시지 |
| traceId | string | 추적 ID  |

---

## 1) API Endpoints

표는 Notion `API Endpoints (1)`을 기준으로 카테고리별로 정리했습니다.

### 1-1) 회원/인증

| API 명칭 | Method | Endpoint URL           | Query | Request Type | Response Type  | Authentication | Authorization | Status | Status Detail |
|--------|--------|------------------------|-------|--------------|----------------|----------------|---------------|--------|---------------|
| 회원가입   | POST   | `/api/v1/users/signup` | -     | JSON         | SignupResponse | -              | -             | 완료     | -             |
| 로그인    | POST   | `/api/v1/auth/login`   | -     | JSON         | LoginResponse  | -              | -             | 완료     | -             |

### 1-2) 사용자/주소

| API 명칭       | Method | Endpoint URL                                   | Query | Request Type | Response Type             | Authentication | Authorization | Status | Status Detail |
|--------------|--------|------------------------------------------------|-------|--------------|---------------------------|----------------|---------------|--------|---------------|
| 계정 삭제(비활성화)  | DELETE | `/api/v1/users/me/deactivate`                  | -     | 없음           | 없음                        | Bearer         | 본인(사용자)       | 완료     | -             |
| 비밀번호 변경      | PATCH  | `/api/v1/users/me/password`                    | -     | JSON         | 메시지(String)               | Bearer         | 본인(사용자)       | 완료     | -             |
| 사용자 정보 업데이트  | PUT    | `/api/v1/users/{userId}`                       | -     | JSON         | UserResponse              | Bearer         | 본인(사용자)       | 완료     | -             |
| 사용자 정보 조회    | GET    | `/api/v1/users/{userId}`                       | -     | 없음           | UserResponse              | Bearer         | 본인(사용자)       | 완료     | -             |
| 사용자 주소 목록 조회 | GET    | `/api/v1/users/{userId}/addresses`             | -     | 없음           | List<UserAddressResponse> | Bearer         | 본인(사용자)       | 완료     | -             |
| 배송지 설정       | PUT    | `/api/v1/users/{userId}/addresses/{addressId}` | -     | 없음           | UserAddressResponse       | Bearer         | 본인(사용자)       | 완료     | -             |
| 사용자 주소 수정    | PUT    | `/api/v1/users/{userId}/addresses/{addressId}` | -     | JSON         | UserAddressResponse       | Bearer         | 본인(사용자)       | 완료     | -             |
| 사용자 주소 삭제    | DELETE | `/api/v1/users/{userId}/addresses/{addressId}` | -     | 없음           | 없음                        | Bearer         | 본인(사용자)       | 완료     | -             |

### 1-3) 스토어/팝업(OWNER)

| API 명칭                     | Method | Endpoint URL                           | Query | Request Type       | Response Type             | Authentication | Authorization | Status | Status Detail |
|----------------------------|--------|----------------------------------------|-------|--------------------|---------------------------|----------------|---------------|--------|---------------|
| 내 가게 정보 조회                 | GET    | `/api/v1/owner/{ownerid}/stores`       | -     | 없음                 | StoreResponse or Map      | Bearer         | 본인(가게)        | 완료     | -             |
| 가게 등록                      | POST   | `/api/v1/owner/stores`                 | -     | StoreCreateRequest | StoreResponse             | Bearer         | 본인(가게)        | 완료     | -             |
| 가게 기본 정보 수정                | PUT    | `/api/v1/owner/store/{storeId}`        | -     | StoreUpdateRequest | StoreResponse             | Bearer         | 본인(가게)        | 완료     | -             |
| 가게 삭제                      | DELETE | `/api/v1/owner/stores/{storeId}`       | -     | 없음                 | 없음                        | Bearer         | 본인(가게)        | 완료     | -             |
| 제품 상태 수정                   | PATCH  | `/api/v1/owner/store/{storeId}/status` | -     | StoreStatusRequest | StoreStatusModifyResponse | Bearer         | 본인(가게)        | 완료     | -             |
| 내 제품 정보 조회                 | GET    | `/api/v1/owner/popups`                 | -     | 없음                 | String                    | Bearer         | 본인(가게)        | 완료     | -             |
| 제품 등록                      | POST   | `/api/v1/owner/popups`                 | -     | PopupCreateRequest | PopupResponse                        | Bearer              | OWNER             | 완료   | -             |
| 제품 기본 정보 수정 (장소, 이름, 카테고리) | PUT    | `/api/v1/owner/popups/{popupId}`       | -     | PopupUpdateRequest | PopupResponse                        | Bearer              | OWNER             | 완료   | -             |
| 제품 삭제                      | DELETE | `/api/v1/owner/popups/{popupId}`       | -     | 없음                 | 없음                        | Bearer              | OWNER             | 완료   | -             |

### 1-4) 팝업 회차(OWNER)

| API 명칭           | Method | Endpoint URL                                                   | Query | Request Type           | Response Type           | Authentication | Authorization | Status | Status Detail |
|------------------|--------|----------------------------------------------------------------|-------|------------------------|-------------------------|----------------|---------------|--------|---------------|
| ⏰ 제품 회차 조회       | GET    | `/api/v1/owner/popups/{popupId}/schedules`                     | -     | 없음                     | List<StoreHourResponse> | Bearer         | 본인(가게)        | 완료     | -             |
| ⏰ 제품 회차 등록       | POST   | `/api/v1/owner/popups/{popupId}/schedules`                     | -     | StoreLocationRequest   | StoreResponse           | Bearer         | 본인(가게)        | 완료     | -             |
| ⏰ 제품 회차 정보 수정    | PUT    | `/api/v1/owner/popups/{popupId}/schedules/{scheduleId}`        | -     | List<StoreHourRequest> | List<StoreHourResponse> | Bearer         | 본인(가게)        | 완료     | -             |
| ⏰ 제품 회차 활성 상태 수정 | PATCH  | `/api/v1/owner/popups/{popupId}/schedules/{scheduleId}/active` | -     | StoreContactRequest    | StoreResponse           | Bearer         | 본인(가게)        | 완료     | -             |
| ⏰ 제품 회차 삭제       | DELETE | `/api/v1/owner/popups/{popupId}/schedules/{scheduleId}`        | -     | MultipartFile          | String                  | Bearer         | 본인(가게)        | 완료     | -             |

### 1-5) 팝업 굿즈(OWNER)

| API 명칭         | Method | Endpoint URL                                            | Query | Request Type | Response Type | Authentication | Authorization | Status | Status Detail |
|----------------|--------|---------------------------------------------------------|-------|--------------|---------------|----------------|---------------|--------|---------------|
| 제품 굿즈 조회       | GET    | `/api/v1/owner/popups/{popupId}/goods`                  | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 제품 굿즈 등록       | POST   | `/api/v1/owner/popups/{popupId}/goods`                  | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 제품 굿즈 기본 정보 수정 | PUT    | `/api/v1/owner/popups/{popupId}/goods/{goods}`          | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 제품 굿즈 삭제       | DELETE | `/api/v1/owner/popups/{popupId}/goods/{goodsId}`        | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 제품 굿즈 상태 변경    | PATCH  | `/api/v1/owner/popups/{popupId}/goods/{goodsId}/status` | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |

### 1-6) 상품(고객)

| API 명칭    | Method | Endpoint URL                        | Query | Request Type | Response Type | Authentication | Authorization | Status | Status Detail |
|-----------|--------|-------------------------------------|-------|--------------|---------------|----------------|---------------|--------|---------------|
| 상품 목록 조회  | GET    | `/api/v1/popups`                    | -     | 없음           | 없음            | Bearer         | -             | 완료     | -             |
| 상품 상세 조회  | GET    | `/api/v1/popups/{popupId}`          | -     | 없음           | 없음            | Bearer         | -             | 완료     | -             |
| 회차(슬롯) 조회 | GET    | `/api/v1/popups/{popupId}/sessions` | -     | 없음           | 없음            | Bearer         | -             | 완료     | -             |
| 옵션 조회     | GET    | `/api/v1/popups/{popupId}/options`  | -     | 없음           | 없음            | Bearer         | -             | 시작 전   | -             |

### 1-7) 주문/예약

| API 명칭                                        | Method | Endpoint URL                                     | Query | Request Type | Response Type  | Authentication | Authorization                         | Status | Status Detail |
|-----------------------------------------------|--------|--------------------------------------------------|-------|--------------|----------------|----------------|---------------------------------------|--------|---------------|
| 주문 생성/예약 생성                                   | POST   | `/api/v1/orders`                                 | -     | 없음           | 없음             | Bearer         | CUSTOMER                              | 완료     | -             |
| 주문 상세 조회 (CUSTOMER / OWNER / MANAGER / ADMIN) | GET    | `/api/v1/orders/{orderId}`                       | -     | 없음           | OrderDetailDto | Bearer         | -                                     | 완료     | -             |
| 주문/예약 상세 조회 (CUSTOMER 포함)                     | POST   | `/api/v1/orders/{orderId}`                       | -     | 없음           | 없음             | Bearer         | CUSTOMER(본인)/OWNER/MANAGER(범위)/MASTER | 완료     | -             |
| 내 주문/예약 목록 (CUSTOMER)                         | GET    | `/api/v1/orders/me`                              | -     | 없음           | 없음             | Bearer         | CUSTOMER                              | 완료     | -             |
| 주문/예약 상태만 확인 (CUSTOMER)                       | GET    | `/api/v1/orders/{orderId}/status`                | -     | 없음           | OrderStatusDto | -              | -                                     | 완료     | -             |
| 주문/예약 상태 변경 (OWNER / MANAGER 운영)              | PATCH  | `/api/v1/orders/{orderId}/status`                | -     | 없음           | 없음             | Bearer         | OWNER                                 | 완료     | -             |
| 주문/예약 취소 요청 (CUSTOMER)                        | DELETE | `/api/v1/orders/{orderId}/cancel`                | -     | 없음           | 없음             | Bearer         | CUSTOMER                              | 완료     | -             |
| 내 가게 주문/예약 목록 (OWNER / MANAGER)               | GET    | `/api/v1/orders/store/{storeId}`                 | -     | 없음           | 없음             | Bearer         | OWNER / MANAGER                       | 완료     | -             |
| 주문 상태 목록 조회 (OWNER / MANAGER)                | GET    | `/api/v1/orders/status/ops`                     | Y     | 없음           | 없음             | Bearer         | OWNER / MANAGER                       | 완료     | storeId 또는 popupId 필수 |
| 개별 주문 상태 조회 (OWNER / MANAGER)                | GET    | `/api/v1/orders/{orderId}/status/ops`            | -     | 없음           | 없음             | Bearer         | OWNER / MANAGER                       | 완료     | -             |
| 제품 주문 내역 조회                                   | GET    | `/api/v1/owner/popups/{popupId}/order`           | -     | 없음           | PopupOrderListResponse             | Bearer              | OWNER                                     | 완료   | -             |
| 제품 주문 내역 상세 조회                                | GET    | `/api/v1/owner/popups/{popupId}/order/{orderId}` | -     | 없음           | PopupOrderDetailResponse             | Bearer              | OWNER                                     | 완료   | -             |

### 1-8) 결제

| API 명칭          | Method | Endpoint URL                           | Query | Request Type | Response Type | Authentication | Authorization | Status | Status Detail |
|-----------------|--------|----------------------------------------|-------|--------------|---------------|----------------|---------------|--------|---------------|
| 주문 결제 기록 생성     | POST   | `/api/v1/orders/{orderId}/payments`    | -     | 없음           | 없음            | Bearer         | CUSTOMER      | 시작 전   | -             |
| 예약 결제 기록 생성     | POST   | `/api/v1/orders/{orderId}/payments`    | -     | 없음           | 없음            | Bearer         | CUSTOMER      | 시작 전   | -             |
| 결제 생성(READY)    | POST   | `/api/v1/orders/{orderId}/payments`    | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 결제 조회(주문 기준)    | GET    | `/api/v1/orders/{orderId}/payments`    | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 결제 단건 조회(결제ID)  | GET    | `/api/v1/payments/{paymentId}`         | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 결제 승인 처리(성공 확정) | POST   | `/api/v1/payments/{paymentId}/approve` | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 결제 실패 처리        | POST   | `/api/v1/payments/{paymentId}/fail`    | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 결제 취소/환불        | POST   | `/api/v1/payments/{paymentId}/cancel`  | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |
| 결제 삭제(소프트 삭제)   | DELETE | `/api/v1/payments/{paymentId}`         | -     | 없음           | 없음            | -              | -             | 시작 전   | -             |

### 1-9) QR/체크인

| API 명칭                            | Method | Endpoint URL                   | Query | Request Type | Response Type | Authentication | Authorization           | Status | Status Detail |
|-----------------------------------|--------|--------------------------------|-------|--------------|---------------|----------------|-------------------------|--------|---------------|
| QR 발급 (CUSTOMER, RESERVED 상태)     | POST   | `/api/v1/orders/{orderId}/qr`  | -     | 없음           | 없음            | Bearer         | CUSTOMER                | 시작 전   | -             |
| QR 조회 (표시용)                       | GET    | `/api/v1/orders/{orderId}/qr`  | -     | 없음           | 없음            | Bearer         | CUSTOMER                | 시작 전   | -             |
| QR 검증(스캔 전용)                      | POST   | `/api/v1/qr/verify`            | -     | 없음           | 없음            | -              | -                       | 시작 전   | -             |
| 체크인 목록 조회 (OWNER/ADMIN 또는 스캐너 계정) | POST   | `/api/v1/checkins`             | -     | 없음           | 없음            | Bearer         | OWNER/ADMIN(or Scanner) | 시작 전   | -             |
| 체크인 상세 조회                         | GET    | `/api/v1/checkins/{checkinId}` | -     | 없음           | 없음            | -              | -                       | 시작 전   | -             |

### 1-10) 관리자/운영(OWNER/MANAGER)

| API 명칭           | Method | Endpoint URL                                      | Query | Request Type             | Response Type             | Authentication | Authorization   | Status  | Status Detail          |
|------------------|--------|---------------------------------------------------|-------|--------------------------|---------------------------|----------------|-----------------|---------|------------------------|
| (승인된)팝업 주문 취소    | POST   | `/api/v1/manager/popups?status=CANCELED`          | Y     | OrderCancelRequest       | OrderCancelResponse       | -              | OWNER / MANAGER | 시작 전    | 승인된 팝업 데이터만 들어가는게 맞는지. |
| (승인된)팝업 주문 변경    | PUT    | `/api/v1/manager/popups/{popupId}/status`         | -     | OrderStatusUpdateRequest | OrderStatusUpdateResponse | -              | OWNER / MANAGER | 시작 전    | 내부처리 status = APPROVED |
| (승인된)팝업 주문 상세 조회 | GET    | `/api/v1/manager/popups/{popupId}`                | -     | 없음                       | OrderDetailResponse       | -              | OWNER / MANAGER | 시작 전    | -                      |
| (승인된)팝업 주문 목록 조회 | GET    | `/api/v1/manager/popups?status=APPROVED`          | Y     | 없음                       | OrderListResponse         | -              | OWNER / MANAGER | 시작 전    | -                      |
| (승인된)팝업 강제 중단    | POST   | `/api/v1/manager/popups/{popupId}/force_stop`     | -     | StoreForceStopRequest    | StoreForceStopResponse    | -              | OWNER / MANAGER | 시작 전    | -                      |
| 팝업 게시 승인         | POST   | `/api/v1/manager/popups/{popupId}/approve`        | -     | StoreApproveRequest      | StoreApproveResponse      | -              | OWNER / MANAGER | 시작 전    | 승인된 데이터 생성             |
| 팝업 게시 반려         | POST   | `/api/v1/manager/popups/{popupId}/reject`         | -     | StoreRejectRequest       | StoreRejectResponse       | -              | OWNER / MANAGER | 시작 전    | -                      |
| 팝업 승인 전 취소(철회)   | POST   | `/api/v1/manager/popups/{popupId}/withdraw`       | -     | StoreWithdrawRequest     | StoreWithdrawResponse     | -              | -               | 시작 전    | -                      |
| 팝업 검토 대기 목록 조회   | GET    | `/api/v1/manager/popups/pending`                  | -     | 없음                       | PendingStoreListResponse  | -              | OWNER / MANAGER | 시작 전    | -                      |
| 주인(Owner) 계정승인   | POST   | `/api/v1/manager/owner/{userId}/approve`          | -     | 없음                       | OwnerApproveResponse      | -              | OWNER / MANAGER | 시작 전    | -                      |
| 주인(Owner) 계정중지   | POST   | `/api/v1/manager/owner/{userId}/force_stop`       | -     | OwnerForceStopRequest    | OwnerForceStopResponse    | -              | OWNER / MANAGER | 시작 전    | -                      |
| 사용자(User) 계정 중지  | POST   | `/api/v1/manager/users/{userId}/force_stop`       | -     | UserForceStopRequest     | UserForceStopResponse     | -              | OWNER / MANAGER | 시작 전    | -                      |
| 고객 문의 목록 조회      | GET    | `~~/api/v1/manager/inquiries~~`                   | -     | 없음                       | InquiryListResponse       | -              | OWNER / MANAGER | 시작 전 보류 | -                      |
| 고객 문의 답변 처리      | POST   | `~~/api/v1/manager/inquiries/{inquiryId}/reply~~` | -     | InquiryReplyRequest      | InquiryReplyResponse      | -              | OWNER / MANAGER | 시작 전 보류 | -                      |
| 시스템 이슈 관리자 전달    | POST   | `~~/api/v1/manager/issues/escalate~~`             | -     | IssueEscalateRequest     | IssueEscalateResponse     | -              | OWNER / MANAGER | 시작 전 보류 | -                      |
| 시스템 이슈 관리 조회     | GET    | `~~/api/v1/manager/issues/search~~`               | -     | 없음                       | IssueSearchResponse       | -              | -               | 시작 전 보류 | -                      |

### 1-11) 관리자(ADMIN)

| API 명칭           | Method | Endpoint URL                        | Query | Request Type | Response Type | Authentication | Authorization | Status  | Status Detail |
|------------------|--------|-------------------------------------|-------|--------------|---------------|----------------|---------------|---------|---------------|
| 상태 강제 변경         | POST   | `/api/v1/admin/status-override`     | -     | 없음           | 없음            | Bearer         | 관리자           | 시작 전    | -             |
| 상태 변경 히스토리 조회    | GET    | `/api/v1/admin/status-histories`    | -     | 없음           | 없음            | Bearer         | 관리자           | 시작 전    | -             |
| 전체 주문 목록 (ADMIN) | GET    | `/api/v1/admin/orders`              | -     | 없음           | 없음            | Bearer         | 관리자           | 시작 전    | -             |
| 플랫폼 정책 조회        | GET    | `/api/v1/admin/policies`            | -     | 없음           | 없음            | -              | -             | 시작 전    | -             |
| 플랫폼 정책 수정        | PATCH  | `/api/v1/admin/policies/{policyId}` | -     | 없음           | 없음            | -              | -             | 시작 전    | -             |
| 시스템 장애 공지 등록     | POST   | `/api/v1/admin/notices`             | -     | 없음           | 없음            | -              | -             | 시작 전    | -             |
| 관리자 정보 조회        | GET    | `~~/api/v1/admin/{managerId}~~`     | -     | 없음           | 없음            | -              | 관리자           | 시작 전 보류 | -             |

---
