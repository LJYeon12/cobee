"""
Pydantic 데이터 모델 스키마
API 요청/응답 및 데이터 검증에 사용됩니다. (springboot DTO 역할)
ORM 모델(snake_case)과 API(camelCase) 양방향 호환
"""

from datetime import datetime, date
from typing import Optional, List, Dict, Any
from enum import Enum
from pydantic import BaseModel, Field, field_validator


# ========== Enum 정의 ==========


class Gender(str, Enum):
    """성별"""

    MAN = "MAN"
    FEMALE = "FEMALE"
    NONE = "NONE"  # 선호도에서 "상관없음"


class Lifestyle(str, Enum):
    """생활 패턴"""

    MORNING = "MORNING"
    EVENING = "EVENING"


class Personality(str, Enum):
    """성격 유형"""

    INTROVERT = "INTROVERT"
    EXTROVERT = "EXTROVERT"


class RecruitStatus(str, Enum):
    """모집 상태"""

    RECRUITING = "RECRUITING"
    ON_CONTACT = "ON_CONTACT"
    RECRUIT_OVER = "RECRUIT_OVER"


class MatchStatus(str, Enum):
    """매칭 상태"""

    ON_WAIT = "ON_WAIT"
    MATCHING = "MATCHING"
    REJECTED = "REJECTED"
    MATCHED = "MATCHED"


class MemberInformation(BaseModel):
    """
    회원 정보 (기본 정보 + 선호도 + 공개 프로필)
    """

    # 기본 정보
    memberId: int = Field(alias="member_id")
    gender: Gender
    birthDate: datetime = Field(alias="birth_date")

    # 선호도 (Preferred)
    preferredGender: Optional[Gender] = Field(None, alias="preferred_gender")
    preferredLifeStyle: Optional[Lifestyle] = Field(None, alias="preferred_life_style")
    preferredPersonality: Optional[Personality] = Field(
        None, alias="preferred_personality"
    )
    possibleSmoking: bool = Field(False, alias="possible_smoking")
    possibleSnoring: bool = Field(False, alias="possible_snoring")
    hasPetAllowed: bool = Field(False, alias="has_pet_allowed")
    cohabitantCount: Optional[int] = Field(None, alias="cohabitant_count")
    preferredAgeMin: Optional[int] = Field(
        None, ge=19, le=34, alias="preferred_age_min"
    )
    preferredAgeMax: Optional[int] = Field(
        None, ge=19, le=34, alias="preferred_age_max"
    )

    # 본인 특성 (my)
    myLifestyle: Optional[Lifestyle] = Field(None, alias="my_lifestyle")
    myPersonality: Optional[Personality] = Field(None, alias="my_personality")
    isSmoking: bool = Field(False, alias="is_smoking")
    isSnoring: bool = Field(False, alias="is_snoring")
    hasPet: bool = Field(False, alias="has_pet")

    # 타임스탬프
    createdAt: datetime = Field(default_factory=datetime.now, alias="created_at")
    updatedAt: Optional[datetime] = Field(None, alias="updated_at")

    class Config:
        from_attributes = True  # SQLAlchemy ORM 호환
        populate_by_name = True  # camelCase와 snake_case 모두 허용

    @field_validator("preferredAgeMin", "preferredAgeMax")
    @classmethod
    def validate_age_range(cls, v):
        """나이 범위 검증"""
        if v is not None and not (19 <= v <= 34):
            raise ValueError("나이는 19세에서 34세 사이여야 합니다.")
        return v

    def calculate_age(self) -> int:
        """만 나이 계산"""
        today = date.today()
        birth = (
            self.birthDate.date()
            if isinstance(self.birthDate, datetime)
            else self.birthDate
        )
        age = today.year - birth.year
        # 생일이 아직 안 지났으면 -1
        if (today.month, today.day) < (birth.month, birth.day):
            age -= 1
        return age


class RecruitPost(BaseModel):
    """모집 게시글"""

    recruitPostId: int = Field(alias="recruit_post_id")
    recruitCount: int = Field(alias="recruit_count")
    rentCostMin: Optional[int] = Field(None, alias="rent_cost_min")
    rentCostMax: Optional[int] = Field(None, alias="rent_cost_max")
    monthlyCostMin: Optional[int] = Field(None, alias="monthly_cost_min")
    monthlyCostMax: Optional[int] = Field(None, alias="monthly_cost_max")

    # 선호 조건
    preferredGender: Optional[Gender] = Field(None, alias="preferred_gender")
    preferredLifeStyle: Optional[Lifestyle] = Field(None, alias="preferred_life_style")
    preferredPersonality: Optional[Personality] = Field(
        None, alias="preferred_personality"
    )
    isSmoking: bool = Field(False, alias="is_smoking")
    isSnoring: bool = Field(False, alias="is_snoring")
    isPetAllowed: bool = Field(False, alias="is_pet_allowed")
    cohabitantCount: Optional[int] = Field(None, alias="cohabitant_count")
    preferredAgeMin: Optional[int] = Field(
        None, ge=19, le=34, alias="preferred_age_min"
    )
    preferredAgeMax: Optional[int] = Field(
        None, ge=19, le=34, alias="preferred_age_max"
    )

    # 방 정보
    hasRoom: bool = Field(False, alias="has_room")
    address: Optional[str] = None
    regionLatitude: Optional[float] = Field(None, alias="region_latitude")
    regionLongitude: Optional[float] = Field(None, alias="region_longitude")

    # 상태
    recruitStatus: RecruitStatus = Field(alias="recruit_status")

    # 외래키
    memberId: int = Field(alias="member_id")  # 작성자

    # 타임스탬프
    createdAt: datetime = Field(alias="created_at")
    updatedAt: Optional[datetime] = Field(None, alias="updated_at")

    class Config:
        from_attributes = True
        populate_by_name = True


class ApplyRecord(BaseModel):
    """지원 기록"""

    recordId: int = Field(alias="record_id")
    matchStatus: MatchStatus = Field(alias="match_status")
    submittedAt: datetime = Field(alias="submitted_at")

    # 외래키
    memberId: int = Field(alias="member_id")
    recruitPostId: int = Field(alias="recruit_post_id")

    # 타임스탬프
    createdAt: datetime = Field(alias="created_at")
    updatedAt: Optional[datetime] = Field(None, alias="updated_at")

    class Config:
        from_attributes = True
        populate_by_name = True


class Bookmark(BaseModel):
    """북마크 (관심 표시)"""

    bookmarkId: int = Field(alias="bookmark_id")

    # 외래키
    memberId: int = Field(alias="member_id")
    recruitPostId: int = Field(alias="recruit_post_id")

    # 타임스탬프
    createdAt: datetime = Field(alias="created_at")
    updatedAt: Optional[datetime] = Field(None, alias="updated_at")

    class Config:
        from_attributes = True
        populate_by_name = True


class Comment(BaseModel):
    """댓글"""

    commentId: int = Field(alias="comment_id")

    # 외래키
    memberId: int = Field(alias="member_id")
    recruitPostId: int = Field(alias="recruit_post_id")

    # 타임스탬프
    createdAt: datetime = Field(alias="created_at")
    updatedAt: Optional[datetime] = Field(None, alias="updated_at")

    class Config:
        from_attributes = True
        populate_by_name = True


# ========== API 요청/응답 스키마 ==========


class RecommendationRequest(BaseModel):
    """추천 요청"""

    user_id: int = Field(..., gt=0, description="사용자 ID")
    limit: int = Field(10, ge=1, le=100, description="추천 개수")
    include_explanations: bool = Field(True, description="추천 이유 포함 여부")


class RecommendationExplanation(BaseModel):
    """추천 이유 설명"""

    score: float = Field(..., ge=0.0, le=1.0, description="매칭 점수")
    percentage: str = Field(..., description="매칭 퍼센트 (예: 85%)")
    reasons: List[str] = Field(default_factory=list, description="일치하는 항목들")
    details: Optional[Dict[str, Any]] = Field(None, description="상세 매칭 정보")


class RecommendationItem(BaseModel):
    """추천 항목"""

    recruit_post: RecruitPost
    score: float = Field(..., ge=0.0, le=1.0, description="추천 점수")
    rank: int = Field(..., ge=1, description="추천 순위")
    explanation: Optional[RecommendationExplanation] = None


class RecommendationResponse(BaseModel):
    """추천 응답"""

    user_id: int
    recommendations: List[RecommendationItem]
    total_count: int = Field(..., description="추천된 총 개수")
    phase: str = Field(..., description="현재 Phase (P1, P2, P3)")
    model_version: Optional[str] = Field(None, description="모델 버전")
    generated_at: datetime = Field(
        default_factory=datetime.now, description="생성 시각"
    )


# ========== 데이터 수집용 스키마 ==========


class BatchExportRequest(BaseModel):
    """배치 데이터 수집 요청"""

    last_sync_timestamp: Optional[datetime] = Field(
        None, description="마지막 동기화 시각 (이후 변경된 데이터만 가져오기)"
    )
    include_tables: Optional[List[str]] = Field(
        None, description="수집할 테이블 목록 (None이면 전체)"
    )


class BatchExportResponse(BaseModel):
    """배치 데이터 수집 응답"""

    members: List[MemberInformation]
    recruit_posts: List[RecruitPost]
    apply_records: List[ApplyRecord]
    bookmarks: List[Bookmark]
    comments: List[Comment]
    export_timestamp: datetime = Field(default_factory=datetime.now)
    total_records: int


# ========== Phase 업데이트용 스키마 ==========


class PhaseUpdateRequest(BaseModel):
    """Phase 업데이트 요청"""

    interaction_count: int = Field(..., ge=0, description="현재 상호작용 총합")


class PhaseUpdateResponse(BaseModel):
    """Phase 업데이트 응답"""

    old_phase: str
    new_phase: str
    interaction_count: int
    weights: Dict[str, float]
    updated_at: datetime = Field(default_factory=datetime.now)
