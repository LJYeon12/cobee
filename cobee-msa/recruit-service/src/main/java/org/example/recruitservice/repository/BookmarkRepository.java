package org.example.recruitservice.repository;

import org.example.recruitservice.domain.Bookmark;
import org.example.recruitservice.domain.RecruitPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByMemberIdAndRecruitPost(Long memberId, RecruitPost recruitPost);

    @Query("SELECT b FROM Bookmark b JOIN FETCH b.recruitPost WHERE b.memberId = :memberId ORDER BY b.createdAt DESC")
    List<Bookmark> findAllByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    Optional<Bookmark> findById(Long bookmarkId);

    int deleteByMemberId(Long memberId);

    List<Bookmark> findAllByUpdatedAtAfter(java.time.LocalDateTime updatedAt);

    @Query("SELECT b FROM Bookmark b")
    List<Bookmark> findAllForSync();
}
