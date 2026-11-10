package org.example.recruitservice.dto.map;

import lombok.Getter;
import org.example.recruitservice.domain.RecruitPost;

@Getter
public class RecruitMapFilterResponse {
    private Long postId;
    private String title;
    private String name;
    private String recruitAddress;
    private Double recruitLatitude;
    private Double recruitLongitude;
    private String recruitDescription;

    public RecruitMapFilterResponse(RecruitPost post) {
        this.postId = post.getId();
        this.name= post.getOwnerName();
        this.title = post.getTitle();
        this.recruitAddress = post.getAddress();
        this.recruitLatitude = post.getRegionLatitude();
        this.recruitLongitude = post.getRegionLongitude();
        this.recruitDescription = post.getDetailDescription();
    }
}
