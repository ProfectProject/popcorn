package com.popcorn.gateway.dto;

public record PassportEnvelope(PassportPayload payload, String userIntegrity) {}
