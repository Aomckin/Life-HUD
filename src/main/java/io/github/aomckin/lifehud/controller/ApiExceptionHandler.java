package io.github.aomckin.lifehud.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/** Stable JSON error boundary shared by browser clients and external Agent tools. */
@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException error, HttpServletRequest request) {
        return response(error.getStatusCode().value(), detail(error.getReason(), "请求无法完成"), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST.value(), "请求格式或字段值无效", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> uploadTooLarge(MaxUploadSizeExceededException error,
                                                       HttpServletRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "文件太大：单个文件不能超过 64MB", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> dataFailure(IllegalStateException error, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                detail(error.getMessage(), "数据读取失败，请检查数据文件"), request);
    }

    private ResponseEntity<Map<String, Object>> response(int status, String detail, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("detail", detail);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private String detail(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
