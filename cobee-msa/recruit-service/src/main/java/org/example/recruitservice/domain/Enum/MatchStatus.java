package org.example.recruitservice.domain.Enum;

public enum MatchStatus {
    ON_WAIT,     // 지원한 후 승인 받기전 상태의 대기중
    MATCHING, // 승인 받음
    REJECTED, // 거절당함
    MATCHED  // 최종매칭
}
