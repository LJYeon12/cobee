package org.example.recruitservice.repository;

import org.example.recruitservice.domain.Images;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends JpaRepository<Images, Long> {
    void deleteAllByRecruitPost_Id(Long postId);
}
