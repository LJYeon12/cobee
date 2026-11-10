package org.example.recruitservice.dto.bookmark;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookmarkListResponse {
    private List<BookmarkResponse> bookmarks;
    private int totalCount;

    public static BookmarkListResponse from(List<BookmarkResponse> bookmarks) {
        return BookmarkListResponse.builder()
                .bookmarks(bookmarks)
                .totalCount(bookmarks.size())
                .build();
    }
}
