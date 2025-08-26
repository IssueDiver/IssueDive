package com.example.issueDive.dto;

import java.time.LocalDateTime;

/**
 * 요청 성공 시 공통 응답 포맷
 * @param success true
 * @param data response
 * @param timestamp now()
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data,
                LocalDateTime.now().toString());
    }
}

