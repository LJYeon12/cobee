package org.example.memberservice.dto.memberPreferences;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.memberservice.domain.enums.Gender;
import org.example.memberservice.domain.enums.LifeStyle;
import org.example.memberservice.domain.enums.Personality;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberPreferencesRequestDto {

    private Gender preferredGender;
    private LifeStyle lifestyle;
    private Personality personality;
    private Boolean smokingPreference;
    private Boolean snoringPreference;
    private Boolean petPreference;
    @NotNull(message = "동거인 수는 필수입니다")
    @Min(value = 2, message = "동거인 수는 최소 2명입니다")
    @Max(value = 10, message = "동거인 수는 최대 10명입니다")
    private Integer cohabitantCount;
    private Integer preferredAgeMin;
    private Integer preferredAgeMax;
}
