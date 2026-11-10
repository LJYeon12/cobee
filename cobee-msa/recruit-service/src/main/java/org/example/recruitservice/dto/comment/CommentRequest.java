package org.example.recruitservice.dto.comment;

import lombok.Getter;

@Getter
public class CommentRequest {
    private Long parentId;
    private String content;
    private Boolean isPrivate;
}
