package com.popcorn.demo.domain.order.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

	@Override
	public String convertToDatabaseColumn(PaymentMethod attribute) {
		if (attribute == null) {
			return null;
		}
		if (attribute == PaymentMethod.TRANSFER) {
			return PaymentMethod.VIRTUAL.name();
		}
		return attribute.name();
	}

	@Override
	public PaymentMethod convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return null;
		}
		PaymentMethod method = PaymentMethod.valueOf(dbData);
		if (method == PaymentMethod.VIRTUAL) {
			return PaymentMethod.TRANSFER;
		}
		return method;
	}
}
