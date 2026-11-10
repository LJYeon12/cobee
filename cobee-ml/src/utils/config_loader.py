"""
설정 파일 로더 모듈
config.json과 .env 파일을 로드하고 핫리로드 기능을 제공합니다.
"""

import json
from pathlib import Path
from typing import Dict, Any, Optional
from datetime import datetime
from pydantic_settings import BaseSettings, SettingsConfigDict


class AppSettings(BaseSettings):
    """환경 변수 로드 클래스"""

    # MSA Backend
    msa_backend_url: str
    sync_secret_key: str

    # Database
    postgres_host: str
    postgres_port: int
    postgres_db: str
    postgres_user: str
    postgres_password: str

    # Model Path
    model_path: str = "models/svd_model.pkl"

    model_config = SettingsConfigDict(
        env_file=".env", env_file_encoding="utf-8", extra="ignore"
    )


class ConfigLoader:
    """설정 파일 로더 클래스 (싱글톤 패턴)"""

    _instance = None
    _config: Optional[Dict[str, Any]] = None
    _config_path: Optional[Path] = None
    _last_loaded: Optional[datetime] = None
    _settings: Optional[AppSettings] = None

    def __new__(cls):
        """싱글톤 인스턴스 생성"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        """초기화 (싱글톤이므로 한 번만 실행됨)"""
        if self._settings is None:
            self._settings = AppSettings()
            print("✓ 환경 변수 로드 완료")
            print(f"  MSA Backend URL: {self._settings.msa_backend_url}")

    def load_config(self, config_path: str = "config/config.json") -> Dict[str, Any]:
        """
        설정 파일을 로드합니다.

        Args:
            config_path: config.json 파일 경로

        Returns:
            설정 딕셔너리

        Raises:
            FileNotFoundError: 설정 파일이 없을 때
            json.JSONDecodeError: JSON 파싱 실패 시
        """
        self._config_path = Path(config_path)

        if not self._config_path.exists():
            print(f"✗ 설정 파일을 찾을 수 없습니다: {config_path}")
            raise FileNotFoundError(f"Config file not found: {config_path}")

        try:
            with open(self._config_path, "r", encoding="utf-8") as f:
                self._config = json.load(f)

            self._last_loaded = datetime.now()
            print(f"✓ 설정 파일 로드 완료: {config_path}")
            print(
                f"  현재 Phase: {self._config.get('phase', {}).get('current', 'Unknown')}"
            )

            return self._config

        except json.JSONDecodeError as e:
            print(f"✗ 설정 파일 JSON 파싱 실패: {e}")
            raise

    def reload_if_changed(self) -> bool:
        """
        설정 파일이 변경되었으면 다시 로드합니다.

        Returns:
            재로드 여부 (True: 재로드됨, False: 변경 없음)
        """
        if self._config_path is None or self._config is None:
            print("⚠ 설정이 아직 로드되지 않았습니다. load_config()를 먼저 호출하세요.")
            return False

        try:
            # 파일 수정 시간 확인
            file_mtime = datetime.fromtimestamp(self._config_path.stat().st_mtime)

            if file_mtime > self._last_loaded:
                print("✓ 설정 파일이 변경되어 재로드합니다.")
                self.load_config(str(self._config_path))
                return True

            return False

        except Exception as e:
            print(f"✗ 설정 재로드 중 오류 발생: {e}")
            return False

    def get(self, key: str, default: Any = None) -> Any:
        """
        설정 값을 가져옵니다. 점(.)으로 중첩된 키를 지원합니다.

        Args:
            key: 설정 키 (예: "phase.current", "weights.P1.rule_based")
            default: 키가 없을 때 반환할 기본값

        Returns:
            설정 값 또는 기본값

        Example:
            config.get("phase.current")  # "P1"
            config.get("weights.P1.rule_based")  # 1.0
        """
        if self._config is None:
            print("⚠ 설정이 로드되지 않았습니다. 기본값을 반환합니다.")
            return default

        # 중첩된 키를 처리
        keys = key.split(".")
        value = self._config

        for k in keys:
            if isinstance(value, dict) and k in value:
                value = value[k]
            else:
                return default

        return value

    def get_current_phase(self) -> str:
        """현재 Phase를 반환합니다."""
        return self.get("phase.current", "P1")

    def get_weights(self, phase: Optional[str] = None) -> Dict[str, float]:
        """
        특정 Phase의 가중치를 반환합니다.

        Args:
            phase: Phase 이름 (None이면 현재 Phase)

        Returns:
            가중치 딕셔너리 {"rule_based": 1.0, "matrix_factorization": 0.0}
        """
        if phase is None:
            phase = self.get_current_phase()

        weights = self.get(f"weights.{phase}", {})

        return {
            "rule_based": weights.get("rule_based", 1.0),
            "matrix_factorization": weights.get("matrix_factorization", 0.0),
        }

    def get_interaction_count(self) -> int:
        """현재 상호작용 총합을 반환합니다."""
        return self.get("phase.interaction_count", 0)

    def update_phase(self, new_phase: str, interaction_count: int) -> None:
        """
        Phase를 업데이트하고 파일에 저장합니다.

        Args:
            new_phase: 새로운 Phase ("P1", "P2", "P3")
            interaction_count: 현재 상호작용 총합
        """
        if self._config is None or self._config_path is None:
            print("✗ 설정이 로드되지 않아 업데이트할 수 없습니다.")
            return

        old_phase = self.get_current_phase()

        # 설정 업데이트
        self._config["phase"]["current"] = new_phase
        self._config["phase"]["interaction_count"] = interaction_count
        self._config["last_updated"] = datetime.now().isoformat()

        # 파일에 저장
        try:
            with open(self._config_path, "w", encoding="utf-8") as f:
                json.dump(self._config, f, indent=2, ensure_ascii=False)

            self._last_loaded = datetime.now()
            print(
                f"✓ Phase 업데이트 완료: {old_phase} -> {new_phase} (상호작용: {interaction_count})"
            )

        except Exception as e:
            print(f"✗ 설정 파일 저장 실패: {e}")
            raise

    @property
    def config(self) -> Dict[str, Any]:
        """전체 설정 딕셔너리를 반환합니다."""
        return self._config or {}

    @property
    def settings(self) -> AppSettings:
        """환경 변수 설정을 반환합니다."""
        if self._settings is None:
            self._settings = AppSettings()
        return self._settings


# 전역 설정 인스턴스 (싱글톤)
config = ConfigLoader()
