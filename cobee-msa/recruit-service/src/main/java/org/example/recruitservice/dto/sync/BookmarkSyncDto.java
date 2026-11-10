package org.example.recruitservice.dto.sync;

import lombok.Builder;
import lombok.Data;
import org.example.recruitservice.domain.Bookmark;
import org.example.common.BaseEntity;

import java.time.LocalDateTime;

@Data
@Builder
public class BookmarkSyncDto {
    private Long bookmark_id;
    private Long member_id;
    private Long recruit_post_id;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    public static BookmarkSyncDto from(Bookmark bookmark) {
        return BookmarkSyncDto.builder()
                .bookmark_id(bookmark.getId())
                .member_id(bookmark.getMemberId())
                .recruit_post_id(bookmark.getRecruitPost().getId())
                .created_at(bookmark.getCreatedAt())
                .updated_at(bookmark.getUpdatedAt())
                .build();
    }
}
