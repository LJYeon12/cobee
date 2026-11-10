"""
모델 오프라인 평가

학습된 Matrix Factorization 모델을 평가하고
Precision@K, Recall@K, NDCG 등의 지표를 계산합니다.

실행 방법:
    python scripts/evaluate_model.py

필요 파일:
    - models/svd_model.pkl (train_model.py 실행 후 생성)
    - data/interactions.csv
"""

import os
import sys
import pandas as pd
import pickle
import numpy as np
from collections import defaultdict
from surprise import Dataset, Reader
from surprise.model_selection import train_test_split

# 프로젝트 루트를 Python 경로에 추가
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.utils.logger import get_logger
from src.utils.config_loader import ConfigLoader

# MLflow import
import mlflow

logger = get_logger(__name__)


class ModelEvaluator:
    """
    추천 모델 오프라인 평가

    평가 지표:
    1. Precision@K: 추천한 K개 중 실제로 관심 있는 비율
    2. Recall@K: 관심 있는 전체 중 추천한 비율
    3. NDCG@K: 추천 순서의 품질 평가
    4. Coverage: 전체 아이템 중 추천에 노출된 비율
    """

    def __init__(self, config: ConfigLoader):
        """
        Args:
            config: 설정 로더
        """
        self.config = config

        # MLflow 설정
        mlflow.set_tracking_uri("file:./mlruns")
        mlflow.set_experiment("matrix-factorization")

        logger.info("ModelEvaluator 초기화 완료")

    def load_model(self, model_path: str):
        """
        학습된 모델 로드

        Args:
            model_path: 모델 파일 경로

        Returns:
            학습된 SVD 모델
        """
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"모델 파일을 찾을 수 없습니다: {model_path}")

        with open(model_path, "rb") as f:
            model = pickle.load(f)

        logger.info(f"모델 로드 완료: {model_path}")
        return model

    def load_data(self, data_path: str = "data/interactions.csv"):
        """
        평가용 데이터 로드

        Args:
            data_path: CSV 파일 경로

        Returns:
            tuple: (Surprise Dataset, DataFrame)
        """
        df = pd.read_csv(data_path)

        reader = Reader(rating_scale=(1.0, 5.0))
        data = Dataset.load_from_df(df[["user_id", "item_id", "rating"]], reader)

        logger.info(f"평가 데이터 로드 완료: {len(df)}개")
        return data, df

    def get_top_k_recommendations(self, model, user_id, all_items, k=10):
        """
        특정 사용자에 대한 Top-K 추천 생성

        Args:
            model: 학습된 모델
            user_id: 사용자 ID
            all_items: 전체 아이템 ID 리스트
            k: 추천 개수

        Returns:
            list: Top-K 아이템 ID 리스트
        """
        # 모든 아이템에 대한 예측 평점 계산
        predictions = []
        for item_id in all_items:
            pred = model.predict(user_id, item_id)
            predictions.append((item_id, pred.est))

        # 예측 평점 기준 내림차순 정렬
        predictions.sort(key=lambda x: x[1], reverse=True)

        # Top-K 선택
        top_k = [item_id for item_id, _ in predictions[:k]]
        return top_k

    def precision_at_k(self, recommended, relevant, k):
        """
        Precision@K 계산

        추천한 K개 중 실제로 관심 있는(relevant) 비율

        Args:
            recommended: 추천한 아이템 리스트 (순서 있음)
            relevant: 실제 관심 있는 아이템 집합
            k: K 값

        Returns:
            float: Precision@K 값 (0~1)

        예시:
            recommended = [1, 2, 3, 4, 5]
            relevant = {2, 4, 7}
            precision@5 = 2/5 = 0.4 (추천 5개 중 2개가 relevant)
        """
        recommended_k = recommended[:k]
        num_relevant_and_recommended = len(set(recommended_k) & relevant)
        return num_relevant_and_recommended / k if k > 0 else 0.0

    def recall_at_k(self, recommended, relevant, k):
        """
        Recall@K 계산

        관심 있는 전체 중 추천한 비율

        Args:
            recommended: 추천한 아이템 리스트
            relevant: 실제 관심 있는 아이템 집합
            k: K 값

        Returns:
            float: Recall@K 값 (0~1)

        예시:
            recommended = [1, 2, 3, 4, 5]
            relevant = {2, 4, 7}
            recall@5 = 2/3 = 0.667 (관심 3개 중 2개를 추천)
        """
        recommended_k = recommended[:k]
        num_relevant_and_recommended = len(set(recommended_k) & relevant)
        num_relevant = len(relevant)
        return num_relevant_and_recommended / num_relevant if num_relevant > 0 else 0.0

    def ndcg_at_k(self, recommended, relevant, k):
        """
        NDCG@K (Normalized Discounted Cumulative Gain) 계산

        추천 순서의 품질을 평가. 상위에 relevant 아이템이 많을수록 높은 점수

        Args:
            recommended: 추천한 아이템 리스트 (순서 중요!)
            relevant: 실제 관심 있는 아이템 집합
            k: K 값

        Returns:
            float: NDCG@K 값 (0~1)

        공식:
            DCG@K = Σ (rel_i / log2(i+1))  # i는 위치 (1-indexed)
            IDCG@K = 이상적인 순서일 때의 DCG (모든 relevant가 상위에)
            NDCG@K = DCG@K / IDCG@K

        예시:
            recommended = [1, 2, 3, 4, 5]  # 순서: 1위, 2위, ...
            relevant = {2, 4, 7}

            DCG = 1/log2(2) + 1/log2(4) = 1.0 + 0.5 = 1.5
            IDCG = 1/log2(2) + 1/log2(3) = 1.0 + 0.63 = 1.63
            NDCG = 1.5 / 1.63 = 0.92
        """
        recommended_k = recommended[:k]

        # DCG 계산
        dcg = 0.0
        for i, item_id in enumerate(recommended_k, start=1):
            if item_id in relevant:
                dcg += 1.0 / np.log2(i + 1)

        # IDCG 계산 (이상적인 경우: 모든 relevant가 상위에)
        idcg = 0.0
        for i in range(1, min(len(relevant), k) + 1):
            idcg += 1.0 / np.log2(i + 1)

        return dcg / idcg if idcg > 0 else 0.0

    def coverage(self, all_recommendations, all_items):
        """
        Coverage 계산

        전체 아이템 중 추천에 노출된 비율
        롱테일 아이템이 추천되는지 확인

        Args:
            all_recommendations: 모든 사용자에게 추천한 아이템들의 집합
            all_items: 전체 아이템 집합

        Returns:
            float: Coverage 값 (0~1)

        예시:
            all_recommendations = {1, 2, 3, 5, 7}
            all_items = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
            coverage = 5/10 = 0.5 (전체 10개 중 5개가 추천됨)
        """
        num_recommended = len(all_recommendations)
        num_total = len(all_items)
        return num_recommended / num_total if num_total > 0 else 0.0

    def evaluate(self, model, data, k_values=[5, 10, 20]):
        """
        모델 전체 평가

        Args:
            model: 학습된 모델
            data: Surprise Dataset
            k_values: 평가할 K 값들

        Returns:
            dict: 평가 결과
        """
        logger.info("=" * 60)
        logger.info("모델 평가 시작")
        logger.info("=" * 60)

        # Train/Test 분할 (학습과 동일하게)
        trainset, testset = train_test_split(data, test_size=0.2, random_state=42)

        # 전체 아이템 ID 추출
        all_items = list(set([iid for (_, iid, _) in trainset.all_ratings()]))

        # 테스트셋을 user별로 그룹화 (relevant items)
        user_relevant_items = defaultdict(set)
        for uid, iid, rating in testset:
            # rating이 4.0 이상이면 relevant로 간주
            if rating >= 4.0:
                user_relevant_items[uid].add(iid)

        logger.info(f"평가 대상 사용자 수: {len(user_relevant_items)}")

        # 각 K 값에 대해 평가
        results = {}
        all_recommended_items = set()

        for k in k_values:
            precision_list = []
            recall_list = []
            ndcg_list = []

            for user_id, relevant_items in user_relevant_items.items():
                # Top-K 추천 생성
                recommended = self.get_top_k_recommendations(
                    model, user_id, all_items, k
                )

                # 추천된 아이템 기록 (Coverage 계산용)
                all_recommended_items.update(recommended)

                # 지표 계산
                precision = self.precision_at_k(recommended, relevant_items, k)
                recall = self.recall_at_k(recommended, relevant_items, k)
                ndcg = self.ndcg_at_k(recommended, relevant_items, k)

                precision_list.append(precision)
                recall_list.append(recall)
                ndcg_list.append(ndcg)

            # 평균 계산
            avg_precision = np.mean(precision_list)
            avg_recall = np.mean(recall_list)
            avg_ndcg = np.mean(ndcg_list)

            results[f"precision@{k}"] = avg_precision
            results[f"recall@{k}"] = avg_recall
            results[f"ndcg@{k}"] = avg_ndcg

            logger.info(f"K={k}:")
            logger.info(f"  - Precision@{k}: {avg_precision:.4f}")
            logger.info(f"  - Recall@{k}: {avg_recall:.4f}")
            logger.info(f"  - NDCG@{k}: {avg_ndcg:.4f}")

        # Coverage 계산
        cov = self.coverage(all_recommended_items, set(all_items))
        results["coverage"] = cov

        logger.info(
            f"Coverage: {cov:.4f} ({len(all_recommended_items)}/{len(all_items)} 아이템 추천됨)"
        )

        return results

    def log_to_mlflow(self, results):
        """
        평가 결과를 MLflow에 기록

        Args:
            results: 평가 결과 딕셔너리
        """
        # 가장 최근 run에 추가 기록
        with mlflow.start_run(
            run_name=f"Evaluation_{pd.Timestamp.now().strftime('%Y%m%d_%H%M%S')}"
        ):
            for metric_name, value in results.items():
                # @ 문자를 _at_로 변경 (MLflow 호환)
                safe_metric_name = metric_name.replace("@", "_at_")
                mlflow.log_metric(safe_metric_name, value)

            mlflow.set_tag("evaluation", "offline")

            logger.info("MLflow에 평가 결과 기록 완료")

    def run(self, data_path: str = "data/interactions.csv"):
        """
        전체 평가 파이프라인 실행

        Args:
            data_path: 데이터 파일 경로
        """
        try:
            # 1. 모델 로드
            model_path = self.config.settings.model_path
            model = self.load_model(model_path)

            # 2. 데이터 로드
            data, df = self.load_data(data_path)

            # 3. 평가 실행
            results = self.evaluate(model, data, k_values=[5, 10, 20])

            # 4. MLflow 기록
            self.log_to_mlflow(results)

            logger.info("=" * 60)
            logger.info("평가 완료")
            logger.info("=" * 60)

            return results

        except Exception as e:
            logger.error(f"평가 실패: {e}", exc_info=True)
            raise


def main():
    """메인 실행 함수"""
    try:
        # 설정 로드
        config = ConfigLoader()

        # 모델 평가
        evaluator = ModelEvaluator(config)
        evaluator.run()

        sys.exit(0)  # 성공

    except Exception as e:
        logger.error(f"평가 실패: {e}", exc_info=True)
        sys.exit(1)  # 실패


if __name__ == "__main__":
    main()
