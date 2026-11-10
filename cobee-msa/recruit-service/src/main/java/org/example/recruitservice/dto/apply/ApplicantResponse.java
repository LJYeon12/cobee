package org.example.recruitservice.dto.apply;

import lombok.Builder;
import lombok.Getter;
import org.example.recruitservice.domain.Enum.Gender;
import org.example.recruitservice.domain.Enum.MatchStatus;
@Getter
@Builder
public class ApplicantResponse {
    private Long applyId;
    private Long applierId;
    //private String applierName;
    private Long publicProfileId;
    private Gender gender;
    private String birthDate;
    private MatchStatus matchStatus;
}
