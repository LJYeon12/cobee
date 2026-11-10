package org.example.recruitservice.withdrawal;
import lombok.RequiredArgsConstructor;
import org.example.common.domain.enums.*;
import org.example.common.dto.alarm.CreateAlarmRequest;
import org.example.recruitservice.domain.*;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.comment.*;
import org.example.recruitservice.dto.converter.CommentConverter;
import org.example.recruitservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class CommentWriter {
    private final CommentRepository comments;
    private final RecruitRepository posts;
    private final MemberWriteGuard guard;
    public record Result(CommentResponse response, CreateAlarmRequest notification) {
    }
    @Transactional(timeout = 10)
    public Result save(long postId, CommentRequest request, long memberId, MemberCoreResponse member) {
        if (!Long.valueOf(memberId).equals(member.getId()))
            throw new IllegalStateException("Member identity mismatch");
        RecruitPost post = posts.findById(postId).orElseThrow();
        Comment parent = request.getParentId() == null ? null : comments.findById(request.getParentId()).orElseThrow();
        if (parent != null && !parent.getPost().getId().equals(postId))
            throw new IllegalArgumentException("Parent belongs to another post");
        if (parent == null)
            guard.requireWritable(memberId, post.getOwnerId());
        else
            guard.requireWritable(memberId, post.getOwnerId(), parent.getCommentAuthorId());
        Comment comment = Comment.builder().post(post).parent(parent).commentAuthorId(memberId)
                .content(request.getContent()).isPrivate(request.getIsPrivate()).build();
        comments.save(comment);
        Long recipient = parent == null ? post.getOwnerId() : parent.getCommentAuthorId();
        CreateAlarmRequest alarm = null;
        if (!Long.valueOf(memberId).equals(recipient)) {
            alarm = CreateAlarmRequest.builder().alarmType(AlarmType.COMMENT).sourceType(AlarmSourceType.COMMENT)
                    .sourceId(comment.getId()).fromUserId(memberId).toUserId(recipient)
                    .title(parent == null ? "새 댓글 알림" : "대댓글 알림").body("새 댓글이 도착했습니다.").build();
        }
        return new Result(CommentConverter.toCommentResponse(comment, member), alarm);
    }
}
