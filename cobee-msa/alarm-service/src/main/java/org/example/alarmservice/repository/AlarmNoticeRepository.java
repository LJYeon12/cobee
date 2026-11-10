package org.example.alarmservice.repository;

import org.example.alarmservice.domain.AlarmNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmNoticeRepository extends JpaRepository<AlarmNotice, Long> {
}
