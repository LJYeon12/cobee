package org.example.memberservice.withdrawal;
import java.util.UUID;
public record WithdrawalStatus(UUID operationId, long memberId, String state, String stage, int attempts) {
}
