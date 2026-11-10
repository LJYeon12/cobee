package org.example.memberservice.dto.member;

import lombok.Builder;
import lombok.Getter;
import org.example.memberservice.domain.Member;

@Getter
@Builder
public class MemberResponseDto {
    private Long id;
    private String name;
    private String email;
    private String profileUrl;

    // Member 엔티티를 DTO로 변환하는 정적 메서드
    public static MemberResponseDto from(Member member) {
        return MemberResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .profileUrl(member.getProfileUrl())
                .build();
    }
}
