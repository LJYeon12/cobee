package org.example.memberservice.withdrawal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WithdrawalStore {
    private final JdbcTemplate jdbc;
    private final MemberRepository members;
    public record Claim(UUID id, long memberId, String stage, UUID owner) {
    }
    private WithdrawalStatus map(java.sql.ResultSet r, int row) throws java.sql.SQLException {
        String stage = r.getString("stage");
        return new WithdrawalStatus(r.getObject("id", UUID.class), r.getLong("member_id"),
                stage.equals("COMPLETE") ? "WITHDRAWN" : "WITHDRAWING", stage, r.getInt("attempts"));
    }
    public WithdrawalStatus status(long memberId) {
        var rows = jdbc.query("SELECT * FROM withdrawal_job WHERE member_id=?", this::map, memberId);
        if (rows.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND");
        return rows.getFirst();
    }
    @Transactional
    public WithdrawalStatus accept(long memberId) {
        var existing = jdbc.query("SELECT * FROM withdrawal_job WHERE member_id=?", this::map, memberId);
        if (!existing.isEmpty())
            return existing.getFirst();
        var member = jdbc.queryForList("SELECT lifecycle_status FROM member WHERE id=? FOR UPDATE", String.class,
                memberId);
        if (member.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND");
        // A concurrent accept may have committed while this transaction waited for the
        // member row.
        existing = jdbc.query("SELECT * FROM withdrawal_job WHERE member_id=?", this::map, memberId);
        if (!existing.isEmpty())
            return existing.getFirst();
        if (!"ACTIVE".equals(member.getFirst()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "MEMBER_NOT_ACTIVE");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO withdrawal_job(id,member_id,stage) VALUES(?,?,'CACHE_BLOCK')", id, memberId);
        jdbc.update("UPDATE member SET lifecycle_status='WITHDRAWING' WHERE id=?", memberId);
        return status(memberId);
    }
    @Transactional
    public Claim claim(UUID owner) {
        var rows = jdbc.query("""
                SELECT id,member_id,stage FROM withdrawal_job WHERE stage<>'COMPLETE'
                 AND next_attempt_at<=CURRENT_TIMESTAMP AND (lease_until IS NULL OR lease_until<=CURRENT_TIMESTAMP)
                ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (r, n) -> new Claim(r.getObject(1, UUID.class), r.getLong(2), r.getString(3), owner));
        if (rows.isEmpty())
            return null;
        Claim c = rows.getFirst();
        jdbc.update(
                "UPDATE withdrawal_job SET owner_token=?,lease_until=CURRENT_TIMESTAMP+interval '30 seconds' WHERE id=?",
                owner, c.id());
        return c;
    }
    @Transactional
    public void advance(Claim claim, String next) {
        String expected = switch (claim.stage()) {
            case "CACHE_BLOCK" -> "RECRUIT_BLOCK";
            case "RECRUIT_BLOCK" -> "RECRUIT_CLEANUP";
            case "RECRUIT_CLEANUP" -> "REFRESH_DELETE";
            case "REFRESH_DELETE" -> "MEMBER_REMOVE";
            case "MEMBER_REMOVE" -> "COMPLETE";
            default -> throw new IllegalArgumentException("Withdrawal is already complete");
        };
        if (!expected.equals(next))
            throw new IllegalArgumentException("Withdrawal stage cannot be skipped");
        int changed = jdbc.update(
                """
                        UPDATE withdrawal_job SET stage=?,owner_token=NULL,lease_until=NULL,last_error=NULL,next_attempt_at=CURRENT_TIMESTAMP
                         WHERE id=? AND stage=? AND owner_token=? AND lease_until>CURRENT_TIMESTAMP
                        """,
                next, claim.id(), claim.stage(), claim.owner());
        if (changed != 1)
            throw new IllegalStateException("Withdrawal claim expired");
    }
    @Transactional
    public void retry(Claim claim, RuntimeException error) {
        jdbc.update("""
                UPDATE withdrawal_job SET attempts=attempts+1,last_error=?,owner_token=NULL,lease_until=NULL,
                 next_attempt_at=CURRENT_TIMESTAMP+interval '2 seconds'
                WHERE id=? AND owner_token=? AND lease_until>CURRENT_TIMESTAMP
                """, error.getClass().getSimpleName(), claim.id(), claim.owner());
    }
    @Transactional
    public void complete(Claim claim) {
        if (!"MEMBER_REMOVE".equals(claim.stage()))
            throw new IllegalStateException("Recruit cleanup is incomplete");
        var valid = jdbc.queryForList(
                "SELECT id FROM withdrawal_job WHERE id=? AND owner_token=? AND lease_until>CURRENT_TIMESTAMP FOR UPDATE",
                claim.id(), claim.owner());
        if (valid.isEmpty())
            throw new IllegalStateException("Withdrawal claim expired");
        members.findById(claim.memberId()).ifPresent(members::delete);
        members.flush();
        advance(claim, "COMPLETE");
    }
}
