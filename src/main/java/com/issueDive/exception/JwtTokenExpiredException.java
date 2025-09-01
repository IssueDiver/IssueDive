package com.issueDive.exception;

public class JwtTokenExpiredException extends BaseException {
    public JwtTokenExpiredException() {
        super(ErrorCode.AuthenticationFailed, "JWT 토큰이 만료되었습니다. 다시 로그인해주세요.");
    }

    public JwtTokenExpiredException(String message) {
        super(ErrorCode.AuthenticationFailed, message);
    }
}
