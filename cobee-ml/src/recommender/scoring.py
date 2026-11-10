"""Recommendation score normalization without database or model dependencies."""

from typing import Mapping


def normalize_scores(scores: Mapping[int, float]) -> dict[int, float]:
    """Scale scores to 0..1; equal scores receive the neutral value 0.5."""
    if not scores:
        return {}

    min_score = min(scores.values())
    max_score = max(scores.values())
    if max_score == min_score:
        return {post_id: 0.5 for post_id in scores}

    return {
        post_id: (score - min_score) / (max_score - min_score)
        for post_id, score in scores.items()
    }
