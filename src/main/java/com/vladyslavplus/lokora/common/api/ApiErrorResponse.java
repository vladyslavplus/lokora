package com.vladyslavplus.lokora.common.api;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path
) {
}
