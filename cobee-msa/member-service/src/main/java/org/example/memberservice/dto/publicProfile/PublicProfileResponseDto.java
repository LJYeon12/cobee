package org.example.memberservice.dto.publicProfile;

import org.example.memberservice.domain.Member;
import org.example.memberservice.domain.PublicProfile;
import org.example.memberservice.domain.enums.LifeStyle;
import org.example.memberservice.domain.enums.Personality;

public record PublicProfileResponseDto(
        Long memberId,
        String name,
        String gender,
        String profileImageUrl,
        LifeStyle lifestyle,
        Personality personality,
        Boolean mSmoking,
        Boolean mSnoring,
        Boolean mPet,
        String myInfo
) {
    public static PublicProfileResponseDto from(PublicProfile publicProfile, Member  member) {
        return new PublicProfileResponseDto(
                member.getId(),
                member.getName(),
                member.getGender(),
                member.getProfileUrl(),
                publicProfile.getLifestyle(),
                publicProfile.getPersonality(),
                publicProfile.getIsSmoking(),
                publicProfile.getIsSnoring(),
                publicProfile.getHasPet(),
                publicProfile.getMyInfo()
        );
    }
}
