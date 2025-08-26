package com.example.issueDive.dto;

import com.example.issueDive.exception.ErrorCode;

import java.time.LocalDateTime;

/**
 * 요청 실패 시 공통 응답 포맷
 * @param success false
 * @param error ErrorDetail: errorCode name, message
 * @param timestamp now()
 */
public record ErrorResponse(
        boolean success,
        ErrorDetail error,
        String timestamp
) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(false, new ErrorDetail(code.name(), message),
                LocalDateTime.now().toString());
    }

    public record ErrorDetail(String code, String message) {}
}
