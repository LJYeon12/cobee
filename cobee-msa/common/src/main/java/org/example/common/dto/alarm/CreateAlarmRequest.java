package org.example.common.dto.alarm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.common.domain.enums.AlarmSourceType;
import org.example.common.domain.enums.AlarmType;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAlarmRequest {

    @NotNull
    private AlarmType alarmType;

    @NotNull
    private AlarmSourceType sourceType;

    @NotNull
    private Long sourceId;

    @NotNull
    private Long fromUserId;

    @NotNull
    private Long toUserId;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private Map<String, String> data;
}