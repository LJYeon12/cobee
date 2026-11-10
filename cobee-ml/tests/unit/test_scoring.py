import pytest

from src.recommender.scoring import normalize_scores


@pytest.mark.parametrize(
    ("scores", "expected"),
    [
        ({}, {}),
        ({7: 4.0}, {7: 0.5}),
        ({7: 4.0, 8: 4.0}, {7: 0.5, 8: 0.5}),
        ({10: 2.0, 20: 4.0, 30: 6.0}, {10: 0.0, 20: 0.5, 30: 1.0}),
        ({10: -4.0, 20: -2.0, 30: 0.0}, {10: 0.0, 20: 0.5, 30: 1.0}),
    ],
)
def test_normalization_preserves_relative_scores(scores, expected):
    assert normalize_scores(scores) == pytest.approx(expected)


def test_normalization_does_not_change_input_scores():
    scores = {10: 2.0, 20: 6.0}

    normalized = normalize_scores(scores)

    assert scores == {10: 2.0, 20: 6.0}
    assert normalized == {10: 0.0, 20: 1.0}
