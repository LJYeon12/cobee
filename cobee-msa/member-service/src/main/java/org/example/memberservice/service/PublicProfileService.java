package org.example.memberservice.service;


import lombok.RequiredArgsConstructor;
import org.example.memberservice.domain.Member;
import org.example.memberservice.domain.PublicProfile;
import org.example.memberservice.dto.publicProfile.PublicProfileRequestDto;
import org.example.memberservice.dto.publicProfile.PublicProfileResponseDto;
import org.example.memberservice.dto.publicProfile.PublicProfileUpdateRequestDto;
import org.example.memberservice.repository.MemberRepository;
import org.example.memberservice.repository.PublicProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final PublicProfileRepository publicProfileRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void createPublicProfile(Long memberId, PublicProfileRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (member.getPublicProfile() != null) {
            throw new RuntimeException("Public profile already exists");
        }
        PublicProfile publicProfile = new PublicProfile(
                requestDto.lifestyle(),
                requestDto.personality(),
                requestDto.isSmoking(),
                requestDto.isSnoring(),
                requestDto.hasPet(),
                requestDto.myInfo(),
                member
        );
        member.setPublicProfile(publicProfile);
        member.setIsCompleted(true);
        publicProfileRepository.save(publicProfile);
    }
    @Transactional(readOnly = true)
    public PublicProfileResponseDto getPublicProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        PublicProfile publicProfile = member.getPublicProfile();
        if (publicProfile == null) {
            throw new RuntimeException("Public profile not found");
        }
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
    @Transactional
    public void updatePublicProfile(Long memberId, PublicProfileUpdateRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        PublicProfile publicProfile = member.getPublicProfile();
        if (publicProfile == null) {
            throw new RuntimeException("Public profile not found");
        }
        publicProfile.update(
            requestDto.lifestyle(),
            requestDto.personality(),
            requestDto.isSmoking(),
            requestDto.isSnoring(),
            requestDto.hasPet(),
            requestDto.myInfo()
        );
    }
    @Transactional
    public void deletePublicProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        PublicProfile publicProfile = member.getPublicProfile();
        if (publicProfile == null) {
            throw new RuntimeException("Public profile not found");
        }
        member.setPublicProfile(null);
        publicProfileRepository.delete(publicProfile);
    }
    @Transactional(readOnly = true)
    public PublicProfileResponseDto getPublicProfileById(Long publicProfileId) {
        PublicProfile publicProfile = publicProfileRepository.findById(publicProfileId)
                .orElseThrow(() -> new RuntimeException("Public profile not found"));
        Member member = memberRepository.findByPublicProfile(publicProfile)
                .orElseThrow(() -> new RuntimeException("Member not found"));

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
