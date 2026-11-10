package org.example.recruitservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.recruitservice.dto.recruit.RecruitCoreResponse;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.example.recruitservice.dto.recruit.RecruitResponse;
import org.example.recruitservice.service.ApplyService;
import org.example.recruitservice.service.RecruitService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/recruit")
@RequiredArgsConstructor
public class RecruitController {
    private final RecruitService recruitService;
    private final ApplyService applyService;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RecruitResponse> createRecruit(@RequestPart("request") RecruitRequest recruitRequest, // 3.
                                                                                                             // @RequestBody
                                                                                                             // ->
                                                                                                             // @RequestPart("key")
            @RequestPart(value = "images", required = false) MultipartFile[] images, // 4. 이미지 파트 추가
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId) {
        RecruitResponse response = recruitService.createRecruitPostWithImages(recruitRequest, memberId, images);
        return ApiResponse.success("구인글 생성 완료", "RECRUIT-001", response);
    }

    @GetMapping("/{postId}")
    public ApiResponse<RecruitResponse> getRecruitInfo(@PathVariable("postId") Long postId) {
        return ApiResponse.success("구인글 조회 완료", "RECRUIT-003", recruitService.getOneRecruitInfo(postId));
    }

    @GetMapping("")
    public ApiResponse<List<RecruitCoreResponse>> getAllRecruitsInfo() {
        return ApiResponse.success("모든 구인글 조회 완료", "RECRUIT-004", recruitService.getAllRecruitInfo());
    }

    @GetMapping("/my")
    public ApiResponse<List<RecruitCoreResponse>> getMyRecruitPostsInfo(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long userId) {
        return ApiResponse.success("나의 구인글 모두 조회 완료", "RECRUIT-005", recruitService.getMyAllRecruitInfo(userId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<String> deleteRecruit(@RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @PathVariable("postId") Long postId) {
        return ApiResponse.success("구인글 삭제하기 완료", "RECRUIT-006", recruitService.deleteRecruitPost(postId, memberId));
    }

    @PostMapping("/{postId}/images")
    public ApiResponse<List<String>> uploadRecruitImages(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId, @PathVariable("postId") Long postId,
            @RequestParam("images") MultipartFile[] images // 여러 파일을 받도록 배열로 설정
    ) {
        try {
            List<String> imageUrls = recruitService.addImages(postId, memberId, images);
            return ApiResponse.success("구인글 이미지 업로드 완료", "RECRUIT-007", imageUrls);
        } catch (Exception e) {
            return ApiResponse.failure("이미지 업로드 실패", "500", e.getMessage());
        }
    }
}
