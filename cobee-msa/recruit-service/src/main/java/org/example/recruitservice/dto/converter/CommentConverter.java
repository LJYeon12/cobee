package org.example.recruitservice.dto.converter;

import lombok.Builder;
import org.example.recruitservice.domain.Comment;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.comment.CommentResponse;

@Builder
public class CommentConverter {

    public static CommentResponse toCommentResponse(Comment comment, MemberCoreResponse member) {
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        return CommentResponse.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .isPrivate(comment.getIsPrivate())
                .parentId(parentId)
                .postId(comment.getPost().getId())
                .username(member.getName())
                .profileImage(member.getProfileUrl())
                .build();
    }
}
