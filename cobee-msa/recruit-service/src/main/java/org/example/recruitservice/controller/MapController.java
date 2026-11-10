package org.example.recruitservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.recruitservice.dto.map.RecruitMapFilterResponse;
import org.example.recruitservice.dto.recruit.RecruitResponse;
import org.example.recruitservice.service.RecruitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MapController {
    private final RecruitService recruitService;

    //구인글 필터 (위치, 모집인원, 월세, 보증금)
    @GetMapping("/posts/filter")
    public ApiResponse<List<RecruitMapFilterResponse>> getRecruitPosts(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Integer recruitCount,
            @RequestParam(required = false) Integer rentCostMin,
            @RequestParam(required = false) Integer rentCostMax,
            @RequestParam(required = false) Integer monthlyCostMin,
            @RequestParam(required = false) Integer monthlyCostMax
    ) {

        List<RecruitMapFilterResponse> result = recruitService.getfilterRecruitPosts(latitude, longitude, radius, recruitCount,
                rentCostMin, rentCostMax, monthlyCostMin, monthlyCostMax);
        return ApiResponse.success("해당 내역의 모든 구인글 조회 완료", "RECRUIT_FILTER_GET_ALL", result);
    }

}

