package org.example.memberservice.withdrawal;
import org.example.memberservice.domain.Member;
import org.example.memberservice.client.RecruitClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class MemberAccessInitializer {
    private final RecruitClient recruits;
    private final MemberAccessRegistry access;
    private final String key;
    public MemberAccessInitializer(RecruitClient recruits, MemberAccessRegistry access,
            @Value("${internal.api-key:}") String key) {
        this.recruits = recruits;
        this.access = access;
        this.key = key;
    }
    public void ensureReady(Member member) {
        member.requireActive();
        if (key.isBlank())
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "INTERNAL_KEY_NOT_CONFIGURED");
        recruits.activate(member.getId(), key);
        access.activate(member.getId());
    }
}
