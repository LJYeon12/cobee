package org.example.memberservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.memberservice.dto.memberPreferences.MemberPreferencesRequestDto;
import org.example.memberservice.dto.memberPreferences.MemberPreferencesResponseDto;
import org.example.memberservice.service.MemberPreferenceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences")
@Slf4j
@RequiredArgsConstructor
public class MemberPreferencesController {

    private final MemberPreferenceService memberPreferenceService;

    @PostMapping()
    public ApiResponse<MemberPreferencesResponseDto> createMemberPreferences(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestBody MemberPreferencesRequestDto requestDto
    )
    {
        MemberPreferencesResponseDto responseDto = memberPreferenceService.createMemberPreferences(memberId, requestDto);
        return ApiResponse.success("사용자 선호도 등록이 완료되었습니다.", "201", responseDto);
    }
    @GetMapping()
    public ApiResponse<MemberPreferencesResponseDto> getMemberPreferences(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId
    )
    {
        MemberPreferencesResponseDto responseDto = memberPreferenceService.getMemberPreferences(memberId);
        return ApiResponse.success("사용자 선호도 조회가 완료되었습니다.", "200", responseDto);
    }
    @PutMapping()
    public ApiResponse<MemberPreferencesResponseDto> updateMemberPreferences(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @Valid @RequestBody MemberPreferencesRequestDto requestDto
    ) {
        MemberPreferencesResponseDto responseDto = memberPreferenceService.updateMemberPreferences(memberId, requestDto);
        return ApiResponse.success("사용자 선호도 수정이 완료되었습니다.", "200", responseDto);
    }

    @DeleteMapping()
    public ApiResponse<Void> deleteMemberPreferences(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId
    ) {
        try {
            memberPreferenceService.deleteMemberPreferences(memberId);
            return ApiResponse.success("사용자 선호도 삭제가 완료되었습니다.", "200");
        } catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
    }
}
