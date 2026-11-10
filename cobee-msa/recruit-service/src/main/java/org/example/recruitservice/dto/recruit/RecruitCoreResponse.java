package org.example.recruitservice.dto.recruit;

import lombok.Builder;
import lombok.Getter;
import org.example.recruitservice.domain.Enum.RecruitStatus;

import java.time.LocalDate;

@Getter
@Builder
public class RecruitCoreResponse {  // 리스트업시 보이는 정보
    private Long postId;
    private String title;
    private String address;
    private int monthlyMinCost;
    private int rentCostMin;
    private int commentCount;
    private int applicantCount;
    private LocalDate createdDate;
    private RecruitStatus recruitStatus;
    private int recruitCount;
}
