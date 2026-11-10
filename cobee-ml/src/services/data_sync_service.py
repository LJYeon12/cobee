"""
Data Synchronization Service

Handles fetching new data from the MSA backend, storing it in the local database,
and triggering the model retraining pipeline.
"""

import json
import os
import asyncio
from datetime import datetime, timezone
from sqlalchemy.orm import Session

from src.clients.msa_backend_client import ApiClient
from src.models.sync_schemas import SyncData
from src.services.model_training_service import ModelTrainingService
from src.utils.database import get_db
from src.utils.logger import get_logger
from src.models.orm_models import (
    MemberInformationORM as Member,
    RecruitPostORM as RecruitPost,
    ApplyRecordORM as ApplyRecord,
    BookmarkORM as Bookmark,
)

logger = get_logger(__name__)

SYNC_STATUS_FILE = "data/sync_status.json"


class DataSyncService:
    def __init__(self, api_client: ApiClient):
        self.api_client = api_client
        self.model_training_service = ModelTrainingService()

    def _get_last_sync_time(self) -> str | None:
        """Reads the last successful sync time from the status file."""
        if not os.path.exists(SYNC_STATUS_FILE):
            return None
        try:
            with open(SYNC_STATUS_FILE, "r") as f:
                data = json.load(f)
                return data.get("lastSyncTime")
        except (IOError, json.JSONDecodeError) as e:
            logger.warning(f"Could not read sync status file: {e}")
            return None

    def _save_sync_time(self, sync_time: datetime):
        """Saves the successful sync time to the status file."""
        try:
            os.makedirs(os.path.dirname(SYNC_STATUS_FILE), exist_ok=True)
            with open(SYNC_STATUS_FILE, "w") as f:
                json.dump({"lastSyncTime": sync_time.isoformat()}, f, indent=2)
            logger.info(f"Successfully saved new sync time: {sync_time.isoformat()}")
        except IOError as e:
            logger.error(f"Failed to save sync status file: {e}", exc_info=True)

    async def _insert_data(self, db: Session, data: SyncData):
        """Inserts synchronized data into the database."""
        # Using bulk inserts/updates would be more efficient in a real scenario
        for member_data in data.members:
            db.merge(Member(**member_data.dict()))
        for post_data in data.recruit_posts:
            db.merge(RecruitPost(**post_data.dict()))
        for apply_data in data.apply_records:
            db.merge(ApplyRecord(**apply_data.dict()))
        for bookmark_data in data.bookmarks:
            db.merge(Bookmark(**bookmark_data.dict()))
        logger.info("Data insertion/update complete.")

    async def run_sync(self):
        """
        Main method to run the entire data synchronization and model retraining pipeline.
        """
        logger.info("Starting data synchronization pipeline...")
        last_sync_time = self._get_last_sync_time()
        logger.info(f"Last sync time: {last_sync_time or 'Never'}")

        db: Session = next(get_db())
        try:
            # 1. Fetch data from MSA backend in parallel
            logger.info("Fetching new data from MSA backend...")
            results = await asyncio.gather(
                self.api_client.get_members(last_sync_time),
                self.api_client.get_recruit_posts(last_sync_time),
                self.api_client.get_apply_records(last_sync_time),
                self.api_client.get_bookmarks(last_sync_time),
                self.api_client.get_comments(last_sync_time),
            )
            new_data = SyncData(
                members=results[0],
                recruit_posts=results[1],
                apply_records=results[2],
                bookmarks=results[3],
                comments=results[4],
            )

            if not any(
                [
                    new_data.members,
                    new_data.recruit_posts,
                    new_data.apply_records,
                    new_data.bookmarks,
                    new_data.comments,
                ]
            ):
                logger.info("No new data to synchronize.")
                db.close()
                return

            # 2. Insert data in a single transaction
            current_sync_time = datetime.now(timezone.utc)
            await self._insert_data(db, new_data)
            db.commit()
            logger.info("Database transaction committed successfully.")

            # 3. Trigger model retraining
            await self.model_training_service.run_training()

            # 4. Save the new sync time
            self._save_sync_time(current_sync_time)

            logger.info("Data synchronization pipeline completed successfully.")

        except Exception as e:
            logger.error(f"Data synchronization pipeline failed: {e}", exc_info=True)
            db.rollback()
            logger.warning("Database transaction was rolled back.")
            raise  # Re-raise the exception to the caller
        finally:
            db.close()
