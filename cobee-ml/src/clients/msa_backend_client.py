"""
MSA Backend API Client
"""

from datetime import datetime
from typing import Optional, List, Type, TypeVar
import httpx
from src.utils.config_loader import config
from src.models.sync_schemas import (
    MemberSyncDto,
    RecruitPostSyncDto,
    ApplyRecordSyncDto,
    BookmarkSyncDto,
    CommentSyncDto,
)
from src.utils.logger import get_logger

logger = get_logger(__name__)

T = TypeVar("T")


class ApiClient:
    def __init__(self):
        self.base_url = config.settings.msa_backend_url
        self.secret_key = config.settings.sync_secret_key
        self.headers = {"X-Sync-Key": self.secret_key}

    async def _get(
        self,
        endpoint: str,
        dto_class: Type[T],
        last_sync_time: Optional[datetime] = None,
    ) -> List[T]:
        url = f"{self.base_url}{endpoint}"
        params = {}
        if last_sync_time:
            params["lastSyncTime"] = last_sync_time.isoformat()

        async with httpx.AsyncClient() as client:
            try:
                response = await client.get(url, headers=self.headers, params=params)
                response.raise_for_status()
                data = response.json()
                return [dto_class.model_validate(item) for item in data]
            except httpx.HTTPStatusError as e:
                logger.error(f"HTTP error occurred while fetching {url}: {e}")
                raise
            except Exception as e:
                logger.error(f"An error occurred while fetching {url}: {e}")
                raise

    async def get_members(
        self, last_sync_time: Optional[datetime] = None
    ) -> List[MemberSyncDto]:
        return await self._get("/sync/members", MemberSyncDto, last_sync_time)

    async def get_recruit_posts(
        self, last_sync_time: Optional[datetime] = None
    ) -> List[RecruitPostSyncDto]:
        return await self._get(
            "/sync/recruit-posts", RecruitPostSyncDto, last_sync_time
        )

    async def get_apply_records(
        self, last_sync_time: Optional[datetime] = None
    ) -> List[ApplyRecordSyncDto]:
        return await self._get(
            "/sync/apply-records", ApplyRecordSyncDto, last_sync_time
        )

    async def get_bookmarks(
        self, last_sync_time: Optional[datetime] = None
    ) -> List[BookmarkSyncDto]:
        return await self._get("/sync/bookmarks", BookmarkSyncDto, last_sync_time)

    async def get_comments(
        self, last_sync_time: Optional[datetime] = None
    ) -> List[CommentSyncDto]:
        return await self._get("/sync/comments", CommentSyncDto, last_sync_time)
