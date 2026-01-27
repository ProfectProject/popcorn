"use client";

import { useEffect, useRef, useState, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";

export const dynamic = 'force-dynamic';

function PaymentSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const paymentKey = searchParams.get("paymentKey");
  const orderId = searchParams.get("orderId");
  const amount = searchParams.get("amount");
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
  const paymentApiBase = process.env.NEXT_PUBLIC_PAYMENT_API_BASE_URL || "http://localhost:8080";

  // JWT 토큰 불필요 - orderId 기반 간단 결제 승인

  const [status, setStatus] = useState("confirming");
  const [message, setMessage] = useState("");
  const [countdown, setCountdown] = useState(5);
  const hasConfirmedRef = useRef(false);
  const [latestStatus, setLatestStatus] = useState("");
  const sanitize = (value) => {
    if (!value) {
      return "-";
    }
    return String(value).replace(/[^A-Za-z0-9@._-]/g, "");
  };

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    if (window.opener && !window.opener.closed) {
      try {
        window.opener.location.href = window.location.href;
        window.close();
      } catch (err) {
        // ignore cross-window errors
      }
    }
  }, []);

  useEffect(() => {
    const confirmPayment = async () => {
      if (hasConfirmedRef.current) {
        return;
      }
      hasConfirmedRef.current = true;
      if (!paymentKey || !orderId || !amount) {
        setStatus("error");
        setMessage("결제 승인 파라미터가 부족합니다.");
        return;
      }

      try {
        const headers = {
          "Content-Type": "application/json"
        };

        const response = await fetch(`${paymentApiBase}/api/pay/v1/payments/confirm-async`, {
          method: "POST",
          headers,
          body: JSON.stringify({
            paymentKey,
            orderId,
            amount: Number(amount)
          })
        });

        if (!response.ok && response.status !== 202) {
          let errorMessage = "결제 승인에 실패했습니다.";
          try {
            const errorResult = await response.json();
            errorMessage = errorResult.message || errorMessage;
          } catch (parseError) {
            // JSON 파싱 실패 시 기본 메시지 사용
          }
          setStatus("error");
          setMessage(`${errorMessage} (HTTP ${response.status})`);
          return;
        }

        const result = await response.json();
        if (response.status === 202) {
          setStatus("pending");
          setMessage("승인 처리 중입니다. 잠시 후 결제 내역을 확인해주세요.");
          return;
        }
        setStatus("success");
        setMessage(`승인 완료: ${result?.data?.orderStatus || "PAID"}`);
      } catch (err) {
        setStatus("error");
        setMessage(`네트워크 오류: ${err.message}`);
      }
    };

    confirmPayment();
  }, [paymentKey, orderId, amount, apiBase]);

  useEffect(() => {
    if (status !== "pending" || !orderId) {
      return;
    }
    let attempts = 0;
    const maxAttempts = 10;
    const intervalId = setInterval(async () => {
      attempts += 1;
      try {
        const response = await fetch(`${paymentApiBase}/api/pay/v1/payments/orders/${orderId}/latest`);
        if (!response.ok) {
          return;
        }
        const result = await response.json();
        const paymentStatus = result?.data?.status;
        if (paymentStatus) {
          setLatestStatus(paymentStatus);
        }
        if (paymentStatus === "PAID") {
          setStatus("success");
          setMessage("결제가 성공적으로 완료되었습니다.");
          clearInterval(intervalId);
          return;
        }
        if (paymentStatus === "FAILED" || paymentStatus === "CANCELLED") {
          setStatus("error");
          setMessage(`결제가 ${paymentStatus} 상태입니다.`);
          clearInterval(intervalId);
          return;
        }
      } catch (err) {
        // ignore polling errors
      }
      if (attempts >= maxAttempts) {
        setMessage("승인 처리 지연 중입니다. 잠시 후 다시 확인해주세요.");
        clearInterval(intervalId);
      }
    }, 3000);

    return () => clearInterval(intervalId);
  }, [status, orderId, apiBase]);

  // 결제 성공 시 카운트다운 후 홈으로 리다이렉트
  useEffect(() => {
    if (status === "success" && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (status === "success" && countdown === 0) {
      router.push("/");
    }
  }, [status, countdown, router]);

  return (
    <main>
      <div className="container">
        <section className="hero">
          <h1 className="title">
            {status === "success"
              ? "결제 완료"
              : status === "error"
              ? "결제 실패"
              : status === "pending"
              ? "결제 승인 처리 중"
              : "결제 승인 중"}
          </h1>
          <p className="subtitle">
            {status === "success"
              ? "결제가 성공적으로 완료되었습니다."
              : status === "error"
              ? "결제 승인 과정에서 문제가 발생했습니다."
              : status === "pending"
              ? "승인 처리 중입니다."
              : "승인 결과를 확인하고 있습니다."}
          </p>

          <div className="status">
            <p className="mono">paymentKey: {sanitize(paymentKey)}</p>
            <p className="mono">orderId: {sanitize(orderId)}</p>
            <p className="mono">amount: {sanitize(amount)}</p>
            <p className="mono">status: {status}</p>
            {latestStatus ? <p className="mono">paymentStatus: {latestStatus}</p> : null}
            {message ? <p>{message}</p> : null}
          </div>

          {status === "success" && (
            <div style={{ marginTop: "20px", textAlign: "center" }}>
              <p>{countdown}초 후 메인 페이지로 이동합니다.</p>
              <button
                className="button"
                onClick={() => router.push("/")}
                style={{ marginTop: "10px" }}
              >
                지금 이동하기
              </button>
            </div>
          )}

          {status === "error" && (
            <div style={{ marginTop: "20px", textAlign: "center" }}>
              <button
                className="button"
                onClick={() => router.push("/")}
                style={{ marginTop: "10px" }}
              >
                메인 페이지로 이동
              </button>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

export default function PaymentSuccessPage() {
  return (
    <Suspense fallback={<div>결제 승인 확인 중...</div>}>
      <PaymentSuccessContent />
    </Suspense>
  );
}
