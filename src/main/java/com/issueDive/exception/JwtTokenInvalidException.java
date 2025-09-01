package com.issueDive.exception;

public class JwtTokenInvalidException extends BaseException {
    public JwtTokenInvalidException(){
        super(ErrorCode.AuthenticationFailed, "유효하지 않은 JWT 토큰입니다.");
    }

    public JwtTokenInvalidException(String message) {
        super(ErrorCode.AuthenticationFailed,message);
    }
}
