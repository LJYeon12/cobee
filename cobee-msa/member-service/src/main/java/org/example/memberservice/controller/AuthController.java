package org.example.memberservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.memberservice.dto.member.MemberInfoDto;
import org.example.memberservice.dto.token.TokenRefreshRequest;
import org.example.memberservice.dto.token.TokenRefreshResponse;
import org.example.memberservice.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final org.example.memberservice.withdrawal.WithdrawalCoordinator withdrawal;

    @GetMapping
    public ApiResponse<?> getMemberInfo(@RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        MemberInfoDto memberInfo = authService.getMemberInfo(memberId);
        return ApiResponse.success("사용자 정보 조회 성공", "200", memberInfo);
    }
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse tokenRefreshResponse = authService.refreshTokens(request.getRefreshToken());
        return ApiResponse.success("토큰 재발급 성공", "TOKEN_REFRESH_SUCCESS", tokenRefreshResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<MemberInfoDto> logout(@RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        MemberInfoDto memberInfoDto = authService.logout(memberId);
        return ApiResponse.success("로그아웃 성공", "200", memberInfoDto);
    }

    @PostMapping("/withdraw")
    public org.springframework.http.ResponseEntity<ApiResponse<org.example.memberservice.withdrawal.WithdrawalStatus>> withdraw(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        return org.springframework.http.ResponseEntity.accepted()
                .body(ApiResponse.success("회원 탈퇴 처리 중", "WITHDRAWAL_ACCEPTED", withdrawal.accept(memberId)));
    }

    @GetMapping("/withdrawal")
    public ApiResponse<org.example.memberservice.withdrawal.WithdrawalStatus> withdrawalStatus(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        return ApiResponse.success("회원 탈퇴 상태", "WITHDRAWAL_STATUS", withdrawal.status(memberId));
    }
}
