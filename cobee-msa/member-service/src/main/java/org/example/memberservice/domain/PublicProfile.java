package org.example.memberservice.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.common.BaseEntity;
import org.example.memberservice.domain.enums.Gender;
import org.example.memberservice.domain.enums.LifeStyle;
import org.example.memberservice.domain.enums.Personality;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    private String myInfo;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public PublicProfile(
            LifeStyle lifestyle, Personality personality,
            Boolean isSmoking, Boolean isSnoring, Boolean hasPet,
            String myInfo, Member member)
    {
        this.lifestyle = lifestyle;
        this.personality = personality;
        this.isSmoking = isSmoking;
        this.isSnoring = isSnoring;
        this.hasPet = hasPet;
        this.myInfo = myInfo;
        this.member = member;
    }
    public void update(
            LifeStyle lifestyle, Personality personality,
            Boolean isSmoking, Boolean isSnoring, Boolean hasPet,
            String myInfo)
    {
        this.lifestyle = lifestyle;
        this.personality = personality;
        this.isSmoking = isSmoking;
        this.isSnoring = isSnoring;
        this.hasPet = hasPet;
        this.myInfo = myInfo;
    }
}
