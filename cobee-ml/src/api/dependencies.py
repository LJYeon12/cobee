"""
FastAPI 의존성 주입
DB 세션, Config, 추천 엔진 등을 제공합니다.
"""

from typing import Generator
from sqlalchemy.orm import Session
from src.utils.database import get_db
from src.utils.config_loader import config
from src.recommender.knn.rule_based import RuleBasedKNNRecommender


def get_database() -> Generator[Session, None, None]:
    """
    데이터베이스 세션 의존성

    Usage:
        @app.get("/endpoint")
        def endpoint(db: Session = Depends(get_database)):
            ...
    """
    db = next(get_db())
    try:
        yield db
    finally:
        db.close()


def get_config():
    """
    Config 의존성

    Returns:
        ConfigLoader: 싱글톤 config 인스턴스
    """
    # 최신 설정 반영 (핫리로드)
    config.reload_if_changed()
    return config
