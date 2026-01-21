import { NextResponse } from "next/server";

export function middleware(request) {
  const { pathname, searchParams } = request.nextUrl;
  if (pathname !== "/payments") {
    return NextResponse.next();
  }
  const token = searchParams.get("token");
  if (!token) {
    return NextResponse.next();
  }
  console.log("[middleware] redirect /payments -> /auto-payment");
  const url = request.nextUrl.clone();
  url.pathname = "/auto-payment";
  return NextResponse.redirect(url);
}

export const config = {
  matcher: ["/payments", "/payments/"]
};
