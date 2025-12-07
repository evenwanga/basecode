package com.company.platform.common;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(boolean success, String message, T data, Integer status) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, HttpStatus.OK.value());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, null, data, HttpStatus.CREATED.value());
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus status) {
        return new ApiResponse<>(false, message, null, status.value());
    }
}
