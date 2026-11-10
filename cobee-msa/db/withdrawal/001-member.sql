-- Apply to the member-service database after confirming the target and taking a backup.
BEGIN;
ALTER TABLE member ADD COLUMN IF NOT EXISTS lifecycle_status varchar(24) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE member ALTER COLUMN lifecycle_status SET DEFAULT 'ACTIVE';
CREATE TABLE IF NOT EXISTS withdrawal_job (
 id uuid PRIMARY KEY,
 member_id bigint NOT NULL UNIQUE,
 stage varchar(32) NOT NULL CHECK(stage IN ('CACHE_BLOCK','RECRUIT_BLOCK','RECRUIT_CLEANUP','REFRESH_DELETE','MEMBER_REMOVE','COMPLETE')),
 attempts integer NOT NULL DEFAULT 0,
 owner_token uuid,
 lease_until timestamptz,
 next_attempt_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 last_error varchar(128)
);
CREATE INDEX IF NOT EXISTS withdrawal_pending ON withdrawal_job(next_attempt_at,created_at) WHERE stage<>'COMPLETE';
COMMIT;
