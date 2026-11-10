package org.example.memberservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.example.common.BaseEntity;
import org.example.memberservice.domain.enums.Gender;
import org.example.memberservice.domain.enums.LifeStyle;
import org.example.memberservice.domain.enums.Personality;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPreference extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column
    @Enumerated(EnumType.STRING)
    private LifeStyle lifestyle;

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
    @Min(2)
    @Max(10)
    private Integer cohabitantCount;
    @Column
    private Integer preferredAgeMin;
    @Column
    private Integer preferredAgeMax;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void updatePreferences(
            Gender gender, LifeStyle lifestyle, Personality personality,
            Boolean isSmoking, Boolean isSnoring, Boolean hasPet,Integer cohabitantCount,
            Integer preferredAgeMin, Integer preferredAgeMax
    ) {
        this.gender = gender;
        this.lifestyle = lifestyle;
        this.personality = personality;
        this.isSmoking = isSmoking;
        this.isSnoring = isSnoring;
        this.hasPet = hasPet;
        this.cohabitantCount = cohabitantCount;
        this.preferredAgeMin = preferredAgeMin;
        this.preferredAgeMax = preferredAgeMax;
    }
}
