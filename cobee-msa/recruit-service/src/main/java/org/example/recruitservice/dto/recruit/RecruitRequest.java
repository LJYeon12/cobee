package org.example.recruitservice.dto.recruit;

import lombok.Getter;
import org.example.recruitservice.domain.Enum.Gender;
import org.example.recruitservice.domain.Enum.LifeStyle;
import org.example.recruitservice.domain.Enum.Personality;

import java.util.List;

@Getter
public class RecruitRequest {
    private String title;
    private Integer recruitCount;
    private Integer rentCostMin;
    private Integer rentCostMax;
    private Integer monthlyCostMin;
    private Integer monthlyCostMax;
    private Integer minAge;
    private Integer maxAge;
    private Gender gender;
    private LifeStyle lifestyle;
    private Personality personality;
    private Boolean isSmoking;
    private Boolean isSnoring;
    private Boolean hasPet;
    private Boolean hasRoom;
    private List<String> imgUrl;
    private String address;
    private String detailDescription;
    private String additionalDescription;
}