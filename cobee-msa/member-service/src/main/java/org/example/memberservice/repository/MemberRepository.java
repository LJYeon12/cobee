package org.example.memberservice.repository;

import org.example.memberservice.domain.Member;
import org.example.memberservice.domain.PublicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findBySocialId(String socialId);
    Optional<Member> findByPublicProfile(PublicProfile publicProfile);
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.memberPreference mp LEFT JOIN FETCH m.publicProfile pp WHERE m.updatedAt > :updatedAt")
    List<Member> findAllByUpdatedAtAfter(@Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.memberPreference mp LEFT JOIN FETCH m.publicProfile pp")
    List<Member> findAllWithDetails();
}
