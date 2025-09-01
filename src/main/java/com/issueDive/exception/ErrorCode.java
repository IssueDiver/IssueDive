package com.issueDive.exception;

public enum ErrorCode {
    ValidationError, InvalidQueryParam, Unauthorized, Forbidden, BadRequest, InternalServerError,
    IssueNotFound, InvalidStatus,

    LabelNotFound, IssueLabelNotFound, DuplicateLabel,

    CommentNotFound, InvalidParentComment,
    UserNotFound, DuplicateEmail, AuthenticationFailed,

    // JWT 관련 에러 코드 추가
    JwtTokenExpired, JwtTokenInvalid, JwtTokenMalformed
}