package org.example.recruitservice.dto.bookmark;

import lombok.Builder;
import lombok.Getter;
import org.example.recruitservice.domain.Bookmark;
import org.example.recruitservice.domain.Enum.Gender;
import org.example.recruitservice.domain.Enum.LifeStyle;
import org.example.recruitservice.domain.Enum.Personality;
import org.example.recruitservice.domain.Enum.RecruitStatus;
import org.example.recruitservice.domain.RecruitPost;

import java.time.LocalDateTime;

@Getter
@Builder
public class BookmarkResponse {
    private Long postId;
    private Long bookmarkId;
    private String title;
    private RecruitStatus status;
    private String authorName;
    private Integer recruitCount;
    private Integer rentalCostMin;
    private Integer rentalCostMax;
    private Integer monthlyCostMin;
    private Integer monthlyCostMax;
    private Gender preferedGender;
    private Integer preferedMinAge;
    private Integer preferedMaxAge;
    private LifeStyle preferedLifeStyle;
    private Personality preferedPersonality;
    private Boolean preferedSmoking;
    private Boolean preferedSnoring;
    private Boolean preferedHasPet;
    private String address;
    private LocalDateTime createdAt;

    public static BookmarkResponse from(RecruitPost post, Bookmark bookmark) {
        return BookmarkResponse.builder()
                .postId(post.getId())
                .bookmarkId(bookmark.getId())
                .title(post.getTitle())
                .status(post.getStatus())
                .authorName(post.getOwnerName())
                .recruitCount(post.getRecruitCount())
                .rentalCostMin(post.getRentCostMin())
                .rentalCostMax(post.getRentCostMax())
                .monthlyCostMin(post.getMonthlyCostMin())
                .monthlyCostMax(post.getMonthlyCostMax())
                .preferedGender(post.getPreferredGender())
                .preferedMinAge(post.getMinAge())
                .preferedMaxAge(post.getMaxAge())
                .preferedLifeStyle(post.getLifeStyle())
                .preferedPersonality(post.getPersonality())
                .preferedSmoking(post.getIsSmoking())
                .preferedSnoring(post.getIsSnoring())
                .preferedHasPet(post.getHasPet())
                .address(post.getAddress())
                .createdAt(bookmark.getCreatedAt())
                .build();
    }
}

