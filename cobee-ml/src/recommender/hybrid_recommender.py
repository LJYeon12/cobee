"""
하이브리드 추천 엔진

Phase에 따라 Rule-Based와 Matrix Factorization을 혼합하여 추천합니다.

Phase 전략:
- P1: Rule-Based 100%
- P2: Rule 60% + MF 40%
- P3: Rule 20% + MF 80%
"""

import pickle
import os
from typing import List, Dict, Set
from sqlalchemy.orm import Session

from src.models.orm_models import MemberInformationORM, RecruitPostORM
from src.models.schemas import RecommendationItem, RecommendationExplanation
from src.recommender.knn.rule_based import RuleBasedKNNRecommender
from src.recommender.scoring import normalize_scores
from src.utils.config_loader import ConfigLoader
from src.utils.logger import get_logger

logger = get_logger(__name__)


class HybridRecommender:
    """
    하이브리드 추천 시스템

    Rule-Based와 Matrix Factorization을 Phase에 따라 혼합
    """

    def __init__(self, db: Session, config: ConfigLoader):
        """
        Args:
            db: SQLAlchemy 세션
            config: 설정 로더
        """
        self.db = db
        self.config = config

        # Rule-Based 추천기 초기화
        self.rule_recommender = RuleBasedKNNRecommender(db, config)

        # MF 모델 로드 (P2, P3에서 사용)
        self.mf_model = None
        self.mf_model_path = config.settings.model_path

        logger.info("HybridRecommender 초기화 완료")

    def load_mf_model(self):
        """
        Matrix Factorization 모델 로드

        Returns:
            bool: 로드 성공 여부
        """
        if not os.path.exists(self.mf_model_path):
            logger.warning(f"MF 모델 파일을 찾을 수 없습니다: {self.mf_model_path}")
            return False

        try:
            with open(self.mf_model_path, "rb") as f:
                self.mf_model = pickle.load(f)
            logger.info("MF 모델 로드 완료")
            return True
        except Exception as e:
            logger.error(f"MF 모델 로드 실패: {e}")
            return False

    def get_mf_predictions(self, user_id: int, post_ids: List[int]) -> Dict[int, float]:
        """
        MF 모델로 특정 게시글들에 대한 예측 평점 계산

        Args:
            user_id: 사용자 ID
            post_ids: 게시글 ID 리스트

        Returns:
            Dict[int, float]: {post_id: predicted_rating}
        """
        if self.mf_model is None:
            logger.warning("MF 모델이 로드되지 않았습니다")
            return {}

        predictions = {}
        for post_id in post_ids:
            try:
                pred = self.mf_model.predict(user_id, post_id)
                predictions[post_id] = pred.est
            except Exception as e:
                logger.debug(f"MF 예측 실패 (user={user_id}, item={post_id}): {e}")
                predictions[post_id] = 3.0  # 기본값

        return predictions

    def normalize_scores(self, scores: Dict[int, float]) -> Dict[int, float]:
        """
        점수를 0~1 범위로 정규화

        Args:
            scores: {post_id: score}

        Returns:
            Dict[int, float]: 정규화된 점수
        """
        return normalize_scores(scores)

    def blend_recommendations(
        self,
        rule_items: List[RecommendationItem],
        mf_predictions: Dict[int, float],
        rule_weight: float,
        mf_weight: float,
        limit: int,
    ) -> List[RecommendationItem]:
        """
        Rule-Based와 MF 결과를 혼합

        Args:
            rule_items: Rule-Based 추천 결과
            mf_predictions: MF 예측 평점 {post_id: rating}
            rule_weight: Rule-Based 가중치
            mf_weight: MF 가중치
            limit: 최종 추천 개수

        Returns:
            List[RecommendationItem]: 혼합된 추천 결과
        """
        # Rule-Based 점수 추출 및 정규화
        rule_scores = {
            item.recruit_post.recruitPostId: item.score for item in rule_items
        }
        rule_scores_norm = self.normalize_scores(rule_scores)

        # MF 점수 정규화 (rating 1~5 → 0~1)
        mf_scores_norm = self.normalize_scores(mf_predictions)

        # 혼합 점수 계산
        blended_scores = {}
        all_post_ids = set(rule_scores.keys()) | set(mf_predictions.keys())

        for post_id in all_post_ids:
            rule_score = rule_scores_norm.get(post_id, 0.0)
            mf_score = mf_scores_norm.get(post_id, 0.0)

            # 가중 평균
            blended_score = (rule_score * rule_weight) + (mf_score * mf_weight)
            blended_scores[post_id] = blended_score

        # 점수 기준 정렬
        sorted_post_ids = sorted(
            blended_scores.keys(), key=lambda x: blended_scores[x], reverse=True
        )[:limit]

        # RecommendationItem으로 변환
        result = []
        rule_items_dict = {item.recruit_post.recruitPostId: item for item in rule_items}

        for rank, post_id in enumerate(sorted_post_ids, start=1):
            if post_id in rule_items_dict:
                # Rule-Based에 있던 아이템 재사용
                item = rule_items_dict[post_id]
                item.score = blended_scores[post_id]
                item.rank = rank

                # ✨ MF 정보를 explanation에 추가
                if item.explanation:
                    # 기존 details 유지하면서 MF 정보 추가
                    if item.explanation.details is None:
                        item.explanation.details = {}

                    # MF 원본 예측값
                    if post_id in mf_predictions:
                        item.explanation.details["mf_rating"] = round(
                            mf_predictions[post_id], 4
                        )

                    # 정규화된 점수들
                    if post_id in rule_scores_norm:
                        item.explanation.details["rule_score_normalized"] = round(
                            rule_scores_norm[post_id], 4
                        )

                    if post_id in mf_scores_norm:
                        item.explanation.details["mf_score_normalized"] = round(
                            mf_scores_norm[post_id], 4
                        )

                    # 혼합 가중치
                    item.explanation.details["blend_weights"] = {
                        "rule": rule_weight,
                        "mf": mf_weight,
                    }

                    # 최종 혼합 점수
                    item.explanation.details["final_blended_score"] = round(
                        blended_scores[post_id], 4
                    )

                result.append(item)
            else:
                # MF에서만 나온 아이템 (새로 생성)
                post = (
                    self.db.query(RecruitPostORM)
                    .filter_by(recruit_post_id=post_id)
                    .first()
                )

                if post:
                    from src.models.schemas import RecruitPost

                    post_schema = RecruitPost.from_orm(post)

                    result.append(
                        RecommendationItem(
                            recruit_post=post_schema,
                            score=blended_scores[post_id],
                            rank=rank,
                            explanation=RecommendationExplanation(
                                score=blended_scores[post_id],
                                percentage=f"{int(blended_scores[post_id] * 100)}%",
                                reasons=["MF 기반 추천"],
                                details={
                                    "mf_rating": round(
                                        mf_predictions.get(post_id, 0), 4
                                    ),
                                    "mf_score_normalized": round(
                                        mf_scores_norm.get(post_id, 0), 4
                                    ),
                                    "blend_weights": {
                                        "rule": rule_weight,
                                        "mf": mf_weight,
                                    },
                                    "final_blended_score": round(
                                        blended_scores[post_id], 4
                                    ),
                                },
                            ),
                        )
                    )

        return result

    def recommend(
        self, user_id: int, limit: int = 20, include_explanations: bool = True
    ) -> List[RecommendationItem]:
        """
        Phase에 따른 하이브리드 추천

        Args:
            user_id: 사용자 ID
            limit: 추천 개수
            include_explanations: 설명 포함 여부

        Returns:
            List[RecommendationItem]: 추천 결과
        """
        # 현재 Phase 확인
        current_phase = self.config.get_current_phase()
        weights = self.config.get_weights(current_phase)

        rule_weight = weights["rule_based"]
        mf_weight = weights["matrix_factorization"]

        logger.info(
            f"하이브리드 추천 시작: Phase={current_phase}, Rule={rule_weight}, MF={mf_weight}"
        )

        # Phase P1: Rule-Based만 사용
        if current_phase == "P1" or mf_weight == 0.0:
            logger.info("P1 Phase: Rule-Based 100% 사용")
            return self.rule_recommender.recommend(
                user_id=user_id, limit=limit, include_explanations=include_explanations
            )

        # Phase P2/P3: 하이브리드
        logger.info(f"{current_phase} Phase: 하이브리드 추천")

        # 1. Rule-Based 추천 (더 많이 가져옴)
        rule_items = self.rule_recommender.recommend(
            user_id=user_id,
            limit=limit * 2,  # 혼합을 위해 2배 가져옴
            include_explanations=include_explanations,
        )

        if not rule_items:
            logger.warning("Rule-Based 추천 결과 없음")
            return []

        # 2. MF 모델 로드
        if self.mf_model is None:
            if not self.load_mf_model():
                logger.warning("MF 모델 로드 실패, Rule-Based만 사용")
                return rule_items[:limit]

        # 3. MF 예측
        # Rule-Based 결과 + 모집 중인 다른 게시글에 대해 MF 예측
        rule_post_ids = [item.recruit_post.recruitPostId for item in rule_items]

        # 추가로 모집 중인 게시글 가져오기
        additional_posts = (
            self.db.query(RecruitPostORM)
            .filter_by(recruit_status="RECRUITING")
            .limit(50)
            .all()
        )

        all_post_ids = list(
            set(rule_post_ids + [p.recruit_post_id for p in additional_posts])
        )

        mf_predictions = self.get_mf_predictions(user_id, all_post_ids)

        logger.info(f"MF 예측 완료: {len(mf_predictions)}개 게시글")

        # 4. 혼합
        blended_items = self.blend_recommendations(
            rule_items=rule_items,
            mf_predictions=mf_predictions,
            rule_weight=rule_weight,
            mf_weight=mf_weight,
            limit=limit,
        )

        logger.info(f"하이브리드 추천 완료: {len(blended_items)}개")
        return blended_items
