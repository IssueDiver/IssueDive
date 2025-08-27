package com.example.issueDive.exception;

public enum ErrorCode {
    ValidationError, InvalidQueryParam, Unauthorized, Forbidden, InternalServerError,
    IssueNotFound, InvalidStatus,
    LabelNotFound, IssueLabelNotFound,
    CommentNotFound, InvalidParentComment,
    UserNotFound, DuplicateEmail, AuthenticationFailed
}

