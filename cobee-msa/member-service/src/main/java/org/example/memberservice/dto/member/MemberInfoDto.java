package org.example.memberservice.dto.member;

import lombok.Builder;
import lombok.Getter;
import org.example.memberservice.domain.Member;

@Builder
@Getter
public class MemberInfoDto {
    private Long id;
    private String name;
    private String email;
    private String birthDate;
    private String gender;
    private String socialId;
    private Boolean isCompleted;
    private Boolean ocrValidation;

    public static MemberInfoDto from(Member member) {
        return MemberInfoDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .birthDate(member.getBirthDate())
                .gender(member.getGender())
                .socialId(member.getSocialId())
                .isCompleted(member.getIsCompleted())
                .ocrValidation(member.getOcrValidation())
                .build();
    }
}