package org.example.recruitservice.dto.recruit;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.recruitservice.domain.Enum.Gender;
import org.example.recruitservice.domain.Enum.LifeStyle;
import org.example.recruitservice.domain.Enum.Personality;
import org.example.recruitservice.domain.Enum.RecruitStatus;
import org.example.recruitservice.domain.RecruitPost;
import org.example.recruitservice.dto.comment.CommentResponse;

import java.util.List;

@Getter
@Builder
public class RecruitResponse{
    /* 제목 */
    private Long postId;
    private String title;
    private Integer viewed;
    private Integer bookmarked;
    private RecruitStatus status;

    /* 작성자 정보 */
    private Long authorId;
    private String authorName;
    private Gender authorGender;
    private String birthdate; // 나이 변환은 프론트에서??

    /* 구인글 정보 */
    private Integer recruitCount;
    private Boolean hasRoom;
    private Integer rentalCostMin;
    private Integer rentalCostMax;
    private Integer monthlyCostMin;
    private Integer monthlyCostMax;

    /* 구인글 메이트 선호 정보 */
    private Gender preferredGender;
    private Integer preferredMinAge;
    private Integer preferredMaxAge;
    private LifeStyle preferedLifeStyle;
    private Personality preferedPersonality;
    private Boolean preferredSmoking;
    private Boolean preferredSnoring;
    private Boolean preferredHasPet;

    /* 지도 정보 */
    private String address;
    private Double latitude;
    private Double longitude;

    /* 추가 정보 */
    private String detailInfo;
    private String additionalInfo;
    private int applicantCount;

    /* 나중에 추가해야하는 것
     * private String firstImage;
     * private String authorProfileImg; */
    private List<String> imgUrl;
}