# 테이블 명세서

## 스키마 컨벤션

- PK: `p_users.user_id` = BIGINT, 그 외 도메인 엔티티 = UUID
- FK: 참조 PK 타입과 동일 사용 (유저→BIGINT, 도메인→UUID)
- 시간 타입: `timestamp` 권장 (사용하는 테이블에서 `created_at`/`updated_at` NOT NULL, `deleted_at` NULL 허용)
- 네이밍: snake_case 소문자, 중복 접두어 지양

## ENUM 정의

### user_role

| Value    | 의미                      |
|----------|-------------------------|
| CUSTOMER | 일반 사용자 (예약/주문/결제)       |
| OWNER    | 스토어 및 행사 운영자            |
| MANAGER  | 운영 보조 인력 (현장 관리, 상태 변경) |

### store_status

| Value     | 의미                        |
|-----------|---------------------------|
| DRAFT     | 임시저장 상태 (스토어 정보 작성 중)     |
| PENDING   | 승인 대기 상태 (관리자 검토 중)       |
| ACTIVE    | 활성 상태 (정상 운영 중)           |
| SUSPENDED | 일시정지 상태 (운영 중단, 정지 사유 있음) |
| CLOSED    | 폐점 상태 (영구 운영 종료)          |
| HIDDEN    | 숨김 상태                     |

### popup_status

| Value     | 의미                     |
|-----------|------------------------|
| DRAFT     | 임시저장 상태 (팝업 정보 작성 중)   |
| REQUEST   | 승인 요청 상태 (관리자에게 승인 요청) |
| APPROVED  | 승인 완료 상태 (팝업 오픈 준비 완료) |
| OPEN      | 진행 중 상태 (예약/참여 가능)     |
| CLOSED    | 종료 상태 (팝업 일정 완료)       |
| CANCELLED | 취소 상태 (팝업 운영 취소)       |
| HIDDEN    | 숨김 상태                  |

### popup_category

| Value      | 의미                        |
|------------|---------------------------|
| FOOD       | 푸드 팝업 (레스토랑, 카페, 디저트 체험)  |
| IDOL       | 아이돌/연예인 팝업 (굿즈샵, 전시, 체험관) |
| EXHIBITION | 전시 팝업 (아트, 사진, 작품 전시)     |
| WORKSHOP   | 워크샵 (교육, 세미나, 실습)         |
| FASHION    | 패션 팝업 (의류, 액세서리 판매/체험)    |
| BEAUTY     | 뷰티 팝업 (화장품, 스킨케어 체험)      |
| LIFESTYLE  | 라이프스타일 (홈데코, 생활용품)        |
| ART        | 아트/문화 (갤러리, 문화 체험)        |
| GAME       | 게임 팝업 (게임 체험, e-스포츠)      |
| TECH       | 테크/IT (신기술 체험, 가젯 전시)     |
| SPORTS     | 스포츠 (운동 체험, 스포츠 브랜드)      |
| BOOK       | 도서 관련 (북카페, 작가 만남, 독서 모임) |
| PET        | 반려동물 (펫샵, 펫 카페, 반려동물 용품)  |
| ETC        | 기타 (위 카테고리에 속하지 않는 팝업)    |

### order_status

| Value           | 의미          |
|-----------------|-------------|
| REQUESTED       | 주문 생성       |
| ACCEPTED        | 운영자가 주문 수락  |
| REJECTED        | 운영자가 주문 거절  |
| RESERVED        | 수량/좌석 임시 확보 |
| PAYMENT_PENDING | 결제 대기       |
| PAID            | 결제 완료       |
| COMPLETED       | 이용/전달 완료    |
| CANCELLED       | 주문 취소       |

### payment_method

| Value    | 의미    |
|----------|-------|
| CARD     | 카드 결제 |
| TRANSFER | 계좌 이체 |
| EASY_PAY | 간편 결제 |

### payment_status

| Value     | 의미    |
|-----------|-------|
| READY     | 결제 생성 |
| PAID      | 결제 성공 |
| FAILED    | 결제 실패 |
| CANCELLED | 결제 취소 |

---

## 테이블 스키마

### 1. 사용자 (`p_users`)

> 서비스 가입 사용자

| 필드         | 타입           | NOT NULL | 제약           | 설명                           |
|------------|--------------|----------|--------------|------------------------------|
| user_id    | BIGINT       | YES      | PK           | 사용자 ID                       |
| email      | varchar(255) | YES      | UNIQUE       | 이메일                          |
| password   | varchar(255) | YES      |              | 비밀번호                         |
| phone      | varchar(11)  |          |              | 연락처                          |
| name       | varchar(100) | YES      |              | 이름                           |
| role       | user_role    | YES      |              | 역할(Manager, Owner, Customer) |
| is_active  | boolean      | YES      | DEFAULT true | 활성 여부                        |
| created_at | timestamp    | YES      |              | 생성 시각                        |
| updated_at | timestamp    | YES      |              | 수정 시각                        |
| deleted_at | timestamp    |          |              | 삭제 시각                        |
| created_by | BIGINT       |          |              | 행 생성자(운영자/시스템)               |
| updated_by | BIGINT       |          |              | 행 수정자                        |
| deleted_by | BIGINT       |          |              | 행 삭제자                        |

---

### 2. 사용자 주소 (`p_customer_addresses`)

> 사용자의 주소

| 필드          | 타입           | NOT NULL | 제약                            | 설명         |
|-------------|--------------|----------|-------------------------------|------------|
| addr_id     | UUID         | YES      | PK                            | 주소 ID      |
| user_id     | BIGINT       | YES      | FK(user_id → p_users.user_id) | 사용자 ID     |
| addr_name   | varchar(50)  | YES      |                               | 주소 별칭      |
| address1    | varchar(255) | YES      |                               | 기본 주소      |
| address2    | varchar(255) |          |                               | 상세 주소      |
| postal_code | varchar(10)  |          |                               | 우편번호       |
| is_default  | boolean      | YES      | DEFAULT false                 | 기본 주소 여부   |
| created_at  | timestamp    | YES      |                               | 생성 시각      |
| updated_at  | timestamp    | YES      |                               | 수정 시각      |
| deleted_at  | timestamp    |          |                               | 삭제 시각      |
| created_by  | BIGINT       |          |                               | 생성자(보통 본인) |
| updated_by  | BIGINT       |          |                               | 수정자        |
| deleted_by  | BIGINT       |          |                               | 삭제자        |

---

### 3. 스토어 (`p_stores`)

> 오너가 운영하는 스토어

| 필드         | 타입           | NOT NULL | 제약                            | 설명          |
|------------|--------------|----------|-------------------------------|-------------|
| store_id   | UUID         | YES      | PK                            | 스토어 ID      |
| user_id    | BIGINT       | YES      | FK(user_id → p_users.user_id) | 오너 사용자 ID   |
| store_name | varchar(100) | YES      |                               | 스토어명        |
| status     | store_status | YES      | DEFAULT DRAFT                 | 게시 상태       |
| reason     | varchar(500) |          |                               | 비고/정지 사유    |
| created_at | timestamp    | YES      |                               | 생성 시각       |
| updated_at | timestamp    | YES      |                               | 수정 시각       |
| deleted_at | timestamp    |          |                               | 삭제 시각       |
| created_by | BIGINT       |          |                               | 생성자(오너/운영자) |
| updated_by | BIGINT       |          |                               | 수정자         |
| deleted_by | BIGINT       |          |                               | 삭제자         |

---

### 4. 팝업 (`p_popups`)

> 팝업 정보

| 필드          | 타입             | NOT NULL | 제약                               | 설명              |
|-------------|----------------|----------|----------------------------------|-----------------|
| popup_id    | UUID           | YES      | PK                               | 팝업 ID           |
| store_id    | UUID           | YES      | FK(store_id → p_stores.store_id) | 스토어 ID          |
| title       | varchar(200)   | YES      |                                  | 제목              |
| description | text           |          |                                  | 설명              |
| category    | popup_category | YES      |                                  | 카테고리            |
| status      | popup_status   | YES      |                                  | 상태              |
| created_at  | timestamp      | YES      |                                  | 생성              |
| updated_at  | timestamp      | YES      |                                  | 수정              |
| deleted_at  | timestamp      |          |                                  | 삭제              |
| created_by  | BIGINT         |          |                                  | 생성자(스토어 오너/매니저) |
| updated_by  | BIGINT         |          |                                  | 수정자             |
| deleted_by  | BIGINT         |          |                                  | 삭제자             |

---

### 5. 팝업 스케줄 (`p_popup_schedules`)

> 팝업 게시 일자

| 필드                 | 타입        | NOT NULL | 제약                               | 설명         |
|--------------------|-----------|----------|----------------------------------|------------|
| schedule_id        | UUID      | YES      | PK                               | 스케줄 ID     |
| popup_id           | UUID      | YES      | FK(popup_id → p_popups.popup_id) | 팝업 ID      |
| start_at           | timestamp | YES      |                                  | 시작 시각      |
| end_at             | timestamp | YES      |                                  | 종료 시각      |
| price              | int       | YES      |                                  | 가격         |
| capacity           | int       | YES      |                                  | 총 수량       |
| remaining_capacity | int       | YES      |                                  | 잔여 수량      |
| is_active          | boolean   | YES      | DEFAULT false                    | 활성 여부      |
| created_at         | timestamp | YES      |                                  | 생성         |
| updated_at         | timestamp | YES      |                                  | 수정         |
| deleted_at         | timestamp |          |                                  | 삭제         |
| created_by         | BIGINT    |          |                                  | 생성자(팝업 담당) |
| updated_by         | BIGINT    |          |                                  | 수정자        |
| deleted_by         | BIGINT    |          |                                  | 삭제자        |

---

### 6. 굿즈 (`p_goods_variants`)

> 팝업 판매 굿즈

| 필드          | 타입           | NOT NULL | 제약                               | 설명          |
|-------------|--------------|----------|----------------------------------|-------------|
| goods_id    | UUID         | YES      | PK                               | 굿즈 ID       |
| popup_id    | UUID         | YES      | FK(popup_id → p_popups.popup_id) | 팝업 ID       |
| stock_unit  | varchar(64)  |          |                                  | 재고 관리 최소 단위 |
| goods_name  | varchar(100) | YES      |                                  | 이름          |
| goods_price | int          | YES      |                                  | 가격          |
| stock       | int          | YES      |                                  | 재고          |
| is_active   | boolean      |          | DEFAULT true                     | 노출 여부       |
| created_at  | timestamp    | YES      |                                  | 생성          |
| updated_at  | timestamp    | YES      |                                  | 수정          |
| deleted_at  | timestamp    |          |                                  | 삭제          |
| created_by  | BIGINT       |          |                                  | 생성자(팝업 담당)  |
| updated_by  | BIGINT       |          |                                  | 수정자         |
| deleted_by  | BIGINT       |          |                                  | 삭제자         |

---

### 7. 주문 (`p_orders`)

> 주문 내역

| 필드               | 타입           | NOT NULL | 제약                               | 설명            |
|------------------|--------------|----------|----------------------------------|---------------|
| order_id         | UUID         | YES      | PK                               | 주문 ID         |
| order_no         | varchar(32)  |          | UNIQUE                           | 주문 번호         |
| user_id          | BIGINT       | YES      | FK(user_id → p_users.user_id)    | 고객 ID         |
| store_id         | UUID         | YES      | FK(store_id → p_stores.store_id) | 스토어 ID        |
| status           | order_status | YES      |                                  | 주문 상태         |
| cancelable_until | timestamp    |          |                                  | 취소 가능 시각      |
| total_price      | int          | YES      |                                  | 총 금액          |
| created_at       | timestamp    | YES      |                                  | 생성            |
| updated_at       | timestamp    | YES      |                                  | 수정            |
| deleted_at       | timestamp    |          |                                  | 삭제            |
| created_by       | BIGINT       |          |                                  | 주문 생성자(보통 고객) |
| updated_by       | BIGINT       |          |                                  | 주문 수정자        |
| deleted_by       | BIGINT       |          |                                  | 주문 삭제자        |

---

### 8. 주문 항목 (`p_order_goods`)

> 주문에 포함된 옵션/굿즈

| 필드                 | 타입        | NOT NULL | 제약                                                      | 설명            |
|--------------------|-----------|----------|---------------------------------------------------------|---------------|
| order_goods_id     | UUID      | YES      | PK                                                      | 주문 항목 ID      |
| order_id           | UUID      | YES      | FK(order_id → p_orders.order_id)                        | 주문 ID         |
| schedule_option_id | UUID      |          | FK(schedule_option_id → p_schedule_options.schedule_id) | 스케줄 옵션 ID     |
| goods_variant_id   | UUID      |          | FK(goods_variant_id → p_goods_variants.goods_id)        | 굿즈 옵션 ID      |
| qty                | int       | YES      |                                                         | 수량            |
| unit_price         | int       | YES      |                                                         | 단가            |
| price              | int       | YES      |                                                         | 금액            |
| created_at         | timestamp | YES      |                                                         | 생성            |
| updated_at         | timestamp | YES      |                                                         | 수정            |
| deleted_at         | timestamp |          |                                                         | 삭제            |
| created_by         | BIGINT    |          |                                                         | 생성자(주문 작성 흐름) |
| updated_by         | BIGINT    |          |                                                         | 수정자           |
| deleted_by         | BIGINT    |          |                                                         | 삭제자           |

---

### 9. 결제 (`p_payments`)

> 결제 정보

| 필드          | 타입             | NOT NULL | 제약                               | 설명                   |
|-------------|----------------|----------|----------------------------------|----------------------|
| payment_id  | UUID           | YES      | PK                               | 결제 ID                |
| order_id    | UUID           | YES      | FK(order_id → p_orders.order_id) | 주문 ID                |
| method      | payment_method | YES      |                                  | 결제 수단                |
| status      | payment_status | YES      |                                  | 결제 상태                |
| amount      | int            | YES      |                                  | 결제 금액                |
| raw_payload | text           |          |                                  | PG 원본 데이터 (jsonb 권장) |
| approved_at | timestamp      |          |                                  | 승인 시각                |
| created_at  | timestamp      | YES      |                                  | 생성                   |
| updated_at  | timestamp      | YES      |                                  | 수정                   |
| deleted_at  | timestamp      |          |                                  | 삭제                   |
| created_by  | BIGINT         |          |                                  | 결제 생성자(고객/시스템)       |
| updated_by  | BIGINT         |          |                                  | 결제 수정자               |
| deleted_by  | BIGINT         |          |                                  | 결제 삭제자               |

---

### 10. 주문 상태 이력 (`p_order_status_histories`)

> 주문 상태 변경 로그

| 필드              | 타입           | NOT NULL | 제약                               | 설명          |
|-----------------|--------------|----------|----------------------------------|-------------|
| order_status_id | UUID         | YES      | PK                               | 주문 상태 이력 ID |
| order_id        | UUID         | YES      | FK(order_id → p_orders.order_id) | 주문 ID       |
| from_status     | order_status | YES      |                                  | 이전 상태       |
| to_status       | order_status | YES      |                                  | 변경 상태       |
| reason          | varchar(255) |          |                                  | 사유          |
| changed_at      | timestamp    | YES      |                                  | 변경 시각       |
| created_at      | timestamp    | YES      |                                  | 생성          |
| updated_at      | timestamp    | YES      |                                  | 수정          |
| deleted_at      | timestamp    |          |                                  | 삭제          |
| created_by      | BIGINT       |          |                                  | 상태 변경 요청자   |
| updated_by      | BIGINT       |          |                                  | 상태 변경 수정자   |
| deleted_by      | BIGINT       |          |                                  | 상태 변경 삭제자   |

---

### 11. QR 코드 (`p_order_qr_codes`)

> 주문별 입장용 QR 코드

| 필드         | 타입           | NOT NULL | 제약                               | 설명         |
|------------|--------------|----------|----------------------------------|------------|
| qr_id      | UUID         | YES      | PK                               | QR 코드 ID   |
| order_id   | UUID         | YES      | FK(order_id → p_orders.order_id) | 주문 ID      |
| qr_code    | varchar(255) | YES      |                                  | QR 코드 문자열  |
| expires_at | timestamp    |          |                                  | 만료 시각      |
| created_at | timestamp    | YES      |                                  | 생성         |
| created_by | BIGINT       |          |                                  | 생성자(보통 고객) |

---

### 12. 체크인 (`p_checkins`)

> QR 체크인 처리 (행 존재 시 입장 인정)

| 필드               | 타입        | NOT NULL | 제약                                            | 설명                           |
|------------------|-----------|----------|-----------------------------------------------|------------------------------|
| checkin_id       | UUID      | YES      | PK                                            | 체크인 ID                       |
| order_id         | UUID      | YES      |                                               | 주문 ID (p_orders.order_id)    |
| order_qr_code_id | UUID      | YES      | FK(order_qr_code_id → p_order_qr_codes.qr_id) | QR 코드 ID                     |
| created_at       | timestamp | YES      |                                               | 체크인 완료 시각                    |
| created_by       | BIGINT    |          |                                               | 체크인 수행자(OWNER / ADMIN / 스캐너) |
