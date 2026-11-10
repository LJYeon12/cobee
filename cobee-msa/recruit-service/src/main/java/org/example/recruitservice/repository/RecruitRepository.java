package org.example.recruitservice.repository;


import org.example.recruitservice.domain.RecruitPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecruitRepository extends JpaRepository<RecruitPost, Long> {
    List<RecruitPost> findAllByOwnerId(Long id);

    List<RecruitPost> findAllByUpdatedAtAfter(LocalDateTime updatedAt);

    @Query("SELECT rp FROM RecruitPost rp")
    List<RecruitPost> findAllForSync();

    @Query("SELECT p FROM RecruitPost p WHERE " +
            // 거리(반경) 필터
            "(:latitude IS NULL OR :longitude IS NULL OR :radius IS NULL OR " +
            "(6371 * acos(cos(radians(:latitude)) * cos(radians(p.regionLatitude)) * " +
            "cos(radians(p.regionLongitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(p.regionLatitude)))) <= :radius) AND " +
            // 인원수 필터
            "(:recruitCount IS NULL OR p.recruitCount <= :recruitCount) AND " +
            // 보증금 필터
            "(:rentCostMin IS NULL OR p.rentCostMin >= :rentCostMin) AND " +
            "(:rentCostMax IS NULL OR p.rentCostMax <= :rentCostMax) AND " +
            // 월세 필터
            "(:monthlyCostMin IS NULL OR p.monthlyCostMin >= :monthlyCostMin) AND " +
            "(:monthlyCostMax IS NULL OR p.monthlyCostMax <= :monthlyCostMax)")
    List<RecruitPost> findFilteredRecruitPosts(@Param("latitude") Double latitude,
                                               @Param("longitude") Double longitude,
                                               @Param("radius") Double radius,
                                               @Param("recruitCount") Integer recruitCount,
                                               @Param("rentCostMin") Integer rentCostMin,
                                               @Param("rentCostMax") Integer rentCostMax,
                                               @Param("monthlyCostMin") Integer monthlyCostMin,
                                               @Param("monthlyCostMax") Integer monthlyCostMax);
}
