package org.example.recruitservice.withdrawal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.common.domain.enums.*;
import org.example.common.dto.alarm.CreateAlarmRequest;
import org.example.recruitservice.domain.*;
import org.example.recruitservice.domain.Enum.MatchStatus;
import org.example.recruitservice.dto.apply.*;
import org.example.recruitservice.dto.converter.ApplyConverter;
import org.example.recruitservice.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(timeout = 10)
public class ApplyWriter {
    private final ApplyRepository applies;
    private final RecruitRepository posts;
    private final MemberWriteGuard guard;
    public record Result(ApplyResponse response, List<CreateAlarmRequest> notifications) {
    }

    public Result apply(long memberId, long postId) {
        RecruitPost post = posts.findById(postId).orElseThrow();
        guard.requireWritable(memberId, post.getOwnerId());
        if (applies.findApplyRecordsByAppliedMemberIdAndPostId(memberId, postId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ALREADY_APPLIED");
        var apply = ApplyRecord.builder().appliedMemberId(memberId).post(post).matchStatus(MatchStatus.ON_WAIT)
                .submittedAt(LocalDate.now()).build();
        applies.save(apply);
        var alarm = CreateAlarmRequest.builder().alarmType(AlarmType.NEW_APPLY).sourceType(AlarmSourceType.RECRUIT_POST)
                .sourceId(postId).fromUserId(memberId).toUserId(post.getOwnerId()).title("새로운 지원 알림")
                .body("새로운 지원이 도착했습니다.").build();
        return new Result(ApplyConverter.from(apply), List.of(alarm));
    }

    public Result accept(ApplyAcceptRequest request, long memberId) {
        ApplyRecord apply = applies.findById(request.getApplyId()).orElseThrow();
        RecruitPost post = apply.getPost();
        guard.requireWritable(memberId, apply.getAppliedMemberId(), post.getOwnerId());
        if (!post.getOwnerId().equals(memberId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "POST_OWNER_REQUIRED");
        if (request.getIsAccepted() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ACCEPTANCE_REQUIRED");
        boolean accepted = request.getIsAccepted();
        apply.acceptMatching(accepted);
        applies.save(apply);
        List<CreateAlarmRequest> notifications = new ArrayList<>();
        notifications.add(CreateAlarmRequest.builder().alarmType(AlarmType.START_MATCHING)
                .sourceType(AlarmSourceType.RECRUIT_POST).sourceId(post.getId()).fromUserId(memberId)
                .toUserId(apply.getAppliedMemberId()).title(accepted ? "매칭 성공" : "매칭 거절")
                .body(accepted ? "매칭이 성사되었습니다." : "매칭이 거절되었습니다.").build());
        if (accepted)
            notifications
                    .add(CreateAlarmRequest.builder().alarmType(AlarmType.INVITED).sourceType(AlarmSourceType.CHATROOM)
                            .sourceId(apply.getId()).fromUserId(memberId).toUserId(apply.getAppliedMemberId())
                            .title("채팅방 초대").body(post.getTitle() + " 채팅방에 초대되었어요.").build());
        return new Result(ApplyConverter.from(apply), List.copyOf(notifications));
    }
}
