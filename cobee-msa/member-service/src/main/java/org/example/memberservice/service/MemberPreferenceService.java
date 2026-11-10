package org.example.memberservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.memberservice.domain.Member;
import org.example.memberservice.domain.MemberPreference;
import org.example.memberservice.dto.memberPreferences.MemberPreferencesRequestDto;
import org.example.memberservice.dto.memberPreferences.MemberPreferencesResponseDto;
import org.example.memberservice.repository.MemberPreferenceRepository;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberPreferenceService {
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final MemberRepository memberRepository;

    public MemberPreferencesResponseDto createMemberPreferences(Long memberId, MemberPreferencesRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (memberPreferenceRepository.existsByMemberId(memberId)) {
            throw new RuntimeException("Member already exists");
        }
        MemberPreference memberPreference = MemberPreference.builder()
                .gender(requestDto.getPreferredGender())
                .lifestyle(requestDto.getLifestyle())
                .personality(requestDto.getPersonality())
                .isSmoking(requestDto.getSmokingPreference())
                .isSnoring(requestDto.getSnoringPreference())
                .hasPet(requestDto.getPetPreference())
                .cohabitantCount(requestDto.getCohabitantCount())
                .preferredAgeMin(requestDto.getPreferredAgeMin())
                .preferredAgeMax(requestDto.getPreferredAgeMax())
                .member(member)
                .build();
        MemberPreference savedPreferences = memberPreferenceRepository.save(memberPreference);
        log.info("사용자 선호도 등록 완료 - memberId: {}, preferencesId: {}", memberId, savedPreferences.getId());
        return MemberPreferencesResponseDto.from(savedPreferences);
    }
    @Transactional(readOnly = true)
    public MemberPreferencesResponseDto getMemberPreferences(Long memberId) {
        MemberPreference memberPreference = memberPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("MemberPreference not found"));
        return MemberPreferencesResponseDto.from(memberPreference);
    }

    public MemberPreferencesResponseDto updateMemberPreferences(
            Long memberId, MemberPreferencesRequestDto requestDto) {
        MemberPreference memberPreference = memberPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        memberPreference.updatePreferences(
                requestDto.getPreferredGender(),
                requestDto.getLifestyle(),
                requestDto.getPersonality(),
                requestDto.getSmokingPreference(),
                requestDto.getSnoringPreference(),
                requestDto.getPetPreference(),
                requestDto.getCohabitantCount(),
                requestDto.getPreferredAgeMin(),
                requestDto.getPreferredAgeMax()
        );
        MemberPreference updatePreferences = memberPreferenceRepository.save(memberPreference);
        log.info("사용자 선호도 수정 완료 - memberId: {}, preferencesId: {}", memberId, updatePreferences.getId());
        return MemberPreferencesResponseDto.from(updatePreferences);
    }
    @Transactional
    public void deleteMemberPreferences(Long memberId) {
        MemberPreference memberPreference = memberPreferenceRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        memberPreferenceRepository.delete(memberPreference);
        log.info("사용자 선호도 삭제 완료 - memberId: {}, preferencesId: {}", memberId, memberPreference.getId());
    }
}
