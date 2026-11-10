package org.example.recruitservice.repository;

import org.example.recruitservice.domain.ApplyRecord;
import org.example.recruitservice.domain.Enum.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplyRepository extends JpaRepository<ApplyRecord, Long> {
    // 나를 제외한 구인글 지원자 조회
    @Query("""
        select a
        from ApplyRecord a
        where a.post.id = :postId
          and a.appliedMemberId <> :memberId
        """)
    List<ApplyRecord> findMyPostAppliersExceptMe(@Param("postId") Long postId,
                                                 @Param("memberId") Long memberId);

    // 나를 제외한 구인글 지원자 조회 with Status
    @Query("""
        select a
        from ApplyRecord a
        where a.post.id = :postId
          and a.appliedMemberId <> :memberId
                  and a.matchStatus = :status
        """)
    List<ApplyRecord> findMyPostAppliersExceptMeWithStatus(@Param("postId") Long postId,
                                                           @Param("memberId") Long memberId,
                                                           @Param("status") MatchStatus status);

    // 내가 쓴 글이 아닌 글에 지원한 모든 경우
    @Query("""
        select a
        from ApplyRecord a
        where a.post.ownerId <> :memberId
                          and a.appliedMemberId = :memberId
        """)
    List<ApplyRecord> findApplyRecordsByAppliedMemberId(@Param("memberId") Long memberId);

    // 내가 쓴 구인글은 제외하도록 조회 + 상태에 따른 조회
    @Query("""
        select a
        from ApplyRecord a
        where a.post.ownerId <> :memberId
                  and a.matchStatus = :status
                          and a.appliedMemberId = :memberId
        """)
    List<ApplyRecord> findApplyRecordsByAppliedMemberIdAndMatchStatus(@Param("memberId") Long memberId, @Param("status") MatchStatus status);

    // 지원했는지 여부 체크하는 메서드
    Optional<ApplyRecord> findApplyRecordsByAppliedMemberIdAndPostId(@Param("memberId") Long memberId, @Param("postId") Long postId);

    List<ApplyRecord> findAllByUpdatedAtAfter(java.time.LocalDateTime updatedAt);

    @Query("SELECT ar FROM ApplyRecord ar")
    List<ApplyRecord> findAllForSync();
}
