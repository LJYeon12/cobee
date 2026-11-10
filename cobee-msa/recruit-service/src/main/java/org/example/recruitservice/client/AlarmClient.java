package org.example.recruitservice.client;

import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.dto.alarm.CreateAlarmRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "alarm-service")
public interface AlarmClient {

    @PostMapping("/alarm/internal")
    ApiResponse<Void> createAlarm(@RequestBody CreateAlarmRequest request);
}

