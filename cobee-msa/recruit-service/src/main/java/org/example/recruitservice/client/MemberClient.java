package org.example.recruitservice.client;

import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "MEMBER-SERVICE")
public interface MemberClient {
    @GetMapping("/members/me")
    ApiResponse<MemberCoreResponse> getMyInfo(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId);
}
