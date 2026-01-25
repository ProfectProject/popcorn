package com.popcorn.common.dto;

public record PassportEnvelope(PassportPayload payload, String userIntegrity) {}
