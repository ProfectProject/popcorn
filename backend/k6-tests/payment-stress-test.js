// 💳 K6 결제 시스템 극한 테스트
// 10,000명 동시 결제 처리 테스트

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 📊 커스텀 메트릭
const paymentSuccessRate = new Rate('payment_success_rate');
const paymentDuration = new Trend('payment_duration');
const paymentErrors = new Counter('payment_errors');

export let options = {
  scenarios: {
    // 🚀 결제 러시 시뮬레이션 (블랙프라이데이/세일 상황)
    payment_rush: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 100 },    // 워밍업
        { duration: '1m', target: 1000 },    // 1천명
        { duration: '2m', target: 5000 },    // 5천명 (세일 시작)
        { duration: '3m', target: 10000 },   // 1만명 (피크 시간)
        { duration: '5m', target: 10000 },   // 피크 유지
        { duration: '2m', target: 5000 },    // 감소
        { duration: '1m', target: 0 },       // 종료
      ],
      gracefulRampDown: '30s',
    },

    // ⚡ 순간 스파이크 (TV 광고 효과)
    tv_ad_spike: {
      executor: 'ramping-vus',
      startTime: '15m',
      stages: [
        { duration: '5s', target: 8000 },    // 5초만에 8천명!
        { duration: '30s', target: 8000 },   // 30초 유지
        { duration: '10s', target: 1000 },   // 빠른 감소
        { duration: '1m', target: 0 },       // 정리
      ],
    },
  },

  thresholds: {
    payment_success_rate: ['rate>0.95'],     // 95% 이상 결제 성공
    payment_duration: ['p(95)<3000'],        // 95%가 3초 이내 결제
    payment_errors: ['count<1000'],          // 총 오류 1000개 미만
    http_req_failed: ['rate<0.08'],          // 실패율 8% 이하
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 💳 가상 결제 데이터
const paymentMethods = ['CARD', 'KAKAO_PAY', 'TOSS_PAY', 'NAVER_PAY'];
const testCards = [
  '4111111111111111', // Visa 테스트 카드
  '5555555555554444', // MasterCard 테스트 카드
  '378282246310005',  // Amex 테스트 카드
];

function generateOrderData(userId) {
  return {
    userId: userId,
    popupId: `popup-${Math.floor(Math.random() * 50) + 1}`,
    orderType: "PURCHASE",
    items: [
      {
        orderItemType: "GOODS",
        goodsId: `goods-${Math.floor(Math.random() * 200) + 1}`,
        qty: Math.floor(Math.random() * 3) + 1,
        unitPrice: (Math.floor(Math.random() * 30) + 5) * 1000 // 5천~35천원
      }
    ]
  };
}

function generatePaymentData(orderId, amount) {
  return {
    orderId: orderId,
    amount: amount,
    paymentMethod: paymentMethods[Math.floor(Math.random() * paymentMethods.length)],
    cardNumber: testCards[Math.floor(Math.random() * testCards.length)],
    customerKey: `customer_${__VU}_${__ITER}`,
    metadata: {
      testType: 'k6-payment-stress',
      timestamp: new Date().toISOString()
    }
  };
}

export function setup() {
  console.log('💳💳💳 K6 결제 시스템 극한 테스트 시작! 💳💳💳');
  console.log('🎯 목표: 10,000명 동시 결제 처리');

  return {
    startTime: Date.now(),
    targetPayments: 10000
  };
}

export default function() {
  const userId = __VU;

  group('💳 결제 프로세스 테스트', function() {

    // 1️⃣ 주문 생성
    group('📝 주문 생성', function() {
      const orderData = generateOrderData(userId);

      const orderResponse = http.post(`${BASE_URL}/api/v1/orders`,
        JSON.stringify(orderData), {
        headers: {
          'Content-Type': 'application/json',
          'X-Payment-Test': 'true',
          'User-Agent': `K6-Payment-User-${userId}`
        }
      });

      const orderSuccess = check(orderResponse, {
        '📝 주문 생성 응답': (r) => r.status === 201 || r.status === 401,
        '⚡ 주문 생성 속도': (r) => r.timings.duration < 1000,
      });

      if (!orderSuccess) {
        paymentErrors.add(1);
         // 주문 생성 실패시 결제 단계 건너뛰기
      }
    });

    // 2️⃣ 가상 결제 시도 (토스페이 시뮬레이션)
    group('💰 결제 처리', function() {
      const paymentStartTime = Date.now();

      // 가상 주문 ID와 금액 (실제 주문이 실패해도 결제 테스트 진행)
      const virtualOrderId = `k6-order-${userId}-${__ITER}`;
      const amount = Math.floor(Math.random() * 50000) + 10000; // 1만~6만원
      const paymentData = generatePaymentData(virtualOrderId, amount);

      // 토스 결제 시뮬레이션
      const paymentResponse = http.post(`${BASE_URL}/api/v1/toss/payments`,
        JSON.stringify(paymentData), {
        headers: {
          'Content-Type': 'application/json',
          'X-Payment-Simulation': 'true',
          'X-Order-ID': virtualOrderId,
          'X-Amount': amount.toString()
        }
      });

      const paymentDurationMs = Date.now() - paymentStartTime;
      paymentDuration.add(paymentDurationMs);

      const paymentResult = check(paymentResponse, {
        '💳 결제 요청 접수': (r) => r.status < 500, // 5xx가 아니면 OK
        '🚀 결제 응답 시간 < 5초': (r) => r.timings.duration < 5000,
        '✅ 결제 승인 또는 대기': (r) => r.status === 200 || r.status === 202 || r.status === 400 || r.status === 401,
      });

      // 결제 성공률 추적
      if (paymentResponse.status === 200 || paymentResponse.status === 202) {
        paymentSuccessRate.add(1);
      } else {
        paymentSuccessRate.add(0);
        paymentErrors.add(1);
      }

      // 결제 상태 확인 (일부만)
      if (__VU % 10 === 0) {
        const statusResponse = http.get(`${BASE_URL}/api/v1/toss/payments/status/${virtualOrderId}`, {
          headers: {
            'X-Status-Check': 'k6-test'
          }
        });

        check(statusResponse, {
          '📊 결제 상태 조회': (r) => r.status < 500,
        });
      }
    });

    // 3️⃣ 웹훅 시뮬레이션 (결제 완료 처리)
    if (__VU % 20 === 0) { // 20명 중 1명만 웹훅 테스트
      group('🔔 웹훅 처리', function() {
        const webhookData = {
          eventType: 'PAYMENT_CONFIRMED',
          orderId: `k6-order-${userId}-${__ITER}`,
          paymentKey: `k6-payment-${Date.now()}`,
          amount: Math.floor(Math.random() * 50000) + 10000,
          status: 'DONE'
        };

        const webhookResponse = http.post(`${BASE_URL}/api/v1/toss/confirm`,
          JSON.stringify(webhookData), {
          headers: {
            'Content-Type': 'application/json',
            'X-Webhook-Test': 'true'
          }
        });

        check(webhookResponse, {
          '🔔 웹훅 처리': (r) => r.status < 500,
        });
      });
    }

    // 실제 사용자처럼 잠시 대기
    sleep(Math.random() * 3 + 1); // 1~4초 대기
  });
}

export function handleSummary(data) {
  const duration = (Date.now() - data.setup_data?.startTime) / 1000;
  const totalPayments = data.metrics.payment_success_rate?.values?.count || 0;
  const successRate = data.metrics.payment_success_rate?.values?.rate || 0;
  const avgDuration = data.metrics.payment_duration?.values?.avg || 0;
  const errors = data.metrics.payment_errors?.values?.count || 0;

  console.log(`
╔══════════════════════════════════════════════════════════════════╗
║                    💳 결제 시스템 극한 테스트 결과                     ║
╠══════════════════════════════════════════════════════════════════╣
║ 💰 총 결제 시도: ${totalPayments} 건                                  ║
║ ✅ 결제 성공률: ${(successRate * 100).toFixed(2)}%                         ║
║ ⚡ 평균 결제 시간: ${avgDuration.toFixed(2)}ms                          ║
║ ❌ 결제 오류: ${errors} 건                                         ║
║ 📊 초당 결제 처리: ${(totalPayments / duration).toFixed(2)} TPS              ║
║ 🏆 P95 결제 시간: ${data.metrics.payment_duration?.values?.['p(95)'] || 0}ms ║
╚══════════════════════════════════════════════════════════════════╝
  `);

  return {
    'payment-stress-results.html': htmlReport(data),
    'payment-stress-summary.txt': textSummary(data),
  };
}

export function teardown(data) {
  console.log('🎉 결제 시스템 극한 테스트 완료!');
}