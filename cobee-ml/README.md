# cobee-ml - 룸메이트 추천 서비스

MSA의 회원·모집·상호작용 데이터를 받아 모집글을 추천한다. Rule-Based KNN과 Matrix Factorization(SVD)을 Phase에 따라 혼합한다.

작업 전 [ML 작업 규칙](AGENTS.md)과 [공통 작업 규칙](../AGENTS.md)을 읽는다. 이 문서의 상대 경로는 `cobee-ml/` 기준이다. 명령 예시는 Windows PowerShell 기준이다.

공통 작업 절차는 [하니스 문서 목차](../docs/README.md), 검사 범위는 [검증 기준](../docs/verification.md), MSA 연동 조건은 [동기화 계약](../docs/contracts/sync.md)을 본다.

## 코드와 설정 위치

| 경로 | 역할 |
|------|------|
| [requirements.txt](requirements.txt) | Python 의존성과 직접 의존성 버전 |
| [requirements-dev.txt](requirements-dev.txt) | 단위 테스트·검사의 최소 의존성 |
| [pyproject.toml](pyproject.toml), [.flake8](.flake8) | pytest·Black·Flake8 설정 |
| [tests/unit/](tests/unit/) | 외부 서비스 없이 실행하는 핵심 단위 테스트 |
| [.env.example](.env.example) | 로컬 환경 변수 예제 |
| [config/config.json](config/config.json) | Phase 임계값, 가중치, 추천 설정 |
| [src/api/main.py](src/api/main.py) | FastAPI 앱과 예약 작업 |
| `src/api/routers/` | 추천·상태·설정 API |
| [src/clients/msa_backend_client.py](src/clients/msa_backend_client.py) | MSA 동기화 HTTP 호출 |
| `src/services/` | 데이터 동기화와 모델 학습 |
| `src/recommender/` | KNN, 점수 정규화, 하이브리드 추천 |
| `src/models/` | ORM, API 모델, 동기화 스키마 |
| `src/utils/` | 설정 로더, DB 세션, 로그 |
| `scripts/` | Phase 갱신, 모델 평가, 더미 데이터 생성 |
| [database/init.sql](database/init.sql) | 로컬 DB 초기 테이블 |
| [docker-compose.yml](docker-compose.yml) | PostgreSQL·Redis 컨테이너 |
| `data/`, `models/`, `logs/` | 실행 중 생성하는 데이터·모델·로그 |

## 준비 조건

- Python과 `pip`가 필요하다. 코드는 Python 3.10 이상의 문법을 사용한다.
- 전체 서비스의 Python 세부 버전은 아직 고정하지 않았다. 단위 테스트 환경은 Python 3.11에서 확인한다. 선택한 버전에 [requirements.txt](requirements.txt)의 패키지를 설치할 수 있는지 확인한다.
- 로컬 DB를 Compose로 실행하려면 Docker와 Compose가 필요하다.
- MSA 동기화에는 동작하는 Gateway·회원 서비스·모집 서비스와 일치하는 동기화 키가 필요하다.

## 로컬 환경 준비

먼저 `C:/dev/cobee/cobee-ml`로 이동한다.

```powershell
Set-Location C:/dev/cobee/cobee-ml
python --version
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

이후 Python 명령은 이 가상환경의 실행 파일을 사용한다. macOS와 Linux에서는 `.\.venv\Scripts\python.exe` 대신 `.venv/bin/python`을 사용한다.

### 환경 변수

기존 `.env`가 없을 때 예제를 복사한다.

```powershell
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
```

`.env`를 열어 로컬 환경에 맞게 값을 바꾼다. 예제의 키와 비밀번호는 교체한다. 실제 `.env`는 기존 `.gitignore`의 제외 대상이다.

| 변수 | 용도 |
|------|------|
| `MSA_BACKEND_URL` | Gateway의 기본 주소. 끝의 `/`는 생략한다. |
| `SYNC_SECRET_KEY` | `X-Sync-Key` 헤더 값. MSA와 같은 값을 사용한다. |
| `POSTGRES_HOST` | Python 프로세스가 접속할 ML DB 호스트 |
| `POSTGRES_PORT` | ML DB의 호스트 포트. Compose의 공개 포트에도 사용한다. |
| `POSTGRES_DB` | ML 데이터베이스 이름 |
| `POSTGRES_USER` | ML DB 사용자 |
| `POSTGRES_PASSWORD` | ML DB 비밀번호 |
| `MODEL_PATH` | 선택 값. 기본값은 `models/svd_model.pkl`이다. |

설정 기준은 [AppSettings](src/utils/config_loader.py)이다. 필수 값은 앞의 7개이다. 상대 경로인 `.env`, 설정 파일, 모델 경로를 올바르게 읽도록 패키지 루트에서 실행한다.

예제는 Python을 호스트에서 실행하는 구성이다. Python을 컨테이너에서 실행하면 호스트와 포트 설정을 그 네트워크에 맞게 바꾼다.

### PostgreSQL과 Redis

환경 값을 준비한 뒤 Compose 설정을 확인하고 로컬 컨테이너를 실행한다.

```powershell
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d postgres redis
```

- Python은 `POSTGRES_HOST:POSTGRES_PORT`로 접속한다. PostgreSQL 컨테이너 내부 포트는 `5432`이다.
- [database/init.sql](database/init.sql)은 PostgreSQL 컨테이너의 초기화 경로에 연결된다. 기존 DB를 사용할 때는 테이블 구조를 별도로 확인한다.
- Compose에는 PostgreSQL과 Redis만 정의되어 있다. API는 아래 Python 명령으로 실행한다.
- Redis의 호스트 포트는 현재 `6380`이다. 현재 추천 코드에는 Redis 캐시 연동이 구현되어 있지 않다.

## API 실행

환경 값과 DB를 준비한 뒤 단일 프로세스로 실행한다.

```powershell
.\.venv\Scripts\python.exe -m uvicorn src.api.main:app --host 127.0.0.1 --port 8000
```

- [API 문서](http://127.0.0.1:8000/docs)
- [추천 서비스 상태](http://127.0.0.1:8000/recommendations/health)
- [예약 작업 상태](http://127.0.0.1:8000/scheduler/status)

상태 조회에는 환경 설정과 DB 연결이 필요할 수 있다. 추천 요청은 `POST /recommendations/recommend`이다. 요청·응답 형식은 API 문서와 [Pydantic 모델](src/models/schemas.py)에서 확인한다.

### 서버 시작 시 등록하는 작업

[src/api/main.py](src/api/main.py)는 서버 시작 중 다음 작업의 등록과 스케줄러 시작을 시도한다.

| 작업 | 코드의 주기 | 변경 대상 |
|------|-------------|-----------|
| Phase 갱신 | Cron 기준 매일 04:50 | `config/config.json` |
| MSA 동기화 | 1시간 간격 | ML DB, 새 데이터가 있으면 모델·MLflow 기록, `data/sync_status.json` |

시간대는 실제 스케줄러와 실행 환경에서 확인한다. 현재 테스트용 스케줄러 비활성화 설정은 없다. 서버 시작도 예약 작업을 실행하는 절차에 포함된다.

## 수동 갱신

다음 작업은 DB 또는 파일을 쓴다. 실행 대상과 요청 범위를 확인한 뒤 사용한다.

| 진입점 | 동작 |
|--------|------|
| `POST /scheduler/run-sync-now` | MSA 동기화. 새 데이터가 있으면 학습 흐름 호출 |
| `POST /scheduler/run-now` | Phase 갱신 |
| `scripts/update_phase.py` | 서버 없이 Phase 갱신 |
| `scripts/evaluate_model.py` | 모델 평가와 MLflow 기록 |
| `scripts/generate_dummy_data.py` | 더미 데이터 생성 |

Phase를 수동으로 갱신할 때는 패키지 루트에서 실행한다.

```powershell
.\.venv\Scripts\python.exe -m scripts.update_phase
```

데이터 수집·모델 학습만 따로 실행하는 전용 CLI는 현재 없다. 실제 동기화·학습 흐름은 [DataSyncService](src/services/data_sync_service.py)와 [ModelTrainingService](src/services/model_training_service.py)에서 확인한다.

## Phase 기준

상호작용 수는 지원 기록과 북마크 수의 합이다. 임계값과 가중치는 [config/config.json](config/config.json)을 기준으로 한다.

| Phase | 상호작용 수 | Rule-Based | Matrix Factorization |
|-------|------------|-----------|----------------------|
| P1 | 0~99 | 100% | 0% |
| P2 | 100~999 | 60% | 40% |
| P3 | 1000 이상 | 20% | 80% |

정책을 바꾸면 설정, [Phase 갱신 코드](scripts/update_phase.py), [추천 코드](src/recommender/hybrid_recommender.py)를 함께 확인한다.

## 개발 검증

단위 테스트와 정적 검사에는 DB, MSA 서버, `.env`, 모델 파일이 필요하지 않다. [requirements-dev.txt](requirements-dev.txt)는 기존 버전 제약을 사용해 pytest·Black·Flake8·Pydantic만 설치한다. 전체 API 실행에 필요한 의존성은 `requirements.txt`로 준비한다.

### 최소 환경 준비

`cobee-ml/`에서 Python 3.11로 가상환경을 만든다.

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
```

이미 uv를 사용하는 환경에서는 다음 명령으로 준비할 수 있다.

```powershell
uv venv --python 3.11 .venv
uv pip install --python .venv\Scripts\python.exe -r requirements-dev.txt
```

### 검사 명령

```powershell
.\.venv\Scripts\python.exe -m black --check --workers 1 src scripts tests
.\.venv\Scripts\python.exe -m flake8 src scripts tests
.\.venv\Scripts\python.exe -m pytest
```

- [Black 설정](pyproject.toml)은 줄 길이 88과 Python 3.10 문법을 기준으로 한다. 검사 모드는 파일을 바꾸지 않는다.
- [Flake8 설정](.flake8)은 `E9,F63,F7,F82`를 검사한다. 구문·정의 오류 중심이며 전체 스타일 검사는 별도 기준이 필요하다.
- pytest는 `tests/unit/`를 선택한다. 잘못된 설정 키와 등록되지 않은 마커는 오류로 처리한다.
- 테스트 수집이 0개이면 통과로 보고하지 않는다.
- [네트워크 차단 fixture](tests/unit/conftest.py)는 단위 테스트의 소켓 연결과 DNS 조회를 막는다.

| 테스트 | 확인하는 동작 |
|--------|----------------|
| [점수 정규화](tests/unit/test_scoring.py) | 빈 입력, 단일 값, 동점, 음수 점수, 상대 점수, 입력 보존 |
| [Phase 선택](tests/unit/test_phase.py) | 기본·사용자 설정 임계값의 경계 |
| [동기화 스키마](tests/unit/test_sync_schemas.py) | 선택 프로필 누락, 필수 ID·시간, 잘못된 시간 형식 |

핵심 계산은 [scoring.py](src/recommender/scoring.py)와 [phase.py](src/recommender/phase.py)에 있다. 기존 추천기와 Phase 갱신 코드가 이 함수를 사용한다.

서버와 DB 모듈은 여전히 환경 설정과 Engine을 모듈 로드 시 생성한다. 단위 테스트, 서버 구동, DB·MSA·학습 통합 검증 결과를 구분해 보고한다.

## 아직 준비되지 않은 항목

- 전체 서비스의 Python 세부 버전 고정과 전체 의존성 설치 검증
- DB·MSA·학습을 쓰는 통합 검증과 ML 검증 CI
- API 테스트용 설정·DB·스케줄러 분리
- 기존 Pydantic 클래스 기반 설정의 갱신
- 운영 배포와 복구 절차
