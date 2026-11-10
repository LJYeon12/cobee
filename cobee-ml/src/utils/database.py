"""
SQLAlchemy 데이터베이스 설정
Engine 생성, Session 관리, Dependency Injection
"""

import os
from typing import Generator
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.ext.declarative import declarative_base
from src.utils.config_loader import config

# 데이터베이스 URL 조립
settings = config.settings
DATABASE_URL = (
    f"postgresql://{settings.postgres_user}:{settings.postgres_password}@"
    f"{settings.postgres_host}:{settings.postgres_port}/{settings.postgres_db}"
)

# SQLAlchemy Engine 생성 (Connection Pool 자동 관리)
engine = create_engine(
    DATABASE_URL,
    pool_size=10,  # Connection Pool 크기
    max_overflow=20,  # Pool이 가득 찰 때 추가로 생성할 수 있는 연결 수
    pool_pre_ping=True,  # 연결 전 Health Check
    echo=False,  # SQL 로그 출력 (개발 시 True로 변경 가능)
)

# SessionLocal: Session을 생성하는 Factory
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base: 모든 ORM 모델의 부모 클래스
Base = declarative_base()


# Dependency Injection: FastAPI에서 DB 세션 사용
def get_db() -> Generator[Session, None, None]:
    """
    FastAPI Dependency로 사용될 DB 세션 제공

    Usage:
        @app.get("/members/{member_id}")
        def get_member(member_id: int, db: Session = Depends(get_db)):
            member = db.query(MemberInformationORM).filter_by(member_id=member_id).first()
            return member

    Yields:
        Session: SQLAlchemy Session 객체
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# Health Check 함수
def health_check() -> bool:
    """
    데이터베이스 연결 상태 확인

    Returns:
        bool: 연결 성공 시 True, 실패 시 False
    """
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
        return True
    except Exception as e:
        print(f"✗ Database health check 실패: {e}")
        return False


# 데이터베이스 초기화 함수 (테이블 생성)
def init_db():
    """
    데이터베이스 테이블 생성
    ORM 모델이 정의된 후 호출해야 함

    주의: 프로덕션에서는 Alembic 마이그레이션 사용 권장
    """
    Base.metadata.create_all(bind=engine)
    print("✓ 데이터베이스 테이블 생성 완료")


def close_all_connections():
    """
    모든 데이터베이스 연결 종료
    애플리케이션 종료 시 호출
    """
    try:
        engine.dispose()
        print("✓ 데이터베이스 연결 종료 완료")
    except Exception as e:
        print(f"✗ 데이터베이스 연결 종료 실패: {e}")


# 애플리케이션 시작 시 실행
if __name__ == "__main__":
    print(f"Database URL: {DATABASE_URL}")
    if health_check():
        print("✓ 데이터베이스 연결 성공")
    else:
        print("✗ 데이터베이스 연결 실패")
