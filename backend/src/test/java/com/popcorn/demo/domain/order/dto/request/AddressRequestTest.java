package com.popcorn.demo.domain.order.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressRequestTest {

	@Test
	@DisplayName("Address validity depends on address1")
	void validatesAddress() {
		AddressRequest request = AddressRequest.builder().address1(" Road 1 ").build();

		assertThat(request.isValid()).isTrue();

		AddressRequest invalid = AddressRequest.builder().address1("  ").build();
		assertThat(invalid.isValid()).isFalse();
	}

	@Test
	@DisplayName("Full address concatenates address1 and address2")
	void buildsFullAddress() {
		AddressRequest request = AddressRequest.builder()
				.address1("Road 1")
				.address2("Suite 2")
				.build();

		assertThat(request.getFullAddress()).isEqualTo("Road 1 Suite 2");

		AddressRequest empty = AddressRequest.builder().address1(" ").build();
		assertThat(empty.getFullAddress()).isEqualTo("");
	}
}
