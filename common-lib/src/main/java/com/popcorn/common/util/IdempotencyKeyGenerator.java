package com.popcorn.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component("idempotencyKeyGenerator")
@RequiredArgsConstructor
public class IdempotencyKeyGenerator {

    private final ObjectMapper objectMapper;

    public String hash(Object payload) {
        if (payload == null) {
            return "null";
        }
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(json);
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            return Integer.toHexString(String.valueOf(payload).hashCode());
        }
    }
}
