package org.example.alarmservice.service;

import lombok.RequiredArgsConstructor;
import org.example.alarmservice.repository.AlarmRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmService {
    private final AlarmRepository alarmRepository;
}
