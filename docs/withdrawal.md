# 회원 탈퇴 처리와 운영 절차

[문서 목차](README.md) · [구현 계획](../.local/plans/msa-withdrawal-consistency.md) · [검증 기록](../diagnostics/2026-10-31/withdrawal-tdd/README.md)

## 처리 기준

탈퇴 접수는 완료와 다르다. 먼저 저장된 모집 데이터는 탈퇴 과정에서 삭제한다.
모집 DB의 쓰기 차단이 커밋된 뒤에는, 이미 시작한 요청도 해당 회원의 데이터를 저장하지 못한다.
회원은 정리 중에 WITHDRAWING 상태이다. 모집 데이터 정리와 회원 삭제를 마치면 WITHDRAWN으로 조회한다.

모집글, 댓글, 지원, 북마크, 이미지 DB 참조를 처리한다.
외부 파일, 알림의 저장·전달, 채팅과 ML 사본의 완전 삭제는 이 완료 범위에 포함하지 않는다.
탈퇴자의 댓글을 삭제할 때 하위 답글도 삭제하는 기존 cascade 정책을 유지한다.

## HTTP 계약

| 경로 | 결과 |
|---|---|
| POST /auth/withdraw | 202. data에 operationId, memberId, state, stage, attempts 반환 |
| GET /auth/withdrawal | 200. 현재 요청자 본인의 작업 상태 반환. 작업이 없으면 404 |
| 같은 회원의 반복 탈퇴 | 최초 작업 ID와 현재 상태 반환. 완료 후에도 같은 작업 조회 |
| 차단 회원의 일반 보호 경로 | 403 |
| Redis 오류 또는 접근 상태 누락 | 503. 기본 허용으로 처리하지 않음 |

상태 조회와 반복 접수도 유효한 JWT가 필요하다. Gateway가 확인한 subject를 X-USER-ID로 덮어쓴다.
회원 원본 삭제 뒤에도 작업 기록으로 본인의 상태를 조회한다. 토큰 만료 규칙은 그대로 적용한다.
기존의 동기 탈퇴 응답을 사용한 클라이언트는 202 처리와 상태 조회를 연결해야 한다.

내부 경로는 공개 Gateway에 연결하지 않는다. X-Internal-Key로 서비스 자격을 확인한다.
대상 회원 ID는 경로에서 읽는다. X-USER-ID는 내부 호출자의 증명으로 사용하지 않는다.

| 내부 경로 | 동작 |
|---|---|
| PUT /internal/members/{memberId}/activate | 없는 ACTIVE 행만 초기화. BLOCKED는 해제하지 않음 |
| POST /internal/members/{memberId}/withdrawals/{operationId}/block | 상태 행을 잠그고 BLOCKED 커밋 |
| POST /internal/members/{memberId}/withdrawals/{operationId}/cleanup | 차단 작업 ID 확인 후 반복 가능한 데이터 정리 |
| POST /internal/member-access/bootstrap | 회원 서비스에서 기존 ACTIVE 회원의 모집·Redis 상태 초기화. initialized, failed 개수 반환 |

내부 키가 비어 있으면 내부 경로는 503이다. 잘못된 키는 401이다.

## 저장과 복구

모집 쓰기는 같은 트랜잭션에서 MemberWriteState를 잠그고 상태를 검사한 뒤 저장한다.
여러 회원 행은 ID 오름차순으로 잠근다. 차단도 같은 행에 충돌하는 잠금을 얻는다.
지도·회원 HTTP·파일 업로드는 쓰기 트랜잭션 밖에서 준비한다.
알림은 쓰기 처리 Bean이 반환해 DB 연결까지 해제한 뒤 호출한다. 발송은 현재 best effort이며 영속 전달 보장은 별도 과제이다.

| 쓰기 경로 | 잠그는 회원 |
|---|---|
| 모집글 생성 | 작성자 |
| 모집글 수정·삭제·이미지 연결 | 요청자와 글 소유자 |
| 댓글 생성 | 작성자와 글 소유자. 답글은 부모 댓글 작성자도 포함 |
| 지원 생성 | 지원자와 글 소유자 |
| 지원 수락·거절 | 요청자, 지원자, 글 소유자. 요청자가 소유자인지도 확인 |
| 북마크 추가·개별 삭제 | 요청자와 글 소유자 |
| 본인 북마크 전체 삭제 | 요청자 |

목록은 공개 Controller와 내부 Java 쓰기 메서드를 함께 확인한 결과이다.
새 쓰기 경로를 추가할 때도 저장 전에 같은 상태 검사를 적용한다.

탈퇴 작업의 단계는 다음과 같다.

```text
CACHE_BLOCK → RECRUIT_BLOCK → RECRUIT_CLEANUP → REFRESH_DELETE → MEMBER_REMOVE → COMPLETE
```

회원 상태 변경과 작업 생성은 같은 회원 DB 트랜잭션이다.
단계별 작업은 30초 처리권을 선점해 수행한다. 원격 호출은 DB 트랜잭션 밖에서 실행한다.
실패는 2초 뒤 재시도한다. 처리권이 만료되면 다른 프로세스가 이어받는다.
오래된 처리권과 단계 건너뛰기는 거부한다. 회원 삭제와 작업 완료는 같은 회원 DB 트랜잭션으로 커밋한다.
작업 기록은 회원 삭제와 함께 지우지 않는다.

Redis는 차단 상태 확정과 Refresh Token 삭제를 함께 실행한다. 로그인과 재발급도 ACTIVE 확인과 토큰 저장을 함께 실행한다.
현재 단일 Redis 구성의 키를 사용한다. Redis Cluster로 전환할 때는 이 키들을 같은 hash slot에 두는 변경이 필요하다.

## 준비할 설정

| 설정 | 용도 |
|---|---|
| INTERNAL_API_KEY | 회원·모집 서비스에 같은 비어 있지 않은 내부 키 설정 |
| REDIS_HOST, REDIS_PORT, REDIS_PASSWORD | Gateway·회원 서비스가 같은 Redis 사용 |
| WITHDRAWAL_WORKER_ENABLED | 기본 true. 작업 실행을 멈출 때 false. 접수 기록과 차단 상태는 유지 |
| withdrawal.poll-delay-ms | 기본 1000. 한 번의 poll에서 작업 한 단계를 처리 |

회원 Redis의 명령 제한 시간은 spring.data.redis.timeout을 사용한다.
내부 Feign 호출은 연결 2초·응답 8초로 제한하고 리다이렉트를 따르지 않는다.
모집 쓰기와 정리의 트랜잭션은 10초 제한을 사용한다.

## 적용 순서

1. 실제 활성 프로필과 DB 대상을 확인한다. 쓰기 트래픽을 잠시 중지하고 백업한다.
2. 회원 DB에 [001-member.sql](../cobee-msa/db/withdrawal/001-member.sql), 모집 DB에 [002-recruit.sql](../cobee-msa/db/withdrawal/002-recruit.sql)을 적용한다.
3. 내부 키와 공통 Redis 설정을 준비한다. 새 회원·모집·Gateway 인스턴스로 모두 교체한다.
4. 내부 bootstrap을 호출한다. failed가 0인지 확인한다. 신규 회원은 로그인 완료 전에 같은 초기화 절차를 거친다.
5. 정상 회원 쓰기, 차단 회원 거부, 202와 상태 조회를 확인한 뒤 트래픽을 연다.

저장소 설정은 ddl-auto validate이다. 이 변경은 DB를 자동 초기화하지 않는다.
상태 검사가 없는 구버전 인스턴스에는 쓰기 트래픽을 보내지 않는다.
되돌릴 때도 상태 행과 작업 기록을 보존한다. 구버전으로 되돌려야 하면 쓰기를 먼저 중지한다.
이 작업에서 실제 운영 환경 배포는 수행하지 않았다.

## 관측과 남은 범위

정체된 단계, attempts, next_attempt_at, lease_until, last_error를 확인한다.
last_error에는 예외 종류만 보관한다. 토큰·요청 본문·외부 응답 원문은 작업 기록에 넣지 않는다.
DB의 차단 행과 Redis의 BLOCKED를 단순 재시도나 지연된 등록으로 해제하지 않는다.
Redis를 비운 뒤에는 ACTIVE 상태를 bootstrap으로 다시 준비한다. 누락된 상태는 503이다.

회원 DB에서 정체와 재시도를 확인한다. 운영 환경의 스키마에 맞게 실행한다.

```sql
SELECT id, member_id, stage, attempts, created_at, next_attempt_at, lease_until, last_error
FROM withdrawal_job
WHERE stage <> 'COMPLETE'
ORDER BY created_at;
```

모집 DB에서는 작업 ID와 차단·정리 상태를 확인한다.
DB 잠금 대기는 PostgreSQL의 `pg_stat_activity`와 `pg_blocking_pids`로 조회한다.
본문과 토큰을 피하기 위해 SQL 원문은 조회하지 않는다.

```sql
SELECT member_id, withdrawal_id, state, cleaned
FROM member_write_state
WHERE state = 'BLOCKED';

SELECT pid, wait_event_type, wait_event,
       clock_timestamp() - xact_start AS transaction_age,
       pg_blocking_pids(pid) AS blocking_pids
FROM pg_stat_activity
WHERE datname = current_database() AND xact_start IS NOT NULL;
```

정체가 길어도 작업을 COMPLETE로 강제 변경하지 않는다. 오류 원인을 고친 뒤 같은 단계를 재시도한다.
현재 작업자는 한 번에 한 단계씩 실행한다. 대량 탈퇴 처리량과 잠금 대기 알림 기준은 운영 부하에 맞게 별도로 정한다.

첫 상태 초기화는 모집 서비스에 동기 호출한다. 모집 서비스 장애가 로그인 완료에 영향을 줄 수 있다.
삭제가 거부되기 전에 업로드된 외부 파일의 정리와 알림 전달 보장은 별도 후속 범위이다.
