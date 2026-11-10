"""
Rule-Based KNN 추천 엔진
가중치를 적용한 KNN 알고리즘으로 추천을 생성합니다.
성별/나이는 하드 필터가 아닌 소프트 스코어링으로 처리합니다.
"""

import math
from typing import List, Set, Dict, Any
from sqlalchemy.orm import Session

from src.models.orm_models import (
    MemberInformationORM,
    RecruitPostORM,
    BookmarkORM,
    ApplyRecordORM,
)
from src.models.schemas import RecommendationItem, RecommendationExplanation
from src.utils.config_loader import ConfigLoader
from src.utils.logger import get_logger

from .similarity import (
    create_feature_vector,
    create_weight_vector,
    calculate_weighted_euclidean_distance,
    calculate_age_overlap_coefficient,
    calculate_gender_match_score,
)

logger = get_logger(__name__)


class RuleBasedKNNRecommender:
    """
    Rule-Based KNN 추천 엔진

    특징:
    - 가중치 적용 KNN (성별, 나이에 높은 가중치)
    - 하드 필터링: 제외 목록(북마크/지원/본인 게시글)만 필터링
    - 소프트 스코어링: 성별/나이는 거리 계산에만 반영
    """

    def __init__(self, db: Session, config: ConfigLoader):
        """
        Args:
            db: SQLAlchemy 세션
            config: 설정 로더
        """
        self.db = db
        self.config = config

        # config에서 가중치 읽기
        self.gender_weight = config.get("rule_based.feature_weights.gender", 5.0)
        self.age_weight = config.get("rule_based.feature_weights.age", 3.0)

        logger.info(
            f"RuleBasedKNNRecommender 초기화 완료 (gender_weight={self.gender_weight}, age_weight={self.age_weight})"
        )

    def get_member_exclusions(self, member_id: int) -> Set[int]:
        """
        제외할 게시글 ID 목록 수집
        - 이미 북마크한 게시글
        - 이미 지원한 게시글

        Args:
            member_id: 회원 ID

        Returns:
            Set[int]: 제외할 게시글 ID 집합
        """
        excluded_ids = set()

        # 북마크한 게시글
        bookmarks = self.db.query(BookmarkORM).filter_by(member_id=member_id).all()
        for bookmark in bookmarks:
            excluded_ids.add(bookmark.recruit_post_id)

        # 지원한 게시글
        apply_records = (
            self.db.query(ApplyRecordORM).filter_by(member_id=member_id).all()
        )
        for record in apply_records:
            excluded_ids.add(record.recruit_post_id)

        logger.debug(f"회원 {member_id}의 제외 목록: {len(excluded_ids)}개")
        return excluded_ids

    def calculate_weighted_distance(
        self,
        member: MemberInformationORM,
        post: RecruitPostORM,
        author: MemberInformationORM,
    ) -> float:
        """
        가중치가 적용된 KNN 거리 계산 (순수 KNN 방식)

        Args:
            member: 추천을 받는 회원
            post: 게시글
            author: 게시글 작성자

        Returns:
            float: 거리 값 (작을수록 유사)
        """
        # 1. 특성 벡터 생성
        member_vec, author_vec = create_feature_vector(member, post, author)

        # 2. 가중치 벡터 생성
        weight_vec = create_weight_vector(self.gender_weight, self.age_weight)

        # 3. 가중 유클리드 거리 계산
        distance = calculate_weighted_euclidean_distance(
            member_vec, author_vec, weight_vec
        )

        return distance

    def calculate_max_possible_distance(self) -> float:
        """
        이론적 최대 거리 계산

        벡터 구조:
        - 성별 One-Hot (3): 최대 차이 = sqrt(2) → 제곱 = 2
        - 나이 (1): 최대 차이 = 15 (만 34 - 만 19) → 제곱 = 225
        - 생활패턴 One-Hot (2): 최대 차이 = sqrt(2) → 제곱 = 2
        - 성격 One-Hot (2): 최대 차이 = sqrt(2) → 제곱 = 2
        - 습관 Boolean (3): 최대 차이 = 1 → 제곱 = 1
        - 동거인 수 (1): 최대 차이 = 10 (추정) → 제곱 = 100

        Returns:
            float: 최대 가능 거리
        """
        max_distance_squared = (
            3 * self.gender_weight * 2  # 성별: 3개 × weight × 2
            + 1 * self.age_weight * (15**2)  # 나이: 1개 × weight × 225
            + 2 * 1.0 * 2  # 생활패턴: 2개 × 1.0 × 2
            + 2 * 1.0 * 2  # 성격: 2개 × 1.0 × 2
            + 3 * 1.0 * (1**2)  # 습관: 3개 × 1.0 × 1
            + 1 * 1.0 * (10**2)  # 동거인: 1개 × 1.0 × 100
        )
        return math.sqrt(max_distance_squared)

    def generate_explanation(
        self,
        member: MemberInformationORM,
        post: RecruitPostORM,
        author: MemberInformationORM,
        distance: float,
    ) -> RecommendationExplanation:
        """
        추천 이유 설명 생성

        Args:
            member: 추천을 받는 회원
            post: 게시글
            author: 게시글 작성자
            distance: 계산된 거리

        Returns:
            RecommendationExplanation: 설명 객체
        """
        reasons = []

        # 성별 매칭
        gender_score = calculate_gender_match_score(
            member, author, post.preferred_gender
        )
        if gender_score >= 0.9:
            reasons.append("성별 선호 완벽 일치")
        elif gender_score >= 0.5:
            reasons.append("성별 선호 부분 일치")

        # 나이 범위
        age_sim = calculate_age_overlap_coefficient(
            member.preferred_age_min,
            member.preferred_age_max,
            post.preferred_age_min,
            post.preferred_age_max,
        )
        if age_sim >= 0.7:
            reasons.append(f"선호 나이 범위 {int(age_sim * 100)}% 일치")

        # 생활 패턴
        if member.preferred_life_style == author.my_lifestyle:
            reasons.append(f"생활 패턴 일치 ({member.preferred_life_style})")

        # 성격
        if member.preferred_personality == author.my_personality:
            reasons.append(f"성격 유형 일치 ({member.preferred_personality})")

        # 습관
        if author.is_smoking and member.possible_smoking:
            reasons.append("흡연 수용 가능")
        if author.is_snoring and member.possible_snoring:
            reasons.append("코골이 수용 가능")
        if author.has_pet and member.has_pet_allowed:
            reasons.append("반려동물 수용 가능")

        # 거리를 점수로 변환 (0~1)
        # 거리가 작을수록 점수 높음
        max_distance = self.calculate_max_possible_distance()

        # 정규화: score = 1 - (distance / max_distance)
        # distance가 0이면 score = 1.0 (완벽한 매칭)
        # distance가 max_distance면 score = 0.0 (최악의 매칭)
        score = max(0.0, min(1.0, 1.0 - (distance / max_distance)))

        return RecommendationExplanation(
            score=score,
            percentage=f"{int(score * 100)}%",
            reasons=reasons,
            details={
                "distance": round(distance, 4),
                "max_distance": round(max_distance, 4),
                "gender_weight": self.gender_weight,
                "age_weight": self.age_weight,
            },
        )

    def recommend(
        self, user_id: int, limit: int = 20, include_explanations: bool = True
    ) -> List[RecommendationItem]:
        """
        메인 추천 함수

        Args:
            user_id: 추천을 받을 회원 ID
            limit: 추천 개수
            include_explanations: 설명 포함 여부

        Returns:
            List[RecommendationItem]: 추천 목록
        """
        logger.info(f"추천 시작: user_id={user_id}, limit={limit}")

        # 1. 사용자 정보 조회
        member = (
            self.db.query(MemberInformationORM).filter_by(member_id=user_id).first()
        )
        if not member:
            logger.error(f"회원을 찾을 수 없습니다: {user_id}")
            return []

        # 2. 제외 목록 생성
        excluded_ids = self.get_member_exclusions(user_id)

        # 3. 모집 중인 게시글 조회
        recruiting_posts = (
            self.db.query(RecruitPostORM).filter_by(recruit_status="RECRUITING").all()
        )

        logger.info(f"모집 중인 게시글: {len(recruiting_posts)}개")

        # 4. 필터링 및 거리 계산
        candidates = []
        for post in recruiting_posts:
            logger.debug(f"게시글 {post.recruit_post_id} 검사 시작")

            # 하드 필터 1: 제외 목록 체크
            if post.recruit_post_id in excluded_ids:
                logger.debug(f"  -> 제외 목록에 포함 (북마크 또는 지원)")
                continue

            # 하드 필터 2: 본인이 작성한 게시글 제외
            if post.member_id == user_id:
                logger.debug(f"  -> 본인이 작성한 게시글")
                continue

            # 작성자 정보 조회
            author = post.member
            if not author:
                logger.debug(f"  -> 작성자 정보 없음")
                continue

            # 소프트 스코어링: 성별/나이는 필터링하지 않고 거리 계산에만 반영
            try:
                distance = self.calculate_weighted_distance(member, post, author)
                logger.debug(f"  -> ✅ 통과! (거리: {distance:.4f})")

                candidates.append(
                    {"post": post, "author": author, "distance": distance}
                )
            except Exception as e:
                logger.error(f"  -> 거리 계산 실패: {e}")
                continue

        logger.info(f"필터링 후 후보: {len(candidates)}개")

        # 5. 거리 기준 정렬 (가까운 순 = 유사도 높은 순)
        candidates.sort(key=lambda x: x["distance"])

        # 6. 상위 K개 선택
        top_candidates = candidates[:limit]

        # 7. RecommendationItem 형태로 변환
        recommendations = []
        for rank, candidate in enumerate(top_candidates, start=1):
            explanation = None
            if include_explanations:
                explanation = self.generate_explanation(
                    member,
                    candidate["post"],
                    candidate["author"],
                    candidate["distance"],
                )

            # Pydantic 모델로 변환
            from src.models.schemas import RecruitPost

            post_schema = RecruitPost.from_orm(candidate["post"])

            recommendations.append(
                RecommendationItem(
                    recruit_post=post_schema,
                    score=explanation.score if explanation else 0.0,
                    rank=rank,
                    explanation=explanation,
                )
            )

        logger.info(f"추천 완료: {len(recommendations)}개 반환")
        return recommendations
