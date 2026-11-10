package org.example.recruitservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.recruitservice.domain.Enum.MatchStatus;
import org.example.recruitservice.dto.apply.ApplicantResponse;
import org.example.recruitservice.dto.apply.ApplyResponse;
import org.example.recruitservice.dto.recruit.RecruitCoreResponse;
import org.example.recruitservice.dto.apply.ApplyAcceptRequest;
import org.example.recruitservice.service.ApplyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apply")
@RequiredArgsConstructor
public class ApplyController {
    private final ApplyService applyService;

    // TODO : MemberDTO 받으면 로그인 멤버 정보 추가하기
    @PostMapping("/{postId}")
    public ApiResponse<ApplyResponse> apply(@RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long applier,
            @PathVariable Long postId) {
        return ApiResponse.success("해당 구인글에 지원이 완료되었습니다.", "APPLY-001", applyService.applyForRecruit(applier, postId));
    }

    @PostMapping("/accept")
    public ApiResponse<ApplyResponse> accept(@RequestBody ApplyAcceptRequest applyRecord,
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        return ApiResponse.success("지원 수락 여부 결정완료", "APPLY-002", applyService.acceptOrReject(applyRecord, memberId));
    }

    // 특정 구인글에서의 지원자 조회(내가 수락할 지원자 정보) + 상태에 따른 조회
    @GetMapping("/applier/{postId}")
    public ApiResponse<List<ApplicantResponse>> getMyAppliers(@PathVariable Long postId,
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam(name = "status", required = false) MatchStatus status) {
        // 이때 memberId는 로그인한 사용자 본인
        return ApiResponse.success("구인글 지원자 목록 조회 완료", "APPLY-003",
                applyService.getMyApplicants(postId, memberId, status));
    }

    @GetMapping("/my") // 나의 매칭 상태에 따른 지원 구인글 조회 - Long memberId는 본인
    public ApiResponse<List<RecruitCoreResponse>> getMyAppliedPostsInfo(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam(name = "status", required = false) MatchStatus status) {
        return ApiResponse.success("나의 지원 구인글 상태별 조회 완료", "APPLY-004",
                applyService.getMyAppliedPostsByMatchStatus(memberId, status));
    }

    @GetMapping("/isApplied/{postId}") // 내가 해당 구인글에 지원했는지 여부
    public ApiResponse<Boolean> checkIfApplied(@RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @PathVariable(name = "postId") Long postId) {
        return ApiResponse.success("내가 지원했는지 여부 조회 완료", "APPLY-005",
                applyService.checkIfIAppliedThisPost(postId, memberId));
    }

}
