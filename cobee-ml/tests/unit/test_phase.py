import pytest

from src.recommender.phase import select_phase


@pytest.mark.parametrize(
    ("count", "expected"),
    [(0, "P1"), (99, "P1"), (100, "P2"), (999, "P2"), (1000, "P3"), (5000, "P3")],
)
def test_default_phase_boundaries(count, expected):
    assert select_phase(count, {}) == expected


@pytest.mark.parametrize(
    ("count", "expected"), [(19, "P1"), (20, "P2"), (49, "P2"), (50, "P3")]
)
def test_configured_phase_boundaries(count, expected):
    thresholds = {"P2": {"min": 20}, "P3": {"min": 50}}

    assert select_phase(count, thresholds) == expected
