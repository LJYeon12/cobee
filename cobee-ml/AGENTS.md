# cobee-ml 작업 규칙

상위 [공통 작업 규칙](../AGENTS.md)을 함께 따른다. 실행 준비와 명령의 상세 내용은 [README](README.md)를 기준으로 한다. 상대 경로와 명령은 `cobee-ml/` 루트 기준이다.

공통 [문서 목차](../docs/README.md), [구조 안내](../docs/architecture.md), [검증 기준](../docs/verification.md), [동기화 계약](../docs/contracts/sync.md)을 필요한 작업에서 함께 확인한다.

## 목적과 기술 스택

MSA의 회원·모집·상호작용 데이터를 받아 룸메이트 모집글을 추천한다.

- Python, FastAPI, Pydantic, SQLAlchemy
- Rule-Based KNN, scikit-surprise의 SVD
- PostgreSQL, MLflow, APScheduler
- 버전은 [requirements.txt](requirements.txt)에서 확인한다. Python의 세부 버전은 아직 고정하지 않았다.
- 프로젝트 가상환경 `.venv`를 사용한다. Graphify의 분석용 Python 환경과 구분한다.

## 구조와 변경 위치

| 경로 | 역할 |
|------|------|
| `src/api/` | FastAPI 진입점, 라우터, 의존성 주입 |
| `src/clients/` | MSA HTTP 호출 |
| `src/services/` | 데이터 동기화와 모델 학습 |
| `src/recommender/` | 추천 점수와 알고리즘 |
| `src/models/` | ORM, 요청·응답 모델, 동기화 스키마 |
| `src/utils/` | 환경 설정, DB 세션, 로깅 |
| `scripts/` | Phase 갱신, 평가, 더미 데이터 생성 |
| `tests/unit/` | 외부 서비스 없는 단위 테스트와 네트워크 차단 |
| `requirements-dev.txt` | 검사에 필요한 최소 의존성 |
| `config/config.json` | Phase 임계값과 추천 가중치 |
| `database/init.sql` | 로컬 DB 초기 테이블 |
| `data/`, `models/`, `logs/` | 실행 중 만드는 데이터·모델·로그 |

- 생성 파일과 `src/models/`의 코드는 구분한다. Graphify 제외 범위는 상위 규칙을 따른다.
- Python 파일과 함수는 기존의 `snake_case`를 따른다. API·동기화 필드명은 양쪽 계약을 먼저 확인한다.
- 동기화 변경 시 Client → 스키마 → Service → ORM·SQL의 흐름을 확인한다. 대응하는 MSA Controller·DTO도 확인한다.
- 추천 정책을 바꾸면 설정 파일, 추천 코드, Phase 갱신 코드의 기준을 함께 확인한다.
- 핵심 계산은 [점수 정규화](src/recommender/scoring.py)와 [Phase 선택](src/recommender/phase.py)에 있다. 이 모듈은 환경 값, DB, 스케줄러를 초기화하지 않는다.

## 환경과 실행 시 주의점

- [.env.example](.env.example)은 로컬 예제이다. 실제 값은 `.env`나 실행 환경에 둔다.
- [AppSettings](src/utils/config_loader.py)의 필수 변수 7개와 선택 변수 `MODEL_PATH`를 기준으로 예제를 유지한다.
- `ConfigLoader`는 모듈을 읽을 때 설정을 생성한다. DB 모듈도 전역 Engine을 만든다. import 검사도 환경 값과 로깅의 영향을 받을 수 있다.
- API 서버는 시작 시 Phase 갱신과 MSA 동기화 예약을 등록한다. 현재 코드에는 테스트용 스케줄러 비활성화 설정이 없다.
- 동기화는 DB를 갱신하고 새 데이터가 있으면 학습 흐름을 호출한다. Phase 갱신은 `config/config.json`을 쓴다.
- 서버, 동기화, 학습, 더미 데이터 스크립트는 대상 환경과 요청 범위에 맞게 실행한다. 문서 점검의 기본 검증에 포함하지 않는다.
- Compose는 PostgreSQL과 Redis를 정의한다. 현재 `src/`에는 Redis 추천 캐시 연동 코드가 없다.

## 검증과 결과 보고

[개발 검증 안내](README.md#개발-검증)에 따라 `requirements-dev.txt`로 최소 환경을 준비한다. 버전은 기존 `requirements.txt`의 고정 값을 제약 조건으로 사용한다. 단위 테스트는 `.env`, DB, MSA 서버, 모델 파일 없이 실행한다.

Windows PowerShell에서 프로젝트 가상환경을 준비한 뒤 사용한다.

```powershell
.\.venv\Scripts\python.exe -m black --check --workers 1 src scripts tests
.\.venv\Scripts\python.exe -m flake8 src scripts tests
.\.venv\Scripts\python.exe -m pytest
```

- [pyproject.toml](pyproject.toml)은 Black과 pytest 설정이다. Black의 기준 문법은 Python 3.10이다. 단위 검증은 Python 3.11에서 확인한다.
- [.flake8](.flake8)은 구문·정의 오류 중심의 규칙이다. 전체 스타일 검사는 별도 기준이 필요하다.
- Black은 검사 모드로 실행한다. 자동 포맷은 요청 범위에 포함될 때 수행한다.
- pytest는 기본으로 `tests/unit/`를 선택한다. 잘못된 설정 키와 등록되지 않은 마커는 오류로 처리한다.
- 단위 테스트는 점수 정규화, Phase 경계값, 회원 동기화 스키마를 확인한다.
- 단위 테스트의 [conftest.py](tests/unit/conftest.py)는 소켓 연결과 DNS 조회를 차단한다.
- 테스트 수집이 0개이면 통과로 보고하지 않는다.
- DB·MSA·학습을 쓰는 검증은 별도 단계로 둔다. 단위 테스트 통과로 서버 구동이나 모델 학습이 검증됐다고 보고하지 않는다.
- 실행한 명령과 결과, 준비되지 않아 실행하지 못한 검증을 구분해 적는다.
