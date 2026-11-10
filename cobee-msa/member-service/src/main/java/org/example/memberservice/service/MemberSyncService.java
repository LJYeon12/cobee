package org.example.memberservice.service;

import lombok.RequiredArgsConstructor;
import org.example.memberservice.dto.sync.MemberSyncDto;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSyncService {

    private final MemberRepository memberRepository;

    public List<MemberSyncDto> getUpdatedMembers(LocalDateTime lastSyncTime) {
        if (lastSyncTime == null) {
            // Initial load: get all members
            return memberRepository.findAllWithDetails().stream()
                    .map(MemberSyncDto::from)
                    .collect(Collectors.toList());
        } else {
            // Incremental load: get members updated since last sync
            return memberRepository.findAllByUpdatedAtAfter(lastSyncTime).stream()
                    .map(MemberSyncDto::from)
                    .collect(Collectors.toList());
        }
    }
}
