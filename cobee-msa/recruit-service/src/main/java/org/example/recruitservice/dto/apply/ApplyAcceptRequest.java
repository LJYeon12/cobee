package org.example.recruitservice.dto.apply;

import lombok.Getter;

@Getter
public class ApplyAcceptRequest {
    private Boolean isAccepted;
    private Long applyId;
}
