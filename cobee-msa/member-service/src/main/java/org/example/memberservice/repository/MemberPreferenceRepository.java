package org.example.memberservice.repository;

import org.example.memberservice.domain.MemberPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberPreferenceRepository extends JpaRepository<MemberPreference,Long> {
    boolean existsByMemberId(Long memberId);
    Optional<MemberPreference> findByMemberId(Long memberId);
}
