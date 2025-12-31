package com.popcorn.demo.common.context;



public final class RequestContext {

	private static final ThreadLocal<RequestMetadata> CONTEXT = new ThreadLocal<>();



	private RequestContext() {

	}



	public static void set(RequestMetadata metadata) {

		CONTEXT.set(metadata);

	}



	public static RequestMetadata get() {

		return CONTEXT.get();

	}



	public static void clear() {

		CONTEXT.remove();

	}



	/**

	 * 요청 단위 식별 정보 (필수 최소 정보만 보관)

	 */

	public static final class RequestMetadata {

		private final String traceId;

		private final String path;



		public RequestMetadata(String traceId, String path) {

			this.traceId = traceId;

			this.path = path;

		}



		public String getTraceId() {

			return traceId;

		}



		public String getPath() {

			return path;

		}

	}

}

