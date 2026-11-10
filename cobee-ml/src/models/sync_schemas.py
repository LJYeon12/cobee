"""
Pydantic schemas for data synchronization with the MSA backend.
"""

from datetime import datetime, date
from typing import Optional, List
from pydantic import BaseModel, Field


class SyncBase(BaseModel):
    class Config:
        from_attributes = True
        populate_by_name = True


class MemberSyncDto(SyncBase):
    member_id: int
    gender: Optional[str] = None
    birth_date: Optional[str] = None
    preferred_gender: Optional[str] = None
    preferred_life_style: Optional[str] = None
    preferred_personality: Optional[str] = None
    possible_smoking: Optional[bool] = None
    possible_snoring: Optional[bool] = None
    has_pet_allowed: Optional[bool] = None
    cohabitant_count: Optional[int] = None
    preferred_age_min: Optional[int] = None
    preferred_age_max: Optional[int] = None
    my_lifestyle: Optional[str] = None
    my_personality: Optional[str] = None
    is_smoking: Optional[bool] = None
    is_snoring: Optional[bool] = None
    has_pet: Optional[bool] = None
    created_at: datetime
    updated_at: datetime


class RecruitPostSyncDto(SyncBase):
    recruit_post_id: int
    recruit_count: Optional[int] = None
    rent_cost_min: Optional[int] = None
    rent_cost_max: Optional[int] = None
    monthly_cost_min: Optional[int] = None
    monthly_cost_max: Optional[int] = None
    preferred_gender: Optional[str] = None
    preferred_life_style: Optional[str] = None
    preferred_personality: Optional[str] = None
    is_smoking: Optional[bool] = None
    is_snoring: Optional[bool] = None
    is_pet_allowed: Optional[bool] = None
    cohabitant_count: Optional[int] = None
    preferred_age_min: Optional[int] = None
    preferred_age_max: Optional[int] = None
    has_room: Optional[bool] = None
    address: Optional[str] = None
    region_latitude: Optional[float] = None
    region_longitude: Optional[float] = None
    recruit_status: Optional[str] = None
    member_id: int
    created_at: datetime
    updated_at: datetime


class ApplyRecordSyncDto(SyncBase):
    record_id: int
    match_status: Optional[str] = None
    submitted_at: Optional[date] = None
    member_id: int
    recruit_post_id: int
    created_at: datetime
    updated_at: datetime


class BookmarkSyncDto(SyncBase):
    bookmark_id: int
    member_id: int
    recruit_post_id: int
    created_at: datetime
    updated_at: datetime


class CommentSyncDto(SyncBase):
    comment_id: int
    member_id: int
    recruit_post_id: int
    created_at: datetime
    updated_at: datetime


class SyncData(BaseModel):
    """Container for all data types fetched during a sync operation."""

    members: List[MemberSyncDto] = Field(default_factory=list)
    recruit_posts: List[RecruitPostSyncDto] = Field(default_factory=list)
    apply_records: List[ApplyRecordSyncDto] = Field(default_factory=list)
    bookmarks: List[BookmarkSyncDto] = Field(default_factory=list)
    comments: List[CommentSyncDto] = Field(default_factory=list)
