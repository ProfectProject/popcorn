"use client";

import { useEffect, useMemo, useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Script from "next/script";

function PaymentsContent() {
  const normalizeCustomerKey = (value) => {
    if (!value) {
      return "guest";
    }
    const trimmed = value.trim();
    const isValid = /^[A-Za-z0-9\-_.=@]{2,50}$/.test(trimmed);
    return isValid ? trimmed : "guest";
  };

  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY || "";

  const [scriptReady, setScriptReady] = useState(false);
  const [method, setMethod] = useState("CARD");
  const [error, setError] = useState("");
  const [paymentInfo, setPaymentInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  const orderName = useMemo(() => {
    if (!paymentInfo?.orderNo) {
      return "Popcorn Order";
    }
    return `Popcorn Order ${paymentInfo.orderNo}`;
  }, [paymentInfo]);

  // 토큰을 디코딩하여 결제 정보 가져오기
  useEffect(() => {
    const fetchPaymentInfo = async () => {
      if (!token) {
        setError("결제 토큰이 없습니다.");
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(`${apiBase}/api/v1/payments/decode?token=${encodeURIComponent(token)}`);

        if (!response.ok) {
          setError("유효하지 않은 결제 토큰입니다.");
          setLoading(false);
          return;
        }

        const result = await response.json();
        setPaymentInfo(result.data);
        setLoading(false);
      } catch (err) {
        setError("결제 정보를 불러오는데 실패했습니다.");
        setLoading(false);
      }
    };

    fetchPaymentInfo();
  }, [token, apiBase]);

  useEffect(() => {
    if (!scriptReady || !paymentInfo) {
      return;
    }
    if (!clientKey) {
      setError("NEXT_PUBLIC_TOSS_CLIENT_KEY 설정이 필요합니다.");
      return;
    }
    if (!paymentInfo.orderNo || !paymentInfo.amount) {
      setError("결제 파라미터가 부족합니다.");
      return;
    }
    if (typeof window === "undefined" || !window.TossPayments) {
      setError("토스 결제 SDK를 불러오지 못했습니다.");
      return;
    }
  }, [scriptReady, clientKey, paymentInfo]);

  const onPay = async () => {
    if (!paymentInfo) {
      setError("결제 정보가 없습니다.");
      return;
    }

    console.log("Payment info:", paymentInfo);
    console.log("Client key:", clientKey);
    console.log("TossPayments available:", typeof window.TossPayments);

    if (!window.TossPayments) {
      setError("Toss 결제 SDK가 로드되지 않았습니다.");
      return;
    }

    if (!clientKey) {
      setError("결제 클라이언트 키가 설정되지 않았습니다.");
      return;
    }

    setError("");
    try {
      const tossPayments = window.TossPayments(clientKey);
      const paymentRequest = {
        orderId: paymentInfo.orderNo,
        orderName,
        amount: paymentInfo.amount,
        successUrl: paymentInfo.successUrl,
        failUrl: paymentInfo.failUrl,
        customerKey: normalizeCustomerKey(paymentInfo.customerKey)
      };

      console.log("Payment request:", paymentRequest);
      await tossPayments.requestPayment(method, paymentRequest);
    } catch (err) {
      console.error("Payment error:", err);
      setError(`결제 요청에 실패했습니다: ${err.message || err}`);
    }
  };

  return (
    <main>
      <Script
        src="https://js.tosspayments.com/v1"
        strategy="afterInteractive"
        onLoad={() => setScriptReady(true)}
      />
      <div className="container">
        <section className="hero payment-wrap">
          <div>
            <p className="mono">ORDER</p>
            <h1 className="title">결제 진행</h1>
            <p className="subtitle">주문 정보를 확인하고 결제를 진행하세요.</p>
          </div>

          {loading ? (
            <div className="card">
              <p>결제 정보를 불러오는 중...</p>
            </div>
          ) : error ? (
            <div className="notice">{error}</div>
          ) : paymentInfo ? (
            <>
              <div className="grid">
                <div className="card">
                  <h3 className="section-title">주문 정보</h3>
                  <p className="mono">orderNo: {paymentInfo.orderNo || "-"}</p>
                  <p className="mono">paymentId: {paymentInfo.paymentId || "-"}</p>
                  <p className="mono">amount: {paymentInfo.amount || 0} KRW</p>
                  <p className="mono">customerKey: {paymentInfo.customerKey}</p>
                </div>
                <div className="card">
                  <h3 className="section-title">결제 안내</h3>
                  <p>결제 수단을 선택한 뒤 결제하기를 눌러주세요.</p>
                  <p className="mono">🔒 보안 토큰으로 암호화됨</p>
                </div>
              </div>

              <div className="card">
                <h3 className="section-title">결제 수단</h3>
                <div className="mono" style={{ display: "flex", gap: "12px" }}>
                  <label>
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="CARD"
                      checked={method === "CARD"}
                      onChange={() => setMethod("CARD")}
                    />
                    카드
                  </label>
                  <label>
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="TRANSFER"
                      checked={method === "TRANSFER"}
                      onChange={() => setMethod("TRANSFER")}
                    />
                    계좌이체
                  </label>
                </div>
              </div>

              <button
                className="button"
                onClick={onPay}
                disabled={!scriptReady || !paymentInfo}
              >
                결제하기
              </button>
            </>
          ) : null}
        </section>
      </div>
    </main>
  );
}

export default function PaymentsPage() {
  return (
    <Suspense fallback={<div>로딩 중...</div>}>
      <PaymentsContent />
    </Suspense>
  );
}
