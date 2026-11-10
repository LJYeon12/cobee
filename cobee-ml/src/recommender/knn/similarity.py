"""
KNN 추천을 위한 유사도 계산 로직 (순수 KNN 방식)
실제 특성값을 그대로 사용하여 거리 계산
"""

import math
from typing import List, Tuple
from src.models.orm_models import MemberInformationORM, RecruitPostORM


def create_feature_vector(
    member: MemberInformationORM, post: RecruitPostORM, author: MemberInformationORM
) -> Tuple[List[float], List[float]]:
    """
    순수 KNN을 위한 특성 벡터 생성
    회원의 선호도/수용도 vs 작성자의 실제 특성

    Args:
        member: 추천을 받는 회원
        post: 게시글
        author: 게시글 작성자

    Returns:
        Tuple[List[float], List[float]]: (회원 벡터, 작성자 벡터)
    """
    # === 회원 벡터 (선호도 + 수용도) ===
    member_vec = []

    # 1. 성별 선호 (One-Hot Encoding)
    member_vec.append(1.0 if member.preferred_gender == "MAN" else 0.0)
    member_vec.append(1.0 if member.preferred_gender == "FEMALE" else 0.0)
    member_vec.append(1.0 if member.preferred_gender == "NONE" else 0.0)

    # 2. 나이
    member_vec.append(float(member.calculate_age()))

    # 3. 생활 패턴 선호 (One-Hot Encoding)
    member_vec.append(1.0 if member.preferred_life_style == "MORNING" else 0.0)
    member_vec.append(1.0 if member.preferred_life_style == "EVENING" else 0.0)

    # 4. 성격 선호 (One-Hot Encoding)
    member_vec.append(1.0 if member.preferred_personality == "INTROVERT" else 0.0)
    member_vec.append(1.0 if member.preferred_personality == "EXTROVERT" else 0.0)

    # 5. 습관 수용도 (Boolean → 0 or 1)
    member_vec.append(1.0 if member.possible_smoking else 0.0)
    member_vec.append(1.0 if member.possible_snoring else 0.0)
    member_vec.append(1.0 if member.has_pet_allowed else 0.0)

    # 6. 동거인 수 (숫자)
    member_vec.append(float(member.cohabitant_count or 0))

    # === 작성자 벡터 (실제 특성) ===
    author_vec = []

    # 1. 실제 성별 (One-Hot Encoding)
    author_vec.append(1.0 if author.gender == "MAN" else 0.0)
    author_vec.append(1.0 if author.gender == "FEMALE" else 0.0)
    author_vec.append(0.0)  # 작성자는 NONE 없음

    # 2. 실제 나이
    author_vec.append(float(author.calculate_age()))

    # 3. 실제 생활 패턴 (One-Hot Encoding)
    author_vec.append(1.0 if author.my_lifestyle == "MORNING" else 0.0)
    author_vec.append(1.0 if author.my_lifestyle == "EVENING" else 0.0)

    # 4. 실제 성격 (One-Hot Encoding)
    author_vec.append(1.0 if author.my_personality == "INTROVERT" else 0.0)
    author_vec.append(1.0 if author.my_personality == "EXTROVERT" else 0.0)

    # 5. 실제 습관 (Boolean → 0 or 1)
    author_vec.append(1.0 if author.is_smoking else 0.0)
    author_vec.append(1.0 if author.is_snoring else 0.0)
    author_vec.append(1.0 if author.has_pet else 0.0)

    # 6. 모집 인원 (숫자)
    author_vec.append(float(post.recruit_count))

    return member_vec, author_vec


def calculate_weighted_euclidean_distance(
    vec_a: List[float], vec_b: List[float], weights: List[float]
) -> float:
    """
    가중치가 적용된 유클리드 거리 계산

    distance = sqrt(Σ weight_i × (a_i - b_i)²)

    Args:
        vec_a: 벡터 A
        vec_b: 벡터 B
        weights: 각 특성의 가중치

    Returns:
        float: 가중 유클리드 거리
    """
    if len(vec_a) != len(vec_b):
        raise ValueError(f"벡터 길이가 다릅니다: {len(vec_a)} vs {len(vec_b)}")

    if len(weights) != len(vec_a):
        raise ValueError(f"가중치 개수가 다릅니다: {len(weights)} vs {len(vec_a)}")

    distance_squared = 0.0
    for a, b, w in zip(vec_a, vec_b, weights):
        distance_squared += w * ((a - b) ** 2)

    return math.sqrt(distance_squared)


def create_weight_vector(
    gender_weight: float = 5.0, age_weight: float = 3.0
) -> List[float]:
    """
    특성별 가중치 벡터 생성

    벡터 구조:
    [성별(3), 나이(1), 생활패턴(2), 성격(2), 습관(3), 동거인(1)]
    = 총 12개 특성

    Args:
        gender_weight: 성별 가중치
        age_weight: 나이 가중치

    Returns:
        List[float]: 가중치 벡터
    """
    weights = []

    # 1. 성별 (3개 특성)
    weights.extend([gender_weight] * 3)

    # 2. 나이 (1개 특성)
    weights.append(age_weight)

    # 3. 생활 패턴 (2개 특성)
    weights.extend([1.0] * 2)

    # 4. 성격 (2개 특성)
    weights.extend([1.0] * 2)

    # 5. 습관: 흡연, 코골이, 반려동물 (3개 특성)
    weights.extend([1.0] * 3)

    # 6. 동거인 수 (1개 특성)
    weights.append(1.0)

    return weights


def calculate_age_overlap_coefficient(
    member_pref_age_min: int,
    member_pref_age_max: int,
    post_pref_age_min: int,
    post_pref_age_max: int,
) -> float:
    """
    나이 선호 범위의 Overlap Coefficient 계산
    (설명 생성용으로 유지)

    overlap / min(|A|, |B|)

    Args:
        member_pref_age_min: 회원 선호 최소 나이
        member_pref_age_max: 회원 선호 최대 나이
        post_pref_age_min: 게시글 선호 최소 나이
        post_pref_age_max: 게시글 선호 최대 나이

    Returns:
        float: 0~1 사이의 유사도 점수
    """
    if not all(
        [member_pref_age_min, member_pref_age_max, post_pref_age_min, post_pref_age_max]
    ):
        return 0.5  # 선호가 없으면 중립

    # 교집합 계산
    overlap_min = max(member_pref_age_min, post_pref_age_min)
    overlap_max = min(member_pref_age_max, post_pref_age_max)
    overlap = max(0, overlap_max - overlap_min + 1)

    # min(|A|, |B|) 계산
    len_member_range = member_pref_age_max - member_pref_age_min + 1
    len_post_range = post_pref_age_max - post_pref_age_min + 1
    min_range = min(len_member_range, len_post_range)

    return overlap / min_range if min_range > 0 else 0.0


def calculate_gender_match_score(
    member: MemberInformationORM,
    post_author: MemberInformationORM,
    post_preferred_gender: str,
) -> float:
    """
    성별 매칭 점수 계산 (설명 생성용으로 유지)

    Args:
        member: 추천을 받는 회원
        post_author: 게시글 작성자
        post_preferred_gender: 게시글의 선호 성별

    Returns:
        float: 0~1 사이의 점수
    """
    score = 0.0

    # 방향 1: 회원 성별 vs 게시글 선호
    if post_preferred_gender == "NONE":
        score += 0.5
    elif member.gender == post_preferred_gender:
        score += 1.0

    # 방향 2: 게시글 작성자 성별 vs 회원 선호
    if not member.preferred_gender or member.preferred_gender == "NONE":
        score += 0.5
    elif post_author.gender == member.preferred_gender:
        score += 1.0

    return score / 2.0
