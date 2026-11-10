package org.example.memberservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.constant.GatewayConstant;
import org.example.memberservice.dto.member.MemberInfoDto;
import org.example.memberservice.service.OcrService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/verify")
    public ApiResponse<?> verifyOcr(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam("image") MultipartFile imageFile
    ){
        MemberInfoDto memberInfoDto = ocrService.verify(memberId, imageFile);
        return ApiResponse.success("신분증 인증 성공", "200", memberInfoDto);
    }
}
