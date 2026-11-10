package org.example.recruitservice.dto.sync;

import lombok.Builder;
import lombok.Data;
import org.example.recruitservice.domain.ApplyRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ApplyRecordSyncDto {
    private Long record_id;
    private String match_status;
    private LocalDate submitted_at;
    private Long member_id;
    private Long recruit_post_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public static ApplyRecordSyncDto from(ApplyRecord applyRecord) {
        return ApplyRecordSyncDto.builder()
                .record_id(applyRecord.getId())
                .match_status(applyRecord.getMatchStatus() != null ? applyRecord.getMatchStatus().name() : null)
                .submitted_at(applyRecord.getSubmittedAt())
                .member_id(applyRecord.getAppliedMemberId())
                .recruit_post_id(applyRecord.getPost().getId())
                .created_at(applyRecord.getCreatedAt())
                .updated_at(applyRecord.getUpdatedAt())
                .build();
    }
}
