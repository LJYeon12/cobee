package org.example.memberservice.withdrawal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class WithdrawalCoordinator {
    private final WithdrawalStore store;
    public WithdrawalStatus accept(long memberId) {
        return store.accept(memberId);
    }
    public WithdrawalStatus status(long memberId) {
        return store.status(memberId);
    }
}
