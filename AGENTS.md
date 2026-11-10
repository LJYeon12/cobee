# 프로젝트 작업 규칙

- 답변은 ASD-STE100 80% 수준으로 작성한다. 짧은 문장과 쉬운 용어를 사용한다.
- 추가 도구 설치, 외부 서비스 연결, 자동 실행 설정은 사용자가 요청한 범위에서만 수행한다.
- Graphify 지도는 정적 분석 결과이다. 원본 코드가 최종 근거이다.
- 그래프 재생성과 갱신은 사용자가 요청했을 때 수동으로 수행한다. 코드 변경만으로 자동 실행하지 않는다.

## 프로젝트 개요

Cobee는 룸메이트 모집과 추천을 다루는 서비스이다. 이 작업 폴더에는 두 패키지가 있다.

| 경로 | 역할 | 작업 안내 |
|------|------|-----------|
| `cobee-msa/` | Java 21, Spring Boot, Gradle 기반 백엔드 | [MSA 작업 규칙](cobee-msa/AGENTS.md) |
| `cobee-ml/` | Python, FastAPI 기반 추천·동기화·학습 서비스 | [ML 작업 규칙](cobee-ml/AGENTS.md), [개발 안내](cobee-ml/README.md) |
| `docs/` | 공통 개발·구조·검증·계약 문서 | [하니스 문서 목차](docs/README.md) |
| `graphify-out/` | 두 패키지의 정적 분석 지도와 검토 근거 | 아래 Graphify 규칙 |

- 작업 전에 해당 패키지의 `AGENTS.md`를 읽는다. 명령은 그 문서에 적힌 작업 위치에서 실행한다.
- 실제 버전과 의존성은 각 패키지의 빌드·의존성 파일에서 확인한다.
- 현재 작업 폴더에는 Git 메타데이터가 없다. 브랜치, 변경 이력, 원격 저장소 정책은 확인한 경우에만 보고한다.

## 상세 문서 탐색

- 제품 기능과 정책은 [제품 요구사항 정의서](docs/PRD.md)를 본다.
- 작업 흐름은 [개발 절차](docs/development.md), 검사 선택과 명령은 [검증 기준](docs/verification.md)을 따른다.
- 코드 위치와 데이터 흐름은 [구조 안내](docs/architecture.md), MSA·ML 사이의 조건은 [동기화 계약](docs/contracts/sync.md)을 본다.
- 추천 개편 제안은 [RAG 추천 설계안](.local/plans/rag-recommendation.md), 도메인 용어는 [용어집](GLOSSARY.md)을 본다. 설계 제안과 현재 구현을 구분한다.
- 남은 제약과 문서 갱신 방법은 [유지보수](docs/maintenance.md)에 기록한다.

## 변경 범위와 검증

- MSA의 `common`을 바꾸면 이를 사용하는 `alarm-service`, `member-service`, `recruit-service`의 영향도 확인한다. 실제 의존 관계는 각 `build.gradle`에서 다시 확인한다.
- 동기화 API를 바꾸면 MSA의 Controller·DTO·인증 처리와 ML의 HTTP Client·동기화 스키마를 함께 확인한다.
- 소스, 설정, 실행 방법을 바꾸면 관련 작업 안내도 갱신한다.
- 문서와 환경 예제는 파일 경로, 명령의 대상, 설정 키를 원본 코드와 대조한다.
- 코드 변경은 해당 패키지에 적힌 검증 명령 중 필요한 범위를 선택한다. 테스트 기반이 없거나 실행 환경이 준비되지 않았으면 그 사실을 기록한다.
- 서버 시작, DB 초기화, 데이터 동기화, 모델 학습은 실행 환경과 요청 범위를 확인한 뒤 수행한다.
- 결과에는 변경 파일, 실행한 검증, 성공·실패·미실행 항목을 구분해 적는다. 구문 검사나 이미지 빌드만으로 테스트가 통과했다고 보고하지 않는다.

## graphify

- 프로젝트 스킬: `.codex/skills/graphify/SKILL.md`.
- 분석 루트: `C:/dev/cobee`. `cobee-msa`와 `cobee-ml`을 함께 분석한다.
- 산출물: `graphify-out/graph.json`, `graph.html`, `GRAPH_REPORT.md`.
- 코드 질문에서는 그래프가 있으면 먼저 `graphify query`로 범위를 좁힌다. 경로는 `graphify path`, 개별 심벌은 `graphify explain`을 사용한다.
- 같은 이름의 메서드가 여러 개면 전체 노드 ID로 경로를 조회한다. 검색 결과가 잘렸으면 범위를 좁히거나 예산을 늘린다.
- 그래프가 오래되었거나 근거가 부족하면 해당 원본 코드를 확인한다. 경로가 없다는 사실만으로 연결이 없다고 단정하지 않는다.
- `EXTRACTED`는 원문에서 추출한 사실이다. 실행 성공을 뜻하지 않는다. `INFERRED`는 추론한 관계이다. `AMBIGUOUS`와 `외부/미해결`은 정의를 확인하지 못한 참조이다.
- `.graphifyignore`의 제외 범위를 유지한다. `cobee-ml/data`, `models`, `logs`의 생성 파일을 제외하며 `src/data`, `src/models`의 코드는 포함한다.
- 이 분석은 배포 설정을 포함하기 위해 VCS ignore 적용을 끄고 명시적 `.graphifyignore`를 사용했다. 갱신 시 같은 범위를 유지한다.
- `graphify-out/reviewed-links.json`은 소스를 대조해 보강한 HTTP 계약과 데이터 관계이다. 런타임 검증 결과가 아니다. `source_hashes`가 바뀌면 관계를 다시 검토한다.
- 수동 갱신 시 Graphify 스킬 절차를 따른다. 보강 결과 재생성은 `build_graph.py prepare`, `ast`, 문서 의미 분석, `review_project_links.py`, `assemble`, `graphify export html`, `write_project_report.py` 순서이다. 스크립트는 `graphify-out/`에 있다. `prepare`가 문서 캐시 누락을 알리면 새 의미 분석 없이 이전 조각을 재사용하지 않는다.
- 초기 소스 해시는 `graphify-out/source-hashes-before.json`에 기록했다. 분석 작업에서 원본 소스와 설정을 수정하지 않는다.
- Git 커밋 훅, 파일 감시, 클라우드 연결은 설정하지 않았다. 등록 명령이 생성한 `.codex/hooks.json`의 Graphify 항목은 의도적인 no-op이다.
