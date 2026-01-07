# 팝콘 프로젝트 데이터베이스 조회 가이드

## 테스트 계정 정보

### 생성된 테스트 계정들
```sql
-- 계정 조회
SELECT user_id, email, name, role FROM p_users WHERE role IN ('OWNER', 'MANAGER', 'CUSTOMER');
```

**계정 목록:**
- **OWNER 계정들:**
  - `owner1@store.com` - 최사장 (기존 계정)
  - `owner2@store.com` - 정대표 (기존 계정)
  - `owner@test.popcorn` - Test Owner (신규 생성)

- **MANAGER 계정들:**
  - `manager1@store.com` - 한매니저 (기존 계정)
  - `manager@test.popcorn` - Test Manager (신규 생성)

- **CUSTOMER 계정들:**
  - `customer@test.popcorn` - Test Customer (신규 생성)

**공통 패스워드:** `hello` (BCrypt 해시화됨)

## 테스트 데이터

### Store 데이터
```sql
-- 생성된 Store 조회
SELECT store_id, user_id, store_name, status FROM p_stores;
```

- `11111111-1111-1111-1111-111111111111` - Owner Test Store
- `22222222-2222-2222-2222-222222222222` - Second Test Store
- `33333333-3333-3333-3333-333333333333` - Manager Test Store

### Popup 데이터
```sql
-- 생성된 Popup 조회
SELECT popup_id, store_id, title, category, status FROM p_popups;
```

- `44444444-4444-4444-4444-444444444444` - Owner Popup 1 (FOOD)
- `55555555-5555-5555-5555-555555555555` - Owner Popup 2 (IDOL)
- `66666666-6666-6666-6666-666666666666` - Manager Popup (EXHIBITION)

### Order 데이터
```sql
-- 생성된 Order 조회
SELECT order_id, order_no, user_id, store_id, status, total_price FROM p_orders;
```

**여러 상태의 테스트 주문들:**
- `77777777-7777-7777-7777-777777777777` - ORDER-001 (REQUESTED, 15,000원)
- `88888888-8888-8888-8888-888888888888` - ORDER-002 (ACCEPTED, 25,000원)
- `99999999-9999-9999-9999-999999999999` - ORDER-003 (RESERVED, 20,000원)
- `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` - ORDER-004 (PAYMENT_PENDING, 18,000원)
- `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` - ORDER-005 (COMPLETED, 30,000원)

## JWT 설정 정보

### JWT Secret
```yaml
jwt:
  secret: dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItbG9jYWwtZGV2ZWxvcG1lbnQtb25seS1kb25vdC11c2UtaW4tcHJvZHVjdGlvbg==
  expiration: 3600000
```

### 로그인 API
```bash
# JSON 형식으로 로그인 요청
curl -X POST http://localhost:8081/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email": "owner@test.popcorn",
  "password": "hello"
}'
```

## Order API 엔드포인트 (OWNER/MANAGER 전용)

### 1. 주문 상태 단건 조회 (OWNER/MANAGER)
```bash
GET /api/v1/orders/{orderId}/status/ops
Authorization: Bearer <JWT_TOKEN>
```

### 2. 가게 주문 상태 목록 조회 (OWNER/MANAGER)
```bash
GET /api/v1/orders/status/ops?storeId={storeId}&status={status}
Authorization: Bearer <JWT_TOKEN>
```

### 3. 내 가게 주문 목록 (OWNER/MANAGER)
```bash
GET /api/v1/orders/store?storeId={storeId}&status={status}
Authorization: Bearer <JWT_TOKEN>
```

### 테스트 예시
```bash
# 예시: Owner Test Store의 REQUESTED 상태 주문들 조회
curl -H "Authorization: Bearer <JWT_TOKEN>" \
'http://localhost:8081/api/v1/orders/store?storeId=11111111-1111-1111-1111-111111111111&status=REQUESTED'
```

## enum 값들

### Order Status
- REQUESTED, ACCEPTED, REJECTED, RESERVED
- PAYMENT_PENDING, PAID, COMPLETED, CANCELLED

### Store Status
- DRAFT, PENDING, ACTIVE, SUSPENDED, CLOSED, HIDDEN

### Popup Category
- FOOD, IDOL, EXHIBITION, WORKSHOP, FASHION, BEAUTY
- LIFESTYLE, ART, GAME, TECH, SPORTS, BOOK, PET, ETC

### Popup Status
- DRAFT, REQUEST, APPROVED, OPEN, CLOSED, CANCELLED, HIDDEN

## 유용한 조회 쿼리들

### 사용자별 주문 현황
```sql
SELECT
    u.email, u.name, u.role,
    COUNT(o.order_id) as order_count,
    SUM(o.total_price) as total_amount
FROM p_users u
LEFT JOIN p_orders o ON u.user_id = o.user_id
GROUP BY u.user_id, u.email, u.name, u.role;
```

### 가게별 주문 통계
```sql
SELECT
    s.store_name,
    COUNT(o.order_id) as order_count,
    SUM(o.total_price) as total_revenue,
    o.status
FROM p_stores s
LEFT JOIN p_orders o ON s.store_id = o.store_id
GROUP BY s.store_id, s.store_name, o.status
ORDER BY total_revenue DESC;
```

### 완전한 주문 정보 조회
```sql
SELECT
    o.order_no,
    cu.name as customer_name,
    s.store_name,
    o.status,
    o.total_price,
    o.created_at
FROM p_orders o
JOIN p_users cu ON o.user_id = cu.user_id
JOIN p_stores s ON o.store_id = s.store_id
ORDER BY o.created_at DESC;
```

## 서버 실행 정보

**서버 포트:** 8081
**Health Check:** http://localhost:8081/actuator/health
**Swagger UI:** http://localhost:8081/swagger-ui.html

## 주의사항

- 모든 Order API는 JWT 인증이 필요합니다
- OWNER/MANAGER 역할만 가게 관련 주문 조회가 가능합니다
- 테스트 데이터는 여러 개가 생성되어 있어 반복 테스트가 가능합니다
- 데이터 삭제 시 Foreign Key 제약 조건을 주의하세요