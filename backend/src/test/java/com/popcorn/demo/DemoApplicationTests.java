package com.popcorn.demo;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectPackages;

/**
 * 🎬 PopCorn 전체 테스트 스위트
 *
 * 이 클래스는 com.popcorn.demo 패키지 전체의 모든 테스트를 실행합니다.
 * JUnit Platform Suite Engine을 사용하여 패키지 내의 모든 테스트 클래스를 자동으로 검색하고 실행합니다.
 *
 * 실행 방법:
 * - ./gradlew test
 * - IDE에서 이 클래스를 직접 실행
 *
 * 포함되는 테스트:
 * - 도메인 로직 테스트 (order, payment, store, popup, qr 등)
 * - 컨트롤러 통합 테스트
 * - 서비스 레이어 테스트
 * - 설정 및 보안 테스트
 * - 성능 및 부하 테스트
 */
@Suite
@SelectPackages("com.popcorn.demo")
public class DemoApplicationTests {
    // JUnit Platform Suite가 자동으로 com.popcorn.demo 패키지의 모든 테스트를 검색하고 실행
}
