import { redirect } from "next/navigation";
import PaymentsClient from "./PaymentsClient";

export const dynamic = "force-dynamic";

export default function PaymentsPage({ searchParams }) {
  const token = searchParams?.token;
  if (token) {
    redirect(`/auto-payment?token=${encodeURIComponent(token)}`);
  }
  return <PaymentsClient initialToken={token} />;
}
