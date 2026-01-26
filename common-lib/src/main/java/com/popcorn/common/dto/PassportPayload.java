package com.popcorn.common.dto;

public record PassportPayload(PassportUser user, long iat, long exp) {}
