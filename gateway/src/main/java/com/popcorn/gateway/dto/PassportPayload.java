package com.popcorn.gateway.dto;

public record PassportPayload(PassportUser user, long iat, long exp) {}
