package org.example.memberservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.memberservice.dto.member.MemberResponseDto;
import org.example.memberservice.service.MemberService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    /**
     * 인가 테스트 API
     */
    @GetMapping("/members/me")
    public ApiResponse<MemberResponseDto> getMyInfo(@RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        MemberResponseDto myInfo = memberService.getMyInfo(memberId);
        return ApiResponse.success("내 정보 조회 성공", "200", myInfo);
    }

    @PostMapping("/members/profile/image")
    public ApiResponse<String> updateProfileImage(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam("image") MultipartFile imageFile
    ) {
        try {
            String imageUrl = memberService.updateProfileImage(memberId, imageFile);
            return ApiResponse.success("프로필 이미지 변경 성공", "200", imageUrl);
        } catch (Exception e) {
            return ApiResponse.failure("이미지 업로드 실패", "500", e.getMessage());
        }
    }
}
