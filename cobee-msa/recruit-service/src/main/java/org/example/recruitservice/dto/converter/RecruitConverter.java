package org.example.recruitservice.dto.converter;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.example.recruitservice.client.MemberClient;
import org.example.recruitservice.domain.ApplyRecord;
import org.example.recruitservice.domain.RecruitPost;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.recruit.RecruitCoreResponse;
import org.example.recruitservice.dto.recruit.RecruitResponse;

@Builder
@RequiredArgsConstructor
public class RecruitConverter {
    public final MemberClient memberClient;


    public static RecruitResponse from(RecruitPost recruitPost) {
        return baseResponse(recruitPost, null);
    }

    public static RecruitResponse baseResponse(RecruitPost post, MemberCoreResponse member) {

        return RecruitResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .address(post.getAddress())
                .viewed(0)
                .bookmarked(0)
                .status(post.getStatus())

                .authorId(post.getOwnerId())
                .authorName(post.getOwnerName())
                .authorGender(post.getOwnerGender())
                //.birthdate(post.getMember().getBirthDate())

                .recruitCount(post.getRecruitCount())
                .hasRoom(post.getHasRoom())
                .rentalCostMin(post.getRentCostMin())
                .rentalCostMax(post.getRentCostMax())
                .monthlyCostMin(post.getMonthlyCostMin())
                .monthlyCostMax(post.getMonthlyCostMax())

                .preferredGender(post.getPreferredGender())
                .preferredMinAge(post.getMinAge())
                .preferredMaxAge(post.getMaxAge())
                .preferedLifeStyle(post.getLifeStyle())
                .preferedPersonality(post.getPersonality())
                .preferredSmoking(post.getIsSmoking())
                .preferredSnoring(post.getIsSnoring())
                .preferredHasPet(post.getHasPet())
                .latitude(post.getRegionLatitude())
                .longitude(post.getRegionLongitude())
                .detailInfo(post.getDetailDescription())
                .additionalInfo(post.getAdditionalDescription())
                .build();
    }

    public static RecruitCoreResponse fromApplyToRecruit(ApplyRecord record) {
        RecruitPost post = record.getPost();
        return RecruitCoreResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .address(post.getAddress())
                .recruitStatus(post.getStatus())
                .rentCostMin(post.getRentCostMin())
                .monthlyMinCost(post.getMonthlyCostMin())
                //.createdDate(post)
                .recruitCount(post.getRecruitCount())
                //.applicantCount() 필요하면 나중에 - 클릭못하도록
                .build();
    }

    public static RecruitCoreResponse fromRecruit(RecruitPost post) {
        return RecruitCoreResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .address(post.getAddress())
                .recruitStatus(post.getStatus())
                .rentCostMin(post.getRentCostMin())
                .monthlyMinCost(post.getMonthlyCostMin())
                .recruitCount(post.getRecruitCount())
                //.applicantCount()
                //.commentCount()
                .build();
    }
}
