package org.example.common.apiPayload.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 자동 제거
public class ApiResponse<T> {
    private final boolean isSuccess;
    private final String message;
    private final String code;
    private final String error;  // 에러 원인 (성공 시 null)
    private final T data;

    private ApiResponse(boolean isSuccess, String message, String code, String error, T data) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.code = code;
        this.error = error;
        this.data = data;
    }

    // 성공 응답
    public static <T> ApiResponse<T> success(String message, String code, T data) {
        return new ApiResponse<>(true, message, code, null, data);
    }

    // 성공 응답 (데이터 없는 버전)
    public static <T> ApiResponse<T> success(String message, String code) {
        return new ApiResponse<>(true, message, code, null, null);
    }

    // 실패 응답 (데이터 없음)
    public static <T> ApiResponse<T> failure(String message, String code, String error) {
        return new ApiResponse<>(false, message, code, error, null);
    }

    // 실패 응답 (데이터 포함)
    public static <T> ApiResponse<T> failure(String message, String code, String error, T data) {
        return new ApiResponse<>(false, message, code, error, data);
    }
}
