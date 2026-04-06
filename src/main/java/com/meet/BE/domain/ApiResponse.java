package com.meet.BE.domain;

public record ApiResponse<T>(
        T data,
        String message,
        Object errors,
        int status
) {}
