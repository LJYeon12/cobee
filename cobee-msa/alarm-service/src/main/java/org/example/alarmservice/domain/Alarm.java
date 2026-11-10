package org.example.alarmservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.common.domain.enums.AlarmSourceType;
import org.example.common.domain.enums.AlarmType;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmSourceType sourceType; // COMMENT / DIARY / CHATROOM

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private Long fromUserId;

}