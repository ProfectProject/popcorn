package com.popcorn.common.util;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.common.dto.PassportEnvelope;
import com.popcorn.common.dto.PassportPayload;

public class PassportVerifier {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static PassportPayload verify(
            String passportJson,
            String secret
    ) {
        try {
            PassportEnvelope envelope =
                    mapper.readValue(passportJson, PassportEnvelope.class);

            String payloadJson =
                    mapper.writeValueAsString(envelope.payload());

            String expected =
                    HmacUtil.hmacSha256Base64Url(secret, payloadJson);

            if (!expected.equals(envelope.userIntegrity())) {
                throw new SecurityException("Passport integrity mismatch");
            }

            long now = Instant.now().getEpochSecond();
            if (envelope.payload().exp() < now) {
                throw new SecurityException("Passport expired");
            }

            return envelope.payload();

        } catch (Exception e) {
            throw new SecurityException("Invalid passport", e);
        }
    }
}
