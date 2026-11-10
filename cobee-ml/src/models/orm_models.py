"""
SQLAlchemy ORM 모델 정의
데이터베이스 테이블과 Python 클래스를 매핑합니다.
"""

from sqlalchemy import (
    Column,
    Integer,
    String,
    Date,
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Text,
)
from sqlalchemy.orm import relationship
from datetime import datetime
from src.utils.database import Base


# ========================================
# 1. MemberInformation 테이블
# ========================================
class MemberInformationORM(Base):
    """회원 정보 (기본 정보 + 선호도 + 공개 프로필)"""

    __tablename__ = "member_information"

    # 기본 정보
    member_id = Column(Integer, primary_key=True)
    gender = Column(String(10), nullable=False)
    birth_date = Column(Date, nullable=False)

    # 선호도 (Preferred)
    preferred_gender = Column(String(10))
    preferred_life_style = Column(String(10))
    preferred_personality = Column(String(10))
    possible_smoking = Column(Boolean, default=False)
    possible_snoring = Column(Boolean, default=False)
    has_pet_allowed = Column(Boolean, default=False)
    cohabitant_count = Column(Integer)
    preferred_age_min = Column(Integer)
    preferred_age_max = Column(Integer)

    # 본인 특성 (my)
    my_lifestyle = Column(String(10))
    my_personality = Column(String(10))
    is_smoking = Column(Boolean, default=False)
    is_snoring = Column(Boolean, default=False)
    has_pet = Column(Boolean, default=False)

    # 타임스탬프 (자동 생성하지 않음, API에서 받아온 값 사용)
    created_at = Column(DateTime, default=datetime.now)
    updated_at = Column(DateTime)

    # 관계 (Relationships)
    recruit_posts = relationship("RecruitPostORM", back_populates="member")
    apply_records = relationship("ApplyRecordORM", back_populates="member")
    bookmarks = relationship("BookmarkORM", back_populates="member")
    comments = relationship("CommentORM", back_populates="member")

    def calculate_age(self) -> int:
        """만 나이 계산"""
        from datetime import date

        today = date.today()
        birth = self.birth_date
        age = today.year - birth.year
        # 생일이 아직 안 지났으면 -1
        if (today.month, today.day) < (birth.month, birth.day):
            age -= 1
        return age


# ========================================
# 2. RecruitPost 테이블
# ========================================
class RecruitPostORM(Base):
    """모집 게시글"""

    __tablename__ = "recruit_post"

    recruit_post_id = Column(Integer, primary_key=True)
    recruit_count = Column(Integer, nullable=False, default=1)

    # 비용 정보
    rent_cost_min = Column(Integer)
    rent_cost_max = Column(Integer)
    monthly_cost_min = Column(Integer)
    monthly_cost_max = Column(Integer)

    # 선호 조건
    preferred_gender = Column(String(10))
    preferred_life_style = Column(String(10))
    preferred_personality = Column(String(10))
    is_smoking = Column(Boolean, default=False)
    is_snoring = Column(Boolean, default=False)
    is_pet_allowed = Column(Boolean, default=False)
    cohabitant_count = Column(Integer)
    preferred_age_min = Column(Integer)
    preferred_age_max = Column(Integer)

    # 방 정보
    has_room = Column(Boolean, default=False)
    address = Column(Text)
    region_latitude = Column(Float)
    region_longitude = Column(Float)

    # 상태
    recruit_status = Column(String(20), nullable=False)

    # 외래키
    member_id = Column(
        Integer, ForeignKey("member_information.member_id"), nullable=False
    )

    # 타임스탬프
    created_at = Column(DateTime, nullable=False, default=datetime.now)
    updated_at = Column(DateTime)

    # 관계
    member = relationship("MemberInformationORM", back_populates="recruit_posts")
    apply_records = relationship("ApplyRecordORM", back_populates="recruit_post")
    bookmarks = relationship("BookmarkORM", back_populates="recruit_post")
    comments = relationship("CommentORM", back_populates="recruit_post")


# ========================================
# 3. ApplyRecord 테이블
# ========================================
class ApplyRecordORM(Base):
    """지원 기록"""

    __tablename__ = "apply_record"

    record_id = Column(Integer, primary_key=True)
    match_status = Column(String(20), nullable=False)
    submitted_at = Column(DateTime, nullable=False)

    # 외래키
    member_id = Column(
        Integer, ForeignKey("member_information.member_id"), nullable=False
    )
    recruit_post_id = Column(
        Integer, ForeignKey("recruit_post.recruit_post_id"), nullable=False
    )

    # 타임스탬프
    created_at = Column(DateTime, nullable=False, default=datetime.now)
    updated_at = Column(DateTime)

    # 관계
    member = relationship("MemberInformationORM", back_populates="apply_records")
    recruit_post = relationship("RecruitPostORM", back_populates="apply_records")


# ========================================
# 4. Bookmark 테이블
# ========================================
class BookmarkORM(Base):
    """북마크 (관심 표시)"""

    __tablename__ = "bookmark"

    bookmark_id = Column(Integer, primary_key=True)

    # 외래키
    member_id = Column(
        Integer, ForeignKey("member_information.member_id"), nullable=False
    )
    recruit_post_id = Column(
        Integer, ForeignKey("recruit_post.recruit_post_id"), nullable=False
    )

    # 타임스탬프
    created_at = Column(DateTime, nullable=False, default=datetime.now)
    updated_at = Column(DateTime)

    # 관계
    member = relationship("MemberInformationORM", back_populates="bookmarks")
    recruit_post = relationship("RecruitPostORM", back_populates="bookmarks")


# ========================================
# 5. Comment 테이블
# ========================================
class CommentORM(Base):
    """댓글"""

    __tablename__ = "comment"

    comment_id = Column(Integer, primary_key=True)

    # 외래키
    member_id = Column(
        Integer, ForeignKey("member_information.member_id"), nullable=False
    )
    recruit_post_id = Column(
        Integer, ForeignKey("recruit_post.recruit_post_id"), nullable=False
    )

    # 타임스탬프
    created_at = Column(DateTime, nullable=False, default=datetime.now)
    updated_at = Column(DateTime)

    # 관계
    member = relationship("MemberInformationORM", back_populates="comments")
    recruit_post = relationship("RecruitPostORM", back_populates="comments")
