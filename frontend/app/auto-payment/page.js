"use client";

import { useEffect, useState, useRef } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Script from 'next/script';

export default function AutoPaymentPage() {
  const [scriptReady, setScriptReady] = useState(false);
  const [error, setError] = useState('');
  const [paymentStarted, setPaymentStarted] = useState(false);
  const [paymentInfo, setPaymentInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const paymentExecuted = useRef(false); // 🔒 중복 실행 방지
  const dataFetched = useRef(false); // 🔒 API 중복 호출 방지
  const startKeyRef = useRef(null);

  const searchParams = useSearchParams();
  const router = useRouter();

  const token = searchParams.get('token'); // 🔐 암호화된 토큰
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY || "test_ck_AQ92ymxN34LKgMYlpPZy3ajRKXvd";

  // 🔐 토큰 디코딩으로 결제 정보 가져오기
  useEffect(() => {
    const fetchPaymentInfo = async () => {
      if (!token) {
        setError('❌ 결제 토큰이 없습니다.');
        setLoading(false);
        return;
      }

      // 🔒 중복 API 호출 방지
      if (dataFetched.current) return;
      dataFetched.current = true;

      try {
        const response = await fetch(`${apiBase}/api/pay/v1/payments/decode?token=${encodeURIComponent(token)}`);

        if (!response.ok) {
          setError('❌ 유효하지 않은 결제 토큰입니다.');
          setLoading(false);
          return;
        }

        const result = await response.json();
        setPaymentInfo(result.data);
        setLoading(false);
      } catch (err) {
        console.error('❌ 결제 정보 로딩 실패:', err);
        setError('❌ 결제 정보를 불러오는데 실패했습니다.');
        setLoading(false);
        // 🔄 실패 시 재시도 가능하도록 플래그 리셋
        dataFetched.current = false;
      }
    };

    fetchPaymentInfo();
  }, [token, apiBase]);

  // 🚀 페이지 로드 즉시 결제창 자동 실행
  useEffect(() => {
    if (!scriptReady || paymentStarted || loading || !paymentInfo || paymentExecuted.current) return;

    // 🎯 바로 결제창 실행!
    startPayment();
  }, [scriptReady, paymentStarted, loading, paymentInfo]);

  const startPayment = async () => {
    if (paymentStarted || !paymentInfo || paymentExecuted.current) return;

    // 🔒 중복 실행 방지: 즉시 실행 플래그 설정
    paymentExecuted.current = true;
    setPaymentStarted(true);
    if (typeof window !== 'undefined' && paymentInfo.orderId) {
      const storageKey = `payment-started:${paymentInfo.orderId}`;
      startKeyRef.current = storageKey;
      if (window.sessionStorage.getItem(storageKey)) {
        setError('이미 결제가 진행 중입니다. 새로고침하지 마세요.');
        return;
      }
      window.sessionStorage.setItem(storageKey, '1');
    }

    try {
      if (!window.TossPayments) {
        throw new Error('토스 결제 SDK가 로드되지 않았습니다.');
      }

      console.log('🚀 자동 결제 시작:', paymentInfo);

      const tossPayments = window.TossPayments(clientKey);

      // 간단한 결제 승인 - JWT 토큰 불필요
      const successUrl = paymentInfo.successUrl || "http://localhost:3000/payments/success";
      const failUrl = paymentInfo.failUrl || "http://localhost:3000/payments/fail";

      await tossPayments.requestPayment('CARD', {
        orderId: paymentInfo.orderId || paymentInfo.orderNo,
        orderName: `Popcorn Order ${paymentInfo.orderNo}`,
        amount: paymentInfo.amount,
        customerKey: paymentInfo.customerKey,
        successUrl: successUrl,
        failUrl: failUrl
      });

    } catch (err) {
      console.error('❌ 자동 결제 실패:', err);
      setError(`결제 실패: ${err.message || err}`);

      // 🔄 에러 발생 시 재시도 가능하도록 플래그 리셋
      paymentExecuted.current = false;
      setPaymentStarted(false);
      if (typeof window !== 'undefined' && startKeyRef.current) {
        window.sessionStorage.removeItem(startKeyRef.current);
      }

      // 5초 후 테스트 페이지로 리다이렉트
      setTimeout(() => {
        router.push(`/test-payment?token=${encodeURIComponent(token)}&error=${encodeURIComponent(err.message || err)}`);
      }, 5000);
    }
  };

  const goToTestPage = () => {
    router.push(`/test-payment?token=${encodeURIComponent(token)}`);
  };

  return (
    <div style={{
      padding: '40px',
      maxWidth: '600px',
      margin: '0 auto',
      textAlign: 'center',
      fontFamily: 'system-ui, -apple-system, sans-serif'
    }}>
      <Script
        src="https://js.tosspayments.com/v1"
        strategy="afterInteractive"
        onLoad={() => setScriptReady(true)}
      />

      <h1 style={{ fontSize: '32px', marginBottom: '20px' }}>🚀</h1>

      {loading ? (
        <>
          <h2 style={{ color: '#333', marginBottom: '20px' }}>결제 정보 로딩 중...</h2>
          <div style={{
            display: 'inline-block',
            width: '40px',
            height: '40px',
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #007bff',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            marginBottom: '20px'
          }}></div>
          <p style={{ color: '#666', marginBottom: '30px' }}>
            🔐 토큰을 해독하여 결제 정보를 가져오는 중...
          </p>
        </>
      ) : !error && paymentInfo ? (
        <>
          <h2 style={{ color: '#333', marginBottom: '20px' }}>결제창 실행 중...</h2>
          <div style={{
            display: 'inline-block',
            width: '40px',
            height: '40px',
            border: '4px solid #f3f3f3',
            borderTop: '4px solid #007bff',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            marginBottom: '20px'
          }}></div>
          <p style={{ color: '#666', marginBottom: '30px' }}>
            잠시만 기다리세요. 토스 결제창이 곧 나타납니다.
          </p>
          <div style={{
            backgroundColor: '#f8f9fa',
            padding: '20px',
            borderRadius: '8px',
            marginBottom: '20px',
            textAlign: 'left',
            fontSize: '14px',
            color: '#666'
          }}>
            <strong>📋 주문 정보:</strong><br/>
            주문 ID: {paymentInfo.orderId}<br/>
            주문 번호: {paymentInfo.orderNo}<br/>
            결제 금액: {paymentInfo.amount?.toLocaleString()}원<br/>
            고객 키: {paymentInfo.customerKey}<br/>
            <br/>
            <strong>🔐 보안:</strong><br/>
            결제 정보가 JWT 토큰으로 암호화되어 안전하게 전달되었습니다.
          </div>
        </>
      ) : (
        <>
          <h2 style={{ color: '#dc3545', marginBottom: '20px' }}>결제 실행 실패</h2>
          <div style={{
            backgroundColor: '#f8d7da',
            color: '#721c24',
            padding: '20px',
            borderRadius: '8px',
            marginBottom: '20px',
            border: '1px solid #f5c6cb'
          }}>
            {error}
          </div>
          <button
            onClick={goToTestPage}
            style={{
              padding: '12px 24px',
              backgroundColor: '#007bff',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              fontSize: '16px',
              cursor: 'pointer'
            }}
          >
            🧪 테스트 페이지로 이동
          </button>
          <p style={{ color: '#666', fontSize: '14px', marginTop: '20px' }}>
            5초 후 자동으로 테스트 페이지로 이동합니다.
          </p>
        </>
      )}

      <style jsx>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
