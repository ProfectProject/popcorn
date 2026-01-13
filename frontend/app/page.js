export default function Home() {
  return (
    <main>
      <div className="container">
        <section className="hero">
          <p className="mono">POP.CORN / LOCAL PAY</p>
          <h1 className="title">결제 테스트용 프론트</h1>
          <p className="subtitle">
            백엔드에서 <span className="mono">/api/v1/orders/{'{orderId}'}/pay</span>를 호출하면 이 페이지로
            리다이렉트됩니다.
          </p>
          <div className="notice">
            결제 페이지는 <span className="mono">/payments</span> 입니다. 백엔드 리다이렉트 URL을 확인하세요.
          </div>
        </section>
      </div>
    </main>
  );
}
