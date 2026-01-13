import { Suspense } from "react";
import PaymentSuccessClient from "./PaymentSuccessClient";

export default function PaymentSuccessPage() {
  return (
    <Suspense
      fallback={
        <main>
          <div className="container">
            <section className="hero">
              <div className="card">
                <p>결제 결과를 확인하는 중...</p>
              </div>
            </section>
          </div>
        </main>
      }
    >
      <PaymentSuccessClient />
    </Suspense>
  );
}
