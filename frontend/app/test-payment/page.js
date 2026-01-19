"use client";

import { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import Script from 'next/script';

export default function TestPaymentPage() {
  const [scriptReady, setScriptReady] = useState(false);
  const [orderResult, setOrderResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const searchParams = useSearchParams();

  // 🎯 Swagger에서 온 URL 파라미터 처리
  const urlOrderId = searchParams.get('orderId');
  const urlOrderNo = searchParams.get('orderNo');
  const urlAmount = searchParams.get('amount');
  const autoStart = searchParams.get('auto') === 'true';

  // 테스트용 주문 데이터
  const testOrderData = {
    orderType: "PURCHASE",
    popupId: "00000000-0000-0000-0000-000000000101",
    paymentMethod: "CARD",
    items: [
      {
        orderItemType: "GOODS",
        goodsVariantId: "00000000-0000-0000-0000-000000000301",
        qty: 1,
        unitPrice: 15000
      }
    ]
  };

  // 🎯 Swagger에서 온 URL 파라미터로 자동 결제 설정
  useEffect(() => {
    if (urlOrderId && urlOrderNo && urlAmount) {
      const mockOrderResult = {
        orderId: urlOrderId,
        orderNo: urlOrderNo,
        paymentAmount: parseInt(urlAmount),
        clientKey: process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY || "test_ck_AQ92ymxN34LKgMYlpPZy3ajRKXvd",
        customerKey: "guest",
        readyForPayment: true,
        successUrl: "http://localhost:3000/payments/success",
        failUrl: "http://localhost:3000/payments/fail"
      };

      setOrderResult(mockOrderResult);

      // auto=true이면 스크립트 로드 후 자동 결제 시작
      if (autoStart && scriptReady) {
        setTimeout(() => startPaymentWithOrder(mockOrderResult), 1000);
      }
    }
  }, [urlOrderId, urlOrderNo, urlAmount, autoStart, scriptReady]);

  const createTestOrder = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await fetch('/api/v1/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('authToken') || 'test-token'}`
        },
        body: JSON.stringify(testOrderData)
      });

      if (!response.ok) {
        throw new Error(`API 오류: ${response.status}`);
      }

      const result = await response.json();

      if (!result.success) {
        throw new Error('주문 생성 실패');
      }

      setOrderResult(result.data);

      // 🎯 결제 정보 검증
      if (!result.data.readyForPayment) {
        setError('❌ readyForPayment가 false입니다. 결제 준비가 안됨');
        return;
      }

      if (!result.data.clientKey) {
        setError('❌ clientKey가 없습니다.');
        return;
      }

      console.log('✅ 주문 생성 성공:', result.data);

    } catch (err) {
      console.error('주문 생성 오류:', err);
      setError(`주문 생성 실패: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const startPaymentWithOrder = async (order = orderResult) => {
    if (!order) {
      setError('주문 정보가 없습니다. 먼저 주문을 생성하세요.');
      return;
    }

    if (!window.TossPayments) {
      setError('토스 결제 SDK가 로드되지 않았습니다.');
      return;
    }

    try {
      const tossPayments = window.TossPayments(order.clientKey);

      await tossPayments.requestPayment('CARD', {
        orderId: order.orderNo,
        orderName: `Popcorn Test Order ${order.orderNo}`,
        amount: order.paymentAmount,
        customerKey: order.customerKey,
        successUrl: order.successUrl || 'http://localhost:3000/payments/success',
        failUrl: order.failUrl || 'http://localhost:3000/payments/fail'
      });

    } catch (err) {
      console.error('결제 오류:', err);
      setError(`결제 실패: ${err.message}`);
    }
  };

  const startPayment = () => startPaymentWithOrder();

  return (
    <div style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <Script
        src="https://js.tosspayments.com/v1"
        strategy="afterInteractive"
        onLoad={() => setScriptReady(true)}
      />

      <h1>🧪 결제 시스템 테스트</h1>

      <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '8px' }}>
        <h2>1단계: 주문 생성 + 결제 정보 받기</h2>
        <button
          onClick={createTestOrder}
          disabled={loading}
          style={{
            padding: '10px 20px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? '주문 생성 중...' : '🛍️ 테스트 주문 생성'}
        </button>

        {error && (
          <div style={{ marginTop: '10px', padding: '10px', backgroundColor: '#f8d7da', color: '#721c24', borderRadius: '4px' }}>
            {error}
          </div>
        )}
      </div>

      {orderResult && (
        <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #28a745', borderRadius: '8px', backgroundColor: '#d4edda' }}>
          <h2>✅ 주문 생성 성공</h2>
          <pre style={{ backgroundColor: '#f8f9fa', padding: '10px', borderRadius: '4px', overflow: 'auto' }}>
            {JSON.stringify(orderResult, null, 2)}
          </pre>

          <h3>🔍 검증 결과:</h3>
          <ul>
            <li>readyForPayment: {orderResult.readyForPayment ? '✅' : '❌'} {String(orderResult.readyForPayment)}</li>
            <li>clientKey: {orderResult.clientKey ? '✅' : '❌'} {orderResult.clientKey || 'null'}</li>
            <li>paymentKey: {orderResult.paymentKey ? '✅' : '❌'} {orderResult.paymentKey || 'null'}</li>
            <li>paymentAmount: {orderResult.paymentAmount ? '✅' : '❌'} {orderResult.paymentAmount}</li>
          </ul>
        </div>
      )}

      {orderResult && orderResult.readyForPayment && (
        <div style={{ marginBottom: '20px', padding: '15px', border: '1px solid #17a2b8', borderRadius: '8px' }}>
          <h2>2단계: 토스 결제위젯 호출</h2>
          <button
            onClick={startPayment}
            disabled={!scriptReady}
            style={{
              padding: '10px 20px',
              backgroundColor: '#28a745',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: scriptReady ? 'pointer' : 'not-allowed'
            }}
          >
            {scriptReady ? '💳 결제하기 (토스 위젯)' : '토스 SDK 로딩 중...'}
          </button>
        </div>
      )}

      <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '8px' }}>
        <h3>🔧 디버그 정보</h3>
        <ul>
          <li>토스 SDK 로드 상태: {scriptReady ? '✅ 완료' : '⏳ 로딩 중'}</li>
          <li>TossPayments 객체: {typeof window !== 'undefined' && window.TossPayments ? '✅ 사용 가능' : '❌ 없음'}</li>
          <li>주문 생성 상태: {orderResult ? '✅ 완료' : '⏳ 대기 중'}</li>
        </ul>
      </div>
    </div>
  );
}