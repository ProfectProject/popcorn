import { Suspense } from "react";
import PaymentFailClient from "./PaymentFailClient";

export default function PaymentFailPage() {
  return (
    <Suspense
      fallback={
        <main>
          <div className="container">
            <section className="hero">
              <div className="card">
                <p>결제 실패 정보를 불러오는 중...</p>
              </div>
            </section>
          </div>
        </main>
      }
    >
      <PaymentFailClient />
    </Suspense>
  );
}
