package com.issueDive.exception;

public class LabelNotFoundException extends NotFoundException {
    public LabelNotFoundException(String message) {
        super(message);
    }
    public LabelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
