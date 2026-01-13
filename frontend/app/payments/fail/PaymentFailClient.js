"use client";

import { useSearchParams } from "next/navigation";

export default function PaymentFailClient() {
  const searchParams = useSearchParams();
  const message = searchParams.get("message") || "결제에 실패했습니다.";
  const code = searchParams.get("code") || "UNKNOWN";

  return (
    <main>
      <div className="container">
        <section className="hero">
          <h1 className="title">결제 실패</h1>
          <p className="subtitle">다시 시도하거나 문의해주세요.</p>

          <div className="status">
            <p className="mono">code: {code}</p>
            <p>{message}</p>
          </div>
        </section>
      </div>
    </main>
  );
}
