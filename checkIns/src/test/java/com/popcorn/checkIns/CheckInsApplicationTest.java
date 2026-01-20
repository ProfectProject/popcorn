package com.popcorn.checkIns;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CheckInsApplicationTest {

	@Test
	void contextLoads() {
		// Spring context가 성공적으로 로드되었다면 이 테스트는 통과함
		assertThat(true).isTrue();
	}

	@Test
	void mainMethodExists() {
		// main 메서드가 존재하는지 확인하는 간단한 테스트
		try {
			CheckInsApplication.class.getDeclaredMethod("main", String[].class);
			assertThat(true).isTrue();
		} catch (NoSuchMethodException e) {
			assertThat(false).isTrue();
		}
	}
}