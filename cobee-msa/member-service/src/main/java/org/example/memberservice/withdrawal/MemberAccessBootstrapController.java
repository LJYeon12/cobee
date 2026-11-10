package org.example.memberservice.withdrawal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
public class MemberAccessBootstrapController {
    private final MemberRepository members;
    private final MemberAccessInitializer initializer;
    @PostMapping("/internal/member-access/bootstrap")
    public Map<String, Integer> bootstrap() {
        int initialized = 0, failed = 0;
        for (var member : members.findAll())
            if ("ACTIVE".equals(member.getLifecycleStatus())) {
                try {
                    initializer.ensureReady(member);
                    initialized++;
                } catch (RuntimeException error) {
                    failed++;
                }
            }
        return Map.of("initialized", initialized, "failed", failed);
    }
}
