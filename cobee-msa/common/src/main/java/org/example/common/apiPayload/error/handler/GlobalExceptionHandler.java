package org.example.common.apiPayload.error.handler;

import org.example.common.apiPayload.error.code.BaseErrorCode;
import org.example.common.apiPayload.error.exception.CustomException;
import org.example.common.apiPayload.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.failure(ex.getReason(), String.valueOf(ex.getStatusCode().value()), null));
    }

    // CustomException 처리
    // @ExceptionHandler(CustomException.class)
    // public ResponseEntity<ApiResponse<Void>>
    // handleCustomException(CustomException ex) {
    // ErrorCode errorCode = ex.getErrorCode();
    // return ResponseEntity
    // .status(errorCode.getStatus())
    // .body(ApiResponse.failure(
    // errorCode.getMessage(),
    // errorCode.getCode(),
    // ex.getMessage()
    // ));
    // }
    @ExceptionHandler(CustomException.class)
    public ApiResponse<Void> handleCustomException(CustomException ex) {
        BaseErrorCode baseErrorCode = ex.getBaseErrorCode();
        return ApiResponse.failure(baseErrorCode.getMessage(), baseErrorCode.getCode(), ex.getMessage());
    }

    // 서버 내부 오류 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Internal Server Error", "500", ex.getMessage()));
    }
}
