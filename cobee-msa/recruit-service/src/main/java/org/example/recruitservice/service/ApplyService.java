package org.example.recruitservice.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.recruitservice.client.AlarmClient;
import org.example.recruitservice.domain.ApplyRecord;
import org.example.recruitservice.domain.Enum.MatchStatus;
import org.example.recruitservice.dto.apply.ApplicantResponse;
import org.example.recruitservice.dto.apply.ApplyAcceptRequest;
import org.example.recruitservice.dto.apply.ApplyResponse;
import org.example.recruitservice.dto.recruit.RecruitCoreResponse;
import org.example.recruitservice.dto.converter.ApplyConverter;
import org.example.recruitservice.dto.converter.RecruitConverter;
import org.example.recruitservice.repository.ApplyRepository;
import org.springframework.stereotype.Service;

import org.example.recruitservice.withdrawal.ApplyWriter;
import org.example.common.AfterCommit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplyService {
    private final ApplyRepository applyRepository;
    private final AlarmClient alarmClient;
    private final ApplyWriter writer;

    public ApplyResponse applyForRecruit(Long memberId, Long postId) {
        return deliver(writer.apply(memberId, postId));
    }

    public ApplyResponse acceptOrReject(ApplyAcceptRequest request, Long memberId) {
        return deliver(writer.accept(request, memberId));
    }

    private ApplyResponse deliver(ApplyWriter.Result result) {
        for (var notification : result.notifications())
            AfterCommit.run(() -> alarmClient.createAlarm(notification));
        return result.response();
    }

    @Transactional(readOnly = true)
    public List<ApplicantResponse> getMyApplicants(Long postId, Long memberId, MatchStatus matchStatus) {
        List<ApplyRecord> applyRecords = (matchStatus == null)
                ? applyRepository.findMyPostAppliersExceptMe(postId, memberId)
                : applyRepository.findMyPostAppliersExceptMeWithStatus(postId, memberId, matchStatus);
        return applyRecords.stream().map(ApplyConverter::applicantConverter).toList();
    }

    @Transactional(readOnly = true)
    public List<RecruitCoreResponse> getMyAppliedPostsByMatchStatus(Long memberId, MatchStatus matchStatus) {
        List<ApplyRecord> applyRecords = (matchStatus == null)
                ? applyRepository.findApplyRecordsByAppliedMemberId(memberId)
                : applyRepository.findApplyRecordsByAppliedMemberIdAndMatchStatus(memberId, matchStatus);
        return applyRecords.stream().map(RecruitConverter::fromApplyToRecruit).toList();
    }

    @Transactional(readOnly = true)
    public Boolean checkIfIAppliedThisPost(Long postId, Long memberId) {
        Optional<ApplyRecord> apply = applyRepository.findApplyRecordsByAppliedMemberIdAndPostId(postId, memberId);
        return apply.isPresent();
    }

}
