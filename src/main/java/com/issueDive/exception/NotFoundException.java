package com.issueDive.exception;

public class NotFoundException extends RuntimeException {

    /**
     * 부모 클래스인 RuntimeException 생성자를 호출해 예외 메시지 저장
     * 글로벌 예외 처리 클래스 (@RestControllerAdvice)에서 받아 처리
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * 필요시, 캐치된 다른 예외를 원인으로 넘길 수 있는 생성자
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
