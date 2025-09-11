package com.issueDive.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션이 선언된 DTO의 필드는
 * XSS 방지 필터링(Sanitization)을 적용하지 않습니다.
 * (이메일, 비밀번호 등 특수문자가 필요한 필드에 사용)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoXss {
}
