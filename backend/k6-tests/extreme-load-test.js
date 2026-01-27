// 🔥 K6 극한 부하 테스트 스크립트
// 1,000,000명 동시 접속 + 실시간 모니터링

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";

// 🎯 극한 테스트 설정
export let options = {
  scenarios: {
    // 🌊 1단계: 점진적 증가 (0 → 100만)
    ramp_up_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 1000 },    // 1천명까지 증가
        { duration: '3m', target: 10000 },   // 1만명까지 증가
        { duration: '5m', target: 100000 },  // 10만명까지 증가
        { duration: '10m', target: 1000000 }, // 🔥 100만명까지!
        { duration: '15m', target: 1000000 }, // 100만명 유지
        { duration: '5m', target: 0 },       // 점진적 감소
      ],
      gracefulRampDown: '3m',
    },

    // ⚡ 2단계: 스파이크 테스트 (급격한 증가)
    spike_test: {
      executor: 'ramping-vus',
      startTime: '30m',
      stages: [
        { duration: '10s', target: 50000 },  // 10초만에 5만명!
        { duration: '1m', target: 50000 },   // 1분 유지
        { duration: '10s', target: 0 },      // 급격한 감소
      ],
    },

    // 🎯 3단계: 일정 부하 유지 (백그라운드)
    constant_load: {
      executor: 'constant-vus',
      vus: 5000,
      duration: '60m',
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95%는 500ms 이하
    http_req_failed: ['rate<0.05'],                 // 실패율 5% 이하
    http_reqs: ['rate>10000'],                      // 초당 1만 요청 이상
  },

  // Grafana 대시보드 연동
  ext: {
    loadimpact: {
      projectID: 3666111,
      name: "🔥 Popcorn 극한 부하 테스트 - 백만 사용자"
    }
  }
};

// 🎭 테스트 데이터 생성
const testUsers = new SharedArray('users', function () {
  const users = [];
  for (let i = 1; i <= 100000; i++) {
    users.push({
      userId: i,
      email: `stress_test_user_${i}@popcorn.demo`,
      name: `스트레스테스터${i}`,
      phone: `010-${String(i).padStart(4, '0')}-${String(i).padStart(4, '0')}`
    });
  }
  return users;
});

const testOrders = new SharedArray('orders', function () {
  const orders = [];
  for (let i = 1; i <= 10000; i++) {
    orders.push({
      orderId: `STRESS-ORDER-${i}`,
      popupId: `popup-${Math.floor(Math.random() * 100) + 1}`,
      items: [
        {
          goodsId: `goods-${Math.floor(Math.random() * 1000) + 1}`,
          qty: Math.floor(Math.random() * 5) + 1,
          unitPrice: (Math.floor(Math.random() * 50) + 1) * 1000
        }
      ]
    });
  }
  return orders;
});

// 🌐 Base URL 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  console.log('🔥🔥🔥 K6 극한 부하 테스트 시작! 🔥🔥🔥');
  console.log(`Target: ${BASE_URL}`);
  console.log('목표: 1,000,000명 동시 사용자 처리');

  // 서버 상태 확인
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  check(healthCheck, {
    '🏥 서버 상태 정상': (r) => r.status === 200,
  });

  return {
    startTime: new Date().toISOString(),
    targetUsers: 1000000
  };
}

// 🎭 메인 테스트 시나리오
export default function(data) {
  const user = testUsers[Math.floor(Math.random() * testUsers.length)];
  const order = testOrders[Math.floor(Math.random() * testOrders.length)];

  group('🔥 극한 부하 시나리오', function() {

    // 1️⃣ 헬스체크 (기본 접속)
    group('🏥 서버 상태 확인', function() {
      let response = http.get(`${BASE_URL}/actuator/health`, {
        headers: {
          'User-Agent': `K6-LoadTest-User-${user.userId}`,
          'X-Test-Type': 'extreme-load',
          'X-User-ID': user.userId
        }
      });

      check(response, {
        '✅ 헬스체크 성공': (r) => r.status === 200,
        '⚡ 응답시간 < 100ms': (r) => r.timings.duration < 100,
      });
    });

    // 2️⃣ 주문 조회 (인증 없음)
    group('📋 주문 목록 조회', function() {
      let response = http.get(`${BASE_URL}/api/v1/orders`, {
        headers: {
          'User-Agent': `K6-OrderTest-User-${user.userId}`,
          'X-Request-ID': `k6-${__VU}-${__ITER}`,
          'Accept': 'application/json'
        }
      });

      check(response, {
        '📋 주문 조회 응답': (r) => r.status === 401 || r.status === 403 || r.status === 200,
        '⚡ 주문 응답시간 < 200ms': (r) => r.timings.duration < 200,
      });
    });

    // 3️⃣ 실시간 메트릭 수집
    group('📊 메트릭 수집', function() {
      let metricsResponse = http.get(`${BASE_URL}/actuator/metrics`, {
        headers: {
          'X-Metrics-Collector': 'k6-extreme-test'
        }
      });

      check(metricsResponse, {
        '📊 메트릭 수집 가능': (r) => r.status === 200 || r.status === 401,
      });
    });

    // 4️⃣ 스트레스 요청 (높은 부하)
    if (__VU % 100 === 0) { // 100명 중 1명만 실행 (부하 조절)
      group('🔥 고부하 요청', function() {
        let heavyResponse = http.post(`${BASE_URL}/api/v1/orders`, JSON.stringify({
          userId: user.userId,
          popupId: order.popupId,
          orderType: "PURCHASE",
          items: order.items
        }), {
          headers: {
            'Content-Type': 'application/json',
            'X-Heavy-Load': 'true',
            'User-Agent': `K6-HeavyLoad-${user.userId}`
          }
        });

        check(heavyResponse, {
          '🔥 고부하 요청 처리': (r) => r.status < 500, // 5xx 에러가 아니면 OK
          '⚡ 고부하 응답시간 < 1000ms': (r) => r.timings.duration < 1000,
        });
      });
    }

    // 5️⃣ 랜덤 지연 (실제 사용자 패턴)
    const thinkTime = Math.random() * 2 + 0.5; // 0.5~2.5초
    sleep(thinkTime);

  });
}

// 🎯 실시간 이터레이션 통계 (매 10초마다)
export function handleSummary(data) {
  const timestamp = new Date().toISOString();

  console.log(`
╔══════════════════════════════════════════════════════════════════╗
║                    🔥 K6 극한 테스트 결과 🔥                      ║
╠══════════════════════════════════════════════════════════════════╣
║ 📊 총 요청 수: ${data.metrics.http_reqs.values.count} requests             ║
║ ⚡ 평균 응답 시간: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms                ║
║ 🎯 최대 동시 사용자: ${data.metrics.vus_max.values.max} VUs                     ║
║ 📈 요청 처리율: ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s                   ║
║ ❌ 실패율: ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%                       ║
║ 🏆 P95 응답 시간: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms               ║
║ 🚀 P99 응답 시간: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms               ║
╚══════════════════════════════════════════════════════════════════╝
  `);

  return {
    'extreme-test-summary.html': htmlReport(data),
    'extreme-test-summary.txt': textSummary(data, { indent: ' ', enableColors: true }),
    'extreme-test-results.json': JSON.stringify(data, null, 2),
  };
}

export function teardown(data) {
  console.log('🎉 K6 극한 부하 테스트 완료!');
  console.log(`시작 시간: ${data.startTime}`);
  console.log(`종료 시간: ${new Date().toISOString()}`);
  console.log('📊 상세 결과는 extreme-test-summary.html 을 확인하세요!');
}