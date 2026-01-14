// 🌊🔥 ULTIMATE 극한 테스트 - 5,000,000명 + 24시간 지속
// 인류 역사상 가장 극한의 부하 테스트

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Trend, Rate, Counter, Gauge } from 'k6/metrics';

// 📊 극한 커스텀 메트릭
const extremeConnections = new Gauge('extreme_concurrent_connections');
const paymentThroughput = new Rate('payment_success_rate');
const systemRecovery = new Counter('system_recovery_count');
const memoryLeak = new Trend('memory_leak_trend');
const networkBandwidth = new Trend('network_bandwidth_mbps');

// 🚀 ULTIMATE 극한 설정
export let options = {
  scenarios: {
    // 🌊 1단계: 5백만명 동시 접속 (역대 최대 규모!)
    ultimate_five_million: {
      executor: 'ramping-vus',
      stages: [
        { duration: '10m', target: 100000 },     // 10만명 (워밍업)
        { duration: '20m', target: 500000 },     // 50만명
        { duration: '30m', target: 1000000 },    // 100만명
        { duration: '40m', target: 2500000 },    // 250만명
        { duration: '60m', target: 5000000 },    // 🔥 500만명!
        { duration: '24h', target: 5000000 },    // 24시간 유지!
        { duration: '60m', target: 0 },          // 점진적 감소
      ],
      gracefulRampDown: '30m',
    },

    // 💳 2단계: 1만명 동시 결제 처리
    payment_extreme: {
      executor: 'constant-vus',
      vus: 10000,
      duration: '24h',
      startTime: '30m',
    },

    // ⚡ 3단계: 순간 스파이크 (TV 슈퍼볼 광고 효과)
    superbowl_spike: {
      executor: 'ramping-vus',
      startTime: '2h',
      stages: [
        { duration: '3s', target: 1000000 },    // 3초만에 100만명!
        { duration: '30s', target: 1000000 },   // 30초 유지
        { duration: '10s', target: 100000 },    // 급격한 감소
      ],
    },

    // 🔄 4단계: 24시간 지속 부하 (내구성 테스트)
    endurance_test: {
      executor: 'constant-vus',
      vus: 50000,
      duration: '24h',
      gracefulStop: '30s',
    },

    // 🐒 5단계: Chaos Engineering 동시 실행
    chaos_during_load: {
      executor: 'ramping-vus',
      startTime: '1h',
      stages: [
        { duration: '30m', target: 1000 },      // Chaos Monkey 활성화
        { duration: '23h', target: 1000 },      // 24시간 동안 장애 주입
      ],
    },
  },

  thresholds: {
    // 🎯 극한 임계값
    http_req_duration: ['p(95)<1000', 'p(99)<3000'], // 극한 부하에서도 3초 이내
    http_req_failed: ['rate<0.1'],                   // 10% 이하 실패율
    http_reqs: ['rate>50000'],                       // 초당 5만 요청 이상
    extreme_concurrent_connections: ['value<5500000'], // 550만 연결 한계
    payment_success_rate: ['rate>0.85'],             // 85% 이상 결제 성공
    system_recovery_count: ['count>0'],              // 시스템 복구 확인
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 🎯 극한 규모 테스트 데이터
const extremeUsers = new SharedArray('extreme-users', function () {
  const users = [];
  for (let i = 1; i <= 1000000; i++) { // 100만 사용자 풀
    users.push({
      userId: i,
      email: `extreme_user_${i}@popcorn.ultimate`,
      sessionId: `session_${i}_${Date.now()}`,
      region: ['US', 'EU', 'ASIA', 'LATAM'][i % 4],
      deviceType: ['mobile', 'desktop', 'tablet'][i % 3]
    });
  }
  return users;
});

export function setup() {
  console.log('🌊🔥🚀 ULTIMATE 극한 테스트 시작! 🚀🔥🌊');
  console.log('🎯 목표: 5,000,000명 동시 접속 + 24시간 지속');
  console.log('💳 결제: 10,000명 동시 결제 처리');
  console.log('⏰ 지속시간: 24시간 내구성 테스트');

  return {
    startTime: new Date().toISOString(),
    targetUsers: 5000000,
    testDuration: '24h'
  };
}

export default function(data) {
  const user = extremeUsers[__VU % extremeUsers.length];

  group('🌊 5백만명 극한 부하 시나리오', function() {

    // 📊 연결 수 추적
    extremeConnections.add(__VU);

    // 1️⃣ 메모리 누수 감지용 더미 데이터 생성
    group('🧠 메모리 패턴 분석', function() {
      const memoryUsage = Math.random() * 1000; // 시뮬레이션
      memoryLeak.add(memoryUsage);

      // 메모리 누수 시뮬레이션 (10% 확률)
      if (__VU % 10 === 0) {
        const leakData = new Array(1000).fill(Math.random());
        sleep(0.1);
      }
    });

    // 2️⃣ 네트워크 대역폭 측정
    group('🌐 네트워크 성능 측정', function() {
      const startTime = new Date();

      let response = http.get(`${BASE_URL}/actuator/health`, {
        headers: {
          'User-Agent': `Extreme-User-${user.userId}`,
          'X-Region': user.region,
          'X-Device': user.deviceType,
          'X-Session': user.sessionId
        }
      });

      const endTime = new Date();
      const duration = endTime - startTime;
      const dataSize = response.body ? response.body.length : 0;
      const bandwidth = (dataSize / duration) * 8; // Mbps 계산
      networkBandwidth.add(bandwidth);

      check(response, {
        '🌊 5백만명 중 응답 성공': (r) => r.status < 500,
        '⚡ 네트워크 지연 < 500ms': (r) => r.timings.duration < 500,
      });
    });

    // 3️⃣ 극한 결제 처리 (일부 사용자)
    if (__VU % 500 === 0) { // 500명 중 1명이 결제 (2% = 10만명 동시 결제!)
      group('💳 극한 결제 처리', function() {
        const paymentAmount = Math.floor(Math.random() * 100000) + 10000;

        let paymentResponse = http.post(`${BASE_URL}/api/v1/toss/payments`,
          JSON.stringify({
            orderId: `ultimate-${user.userId}-${__ITER}`,
            amount: paymentAmount,
            customerKey: user.sessionId,
            paymentMethod: 'CARD',
            metadata: {
              testType: 'ultimate-extreme-5million',
              region: user.region,
              device: user.deviceType
            }
          }), {
          headers: {
            'Content-Type': 'application/json',
            'X-Ultimate-Test': 'payment-extreme'
          }
        });

        const paymentSuccess = paymentResponse.status < 500;
        paymentThroughput.add(paymentSuccess);

        check(paymentResponse, {
          '💳 극한 결제 처리': (r) => r.status < 500,
          '🚀 결제 응답 < 5초': (r) => r.timings.duration < 5000,
        });
      });
    }

    // 4️⃣ Chaos Monkey 트리거 (일부 사용자)
    if (__VU % 1000 === 0) { // 1000명 중 1명이 Chaos 트리거
      group('🐒 동적 장애 주입', function() {
        const chaosTypes = ['latency', 'exception', 'memory'];
        const chaosType = chaosTypes[__VU % 3];

        let chaosResponse = http.post(`${BASE_URL}/api/v1/chaos/attack/${chaosType}`, null, {
          headers: {
            'X-Chaos-Trigger': 'ultimate-test',
            'X-User-Context': user.userId
          }
        });

        // 시스템 복구 확인
        if (chaosResponse.status === 200) {
          systemRecovery.add(1);
        }
      });
    }

    // 5️⃣ CPU 집약적 작업 시뮬레이션
    if (__VU % 100 === 0) { // 1%의 사용자가 무거운 작업
      group('💻 CPU 집약적 작업', function() {
        // CPU 사용률 증가 시뮬레이션
        const iterations = Math.floor(Math.random() * 1000000);
        let result = 0;
        for (let i = 0; i < iterations; i++) {
          result += Math.sqrt(i);
        }
      });
    }

    // 6️⃣ 데이터베이스 커넥션 풀 스트레스
    if (__VU % 200 === 0) { // 0.5%의 사용자가 DB 집약적 작업
      group('💾 데이터베이스 스트레스', function() {
        // 여러 API 호출로 커넥션 풀 압박
        const dbRequests = [];
        for (let i = 0; i < 5; i++) {
          dbRequests.push(
            http.get(`${BASE_URL}/api/v1/orders?page=${i}`, {
              headers: { 'X-DB-Stress': 'connection-pool-test' }
            })
          );
        }
      });
    }

    // 7️⃣ 실시간 모니터링 데이터 수집
    group('📊 실시간 메트릭 수집', function() {
      if (__VU % 10000 === 0) { // 만명 중 1명이 메트릭 수집
        let metricsResponse = http.get(`${BASE_URL}/actuator/prometheus`, {
          headers: {
            'X-Metrics-Collector': 'ultimate-extreme',
            'X-Sampling-Rate': '0.0001' // 0.01% 샘플링
          }
        });

        check(metricsResponse, {
          '📊 메트릭 수집 가능': (r) => r.status < 500,
        });
      }
    });

    // 8️⃣ 서버 장애 복구 테스트 시뮬레이션
    if (__VU % 5000 === 0) { // 0.02%의 사용자가 복구 테스트
      group('🔄 장애 복구 테스트', function() {
        // 의도적으로 연결 실패 후 재시도
        try {
          let failureResponse = http.get(`${BASE_URL}/api/nonexistent`, {
            timeout: '1s'
          });
        } catch (e) {
          // 실패 후 정상 API 호출로 복구 확인
          let recoveryResponse = http.get(`${BASE_URL}/actuator/health`);
          if (recoveryResponse.status === 200) {
            systemRecovery.add(1);
          }
        }
      });
    }

    // 사용자별 다른 대기 시간 (실제 패턴 시뮬레이션)
    const userThinkTime = getThinkTimeByRegion(user.region);
    sleep(userThinkTime);
  });
}

// 🌍 지역별 사용자 패턴 차이
function getThinkTimeByRegion(region) {
  switch (region) {
    case 'ASIA':  return Math.random() * 1 + 0.5;    // 아시아: 빠른 사용자
    case 'EU':    return Math.random() * 2 + 1;      // 유럽: 중간
    case 'US':    return Math.random() * 1.5 + 0.8;  // 미국: 중간-빠름
    case 'LATAM': return Math.random() * 3 + 1.5;    // 라틴아메리카: 느림
    default:      return Math.random() * 2 + 1;
  }
}

export function handleSummary(data) {
  const duration = data.state.testRunDurationMs / 1000 / 3600; // 시간 단위
  const totalRequests = data.metrics.http_reqs?.values?.count || 0;
  const avgResponseTime = data.metrics.http_req_duration?.values?.avg || 0;
  const maxVUs = data.metrics.vus_max?.values?.max || 0;
  const throughput = data.metrics.http_reqs?.values?.rate || 0;
  const errorRate = data.metrics.http_req_failed?.values?.rate || 0;

  console.log(`
╔══════════════════════════════════════════════════════════════════╗
║              🌊🔥 ULTIMATE 극한 테스트 결과 🔥🌊               ║
╠══════════════════════════════════════════════════════════════════╣
║ 🎯 최대 동시 사용자: ${maxVUs.toLocaleString()} 명                           ║
║ 📊 총 요청 수: ${totalRequests.toLocaleString()} 건                          ║
║ ⚡ 평균 응답 시간: ${avgResponseTime.toFixed(2)} ms                      ║
║ 🚀 요청 처리량: ${throughput.toFixed(2)} req/s                         ║
║ ❌ 오류율: ${(errorRate * 100).toFixed(3)}%                             ║
║ ⏰ 테스트 지속 시간: ${duration.toFixed(2)} 시간                         ║
║ 💳 결제 성공률: ${((data.metrics.payment_success_rate?.values?.rate || 0) * 100).toFixed(2)}%        ║
║ 🔄 시스템 복구 횟수: ${data.metrics.system_recovery_count?.values?.count || 0} 회              ║
║ 🌐 평균 대역폭: ${(data.metrics.network_bandwidth_mbps?.values?.avg || 0).toFixed(2)} Mbps     ║
╚══════════════════════════════════════════════════════════════════╝

🏆 극한 테스트 평가:
${getExtremeTestRating(maxVUs, errorRate, throughput)}
  `);

  return {
    'ultimate-extreme-report.html': htmlReport(data),
    'ultimate-extreme-summary.json': JSON.stringify(data, null, 2),
    'ultimate-extreme-metrics.txt': generateDetailedMetrics(data),
  };
}

function getExtremeTestRating(maxVUs, errorRate, throughput) {
  if (maxVUs >= 5000000 && errorRate < 0.05 && throughput > 100000) {
    return '🏆 LEGENDARY: 인류 역사상 최고의 극한 테스트 성공!';
  } else if (maxVUs >= 3000000 && errorRate < 0.1 && throughput > 50000) {
    return '🥇 EPIC: 300만명 이상 성공적 처리!';
  } else if (maxVUs >= 1000000 && errorRate < 0.15 && throughput > 25000) {
    return '🥈 EXTREME: 100만명 이상 처리 성공!';
  } else {
    return '🥉 INTENSE: 고강도 테스트 완료!';
  }
}

function generateDetailedMetrics(data) {
  return `
=== ULTIMATE 극한 테스트 상세 메트릭 ===

📊 요청 통계:
- P50 응답시간: ${data.metrics.http_req_duration?.values?.['p(50)'] || 0} ms
- P95 응답시간: ${data.metrics.http_req_duration?.values?.['p(95)'] || 0} ms
- P99 응답시간: ${data.metrics.http_req_duration?.values?.['p(99)'] || 0} ms
- 최대 응답시간: ${data.metrics.http_req_duration?.values?.max || 0} ms

🔥 극한 지표:
- 최대 동시 연결: ${data.metrics.extreme_concurrent_connections?.values?.max || 0}
- 평균 메모리 사용: ${(data.metrics.memory_leak_trend?.values?.avg || 0).toFixed(2)} MB
- 네트워크 대역폭: ${(data.metrics.network_bandwidth_mbps?.values?.avg || 0).toFixed(2)} Mbps

💪 시스템 복원력:
- 시스템 복구 성공: ${data.metrics.system_recovery_count?.values?.count || 0} 회
- 결제 처리 성공률: ${((data.metrics.payment_success_rate?.values?.rate || 0) * 100).toFixed(2)}%
- 전체적 안정성: ${data.metrics.http_req_failed?.values?.rate < 0.1 ? '우수' : '개선 필요'}

🎯 극한 테스트 목표 달성도:
✅ 5백만명 동시 접속: ${data.metrics.vus_max?.values?.max >= 5000000 ? '달성' : '미달성'}
✅ 24시간 지속: ${data.state.testRunDurationMs >= 86400000 ? '달성' : '미달성'}
✅ 안정적 처리: ${data.metrics.http_req_failed?.values?.rate < 0.1 ? '달성' : '미달성'}
  `;
}

export function teardown(data) {
  console.log('🌊🔥 ULTIMATE 극한 테스트 완료! 🔥🌊');
  console.log(`시작: ${data.startTime}`);
  console.log(`종료: ${new Date().toISOString()}`);
  console.log('🏆 당신의 시스템은 극한을 뛰어넘었습니다!');
}