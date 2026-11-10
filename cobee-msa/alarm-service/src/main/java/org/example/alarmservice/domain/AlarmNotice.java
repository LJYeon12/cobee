package org.example.alarmservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AlarmNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Boolean isRead;

    @Column(nullable = false)
    private Long toUserId;

    @Column
    private String title;

    @Column
    private String body;

    @ManyToOne
    @JoinColumn(name="alarm_id")
    private Alarm alarm;

}
