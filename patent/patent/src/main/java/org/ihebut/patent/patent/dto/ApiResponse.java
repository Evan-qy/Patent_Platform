package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "操作成功", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "操作成功", null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(1, message, null);
    }
}
