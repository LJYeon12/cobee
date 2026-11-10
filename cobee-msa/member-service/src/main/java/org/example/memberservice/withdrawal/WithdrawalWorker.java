package org.example.memberservice.withdrawal;
import java.util.UUID;
import org.example.memberservice.client.RecruitClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name = "withdrawal.worker-enabled", havingValue = "true", matchIfMissing = true)
public class WithdrawalWorker {
    private final WithdrawalStore store;
    private final MemberAccessRegistry access;
    private final RecruitClient recruits;
    private final String key;
    public WithdrawalWorker(WithdrawalStore store, MemberAccessRegistry access, RecruitClient recruits,
            @Value("${internal.api-key:}") String key) {
        this.store = store;
        this.access = access;
        this.recruits = recruits;
        this.key = key;
    }
    @Scheduled(fixedDelayString = "${withdrawal.poll-delay-ms:1000}")
    public void poll() {
        runOnce();
    }
    public boolean runOnce() {
        var claim = store.claim(UUID.randomUUID());
        if (claim == null)
            return false;
        try {
            switch (claim.stage()) {
                case "CACHE_BLOCK" -> {
                    access.block(claim.memberId());
                    store.advance(claim, "RECRUIT_BLOCK");
                }
                case "RECRUIT_BLOCK" -> {
                    requireKey();
                    recruits.block(claim.memberId(), claim.id(), key);
                    store.advance(claim, "RECRUIT_CLEANUP");
                }
                case "RECRUIT_CLEANUP" -> {
                    requireKey();
                    recruits.cleanup(claim.memberId(), claim.id(), key);
                    store.advance(claim, "REFRESH_DELETE");
                }
                case "REFRESH_DELETE" -> {
                    access.removeRefresh(claim.memberId());
                    store.advance(claim, "MEMBER_REMOVE");
                }
                case "MEMBER_REMOVE" -> store.complete(claim);
                default -> throw new IllegalStateException("Unknown withdrawal stage");
            }
        } catch (RuntimeException error) {
            store.retry(claim, error);
        }
        return true;
    }
    private void requireKey() {
        if (key.isBlank())
            throw new IllegalStateException("Internal API key is required");
    }
}
