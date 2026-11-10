package org.example.recruitservice.dto.sync;

import lombok.Builder;
import lombok.Data;
import org.example.recruitservice.domain.RecruitPost;

import java.time.LocalDateTime;

@Data
@Builder
public class RecruitPostSyncDto {
    private Long recruit_post_id;
    private Integer recruit_count;
    private Integer rent_cost_min;
    private Integer rent_cost_max;
    private Integer monthly_cost_min;
    private Integer monthly_cost_max;
    private String preferred_gender;
    private String preferred_life_style;
    private String preferred_personality;
    private Boolean is_smoking;
    private Boolean is_snoring;
    private Boolean is_pet_allowed;
    private Integer cohabitant_count;
    private Integer preferred_age_min;
    private Integer preferred_age_max;
    private Boolean has_room;
    private String address;
    private Double region_latitude;
    private Double region_longitude;
    private String recruit_status;
    private Long member_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public static RecruitPostSyncDto from(RecruitPost recruitPost) {
        return RecruitPostSyncDto.builder()
                .recruit_post_id(recruitPost.getId())
                .recruit_count(recruitPost.getRecruitCount())
                .rent_cost_min(recruitPost.getRentCostMin())
                .rent_cost_max(recruitPost.getRentCostMax())
                .monthly_cost_min(recruitPost.getMonthlyCostMin())
                .monthly_cost_max(recruitPost.getMonthlyCostMax())
                .preferred_gender(recruitPost.getPreferredGender() != null ? recruitPost.getPreferredGender().name() : null)
                .preferred_life_style(recruitPost.getLifeStyle() != null ? recruitPost.getLifeStyle().name() : null)
                .preferred_personality(recruitPost.getPersonality() != null ? recruitPost.getPersonality().name() : null)
                .is_smoking(recruitPost.getIsSmoking())
                .is_snoring(recruitPost.getIsSnoring())
                .is_pet_allowed(recruitPost.getHasPet())
                .preferred_age_min(recruitPost.getMinAge())
                .preferred_age_max(recruitPost.getMaxAge())
                .has_room(recruitPost.getHasRoom())
                .address(recruitPost.getAddress())
                .region_latitude(recruitPost.getRegionLatitude())
                .region_longitude(recruitPost.getRegionLongitude())
                .recruit_status(recruitPost.getStatus() != null ? recruitPost.getStatus().name() : null)
                .member_id(recruitPost.getOwnerId())
                .created_at(recruitPost.getCreatedAt())
                .updated_at(recruitPost.getUpdatedAt())
                .build();
    }
}
