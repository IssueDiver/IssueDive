package com.issueDive.exception;

public class UserNotFoundException extends BaseException{
    public UserNotFoundException(String message){
        super(ErrorCode.UserNotFound, message);
    }

    public UserNotFoundException(Long userId){
        super(ErrorCode.UserNotFound, userId + "와 같은 사용자를 찾을 수 없습니다.");
    }

    public UserNotFoundException(String field, String value){
        super(ErrorCode.UserNotFound, value +"의 "+ field +"과 같은 사용자를 찾을 수 없습니다.");
    }
}
