"""Phase selection without settings, database, or scheduler initialization."""

from typing import Any, Mapping


def select_phase(count: int, thresholds: Mapping[str, Mapping[str, Any]]) -> str:
    """Use the configured lower bounds, including their boundary values."""
    p2_min = thresholds.get("P2", {}).get("min", 100)
    p3_min = thresholds.get("P3", {}).get("min", 1000)
    if count >= p3_min:
        return "P3"
    if count >= p2_min:
        return "P2"
    return "P1"
