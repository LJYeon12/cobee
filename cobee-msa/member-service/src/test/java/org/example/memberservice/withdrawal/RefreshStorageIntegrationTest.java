package org.example.memberservice.withdrawal;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class RefreshStorageIntegrationTest {
    @Test
    void activeMemberCanStoreExpiringRefreshAndBlockRemovesIt() throws Exception {
        try (var fixture = new WithdrawalRecoveryIntegrationTest.Fixture(true)) {
            fixture.access.activate(7);
            fixture.access.storeRefresh(7, "synthetic-refresh");
            assertEquals("synthetic-refresh", fixture.redis.opsForValue().get("7"));
            Long lifetime = fixture.redis.getExpire("7", TimeUnit.SECONDS);
            assertNotNull(lifetime);
            assertTrue(lifetime > 0 && lifetime <= TimeUnit.DAYS.toSeconds(7));
            fixture.access.block(7);
            assertFalse(Boolean.TRUE.equals(fixture.redis.hasKey("7")));
            assertThrows(ResponseStatusException.class, () -> fixture.access.storeRefresh(7, "late-refresh"));
            assertFalse(Boolean.TRUE.equals(fixture.redis.hasKey("7")));
        }
    }
}
