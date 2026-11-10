package org.example.memberservice.dto.sync;

import lombok.Builder;
import lombok.Data;
import org.example.memberservice.domain.Member;
import org.example.memberservice.domain.MemberPreference;
import org.example.memberservice.domain.PublicProfile;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberSyncDto {
    private Long member_id;
    private String gender;
    private String birth_date;

    // Preferences
    private String preferred_gender;
    private String preferred_life_style;
    private String preferred_personality;
    private Boolean possible_smoking;
    private Boolean possible_snoring;
    private Boolean has_pet_allowed;
    private Integer cohabitant_count;
    private Integer preferred_age_min;
    private Integer preferred_age_max;

    // My characteristics
    private String my_lifestyle;
    private String my_personality;
    private Boolean is_smoking;
    private Boolean is_snoring;
    private Boolean has_pet;

    // Meta
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public static MemberSyncDto from(Member member) {
        MemberPreference pref = member.getMemberPreference();
        PublicProfile profile = member.getPublicProfile();

        return MemberSyncDto.builder()
                .member_id(member.getId())
                .gender(member.getGender())
                .birth_date(member.getBirthDate())
                .preferred_gender(pref != null && pref.getGender() != null ? pref.getGender().name() : null)
                .preferred_life_style(pref != null && pref.getLifestyle() != null ? pref.getLifestyle().name() : null)
                .preferred_personality(pref != null && pref.getPersonality() != null ? pref.getPersonality().name() : null)
                .possible_smoking(pref != null ? pref.getIsSmoking() : null)
                .possible_snoring(pref != null ? pref.getIsSnoring() : null)
                .has_pet_allowed(pref != null ? pref.getHasPet() : null)
                .cohabitant_count(pref != null ? pref.getCohabitantCount() : null)
                .preferred_age_min(pref != null ? pref.getPreferredAgeMin() : null)
                .preferred_age_max(pref != null ? pref.getPreferredAgeMax() : null)
                .my_lifestyle(profile != null && profile.getLifestyle() != null ? profile.getLifestyle().name() : null)
                .my_personality(profile != null && profile.getPersonality() != null ? profile.getPersonality().name() : null)
                .is_smoking(profile != null ? profile.getIsSmoking() : null)
                .is_snoring(profile != null ? profile.getIsSnoring() : null)
                .has_pet(profile != null ? profile.getHasPet() : null)
                .created_at(member.getCreatedAt())
                .updated_at(member.getUpdatedAt())
                .build();
    }
}

