package org.example.recruitservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recruitservice.domain.Bookmark;
import org.example.recruitservice.domain.RecruitPost;
import org.example.recruitservice.dto.bookmark.BookmarkListResponse;
import org.example.recruitservice.dto.bookmark.BookmarkResponse;
import org.example.recruitservice.repository.BookmarkRepository;
import org.example.recruitservice.repository.RecruitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final RecruitRepository recruitRepository;
    private final BookmarkRepository bookmarkRepository;
    private final org.example.recruitservice.withdrawal.MemberWriteGuard guard;

    @Transactional(timeout = 10)
    public BookmarkResponse addBookmark(Long memberId, Long postId) {
        RecruitPost recruitPost = recruitRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Recruit not found"));
        guard.requireWritable(memberId, recruitPost.getOwnerId());
        checkExceptionBookmark(memberId, recruitPost);
        Bookmark bookmark = Bookmark.builder().memberId(memberId).recruitPost(recruitPost).build();
        bookmarkRepository.save(bookmark);
        return BookmarkResponse.from(recruitPost, bookmark);
    }

    public void checkExceptionBookmark(Long memberId, RecruitPost recruitPost) {
        if (bookmarkRepository.existsByMemberIdAndRecruitPost(memberId, recruitPost)) {
            throw new RuntimeException("Bookmark already exists");
        }
        if (recruitPost.getOwnerId().equals(memberId)) {
            throw new RuntimeException("Owner not permitted");
        }
    }

    @Transactional(readOnly = true)
    public BookmarkListResponse getBookmarkList(Long memberId) {
        List<Bookmark> bookmarks = bookmarkRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
        List<BookmarkResponse> bookmarkResponses = bookmarks.stream()
                .map(bookmark -> BookmarkResponse.from(bookmark.getRecruitPost(), bookmark))
                .collect(Collectors.toList());
        return BookmarkListResponse.from(bookmarkResponses);
    }

    @Transactional(timeout = 10)
    public BookmarkResponse deleteBookmark(Long bookmarkId, Long memberId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new RuntimeException("Bookmark not found"));
        guard.requireWritable(memberId, bookmark.getRecruitPost().getOwnerId());
        if (!bookmark.getMemberId().equals(memberId)) {
            throw new RuntimeException("Member not permitted");
        }
        BookmarkResponse bookmarkResponse = BookmarkResponse.from(bookmark.getRecruitPost(), bookmark);
        bookmarkRepository.delete(bookmark);
        return bookmarkResponse;
    }

    @Transactional(timeout = 10)
    public int deleteAllBookmark(Long memberId) {
        guard.requireWritable(memberId);
        return bookmarkRepository.deleteByMemberId(memberId);
    }
}
