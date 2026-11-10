# 검증 기준과 실행 기록

[문서 목차](README.md) · [개발 절차](development.md) · [유지보수](maintenance.md)

명령 예시는 Windows PowerShell 기준이다. 현재 구조의 기준 명령을 이 문서에 모은다.
의존성 준비와 외부 서비스 실행은 구분한다. 최초 의존성 준비에는 패키지 저장소 연결이 필요할 수 있다.

## 검증 단계

| 단계 | 확인하는 내용 | 외부 서비스 |
|------|----------------|-------------|
| 정적 검사 | 포맷, 구문·정의 오류, 문서 경로 | 필요 없음 |
| 단위 테스트 | 가짜 저장소·요청과 실제 계산·검증 로직 | DB·MSA·Redis·학습 서비스 필요 없음 |
| 통합 검증 | Spring 앱, Gateway, DB, 동기화, 학습 | 별도 테스트 환경과 설정 필요 |

기본 검증은 앞의 두 단계이다.
통합 검증 결과는 단위 테스트 통과와 별도로 기록한다.

## MSA

Java 21과 저장소의 Gradle Wrapper 8.14.3을 사용한다.
설정 기준은 [루트 빌드](../cobee-msa/build.gradle)와 [공통 테스트 설정](../cobee-msa/gradle/testing.gradle)이다.

```powershell
Set-Location C:/dev/cobee/cobee-msa
java -version
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle\user-home'
.\gradlew.bat test --no-daemon --console=plain
```

기본 `test`는 `integration` 태그를 제외한다.
기존 `@SpringBootTest` 8개는 `integrationTest`가 선택한다.
모든 모듈에 단위 테스트가 있는 것은 아니다. 공통·회원·모집·Gateway의 단위 테스트를 확인한다.

회원 동기화 테스트 하나만 실행할 때:

```powershell
.\gradlew.bat :member-service:test --tests 'org.example.memberservice.service.MemberSyncServiceTest'
```

통합 테스트는 환경을 준비한 뒤 별도로 실행한다.

```powershell
.\gradlew.bat :member-service:integrationTest
```

### 탈퇴 동시성과 종료 복구

탈퇴 검증은 실제 PostgreSQL·Redis와 전용 스키마를 사용한다.
[준비·종료 스크립트](../diagnostics/2026-10-31/withdrawal-tdd/README.md)로 이 작업의 로컬 실행 대상만 관리한다.
서로 다른 실행에서 같은 스키마를 초기화하므로 동시에 실행하지 않는다.

```powershell
..\diagnostics\2026-10-31\withdrawal-tdd\start-runtime.ps1
.\gradlew.bat :member-service:integrationTest --tests 'org.example.memberservice.withdrawal.*' :recruit-service:integrationTest --tests 'org.example.recruitservice.withdrawal.*' :apigateway-service:integrationTest --tests 'org.example.apigatewayservice.withdrawal.*' --offline --no-daemon --no-parallel --console=plain
..\diagnostics\2026-10-31\withdrawal-tdd\stop-runtime.ps1
```

`--tests`는 각 모듈 작업 바로 뒤에 둔다. 기존 앱 컨텍스트 8개와 탈퇴 통합 검증은 구분한다.
독립 JVM, 실제 JPA 저장, FK 정리, 작업 선점과 종료 복구, Redis 차단을 검사한다.
Feign과 Controller의 계약은 실제 코드로 검사하지만 원격 HTTP 전송은 대역이다.
외부 지도·스토리지·알림 서버를 실행한 결과로 보고하지 않는다.

- 단위 결과: 각 모듈의 `build/test-results/test/`, `build/reports/tests/test/`.
- 통합 결과: 각 모듈의 `build/test-results/integrationTest/`, `build/reports/tests/integrationTest/`.
- Wrapper나 의존성 캐시가 없으면 다운로드가 필요하다.
- macOS와 Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용한다.
- [현재 CI](../cobee-msa/.github/workflows/backend.yml)는 이미지 빌드·푸시용이다. 검증 단계의 대체 수단으로 보지 않는다.

## ML

설치와 환경 준비는 [ML 개발 안내](../cobee-ml/README.md#개발-검증)를 따른다.
단위 검증은 Python 3.11에서 확인했다.
[개발 의존성](../cobee-ml/requirements-dev.txt)은 기존 버전 제약을 사용한 최소 환경이다.

```powershell
Set-Location C:/dev/cobee/cobee-ml
$env:BLACK_CACHE_DIR = Join-Path (Get-Location) '.venv\black-cache'
.\.venv\Scripts\python.exe -m black --check --workers 1 src scripts tests
.\.venv\Scripts\python.exe -m flake8 src scripts tests
.\.venv\Scripts\python.exe -m pytest
```

명령은 하나씩 실행하고 각 종료 코드를 확인한다.

- [Black](../cobee-ml/pyproject.toml): 줄 길이 88, Python 3.10 문법 기준의 포맷 검사.
- [Flake8](../cobee-ml/.flake8): `E9,F63,F7,F82` 규칙. 구문·정의 오류 중심이다.
- pytest: `tests/unit/`를 선택하고 잘못된 설정 키·등록되지 않은 마커를 오류로 처리한다.
- [단위 fixture](../cobee-ml/tests/unit/conftest.py)는 소켓 연결과 DNS 조회를 차단한다.
- `.env`, DB, MSA 서버, 모델 파일은 단위 테스트에 필요하지 않다.
- macOS와 Linux에서는 `.venv/bin/python`을 사용한다.

## 변경별 검사 범위

| 변경 | 기본 검사 |
|------|-------------|
| MSA 서비스 | 해당 모듈의 `test`. 공통 계약 변경이면 전체 MSA `test` |
| MSA `common` | `:common:test :alarm-service:test :member-service:test :recruit-service:test` |
| ML 점수 정규화 | Black·Flake8, `pytest tests/unit/test_scoring.py` |
| ML Phase 선택 | Black·Flake8, `pytest tests/unit/test_phase.py` |
| ML 동기화 스키마 | Black·Flake8, `pytest tests/unit/test_sync_schemas.py` |
| ML 공통 설정·의존성 | Black·Flake8, ML 단위 테스트 전체 |
| HTTP 계약·DB·학습 | 관련 단위 검사와 별도 통합 검증. 단위 검사만으로 완료하지 않음 |
| 문서만 변경 | 링크·경로·앵커·명령 문법·예제 데이터 검사. 앱 시작은 기본 검사에 포함하지 않음 |

ML 부분 테스트 명령은 `.\.venv\Scripts\python.exe -m` 뒤에 붙여 실행한다.
빠른 단위 테스트와 통합 테스트를 나눈 이유는 환경 준비 실패와 업무 로직 실패를 구분하기 위해서이다.
계산 함수는 실제 호출 코드와 같은 함수를 검사한다. 앱 구동 테스트도 별도 작업으로 유지한다.

## 마지막 검증 기록

2026-10-31 탈퇴 TDD의 최신 실행 근거는 [수정 전후 비교 기록](../diagnostics/2026-10-31/withdrawal-tdd/README.md)과 각 모듈의 JUnit XML이다.
운영 배포와 전체 외부 서비스 연결 검증은 수행하지 않았다.

| 대상 | 실제 실행 결과 |
|---|---|
| 공통·회원·모집·Gateway 단위 테스트 | 26개 통과. 실패·오류·건너뜀 0 |
| 회원·모집·Gateway 탈퇴 통합 테스트 | 29개 통과. 실제 PostgreSQL 17.4·Redis 7.4.12 사용 |
| 같은 JPA 재현의 수정 전후 비교 | 한 JVM과 독립 JVM 두 개에서 각각 100회 실행. 정리 후 재저장 100건 → 0건 |
| alarm-service | 공통 변경의 컴파일 영향 확인. 단위 테스트 수집 0개, 통합 미실행 |

다음은 2026-10-27 P1 적용 시 실행한 이전 기록이다. 문서 작성만으로 다시 실행한 결과가 아니다.

| 대상 | 환경과 명령 | 결과 |
|------|-------------|------|
| MSA 전체 모듈 | Java 21.0.7, Gradle 8.14.3, `gradlew.bat test --no-daemon --console=plain` | 컴파일·단위 검증 성공 |
| MSA 공통 | `SyncKeyInterceptorTest` | 4개 통과, 실패·건너뜀 0 |
| MSA 회원 | `MemberSyncServiceTest` | 3개 통과, 실패·건너뜀 0 |
| ML 단위 | Python 3.11.15, pytest 8.4.2, `python -m pytest` | 21개 통과 |
| ML 포맷 | Black 25.9.0, `black --check --workers 1 src scripts tests` | 29개 파일 통과 |
| ML 오류 검사 | Flake8 7.3.0, `flake8 src scripts tests` | 통과 |

MSA 실행 근거는 공통·회원 모듈의 JUnit XML이다.
ML 검사 범위는 [단위 테스트 코드](../cobee-ml/tests/unit/)와 [검사 설정](../cobee-ml/pyproject.toml)을 확인한다.

## 결과 해석과 남은 한계

- 테스트 수집이 0개이면 테스트 통과로 보고하지 않는다.
- 캐시의 `UP-TO-DATE`와 실제 재실행을 구분한다.
- MSA 통합 테스트 8개, 동기화 HTTP 전체 흐름, 모델 학습은 아직 검증하지 않았다.
- ML 전체 런타임 의존성 설치도 최소 개발 환경 검증과 구분한다.
- 기존 Pydantic 클래스 설정 경고 6건, MSA의 Lombok·폐기 예정 API·Gradle 경고가 남아 있다.
- 동기화에서 정적으로 확인한 차이는 [계약 문서](contracts/sync.md#확인-필요-항목)에 있다.
