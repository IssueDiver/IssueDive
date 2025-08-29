package com.issueDive.exception;

public class AuthenticationFailedException extends BaseException{
    public AuthenticationFailedException(String message){
        super(ErrorCode.AuthenticationFailed, message);
    }

    public AuthenticationFailedException(){
        super(ErrorCode.AuthenticationFailed, "로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요.");
    }
}
