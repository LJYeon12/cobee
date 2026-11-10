package org.example.recruitservice.dto.comment;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CommentResponse {
    private Long commentId;
    private Long postId;
    private Long parentId;
    private String username;
    private String content;
    private String profileImage;
    private Boolean isPrivate;
}
