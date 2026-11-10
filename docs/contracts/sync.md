# MSA–ML 동기화 계약

[문서 목차](../README.md) · [구조 안내](../architecture.md) · [검증 기준](../verification.md)

2026-10-27의 원본 코드를 대조했다.
HTTP 전체 흐름을 실행한 계약 검증 결과는 아니다. 현재의 선언과 불일치를 함께 기록한다.

## 원본 위치

| 구간 | 근거 |
|------|------|
| Gateway 라우팅 | [application.yml](../../cobee-msa/apigateway-service/src/main/resources/application.yml) |
| 회원 경로·조회 | [MemberSyncController](../../cobee-msa/member-service/src/main/java/org/example/memberservice/controller/MemberSyncController.java), [MemberSyncService](../../cobee-msa/member-service/src/main/java/org/example/memberservice/service/MemberSyncService.java) |
| 모집·상호작용 경로·조회 | [RecruitSyncController](../../cobee-msa/recruit-service/src/main/java/org/example/recruitservice/controller/RecruitSyncController.java), [RecruitSyncService](../../cobee-msa/recruit-service/src/main/java/org/example/recruitservice/service/RecruitSyncService.java) |
| MSA 응답 | [회원 DTO](../../cobee-msa/member-service/src/main/java/org/example/memberservice/dto/sync/MemberSyncDto.java), [모집 DTO 폴더](../../cobee-msa/recruit-service/src/main/java/org/example/recruitservice/dto/sync/) |
| ML 요청·검증·저장 | [ApiClient](../../cobee-ml/src/clients/msa_backend_client.py), [동기화 스키마](../../cobee-ml/src/models/sync_schemas.py), [DataSyncService](../../cobee-ml/src/services/data_sync_service.py) |

## HTTP 경로

메서드는 모두 `GET`이다. 성공 응답의 선언은 DTO 배열이다. 공통 `ApiResponse` 봉투로 감싸지 않는다.

| 경로 | MSA 모듈 | ML 메서드 | 응답 DTO |
|------|----------|-----------|----------|
| `/sync/members` | member-service | `get_members` | `MemberSyncDto` |
| `/sync/recruit-posts` | recruit-service | `get_recruit_posts` | `RecruitPostSyncDto` |
| `/sync/apply-records` | recruit-service | `get_apply_records` | `ApplyRecordSyncDto` |
| `/sync/bookmarks` | recruit-service | `get_bookmarks` | `BookmarkSyncDto` |
| `/sync/comments` | recruit-service | `get_comments` | `CommentSyncDto` |

ML은 `MSA_BACKEND_URL`과 위 경로를 붙인다. 기본 주소 끝의 `/`는 생략한다.
Gateway에는 `/sync/members/**`와 `/sync/**` 라우팅 선언이 있다.

## 헤더와 증분 조회

- ML Client가 보내는 헤더는 `X-Sync-Key: SYNC_SECRET_KEY 값`이다.
- Gateway의 동기화 라우트도 `X-Sync-Key`를 추가한다.
- 회원 서비스는 `/sync/**`에서 [SyncKeyInterceptor](../../cobee-msa/common/src/main/java/org/example/common/interceptor/SyncKeyInterceptor.java)를 적용한다.
- 모집 Controller는 추가로 숫자형 `X-USER-ID`를 요구한다. 이름은 [GatewayConstant](../../cobee-msa/common/src/main/java/org/example/common/constant/GatewayConstant.java)에 있다.
- 모집 서비스의 실제 인터셉터 경로는 아래 확인 필요 항목을 본다.

선택 쿼리 이름은 `lastSyncTime`이다.
없으면 전체 조회, 있으면 해당 시각 이후의 `updatedAt`을 조회한다.
회원 Repository는 `updatedAt > :updatedAt` 조건을 명시한다. 모집 Repository는 `findAllByUpdatedAtAfter`를 사용한다.

MSA 매개변수는 `LocalDateTime`이며 ISO 날짜·시간 형식을 받도록 선언되어 있다.
ML Client는 `datetime.isoformat()`으로 쿼리 값을 만든다.
시간대 변환과 저장된 커서의 변환은 통합 확인이 필요하다.

## 응답 필드

동기화 JSON의 필드명은 `snake_case`이다.
추천 API의 `camelCase` 모델과 구분한다.

아래 표는 **ML 스키마가 필수로 요구하는 필드**이다.

| DTO | 식별자와 관계 키 | 시간 필드 |
|-----|------------------|-----------|
| MemberSyncDto | `member_id` | `created_at`, `updated_at` |
| RecruitPostSyncDto | `recruit_post_id`, `member_id` | `created_at`, `updated_at` |
| ApplyRecordSyncDto | `record_id`, `member_id`, `recruit_post_id` | `created_at`, `updated_at` |
| BookmarkSyncDto | `bookmark_id`, `member_id`, `recruit_post_id` | `created_at`, `updated_at` |
| CommentSyncDto | `comment_id`, `member_id`, `recruit_post_id` | `created_at`, `updated_at` |

- 식별자는 정수이다. 필수 키와 시간은 누락하거나 `null`로 보내면 ML 검증을 통과하지 못한다.
- `created_at`과 `updated_at`은 ML에서 `datetime`으로 읽는다.
- `submitted_at`은 선택 날짜 필드이다. `birth_date`는 선택 문자열 필드이다.
- 프로필·선호도 등 나머지 필드는 각 DTO의 `Optional`과 기본값을 확인한다.
- Java DTO는 필드 선언과 `from(...)` 매핑을 함께 확인한다. 필드 선언만으로 값이 채워진다고 판단하지 않는다.
- ML 동기화 스키마의 상태·성별 문자열은 모두 enum으로 검증되는 구조가 아니다. 값 정책을 바꿀 때 양쪽 코드를 대조한다.

다음은 `MemberSyncDto`로 읽을 수 있는 문서용 최소 응답 예제이다. 실제 HTTP 응답을 캡처한 자료는 아니다.

```json
[
  {
    "member_id": 42,
    "gender": "MAN",
    "birth_date": "2000-01-02",
    "created_at": "2026-10-01T09:00:00",
    "updated_at": "2026-10-02T10:30:00"
  }
]
```

## 확인 필요 항목

다음은 정적 대조에서 확인한 차이이다. 실행 실패를 재현한 기록과 구분한다.

| ID | 현재 코드의 차이 | 확인과 완료 기준 |
|----|------------------|------------------|
| SYNC-01 | 모집 Controller는 `X-USER-ID`를 요구한다. ML Client 헤더와 현재 Gateway 동기화 라우트에는 이 값을 넣는 선언이 없다. | 서비스 간 호출의 사용자 헤더 정책을 정하고 관련 요청 테스트로 확인 |
| SYNC-02 | 모집 [WebMvcConfig](../../cobee-msa/recruit-service/src/main/java/org/example/recruitservice/config/WebMvcConfig.java)는 `/api/sync/**`에 키 검사를 등록한다. Controller는 `/sync/**`이다. | 의도한 인증 범위를 정하고 실제 Controller 경로의 헤더 검사를 확인 |
| SYNC-03 | 상태 파일에서는 마지막 동기화 시각을 문자열로 읽어 Client에 넘긴다. Client는 값이 있으면 `.isoformat()`을 호출한다. | 저장·복원·요청의 타입과 시간대 정책을 맞추고 두 번째 동기화를 확인 |
| SYNC-04 | 댓글을 가져와 `SyncData.comments`에 담지만 `_insert_data`에는 댓글 저장 반복문이 없다. | 댓글의 저장·사용 정책을 정하고 의도한 데이터 반영을 확인 |

DB 커밋, 학습, 커서 저장은 별도 단계이다.
[학습 코드](../../cobee-ml/src/services/model_training_service.py)는 예외를 기록하고 반환하는 경로가 있으므로, 동기화 성공 로그만으로 학습 성공을 판단하지 않는다.

## 변경 시 확인 순서

1. Gateway 경로·헤더와 양쪽 Controller·Client를 확인한다.
2. MSA DTO의 필드·매핑과 ML 스키마·ORM·SQL을 확인한다.
3. 관련 단위 테스트를 실행한다. 현재 회원 스키마 테스트가 모든 DTO와 HTTP 경로를 검증하는 것은 아니다.
4. 외부 서비스가 필요한 확인은 별도 통합 검증으로 기록한다.
5. 이 계약과 [유지보수 항목](../maintenance.md)을 갱신한다.

Graphify의 `reviewed-links.json`은 정적 대조 근거이다. 현재 코드와 해시를 대조하고, 갱신은 수동 요청 절차를 따른다.
