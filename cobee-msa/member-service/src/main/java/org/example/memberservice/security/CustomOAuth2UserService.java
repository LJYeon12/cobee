package org.example.memberservice.security;

import lombok.RequiredArgsConstructor;
import org.example.memberservice.domain.Member;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        // 1. 네이버로부터 사용자 정보 받아오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(oAuth2UserRequest);

        // 2. 로그인 진행 중인 서비스(네이버)를 구분
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        // 3. OAuth2 로그인 시 키가 되는 필드값 (Primary Key와 같은 역할)
        String userNameAttributeName = oAuth2UserRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        // 4. OAuthAttributes 클래스를 통해 네이버로부터 받은 사용자 정보 파싱
        OAuthAttributes attributes = OAuthAttributes.of(userNameAttributeName, oAuth2User.getAttributes());

        // 5. DB에 사용자 정보 저장 또는 업데이트
        Member member = saveOrUpdate(attributes);
        // 6. Spring Security가 세션에 사용자 정보를 저장할 때 사용할 DTO
        // (우리는 세션을 사용하지 않지만, 인증 절차상 이 객체를 반환해야 함)
        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(), userNameAttributeName);
    }

    // DB에 사용자 정보가 없으면 새로 저장, 있으면 업데이트
    private Member saveOrUpdate(OAuthAttributes attributes) {
        Member member = memberRepository.findBySocialId(attributes.getSocialId()).map(entity -> {
            entity.requireActive();
            return entity.update(attributes.getName(), attributes.getProfileUrl());
        }).orElse(attributes.toEntity());
        return memberRepository.save(member);
    }
}
