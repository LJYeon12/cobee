package org.example.memberservice.service;

import lombok.RequiredArgsConstructor;
import org.example.common.nhn.NhnStorageService;
import org.example.common.util.ImageValidationUtil;
import org.example.memberservice.domain.Member;
import org.example.memberservice.dto.member.MemberResponseDto;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final NhnStorageService nhnStorageService;

    @Transactional(readOnly = true)
    public MemberResponseDto getMyInfo(Long memberId) {
        // Other services also use this target lookup for comment display. Actor and
        // write checks are separate.
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        return MemberResponseDto.from(member);
    }

    @Transactional
    public String updateProfileImage(Long memberId, MultipartFile file) {

        ImageValidationUtil.validateSingleImageFile(file);

        String imageUrl = nhnStorageService.uploadFile(file).block();

        if (imageUrl == null) {
            throw new RuntimeException("NHN 스토리지 업로드에 실패했습니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));

        member.update(member.getName(), imageUrl);
        return imageUrl;
    }
}
