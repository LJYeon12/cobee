package org.example.recruitservice.dto.sync;

import lombok.Builder;
import lombok.Data;
import org.example.recruitservice.domain.Comment;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentSyncDto {
    private Long comment_id;
    private Long member_id;
    private Long recruit_post_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public static CommentSyncDto from(Comment comment) {
        return CommentSyncDto.builder()
                .comment_id(comment.getId())
                .member_id(comment.getCommentAuthorId())
                .recruit_post_id(comment.getPost().getId())
                .created_at(comment.getCreatedAt())
                .updated_at(comment.getUpdatedAt())
                .build();
    }
}