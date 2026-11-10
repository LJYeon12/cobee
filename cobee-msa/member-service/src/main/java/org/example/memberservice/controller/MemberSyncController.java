package org.example.memberservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.memberservice.dto.sync.MemberSyncDto;
import org.example.memberservice.service.MemberSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class MemberSyncController {

    private final MemberSyncService memberSyncService;

    @GetMapping("/members")
    public ResponseEntity<List<MemberSyncDto>> getUpdatedMembers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSyncTime) {
        List<MemberSyncDto> updatedMembers = memberSyncService.getUpdatedMembers(lastSyncTime);
        return ResponseEntity.ok(updatedMembers);
    }
}
