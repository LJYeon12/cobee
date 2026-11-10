package org.example.recruitservice.dto.converter;

import lombok.Builder;
import org.example.recruitservice.domain.ApplyRecord;
import org.example.recruitservice.dto.apply.ApplicantResponse;
import org.example.recruitservice.dto.apply.ApplyResponse;

@Builder
public class ApplyConverter {
    public static ApplyResponse from(ApplyRecord apply) {
        return ApplyResponse.builder()
                .id(apply.getId())
                .appliedPost(apply.getPost().getId())
                .applierId(apply.getAppliedMemberId())
                .matchStatus(apply.getMatchStatus())
                .build();
    }

    public static ApplicantResponse applicantConverter(ApplyRecord apply) {
        return ApplicantResponse.builder()
                .applyId(apply.getId())
                .applierId(apply.getAppliedMemberId())
                /*
                멤버 정보 연결 dto 받으면 아래 정보 추가하기
                .applierName(apply.getApplierName)
                .publicProfileId()
                .gender()
                .birthdate()
                 */
                .matchStatus(apply.getMatchStatus())
                .build();
    }
}
