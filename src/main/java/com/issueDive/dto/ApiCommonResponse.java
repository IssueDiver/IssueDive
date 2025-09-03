package com.issueDive.dto;

import java.time.LocalDateTime;

/**
 * 요청 성공 시 공통 응답 포맷
 * @param success true
 * @param data response
 * @param timestamp now()
 */
public record ApiCommonResponse<T>(
        boolean success,
        T data,
        String timestamp
) {

    public static <T> ApiCommonResponse<T> ok(T data) {
        return new ApiCommonResponse<>(true, data,
                LocalDateTime.now().toString());
    }
}
