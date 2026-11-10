"""
로깅 유틸리티 모듈
통일된 로깅 설정을 제공하여 모든 컴포넌트에서 일관된 로그 형식 사용
"""

import logging
import sys
from pathlib import Path
from logging.handlers import RotatingFileHandler
from datetime import datetime
from typing import Optional


class LoggerSetup:
    """로거 설정 및 관리 클래스"""

    _loggers = {}  # 로거 인스턴스 캐시

    @classmethod
    def get_logger(
        cls,
        name: str,
        log_level: str = "INFO",
        log_dir: str = "logs",
        log_file: Optional[str] = None,
    ) -> logging.Logger:
        """
        설정된 로거 인스턴스를 반환합니다.

        Args:
            name: 로거 이름 (보통 모듈명 __name__ 사용)
            log_level: 로그 레벨 (DEBUG, INFO, WARNING, ERROR, CRITICAL)
            log_dir: 로그 파일이 저장될 디렉토리
            log_file: 로그 파일명 (None이면 날짜 기반 자동 생성)

        Returns:
            설정된 Logger 인스턴스
        """
        # 이미 생성된 로거가 있으면 재사용
        if name in cls._loggers:
            return cls._loggers[name]

        # 새 로거 생성
        logger = logging.getLogger(name)
        logger.setLevel(getattr(logging, log_level.upper()))

        # 핸들러가 이미 있으면 중복 추가 방지
        if logger.handlers:
            return logger

        # 로그 형식 정의
        formatter = logging.Formatter(
            fmt="%(asctime)s - %(name)s - %(levelname)s - %(funcName)s:%(lineno)d - %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )

        # 콘솔 핸들러 설정 (개발 중 실시간 확인용)
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(logging.DEBUG)
        console_handler.setFormatter(formatter)
        logger.addHandler(console_handler)

        # 파일 핸들러 설정 (영구 기록용)
        log_path = Path(log_dir)
        log_path.mkdir(parents=True, exist_ok=True)

        if log_file is None:
            # 날짜별 로그 파일명 자동 생성
            log_file = f"app_{datetime.now().strftime('%Y%m%d')}.log"

        file_handler = RotatingFileHandler(
            filename=log_path / log_file,
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=5,  # 최대 5개 백업 파일 유지
            encoding="utf-8",
        )
        file_handler.setLevel(logging.INFO)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)

        # 캐시에 저장
        cls._loggers[name] = logger

        return logger

    @classmethod
    def set_level(cls, name: str, level: str) -> None:
        """
        특정 로거의 레벨을 동적으로 변경합니다.

        Args:
            name: 로거 이름
            level: 새로운 로그 레벨
        """
        if name in cls._loggers:
            cls._loggers[name].setLevel(getattr(logging, level.upper()))


# 편의성을 위한 함수
def get_logger(name: str, log_level: str = "INFO") -> logging.Logger:
    """
    로거를 가져오는 간편 함수

    Usage:
        from utils.logger import get_logger
        logger = get_logger(__name__)
        logger.info("This is an info message")
    """
    return LoggerSetup.get_logger(name, log_level)


# 모듈 레벨 로거 (이 파일 자체의 로그용)
logger = get_logger(__name__)
