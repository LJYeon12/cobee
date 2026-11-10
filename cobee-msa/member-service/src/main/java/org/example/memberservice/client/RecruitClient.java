package org.example.memberservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "RECRUIT-SERVICE")
public interface RecruitClient {
    @org.springframework.web.bind.annotation.PutMapping("/internal/members/{memberId}/activate")
    void activate(@PathVariable("memberId") long memberId,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String key);
    @org.springframework.web.bind.annotation.PostMapping("/internal/members/{memberId}/withdrawals/{operationId}/block")
    void block(@PathVariable("memberId") long memberId, @PathVariable("operationId") java.util.UUID operationId,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String key);
    @org.springframework.web.bind.annotation.PostMapping("/internal/members/{memberId}/withdrawals/{operationId}/cleanup")
    void cleanup(@PathVariable("memberId") long memberId, @PathVariable("operationId") java.util.UUID operationId,
            @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String key);

}
