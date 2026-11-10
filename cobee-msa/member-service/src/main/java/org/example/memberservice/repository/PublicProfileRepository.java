package org.example.memberservice.repository;

import org.example.memberservice.domain.PublicProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicProfileRepository extends JpaRepository<PublicProfile, Long> {
}
