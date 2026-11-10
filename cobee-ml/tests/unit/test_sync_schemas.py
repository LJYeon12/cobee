from datetime import datetime

import pytest
from pydantic import ValidationError

from src.models.sync_schemas import MemberSyncDto


@pytest.fixture
def member_payload():
    return {
        "member_id": 42,
        "created_at": "2026-10-01T09:00:00",
        "updated_at": "2026-10-02T10:30:00",
    }


def test_member_sync_accepts_missing_optional_profile(member_payload):
    member = MemberSyncDto.model_validate(member_payload)

    assert member.member_id == 42
    assert member.gender is None
    assert member.preferred_gender is None
    assert member.created_at == datetime(2026, 10, 1, 9)
    assert member.updated_at == datetime(2026, 10, 2, 10, 30)


@pytest.mark.parametrize("field", ["member_id", "created_at", "updated_at"])
def test_member_sync_requires_identity_and_timestamps(member_payload, field):
    del member_payload[field]

    with pytest.raises(ValidationError) as error:
        MemberSyncDto.model_validate(member_payload)

    assert error.value.errors()[0]["loc"] == (field,)


def test_member_sync_rejects_invalid_timestamp(member_payload):
    member_payload["updated_at"] = "not-a-timestamp"

    with pytest.raises(ValidationError) as error:
        MemberSyncDto.model_validate(member_payload)

    assert error.value.errors()[0]["loc"] == ("updated_at",)
