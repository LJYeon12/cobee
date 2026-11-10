package org.example.common.apiPayload.error.exception;

import lombok.Getter;
import org.example.common.apiPayload.error.code.BaseErrorCode;


@Getter
public class CustomException extends RuntimeException {
    private final BaseErrorCode baseErrorCode;

    /* throw new CustomException(ErrorCode.XXX)으로 에러 던지기 */
    public CustomException(BaseErrorCode baseErrorCode) {
        super(baseErrorCode.getMessage());
        this.baseErrorCode = baseErrorCode;
    }
}
