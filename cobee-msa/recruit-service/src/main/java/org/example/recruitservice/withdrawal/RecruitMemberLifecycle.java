package org.example.recruitservice.withdrawal;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecruitMemberLifecycle {
    private final JdbcTemplate jdbc;

    @Transactional(timeout = 10)
    public void activate(long memberId) {
        jdbc.update(
                "INSERT INTO member_write_state(member_id,state) VALUES(?,'ACTIVE') ON CONFLICT(member_id) DO NOTHING",
                memberId);
        String state = jdbc.queryForObject("SELECT state FROM member_write_state WHERE member_id=? FOR UPDATE",
                String.class, memberId);
        if (!"ACTIVE".equals(state))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MEMBER_WRITES_BLOCKED");
    }

    @Transactional(timeout = 10)
    public void block(long memberId, UUID operationId) {
        jdbc.update("""
                INSERT INTO member_write_state(member_id,state,withdrawal_id) VALUES(?,'BLOCKED',?)
                ON CONFLICT(member_id) DO UPDATE SET state='BLOCKED',
                  withdrawal_id=COALESCE(member_write_state.withdrawal_id,EXCLUDED.withdrawal_id)
                """, memberId, operationId);
        verifyOperation(memberId, operationId);
    }

    @Transactional(timeout = 10)
    public void cleanup(long memberId, UUID operationId) {
        verifyOperation(memberId, operationId);
        // Child rows on owned posts and records authored on other members' posts are
        // both included.
        jdbc.update("DELETE FROM images WHERE recruit_post_id IN (SELECT id FROM recruit_post WHERE owner_id=?)",
                memberId);
        jdbc.update(
                "DELETE FROM bookmark WHERE member_id=? OR recruit_post_id IN (SELECT id FROM recruit_post WHERE owner_id=?)",
                memberId, memberId);
        jdbc.update(
                "DELETE FROM apply_record WHERE applied_member_id=? OR post_id IN (SELECT id FROM recruit_post WHERE owner_id=?)",
                memberId, memberId);
        // Preserve the existing subtree deletion policy; all descendants are removed in
        // one statement.
        jdbc.update(
                """
                        WITH RECURSIVE doomed(id) AS (
                          SELECT id FROM comment WHERE comment_author_id=? OR post_id IN(SELECT id FROM recruit_post WHERE owner_id=?)
                          UNION SELECT c.id FROM comment c JOIN doomed d ON c.parent_id=d.id
                        ) DELETE FROM comment WHERE id IN(SELECT id FROM doomed)
                        """,
                memberId, memberId);
        jdbc.update("DELETE FROM recruit_post WHERE owner_id=?", memberId);
        jdbc.update("UPDATE member_write_state SET cleaned=true WHERE member_id=?", memberId);
    }

    private void verifyOperation(long memberId, UUID operationId) {
        var rows = jdbc.queryForList("SELECT state,withdrawal_id FROM member_write_state WHERE member_id=? FOR UPDATE",
                memberId);
        if (rows.isEmpty() || !"BLOCKED".equals(rows.getFirst().get("state"))
                || !operationId.equals(rows.getFirst().get("withdrawal_id")))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "WITHDRAWAL_OPERATION_MISMATCH");
    }
}
