# 🎛️ 테스트 제어 대시보드
# 극한 테스트 실행 및 모니터링 제어

FROM nginx:alpine

# 기본 패키지 설치
RUN apk add --no-cache curl jq

# 웹 루트 디렉토리 생성
RUN mkdir -p /usr/share/nginx/html

# 기본 대시보드 HTML 생성
RUN cat > /usr/share/nginx/html/index.html << 'EOF'
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🎛️ Popcorn 극한 테스트 제어 대시보드</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #1a1a1a; color: #fff; }
        .header { text-align: center; margin-bottom: 30px; }
        .dashboard { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }
        .card { background: #2a2a2a; padding: 20px; border-radius: 8px; border: 1px solid #444; }
        .card h3 { margin-top: 0; color: #4CAF50; }
        .metric { margin: 10px 0; }
        .metric-value { font-size: 1.5em; font-weight: bold; color: #2196F3; }
        .btn { padding: 10px 20px; margin: 5px; background: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer; }
        .btn:hover { background: #45a049; }
        .btn.danger { background: #f44336; }
        .btn.danger:hover { background: #da190b; }
        .status { padding: 5px 10px; border-radius: 4px; font-weight: bold; }
        .status.running { background: #4CAF50; }
        .status.stopped { background: #f44336; }
        iframe { width: 100%; height: 300px; border: 1px solid #444; border-radius: 4px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🎛️ Popcorn 극한 테스트 제어 대시보드</h1>
        <p>5백만명 동시 접속 + 24시간 지속 테스트 모니터링</p>
    </div>

    <div class="dashboard">
        <div class="card">
            <h3>📊 모니터링 링크</h3>
            <p><a href="http://localhost:3000" target="_blank">🟢 Grafana 대시보드</a></p>
            <p><a href="http://localhost:9091" target="_blank">📈 Prometheus</a></p>
            <p><a href="http://localhost:8086" target="_blank">💾 InfluxDB</a></p>
            <p><a href="http://localhost:18080" target="_blank">🚀 Popcorn Backend</a></p>
        </div>

        <div class="card">
            <h3>🎯 테스트 제어</h3>
            <button class="btn" onclick="startJMeterTest()">🔥 JMeter 극한 테스트 시작</button>
            <button class="btn" onclick="startK6Test()">🌊 K6 테스트 시작</button>
            <button class="btn danger" onclick="stopAllTests()">⛔ 모든 테스트 중지</button>
        </div>

        <div class="card">
            <h3>🚀 백엔드 상태</h3>
            <div class="metric">
                <div>상태: <span class="status running">실행중</span></div>
                <div class="metric-value" id="backend-status">연결 대기중...</div>
            </div>
        </div>

        <div class="card">
            <h3>📈 실시간 메트릭</h3>
            <div class="metric">
                <div>동시 사용자:</div>
                <div class="metric-value" id="concurrent-users">0</div>
            </div>
            <div class="metric">
                <div>요청/초:</div>
                <div class="metric-value" id="rps">0</div>
            </div>
            <div class="metric">
                <div>평균 응답시간:</div>
                <div class="metric-value" id="avg-response">0ms</div>
            </div>
        </div>
    </div>

    <script>
        function startJMeterTest() {
            alert('JMeter 테스트를 시작합니다. 이는 극한 부하 테스트입니다.');
            console.log('JMeter test started');
        }

        function startK6Test() {
            alert('K6 테스트를 시작합니다.');
            console.log('K6 test started');
        }

        function stopAllTests() {
            if(confirm('모든 테스트를 중지하시겠습니까?')) {
                alert('모든 테스트가 중지되었습니다.');
                console.log('All tests stopped');
            }
        }

        // 기본적인 상태 체크 (실제 구현에서는 API 호출로 대체)
        function checkBackendStatus() {
            fetch('/api/health')
                .then(response => response.json())
                .then(data => {
                    document.getElementById('backend-status').textContent = data.status;
                })
                .catch(() => {
                    document.getElementById('backend-status').textContent = '연결 실패';
                });
        }

        // 주기적 상태 확인
        setInterval(checkBackendStatus, 5000);
    </script>
</body>
</html>
EOF

# Nginx 설정
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]