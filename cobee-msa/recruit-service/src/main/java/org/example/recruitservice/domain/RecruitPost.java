package org.example.recruitservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.common.BaseEntity;
import org.example.recruitservice.domain.Enum.Gender;
import org.example.recruitservice.domain.Enum.LifeStyle;
import org.example.recruitservice.domain.Enum.Personality;
import org.example.recruitservice.domain.Enum.RecruitStatus;
import org.example.recruitservice.dto.recruit.RecruitRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class RecruitPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long ownerId;

    @Column
    private Gender ownerGender;

    @Column
    private String ownerName;

    @Column
    private String ownerEmail;

    @Column
    private String title;

    @Column
    private Integer recruitCount;

    @Column
    private Integer rentCostMin;

    @Column
    private Integer rentCostMax;

    @Column
    private Integer monthlyCostMin;

    @Column
    private Integer monthlyCostMax;

    @Column
    private Integer minAge;

    @Column
    private Integer maxAge;

    @Column
    @Enumerated(EnumType.STRING)
    private Gender preferredGender;

    @Column
    @Enumerated(EnumType.STRING)
    private LifeStyle lifeStyle;

    @Column
    @Enumerated(EnumType.STRING)
    private Personality personality;

    @Column
    private Boolean isSmoking;

    @Column
    private Boolean isSnoring;

    @Column
    private Boolean hasPet;

    @Column
    private Boolean hasRoom;

    @Column
    private String address;

    @Column(columnDefinition = "TEXT")
    private String detailDescription;

    @Column(columnDefinition = "TEXT")
    private String additionalDescription;

    @Column
    @Enumerated(EnumType.STRING)
    private RecruitStatus status;

    @Column
    private Double regionLatitude; // 위도

    @Column
    private Double regionLongitude; // 경도

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<ApplyRecord> applyRecords = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments;

    @OneToMany(mappedBy = "recruitPost", cascade = CascadeType.ALL)
    private List<Bookmark> bookmarks = new ArrayList<>();

    public void updatePost(RecruitRequest dto, Map<String, Object> geocodeData) {
        if (dto.getTitle() != null) {
            this.title = dto.getTitle();
        }
        if (dto.getRecruitCount() != null) {
            this.recruitCount = dto.getRecruitCount();
        }
        if (dto.getRentCostMin() != null) {
            this.rentCostMin = dto.getRentCostMin();
        }
        if (dto.getRentCostMax() != null) {
            this.rentCostMax = dto.getRentCostMax();
        }
        if (dto.getMonthlyCostMin() != null) {
            this.monthlyCostMin = dto.getMonthlyCostMin();
        }
        if (dto.getMonthlyCostMax() != null) {
            this.monthlyCostMax = dto.getMonthlyCostMax();
        }
        if (dto.getMinAge() != null) {
            this.minAge = dto.getMinAge();
        }
        if (dto.getMaxAge() != null) {
            this.maxAge = dto.getMaxAge();
        }
        if(dto.getGender() != null) {
            this.preferredGender = dto.getGender();
        }
        if (dto.getLifestyle() != null) {
            this.lifeStyle = dto.getLifestyle();
        }
        if (dto.getPersonality() != null) {
            this.personality = dto.getPersonality();
        }
        if (dto.getIsSmoking() != null) {
            this.isSmoking = dto.getIsSmoking();
        }
        if (dto.getIsSnoring() != null) {
            this.isSnoring = dto.getIsSnoring();
        }
        if (dto.getHasPet() != null) {
            this.hasPet = dto.getHasPet();
        }
        if (dto.getHasRoom() != null) {
            this.hasRoom = dto.getHasRoom();
        }
        if (dto.getAddress() != null) {
            this.address = dto.getAddress();
        }
        if (dto.getDetailDescription() != null) {
            this.detailDescription = dto.getDetailDescription();
        }
        if (dto.getAdditionalDescription() != null) {
            this.additionalDescription = dto.getAdditionalDescription();
        }
        if (geocodeData != null) {
            this.address = (String) geocodeData.get("formattedAddress");
            this.regionLatitude = (Double) geocodeData.get("latitude");
            this.regionLongitude = (Double) geocodeData.get("longitude");
        }
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

}