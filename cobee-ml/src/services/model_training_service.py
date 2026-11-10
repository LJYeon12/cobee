"""
Model Training Service
Handles the automated retraining of the matrix factorization model.
"""

import pandas as pd
from sqlalchemy import create_engine
from surprise import Dataset, Reader, SVD
import pickle
import mlflow
import asyncio
from src.utils.config_loader import config
from src.utils.logger import get_logger

logger = get_logger(__name__)


class ModelTrainingService:
    RATING_MAP = {
        "MATCHED": 5.0,
        "MATCHING": 4.0,
        "ON_WAIT": 3.0,
        "REJECTED": 1.0,
        "BOOKMARK": 4.0,
    }
    HYPERPARAMETERS = {
        "n_factors": 50,
        "n_epochs": 20,
        "lr_all": 0.005,
        "reg_all": 0.02,
        "random_state": 42,
    }

    def __init__(self):
        settings = config.settings
        connection_string = (
            f"postgresql://{settings.postgres_user}:{settings.postgres_password}@"
            f"{settings.postgres_host}:{settings.postgres_port}/{settings.postgres_db}"
        )
        self.engine = create_engine(connection_string)
        mlflow.set_experiment("matrix-factorization-retrain")

    def _build_interaction_matrix(self) -> pd.DataFrame:
        apply_query = "SELECT member_id as user_id, recruit_post_id as item_id, match_status, submitted_at as timestamp FROM apply_record"
        apply_df = pd.read_sql(apply_query, self.engine)
        apply_df["rating"] = apply_df["match_status"].map(self.RATING_MAP)

        bookmark_query = "SELECT member_id as user_id, recruit_post_id as item_id, created_at as timestamp FROM bookmark"
        bookmark_df = pd.read_sql(bookmark_query, self.engine)
        bookmark_df["rating"] = self.RATING_MAP["BOOKMARK"]

        interactions = pd.concat([apply_df, bookmark_df], ignore_index=True)
        interactions = interactions.sort_values(
            ["user_id", "item_id", "timestamp"]
        ).drop_duplicates(subset=["user_id", "item_id"], keep="last")

        return interactions[["user_id", "item_id", "rating"]]

    def _run_training_sync(self):
        logger.info("Starting model retraining pipeline...")
        try:
            interactions_df = self._build_interaction_matrix()
            if interactions_df.empty:
                logger.info("No interaction data available to train the model.")
                return

            reader = Reader(rating_scale=(1.0, 5.0))
            data = Dataset.load_from_df(interactions_df, reader)
            trainset = data.build_full_trainset()

            model = SVD(**self.HYPERPARAMETERS)
            model.fit(trainset)

            model_path = config.settings.model_path
            with open(model_path, "wb") as f:
                pickle.dump(model, f)
            logger.info(f"Successfully retrained and saved model to {model_path}")

            with mlflow.start_run():
                mlflow.log_params(self.HYPERPARAMETERS)
                mlflow.log_metric("interaction_records", len(interactions_df))
                mlflow.sklearn.log_model(model, "retrained_model")
            logger.info("Logged retraining metadata to MLflow.")

        except Exception as e:
            logger.error(f"Model retraining pipeline failed: {e}", exc_info=True)

    async def run_training(self):
        """Runs the synchronous training method in a separate thread."""
        logger.info("Scheduling model retraining in a background thread.")
        await asyncio.to_thread(self._run_training_sync)
