package com.issueDive.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceResult<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ServiceResult<T> success(T _data){
        return ServiceResult.<T>builder()
                .success(true)
                .data(_data)
                .build();
    }

    public static <T> ServiceResult<T> error(String _code, String _message){
        return ServiceResult.<T>builder()
                .success(false)
                .code(_code)
                .message(_message)
                .build();
    }
}
