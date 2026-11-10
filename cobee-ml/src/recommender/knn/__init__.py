"""
KNN 추천 모듈
Rule-Based KNN 알고리즘을 사용한 추천 시스템
"""

from .rule_based import RuleBasedKNNRecommender

from .similarity import (
    create_feature_vector,
    create_weight_vector,
    calculate_weighted_euclidean_distance,
    calculate_age_overlap_coefficient,
    calculate_gender_match_score,
)

__all__ = [
    "RuleBasedKNNRecommender",
    "create_feature_vector",
    "create_weight_vector",
    "calculate_weighted_euclidean_distance",
    "calculate_age_overlap_coefficient",
    "calculate_gender_match_score",
]
