# cobee-msa 작업 규칙

상위 [공통 작업 규칙](../AGENTS.md)을 함께 따른다. 이 문서의 상대 경로와 명령은 `cobee-msa/` 루트 기준이다. 명령 예시는 Windows PowerShell 기준이다.

공통 [문서 목차](../docs/README.md), [구조 안내](../docs/architecture.md), [검증 기준](../docs/verification.md), [동기화 계약](../docs/contracts/sync.md)을 필요한 작업에서 함께 확인한다.

## 목적과 기술 스택

룸메이트 모집 서비스의 인증, 회원, 모집, 알림 기능을 제공한다.

- Java 21
- Spring Boot 3.5.5, Spring Cloud 2025.0.0
- Gradle Wrapper 8.14.3
- Spring Data JPA, PostgreSQL, Redis, OpenFeign

버전은 [루트 빌드 설정](build.gradle), 각 모듈의 `build.gradle`, [Wrapper 설정](gradle/wrapper/gradle-wrapper.properties)에서 확인한다. 로컬 Gradle 대신 저장소의 Wrapper를 사용한다.

## 구조와 탐색

[settings.gradle](settings.gradle)에 하위 모듈 8개가 등록되어 있다.

| 모듈 | 역할 |
|------|------|
| `member-service` | 인증, 회원, 프로필, 선호도, 회원 동기화 |
| `recruit-service` | 모집글, 지원, 북마크, 댓글, 모집 데이터 동기화 |
| `alarm-service` | 알림과 푸시 연동 |
| `chat-service` | 채팅 서비스 모듈 |
| `apigateway-service` | 요청 라우팅과 인증 필터 |
| `config-service` | 외부 설정 제공 |
| `eureka-server` | 서비스 등록과 탐색 |
| `common` | 공통 응답, 오류, 인터셉터, 외부 연동 도구 |

- 기본 흐름은 `controller → service → repository`이다. 데이터는 `domain`과 `dto`에서 확인한다.
- 서비스 사이의 HTTP 호출은 `client` 패키지에서 확인한다.
- 새 코드는 인접한 파일의 패키지 이름과 구조를 따른다. DTO 필드와 enum 값을 바꾸면 호출하는 쪽도 확인한다.
- `common`은 라이브러리이다. 이 모듈의 `bootJar`는 비활성화되어 있고 `jar`를 만든다.
- `common` 변경 시 `alarm-service`, `member-service`, `recruit-service`를 함께 확인한다. 의존 모듈이 늘었으면 이 목록을 갱신한다.

## 빌드와 검증 명령

현재 작업 위치는 `cobee-msa/`여야 한다. JDK를 확인하고 이 작업 공간의 검증 캐시를 사용한다.

```powershell
Get-Location
java -version
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle\user-home'
```

Wrapper 배포 파일이나 의존성이 캐시에 없으면 다운로드가 필요하다. 추가 설치에 대한 상위 규칙을 따른다.

[gradle/testing.gradle](gradle/testing.gradle)이 루트와 모든 모듈의 테스트 의존성을 설정한다. Spring Boot 테스트 starter와 JUnit Platform launcher는 각 빌드의 BOM 버전을 따른다.

| 목적 | 명령 |
|------|------|
| 회원 모듈 메인 소스 컴파일 | `.\gradlew.bat :member-service:classes` |
| 전체 모듈의 단위 테스트 | `.\gradlew.bat test` |
| 공통·회원 모듈 단위 테스트 | `.\gradlew.bat :common:test :member-service:test` |
| 단위 테스트 클래스 하나 선택 | `.\gradlew.bat :member-service:test --tests 'org.example.memberservice.service.MemberSyncServiceTest'` |
| 공통 라이브러리와 의존 모듈 테스트 | `.\gradlew.bat :common:test :alarm-service:test :member-service:test :recruit-service:test` |
| 회원 모듈 통합 테스트 | `.\gradlew.bat :member-service:integrationTest` |

macOS와 Linux에서는 `.\gradlew.bat` 대신 `./gradlew`를 사용한다.

### 단위 테스트와 통합 테스트

- 기본 `test`는 `integration` 태그를 제외한다. 새 단위 테스트는 Spring 앱을 시작하지 않고 실행한다.
- 기존 `@SpringBootTest` 8개에는 `@Tag("integration")`이 있다. `integrationTest`는 이 태그만 선택한다. DB·외부 설정을 준비한 뒤 실행한다.
- 핵심 단위 테스트는 [동기화 키 인증](common/src/test/java/org/example/common/interceptor/SyncKeyInterceptorTest.java)과 [회원 전체·증분 동기화](member-service/src/test/java/org/example/memberservice/service/MemberSyncServiceTest.java)를 검사한다.
- 새 단위 테스트에는 가짜 저장소와 요청·응답을 사용한다. DB, Config Server, Eureka, Redis를 연결하지 않는다.
- 테스트 결과는 각 모듈의 `build/reports/tests/test/`와 `build/test-results/test/`에서 확인한다.
- [현재 CI](.github/workflows/backend.yml)는 이미지 빌드·푸시용이다. 별도 검증 CI는 아직 없다.
- 통합 테스트의 성공 여부와 환경 격리는 별도 검증 대상이다. 단위 테스트 통과와 구분해 보고한다.

### 탈퇴 동시성과 복구 검증

탈퇴 테스트는 `withdrawal` 패키지에 있다. [비교 기록](../diagnostics/2026-10-31/withdrawal-tdd/README.md)에 환경과 결과를 보관한다.
해당 통합 테스트는 실제 PostgreSQL `127.0.0.1:65438`, Redis `127.0.0.1:6389`를 사용한다.
전용 DB·스키마의 테스트 데이터를 초기화하므로 운영 DB로 연결하지 않는다.
현재 준비 도구는 기존 PostgreSQL 명령과 Docker, 이미 준비한 `redis:7.4-alpine` 이미지를 사용한다. 자동 설치·다운로드는 하지 않는다.

```powershell
..\diagnostics\2026-10-31\withdrawal-tdd\start-runtime.ps1
.\gradlew.bat :member-service:integrationTest --tests 'org.example.memberservice.withdrawal.*' :recruit-service:integrationTest --tests 'org.example.recruitservice.withdrawal.*' :apigateway-service:integrationTest --tests 'org.example.apigatewayservice.withdrawal.*' --offline --no-daemon --no-parallel --console=plain
..\diagnostics\2026-10-31\withdrawal-tdd\stop-runtime.ps1
```

독립 JVM도 이 통합 테스트에서 실행한다. Windows 명령 길이 제한을 피하기 위해 classpath 파일과 Java 인수 파일을 사용한다.
서로 다른 실행에서 같은 전용 스키마를 쓰므로 탈퇴 통합 테스트를 동시에 실행하지 않는다.
`--tests`는 각 `integrationTest` 바로 뒤에 지정한다. 이를 생략하면 외부 설정이 필요한 기존 앱 컨텍스트 검사도 선택된다.

## 로컬 실행과 설정

회원 서비스의 실행 진입점은 다음과 같다. 실행 전에 [회원 서비스 설정](member-service/src/main/resources/application.yml)의 필수 값을 준비한다.

```powershell
.\gradlew.bat :member-service:bootRun
```

- DB, Redis, Config Server, Eureka와 외부 연동 값은 실행 환경에 맞게 준비한다.
- 환경 변수는 실행 프로세스에 전달한다. ML의 `.env`는 MSA 실행 설정을 대신하지 않는다.
- 동기화 키는 `X-Sync-Key`를 사용한다. Gateway·회원 서비스·ML의 `SYNC_SECRET_KEY`를 맞춘다. 모집 서비스의 추가 헤더와 인터셉터 경로 차이는 [계약의 확인 필요 항목](../docs/contracts/sync.md#확인-필요-항목)을 본다.
- 동기화 경로와 응답은 [Gateway 설정](apigateway-service/src/main/resources/application.yml), 각 동기화 Controller·DTO, [ML Client](../cobee-ml/src/clients/msa_backend_client.py), [ML 스키마](../cobee-ml/src/models/sync_schemas.py)를 함께 대조한다.
- 컨테이너 안의 서비스 주소와 호스트의 로컬 주소는 구분한다. 현재 설정을 확인해 연결한다.
- 탈퇴 적용은 [운영 절차](../docs/withdrawal.md)를 따른다. 회원·모집의 `ddl-auto`는 `validate`이다. DDL과 기존 회원 상태를 먼저 준비한다.
- `INTERNAL_API_KEY`는 회원·모집 서비스에 같은 값으로 설정한다. Gateway·회원 서비스도 같은 Redis 접근 설정을 사용한다.
- 새 탈퇴 API는 202 접수와 상태 조회를 사용한다. 모집 쓰기를 바꾸면 해당 회원과 관련 회원의 `MemberWriteGuard` 검사도 확인한다.
