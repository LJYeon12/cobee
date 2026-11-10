package org.example.recruitservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recruitservice.client.AlarmClient;
import org.example.recruitservice.client.MemberClient;
import org.example.recruitservice.domain.Comment;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.comment.CommentRequest;
import org.example.recruitservice.dto.comment.CommentResponse;
import org.example.recruitservice.dto.converter.CommentConverter;
import org.example.recruitservice.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final MemberClient memberClient;
    private final AlarmClient alarmClient;
    private final org.example.recruitservice.withdrawal.CommentWriter writer;

    public CommentResponse createComment(Long postId, CommentRequest request, Long memberId) {
        MemberCoreResponse member = memberClient.getMyInfo(memberId).getData();
        var result = writer.save(postId, request, memberId, member);
        if (result.notification() != null)
            org.example.common.AfterCommit.run(() -> alarmClient.createAlarm(result.notification()));
        return result.response();
    }

    public List<CommentResponse> getAllCommentsInfo(Long postId) {
        List<Comment> comments = commentRepository.findCommentsByPost_Id(postId);
        List<CommentResponse> commentResponses = new ArrayList<>();
        for (Comment comment : comments) {
            commentResponses.add(CommentConverter.toCommentResponse(comment,
                    memberClient.getMyInfo(comment.getCommentAuthorId()).getData()));
        }
        return commentResponses;
    }
}
