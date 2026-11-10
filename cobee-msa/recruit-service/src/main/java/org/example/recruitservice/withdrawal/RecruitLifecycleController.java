package org.example.recruitservice.withdrawal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/members")
public class RecruitLifecycleController {
    private final RecruitMemberLifecycle lifecycle;
    @PutMapping("/{memberId}/activate")
    public void activate(@PathVariable long memberId) {
        lifecycle.activate(memberId);
    }
    @PostMapping("/{memberId}/withdrawals/{operationId}/block")
    public void block(@PathVariable long memberId, @PathVariable UUID operationId) {
        lifecycle.block(memberId, operationId);
    }
    @PostMapping("/{memberId}/withdrawals/{operationId}/cleanup")
    public void cleanup(@PathVariable long memberId, @PathVariable UUID operationId) {
        lifecycle.cleanup(memberId, operationId);
    }
}
