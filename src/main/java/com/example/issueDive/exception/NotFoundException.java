package com.example.issueDive.exception;

import lombok.RequiredArgsConstructor;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String userNotFound) {
    }
}
