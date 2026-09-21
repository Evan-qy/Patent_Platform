package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException e) {
        int httpStatus = e.getStatusCode().value();
        String reason = e.getReason() == null ? "请求失败" : e.getReason();
        ApiResponse<Void> body = new ApiResponse<>(1, reason, null);
        return ResponseEntity.status(httpStatus).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        e.printStackTrace(); // 打印堆栈信息以便排查
        return ResponseEntity.status(500).body(ApiResponse.fail("服务器错误: " + e.getMessage()));
    }
}
