package org.example.memberservice.dto.publicProfile;

import org.example.memberservice.domain.enums.LifeStyle;
import org.example.memberservice.domain.enums.Personality;

public record PublicProfileRequestDto(
        LifeStyle lifestyle,
        Personality personality,
        Boolean isSmoking,
        Boolean isSnoring,
        Boolean hasPet,
        String myInfo
) {
}
