-- Apply to the recruit-service database. No cross-service foreign key is used.
BEGIN;
CREATE TABLE IF NOT EXISTS member_write_state (
 member_id bigint PRIMARY KEY,
 state varchar(16) NOT NULL CHECK(state IN ('ACTIVE','BLOCKED')),
 withdrawal_id uuid,
 cleaned boolean NOT NULL DEFAULT false,
 CHECK((state='ACTIVE' AND withdrawal_id IS NULL) OR (state='BLOCKED' AND withdrawal_id IS NOT NULL))
);
COMMIT;
