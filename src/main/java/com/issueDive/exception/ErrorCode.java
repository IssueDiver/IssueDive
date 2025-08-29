package com.issueDive.exception;

public enum ErrorCode {
    ValidationError, InvalidQueryParam, Unauthorized, Forbidden, InternalServerError,
    IssueNotFound, InvalidStatus,

    LabelNotFound, IssueLabelNotFound, DuplicateLabel,

    CommentNotFound, InvalidParentComment,
    UserNotFound, DuplicateEmail, AuthenticationFailed
}