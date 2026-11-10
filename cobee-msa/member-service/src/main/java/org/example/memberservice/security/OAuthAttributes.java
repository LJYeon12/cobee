package org.example.memberservice.security;

import lombok.Builder;
import lombok.Getter;
import org.example.memberservice.domain.Member;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String socialId; // 네이버 고유 ID
    private String profileUrl; // 프로필 이미지 URL
    private String gender;
    private String birthDate;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String socialId, String profileUrl, String gender, String birthDate) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.socialId = socialId;
        this.profileUrl = profileUrl;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    // 네이버 사용자 정보 파싱 메서드
    public static OAuthAttributes of(String userNameAttributeName, Map<String, Object> attributes) {
        // attributes 맵에서 "response" 키로 감싸진 실제 사용자 정보 맵을 꺼냅니다.
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        String birthyear = (String) response.get("birthyear");
        String birthday = (String) response.get("birthday");
        String birthdate = null;

        if (birthyear != null && birthday != null) {
            birthdate = birthyear + "-" + birthday;
        } else if (birthyear != null) {
            birthdate = birthyear;
        } else if (birthday != null) {
            birthdate = birthday;
        }

        String gender = (String) response.get("gender");
        String processedGender = null;
        if (gender != null) {
            if (gender.equalsIgnoreCase("F")) {
                processedGender = "FEMALE";
            } else if (gender.equalsIgnoreCase("M")) {
                processedGender = "MALE";
            }
        }

        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .socialId((String) response.get("id")) // 네이버 고유 id
                .profileUrl((String) response.get("profile_image"))
                .gender(processedGender)
                .birthDate(birthdate) // 네이버 응답 필드명은 birthday
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public Member toEntity() {
        return Member.builder()
                .name(name)
                .email(email)
                .socialId(socialId)
                .profileUrl(profileUrl)
                .gender(gender)
                .birthDate(birthDate)
                .isCompleted(false) // 추가 정보 미입력 상태로 가입
                .ocrValidation(false) // ocr 인증 미완료 상태로 가입
                .build();
    }

}
