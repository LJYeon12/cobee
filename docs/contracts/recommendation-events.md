# 추천 이벤트 계약 제안

[설계안](../../.local/plans/rag-recommendation.md) · [현재 HTTP 동기화 계약](sync.md) · [문서 목차](../README.md)

작성일: 2026-10-27. 상태: **제안. 아직 구현하지 않음**.
이 계약은 Kafka로 추천용 사본을 만드는 방법이다. 현재 HTTP 동기화의 동작을 설명하지 않는다.

## 1. 전달 원칙

원본 DB 저장과 Outbox 저장은 같은 트랜잭션이다.
Outbox 발행과 Kafka 수신은 **최소 한 번 전달**을 전제로 한다. 중복은 정상 상황으로 처리한다.
추천 DB에 반영한 뒤 offset을 커밋한다. 원본 DB와 Kafka, 추천 DB를 하나의 트랜잭션으로 묶지 않는다.

Kafka 트랜잭션만으로 외부 PostgreSQL 반영까지 정확히 한 번이 되지는 않는다.
따라서 DB의 중복 키와 버전 검사로 결과를 보호한다.
근거: [Kafka 전달 의미](https://kafka.apache.org/41/design/design/#message-delivery-semantics).

첫 발행 Adapter는 서비스별 Outbox 폴링이다. CDC는 후속 대안이다.
폴링은 발행 확인 뒤 `published_at`을 기록한다. 확인 전에 종료되면 다시 발행한다.
동일 aggregate에는 한 발행만 진행하도록 임대를 건다. 전체 aggregate를 한 줄로 직렬화하지 않는다.
추천용 이벤트 때문에 기존 알림 발송 경로를 함께 바꾸지 않는다.

## 2. 소유자, 토픽, 키

이름 끝의 `v1`은 계약의 주 버전이다. 토픽은 최초에는 하나의 소비 그룹으로 읽는다.
주요 버전 변경 때 새 토픽·사본 세대를 사용한다.
회원 토픽의 비공개 조건은 추천 소비자만 읽을 수 있게 권한을 제한한다.
전송 암호화와 DLT 접근 제한도 같은 기준을 쓴다.

| 토픽 | 발행자 | aggregate와 Kafka key | payload의 전체 상태 |
|------|--------|------------------------|-----------------------|
| `cobee.member.recommendation.v1` | member-service | `member:{member_id}` | 회원 활성·공개 상태, 본인 사실, 공개 자기소개, 상대 조건, 주거 조건, 규칙 수용 |
| `cobee.recruit.recommendation.v1` | recruit-service | `post:{post_id}` | 작성자 ID, 제목·두 설명, 비용·위치, 모집 조건·규칙, 상태·인원·공개 여부 |
| `cobee.application.state.v1` | recruit-service | `application:{member_id}:{post_id}` | 지원 ID, 지원자·글 ID, 현재 지원 상태 또는 삭제 |
| `cobee.bookmark.state.v1` | recruit-service | `bookmark:{member_id}:{post_id}` | 북마크 ID, 회원·글 ID, 현재 저장 여부 또는 삭제 |

지원과 북마크의 aggregate는 회원·글 쌍이다.
삭제 뒤 새 DB 행으로 다시 만들 때도 같은 쌍의 버전을 이어 간다.
내부 행 ID가 바뀌었다는 이유로 버전을 1로 되돌리지 않는다.

Kafka 순서는 같은 토픽의 같은 partition 안에서만 성립한다.
다른 토픽의 회원·글·지원에는 전체 순서가 없다.
키별 발행, 버전 검사, 전체 상태 전달을 함께 사용한다.
partition 수를 바꿀 때는 기존 키의 위치가 바뀔 수 있다. 새 토픽 전환이나 쓰기 정지·배출 절차를 사용한다.

## 3. 공통 봉투

JSON UTF-8을 쓴다. 아래 값은 모두 합성 예제이다.
식별자는 문자열이다. Java `Long`이 JavaScript 숫자 정밀도를 넘는 문제를 피한다.

| 필드 | 규칙 |
|------|------|
| `event_id` | 이벤트 하나의 UUID. 재전송·DLT 복구 때 유지 |
| `event_type` | 토픽에 허용한 사건 이름. 본문의 `operation`과 함께 검사 |
| `schema_version` | 정수 1. 소비자가 이해하지 못하는 주 버전은 격리 |
| `producer` | `member-service` 또는 `recruit-service`. 토픽별 발행 권한과 일치 |
| `aggregate_type`, `aggregate_id` | 토픽과 key에 일치. 쌍 ID는 `member_id:post_id` |
| `aggregate_version` | 1부터 증가하는 정수. 시간 대신 새 상태 판정에 사용 |
| `operation` | `UPSERT` 또는 `DELETE` |
| `occurred_at` | UTC RFC 3339. 예: `2026-10-27T03:00:00Z`. 순서 결정에 사용하지 않음 |
| `trace_id` | 원본 변경과 소비 흐름을 찾을 ID. 토큰·개인정보를 넣지 않음 |
| `payload` | UPSERT는 전체 추천용 상태. DELETE는 키·삭제 사실만 포함 |

회원 aggregate 버전은 회원, 프로필, 선호, 공개 설정 중 하나가 바뀔 때 증가한다.
프로필·선호 삭제도 변경이다. 회원 row의 수정 시각에 의존하지 않는다.
버전 증가와 전체 상태 생성은 같은 트랜잭션에서 회원 기준 잠금 또는 낙관적 버전 검사로 처리한다.
모집글 버전도 글, 조건, 상태, 인원 변경을 함께 포함한다.
지원 수락으로 인원이 바뀌면 지원 이벤트와 새 모집글 이벤트를 같은 Outbox 트랜잭션에 기록한다.

생성·수정 이벤트의 이름은 `MemberRecommendationSnapshotChanged`, `RecruitRecommendationSnapshotChanged`,
`ApplicationStateChanged`, `BookmarkStateChanged`이다. DELETE에도 같은 이름과 새 버전을 사용한다.
증분 필드 목록을 보내는 patch 이벤트는 첫 계약에서 사용하지 않는다.
이전 소비자가 무시해도 기존 뜻이 같은 선택 필드만 호환 추가로 본다.
새 필수 조건이나 상태의 뜻을 바꾸는 변경은 새 주 버전이다. 이전 소비자가 모르는 필수 조건을 무시하게 하지 않는다.

### 3.1 회원 전체 상태 예제

배열은 이전 배열 전체를 교체한다. 빈 배열은 조건 없음이다.
선택 항목도 키를 보내고 값이 없으면 `null`로 보낸다. 필수 사실의 `null`을 `false`로 바꾸지 않는다.
아래 `REQUIRED`는 새로 명시한 합성 조건이다. 기존 Boolean에서 자동 변환한 값이 아니다.

```json
{
  "event_id": "29439430-e746-4f50-9346-33472c0b70da",
  "event_type": "MemberRecommendationSnapshotChanged",
  "schema_version": 1,
  "producer": "member-service",
  "aggregate_type": "member",
  "aggregate_id": "42",
  "aggregate_version": 7,
  "operation": "UPSERT",
  "occurred_at": "2026-10-27T03:00:00Z",
  "trace_id": "synthetic-change-42-7",
  "payload": {
    "member_id": "42",
    "member_status": "ACTIVE",
    "recommendation_allowed": true,
    "eligibility_facts": {"gender": "MAN", "birth_date": "2002-10-27"},
    "public_profile": {
      "visibility": "PUBLIC",
      "my_info": "아침에 활동하며 공용 공간을 정리합니다.",
      "self_traits": {
        "lifestyle": "MORNING",
        "personality": null,
        "smokes": false,
        "snores": null,
        "has_pet": false
      }
    },
    "partner_conditions": [
      {"subject": "AUTHOR", "field": "has_pet", "operator": "EQ", "value": false, "mode": "REQUIRED"}
    ],
    "housing_conditions": {
      "currency": "KRW",
      "cost_basis": "PERSON_MONTH",
      "deposit_max": null,
      "monthly_rent_max": 600000,
      "region_codes": [],
      "move_in_range": null,
      "expected_household_size": null
    },
    "rule_acceptance": [{"rule_code": "INDOOR_SMOKING_PROHIBITED", "accepted": true}],
    "dataset": {"kind": "SYNTHETIC", "id": "rag-fixture-v1"}
  }
}
```

`cost_basis=PERSON_MONTH`는 월세가 1인·월 기준이라는 뜻이다.
보증금은 별도의 1인 KRW 일시금이다. 예산 필드는 활성화한 경우 필수 조건으로 처리한다.
원본의 정확한 생년월일은 내부 적격 검사만을 위한 제한 데이터이다. 외부 모델로 보내지 않는다.
`recommendation_allowed`는 이 회원의 자료를 추천에 사용할 수 있다는 뜻이다. 철회하면 요청자·작성자 자료 모두 사용을 중지한다.

### 3.2 나머지 전체 상태의 필드

| payload | 필드와 의미 |
|---------|-------------|
| 모집글 | `post_id`, `owner_id`, `visibility`, `deleted`, `title`, `detail_description`, `additional_description`, `status`, `recruit_target_count`, `remaining_slots`, `expected_household_size`, `has_room`, `cost`, `location`, `move_in_range`, `admission_conditions`, `house_rules`, `created_at`, `dataset` |
| 비용 | `currency=KRW`, `basis=PERSON`, `deposit_min/max`, `monthly_rent_min/max`. 0 이상. min ≤ max. 범위의 양 끝 포함 |
| 위치 | 내부용 `latitude`, `longitude`, 공개용 `region_code`, `region_label`. 상세 주소는 전달 대상에서 제외 |
| 모집 조건 | `subject=APPLICANT`인 조건 배열. 나이·성별·본인 습관에 허용한 연산만 사용 |
| 생활 규칙 | `rule_code`, 구조화 값, `mode`, 공개 설명. 회원의 명시한 수용과 비교 |
| 지원 | `application_id`, `member_id`, `post_id`, `state`, `submitted_at`, `dataset`. `state`는 MSA의 4개 지원 상태. 제출 날짜와 사건 발생 시각을 구분 |
| 북마크 | `bookmark_id`, `member_id`, `post_id`, `active`, `created_at`, `dataset`. 해제는 DELETE. 해제를 부정 선호로 해석하지 않음 |

생략과 삭제는 다르다. 프로필만 삭제하면 회원 UPSERT에서 `public_profile=null`을 보낸다.
회원 전체 삭제는 회원 DELETE를 보낸다.
모집글의 모든 현재 조건과 상태가 UPSERT에 있어야 한다. 제목만 보낸 이벤트는 거부한다.
허용 필드·연산·enum의 기계 검증 스키마는 첫 구현의 산출물이다. 이 표만으로 검증기를 구현했다고 보지 않는다.

### 3.3 삭제 예제

토픽은 `cobee.recruit.recommendation.v1`, key는 `post:301`이다.
삭제 이벤트에 원문을 다시 싣지 않는다. 아래는 Kafka의 null tombstone과 다른 애플리케이션 사건이다.

```json
{
  "event_id": "0c6683b9-042a-4b54-8ed1-a9ca6de83f30",
  "event_type": "RecruitRecommendationSnapshotChanged",
  "schema_version": 1,
  "producer": "recruit-service",
  "aggregate_type": "post",
  "aggregate_id": "301",
  "aggregate_version": 14,
  "operation": "DELETE",
  "occurred_at": "2026-10-27T03:10:00Z",
  "trace_id": "synthetic-delete-301-14",
  "payload": {"post_id": "301", "owner_id": "57", "deleted": true}
}
```

## 4. 소비 트랜잭션과 버전 규칙

추천 DB에는 `consumer_inbox`, aggregate별 `last_version`, 사본, 문서 재구성 작업, `embedding_job`, `quarantine_outbox`를 둔다.
버전 비교와 사본 쓰기는 행 잠금 또는 조건부 UPSERT로 원자적으로 처리한다.

1. 봉투, 토픽·key, enum, 자료형, 전체 상태 필수 키를 검사한다.
2. `(consumer_group, event_id)`가 있으면 본문 해시도 비교한다. 같은 본문은 기존 결과를 쓰고 다른 본문은 계약 오류로 격리한다.
3. 수신 버전 < 저장 버전이면 오래된 사건으로 기록하고 무시한다.
4. 버전이 같고 본문 해시도 같으면 중복이다. 같지만 해시가 다르면 계약 오류로 격리하고 해당 대상을 차단한다.
5. 더 큰 버전이면 전체 상태를 교체한다. 버전이 건너뛰면 계수하고 원본 대조 작업을 예약한다.
6. 문서에 영향을 주면 작업을 등록하고 이전 문서를 검색에서 제외한다. 삭제는 즉시 비활성화한다.
7. 사본·inbox·작업·격리 기록을 같은 DB 트랜잭션으로 커밋한다. 그 뒤 offset을 커밋한다.

partition 안의 DB 반영은 offset 순서로 처리한다.
나중에 병렬 처리하더라도 가장 낮은 미완료 offset 직전까지만 커밋한다.
뒤 사건의 완료 때문에 앞의 미처리 사건을 건너뛰지 않는다.

전체 상태라서 8보다 10이 먼저 와도 10을 적용할 수 있다.
현재 상태를 만드는 흐름이며, 건너뛴 모든 행동 이력을 복원하는 흐름은 아니다.
후속 분석에 모든 행동 사건이 필요하면 별도 활동 로그 계약을 둔다.

회원보다 글이 먼저 올 수 있다. 원본 ID를 보존하고 작성자 연결은 미완료로 둔다.
다른 서비스 키에 강한 외래키를 걸어 사건을 버리지 않는다.
현재 작성자 공개 사실을 확인하기 전에는 해당 글을 추천하지 않는다.
작성자 삭제 표시는 뒤늦게 도착한 모집글로 지울 수 없다.

## 5. 실패·중복·순서 역전

| 상황 | 처리 |
|------|------|
| 원본 저장 성공, Kafka 장애 | 원본·Outbox는 유지. 발행을 재시도. 원본 쓰기와 Kafka 성공을 함께 기다리지 않음 |
| Kafka 전송 성공 직후 발행 프로세스 종료 | 같은 event ID를 다시 보낼 수 있음. 소비자가 중복 제거 |
| 추천 DB 커밋 전 종료 | offset 미커밋. 다시 받아 전체 트랜잭션 재시도 |
| 추천 DB 커밋 뒤 offset 커밋 전 종료 | 재수신. inbox와 버전으로 중복 효과 방지 |
| DB 연결 실패 | offset 미커밋. 짧은 재시도 뒤 해당 partition을 일시 중지. poll·heartbeat는 유지 |
| 잘못된 값·지원하지 않는 주 버전 | 식별 가능한 대상은 즉시 차단. DB에 격리 사건과 DLT 발행 작업을 저장한 뒤 offset 진행 |
| aggregate 자체를 식별할 수 없는 손상 | partition을 중지하고 운영 확인. 대상을 모르는 채 오래된 조건을 정상 노출하지 않음 |
| DLT 발행도 실패 | `quarantine_outbox`에 보존. DLT 성공 여부 때문에 원본 사건을 잃지 않음 |
| 임베딩 API 429·5xx·timeout | DB 작업 큐에서 지연 재시도. Kafka 소비와 HTTP 목록 처리는 계속 진행 |
| 새 버전보다 이전 임베딩 작업이 늦게 완료 | 원본 버전·문서 해시가 다르면 결과 폐기. 최신 벡터를 덮지 않음 |
| 원본에서 삭제·비공개 전환 | 사본·문서·설명 캐시를 차단. 대기 작업은 완료해도 활성화 불가 |
| 오래된 사건이 삭제 뒤 도착 | 삭제 버전 이하이면 무시. 같은 ID의 버전은 다시 시작하지 않음 |

API 작업 재시도 초기값은 1초, 5초, 30초, 5분, 30분에 무작위 지연을 더한 5회이다.
429는 가능한 경우 공급자가 준 대기 시간을 따른다. 영구 오류는 바로 실패 처리한다.
한도를 넘으면 작업을 실패 상태로 남기고 확인할 수 있게 한다. 무한 재시도하지 않는다.
DB 장애는 작업 API 실패와 다른 재시도 한도를 둔다. 장애 동안 offset을 앞당기지 않는다.

생산자 설정의 기본안은 `enable.idempotence=true`, `acks=all`, 재시도 활성이다.
`max.in.flight.requests.per.connection`은 idempotence와 호환되는 5 이하로 둔다.
이는 생산자 재시도 보호이다. Outbox 재발행과 DB 중복 처리를 대신하지 않는다.
근거: [Kafka 생산자 설정](https://kafka.apache.org/41/configuration/producer-configs/).
실제 배포에서는 복제 수·최소 동기 복제 수도 함께 정한다. 한 브로커 실험은 고가용성 검증이 아니다.

## 6. 재처리와 보존

DLT에는 원래 토픽·partition·offset·event ID, 실패 코드, 시도 수, 발생 시각을 남긴다.
원문 payload가 필요하면 접근을 제한한다. 일반 로그에는 원문과 비밀 값을 넣지 않는다.
오류를 고친 뒤 같은 event ID로 복구한다. 정상 처리된 inbox만 중복으로 막는다.
격리 상태는 처리 완료와 구분한다. 복구 성공 뒤 차단을 푼다.
차단 원인의 버전도 보존한다. 더 낮은 버전은 차단을 풀 수 없다.
수정된 소비자로 같은 사건을 복구하거나, 차단 버전보다 큰 정상 전체 상태를 적용한 뒤 해제한다.
틀린 사실 자체를 고칠 때는 원본 서비스가 더 큰 버전의 새 사건을 발행한다.

전체 재구축은 새 `projection_generation`과 새 소비 그룹을 사용한다.

1. 원본 서비스에 Outbox와 버전 기록을 먼저 적용한다. 모든 쓰기·삭제 경로가 이를 사용하는지 검사한다.
2. 서비스별로 읽기 시작할 Kafka partition offset을 먼저 기록한다.
3. 각 원본 서비스가 버전이 포함된 일관된 현재 상태를 내보낸다. 직접 다른 DB를 읽지 않는다.
4. 새 사본에 전체 상태를 적재한다. 기록한 offset부터 사건을 재생한다. 버전으로 겹침을 제거한다.
5. 원본 ID·개수·해시·삭제 표시를 대조한다. 작성자 연결과 임베딩 준비 상태도 확인한다.
6. 정한 시점의 Outbox 배출과 partition별 수신 도달을 확인한다. 검증 뒤 조회 세대 포인터를 교체한다.

서로 다른 서비스 전체의 한 시점 스냅샷을 보장한다고 표현하지 않는다.
교체 전에는 모두 따라잡았는지 확인하고 조회 시 원본 재확인을 유지한다.
첫 합성 실험은 짧은 쓰기 정지로 초기 적재를 단순화할 수 있다.

첫 합성 실험의 Kafka 보존 기간은 7일을 가정한다.
그보다 오래된 재구축은 전체 내보내기와 새 사건을 합친다. Kafka만으로 영구 복구하지 않는다.
삭제 키와 마지막 버전은 재생·스냅샷·백업의 가장 긴 복구 가능 기간보다 오래 유지한다.
복원할 때도 최신 삭제·공개 철회 목록을 먼저 적용한다. 오래된 원문을 다시 활성화하지 않는다.
실제 회원 자료의 Kafka·Outbox·DLT·백업 보존과 파기는 설계안 D5에서 확정한다.

Replay는 지원·알림·학습을 다시 실행하지 않는다. 조회용 사본과 필요한 최신 임베딩 작업만 만든다.
임베딩 작업 키는 `(document_id, source_versions, text_hash, embedding_version)`이다.
같은 작업 키의 중복은 합친다. 원문·정제·모델 버전이 모두 같고 사용 권한도 유효하면 벡터를 재사용한다.
외부 API 성공 직후 DB 저장 전에 종료되면 같은 호출을 다시 할 수 있다.
결과의 중복 반영은 막지만 외부 API 비용까지 정확히 한 번이라고 보장하지 않는다.

## 7. 계약 검증의 완료 조건

- 원본 롤백 시 Outbox도 남지 않는다. 모든 생성·수정·삭제 경로를 포함한다.
- 프로필·선호만 바꿔도 회원 버전과 사건이 바뀐다.
- 같은 사건 10회, 10→8 역순, 삭제→늦은 수정에서 최종 상태가 같다.
- 북마크 추가→해제→재추가에서도 버전이 계속 증가한다.
- DB 커밋 전후 종료와 offset 재수신에서 중복 반영이 없다.
- 글·회원 도착 순서가 바뀌어도 완성 전에는 노출하지 않는다.
- 격리·DLT 장애 뒤 복구가 가능하다. 차단 해제는 성공한 최신 상태에만 적용한다.
- 전체 적재와 replay의 결과가 원본의 현재 상태와 같다.

이 항목은 앞으로 만들 테스트의 기준이다. 이번 문서 작업에서 실행한 결과가 아니다.
