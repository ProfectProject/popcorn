# 모바일 딥링크 대응 계획

## 현재 결제 플로우 URL 구조
- 결제 페이지: `/payments?orderNo=xxx&amount=xxx&customerKey=xxx&paymentId=xxx`
- 성공 페이지: `/payments/success?paymentKey=xxx&orderId=xxx&amount=xxx`
- 실패 페이지: `/payments/fail?message=xxx&code=xxx`

## 딥링크 대응 시 고려사항

### 1. URL Scheme 설계
```
// 앱 내에서 결제 페이지로 이동
popcornapp://payments/order?orderNo=O20260113-235544&amount=30000

// 앱 내에서 결제 결과 페이지로 이동
popcornapp://payments/success?paymentKey=xxx&orderId=xxx&amount=xxx
popcornapp://payments/fail?message=xxx&code=xxx
```

### 2. Web to App 연동
- 웹에서 모바일 앱이 설치되어 있으면 딥링크로 이동
- 앱이 없으면 현재 웹 플로우 유지
- Universal Links (iOS) / App Links (Android) 고려

### 3. 보안 고려사항
- 딥링크에 민감한 정보 노출 최소화
- 토큰 기반 인증으로 파라미터 암호화 검토
- 결제 금액 등 중요 정보는 서버에서 재검증

### 4. 현재 구조에서 준비된 부분
- ✅ 파라미터 기반 URL 구조
- ✅ 에러 처리 및 상태 관리
- ✅ 모바일 반응형 UI
- ✅ 결제 성공/실패 분기 처리

### 5. 추가 구현 필요사항
- [ ] User-Agent 감지로 모바일/웹 분기
- [ ] 딥링크 fallback 처리
- [ ] 앱 설치 여부 확인 로직
- [ ] Intent URL / Custom URL Scheme 처리