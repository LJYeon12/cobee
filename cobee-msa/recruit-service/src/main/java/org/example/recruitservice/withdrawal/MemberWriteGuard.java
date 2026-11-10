package org.example.recruitservice.withdrawal;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class MemberWriteGuard {
    private final JdbcTemplate jdbc;

    /** The caller owns the transaction; its commit releases these locks. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireWritable(Long... memberIds) {
        if (Arrays.stream(memberIds).anyMatch(java.util.Objects::isNull))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MEMBER_ID_REQUIRED");
        Arrays.stream(memberIds).distinct().sorted().forEach(id -> {
            var states = jdbc.queryForList("SELECT state FROM member_write_state WHERE member_id=? FOR UPDATE",
                    String.class, id);
            if (states.isEmpty())
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MEMBER_NOT_READY");
            if (!"ACTIVE".equals(states.getFirst()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MEMBER_WRITES_BLOCKED");
        });
    }
}
