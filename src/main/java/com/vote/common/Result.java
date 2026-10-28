package com.vote.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    public static <T> Result<T> success(T data) { return new Result<>(200, "success", data); }
    public static Result<Void> success(String message) { return new Result<>(200, message, null); }
    public static <T> Result<T> success(String message, T data) { return new Result<>(200, message, data); }
    public static <T> Result<T> error(String message) { return new Result<>(500, message, null); }
    public static <T> Result<T> error(Integer code, String message) { return new Result<>(code, message, null); }
}