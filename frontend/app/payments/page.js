import { Suspense } from "react";
import PaymentsClient from "./PaymentsClient";

export default function PaymentsPage() {
  return (
    <Suspense
      fallback={
        <main>
          <div className="container">
            <section className="hero payment-wrap">
              <div className="card">
                <p>결제 페이지를 불러오는 중...</p>
              </div>
            </section>
          </div>
        </main>
      }
    >
      <PaymentsClient />
    </Suspense>
  );
}
