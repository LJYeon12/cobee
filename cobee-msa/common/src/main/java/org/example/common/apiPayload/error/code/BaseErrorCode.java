package org.example.common.apiPayload.error.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseErrorCode {
    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATROOM-001", "Cannot find ChatRoom"),
    CHAT_ROOM_FULL(HttpStatus.BAD_REQUEST, "CHATROOM-002", "ChatRoom is full"),
    CHAT_ROOM_NAME_DUPLICATED(HttpStatus.BAD_REQUEST, "CHATROOM-003", "ChatRoom name is duplicated"),
    CHAT_ROOM_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "CHATROOM-004", "ChatRoom is not editable"),
    CHAT_ROOM_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATROOM-005", "Cannot find user in ChatRoom"),
    CHAT_ROOM_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "CHATROOM-006", "Cannot find post in ChatRoom"),
    CHAT_ROOM_USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "CHATROOM-007", "User already exists in ChatRoom"),
    CHAT_ROOM_USER_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "CHATROOM-008", "User is not in the ChatRoom"),
    CHAT_ROOM_EXISTS_USER(HttpStatus.BAD_REQUEST, "CHATROOM-009", "User exists in the ChatRoom"),
    CHAT_ROOM_NAME_CANNOT_EMPTY(HttpStatus.BAD_REQUEST, "CHATROOM-010", "ChatRoom name cannot be empty"),

    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOCATION-001", "Cannot find Location"),
    LOCATION_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "LOCATION-002", "Location already exists"),
    GOOGLE_MAP_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GOOGLE-001", "Google Map API error"),
    GOOGLE_NEARBY_API_ERROR(HttpStatus.BAD_GATEWAY, "GOOGLE-002", "Google Nearby Search API error"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "GOOGLE-003", "Address not found"),
    GOOGLE_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GOOGLE-004", "Error parsing Google API response"),
    GOOGLE_OVER_QUERY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "GOOGLE-005", "Google API query limit exceeded"),
    GOOGLE_REQUEST_DENIED(HttpStatus.FORBIDDEN, "GOOGLE-006", "Google API request denied"),
    GOOGLE_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "GOOGLE-007", "Invalid request to Google API");


    private final HttpStatus status;
    private final String code;
    private final String message;

    BaseErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
