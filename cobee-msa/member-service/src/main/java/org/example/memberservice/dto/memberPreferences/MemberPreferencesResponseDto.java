package org.example.memberservice.dto.memberPreferences;

import lombok.Builder;
import lombok.Getter;
import org.example.memberservice.domain.MemberPreference;
import org.example.memberservice.domain.enums.Gender;
import org.example.memberservice.domain.enums.LifeStyle;
import org.example.memberservice.domain.enums.Personality;

@Getter
@Builder
public class MemberPreferencesResponseDto {
    private Long id;
    private Gender preferredGender;
    private LifeStyle lifestyle;
    private Personality personality;
    private Boolean smokingPreference;
    private Boolean snoringPreference;
    private Boolean petPreference;
    private Integer cohabitantCount;
    private Integer preferredAgeMin;
    private Integer preferredAgeMax;

    public static MemberPreferencesResponseDto from(MemberPreference memberPreference) {
        return MemberPreferencesResponseDto.builder()
                .id(memberPreference.getId())
                .preferredGender(memberPreference.getGender())
                .lifestyle(memberPreference.getLifestyle())
                .personality(memberPreference.getPersonality())
                .smokingPreference(memberPreference.getIsSmoking())
                .snoringPreference(memberPreference.getIsSnoring())
                .cohabitantCount(memberPreference.getCohabitantCount())
                .petPreference(memberPreference.getHasPet())
                .preferredAgeMin(memberPreference.getPreferredAgeMin())
                .preferredAgeMax(memberPreference.getPreferredAgeMax())
                .build();
    }
}
