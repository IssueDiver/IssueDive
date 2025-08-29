package com.issueDive.exception;

public class DuplicateEmailException extends BaseException{
    public DuplicateEmailException(String email){
        super(ErrorCode.DuplicateEmail, "이미 사용 중인 이메일 입니다.");
    }
}
