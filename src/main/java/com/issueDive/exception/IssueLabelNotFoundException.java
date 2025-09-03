package com.issueDive.exception;

public class IssueLabelNotFoundException extends NotFoundException {
    public IssueLabelNotFoundException(String message) {
        super(message);
    }
    public IssueLabelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
