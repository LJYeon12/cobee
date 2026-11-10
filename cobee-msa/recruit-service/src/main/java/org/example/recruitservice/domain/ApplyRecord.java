package org.example.recruitservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.common.BaseEntity;
import org.example.recruitservice.domain.Enum.MatchStatus;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ApplyRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long appliedMemberId;  // 지원자 member Id

    @Column
    @Enumerated(EnumType.STRING)
    private MatchStatus matchStatus;

    @Column
    private LocalDate submittedAt;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private RecruitPost post;

    public void acceptMatching(Boolean accept) {
        if(accept) this.matchStatus = MatchStatus.MATCHING;
        else this.matchStatus = MatchStatus.REJECTED;
    }
}