package org.example.recruitservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.constant.GatewayConstant;
import org.example.recruitservice.dto.sync.ApplyRecordSyncDto;
import org.example.recruitservice.dto.sync.BookmarkSyncDto;
import org.example.recruitservice.dto.sync.CommentSyncDto;
import org.example.recruitservice.dto.sync.RecruitPostSyncDto;
import org.example.recruitservice.service.RecruitSyncService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class RecruitSyncController {

    private final RecruitSyncService recruitSyncService;

    @GetMapping("/recruit-posts")
    public ResponseEntity<List<RecruitPostSyncDto>> getUpdatedRecruitPosts(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSyncTime) {
        List<RecruitPostSyncDto> updatedPosts = recruitSyncService.getUpdatedRecruitPosts(lastSyncTime);
        return ResponseEntity.ok(updatedPosts);
    }

    @GetMapping("/apply-records")
    public ResponseEntity<List<ApplyRecordSyncDto>> getUpdatedApplyRecords(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSyncTime) {
        List<ApplyRecordSyncDto> updatedRecords = recruitSyncService.getUpdatedApplyRecords(lastSyncTime);
        return ResponseEntity.ok(updatedRecords);
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<List<BookmarkSyncDto>> getUpdatedBookmarks(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSyncTime) {
        List<BookmarkSyncDto> updatedBookmarks = recruitSyncService.getUpdatedBookmarks(lastSyncTime);
        return ResponseEntity.ok(updatedBookmarks);
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentSyncDto>> getUpdatedComments(
            @RequestHeader(GatewayConstant.GATEWAY_AUTH_HEADER) Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastSyncTime) {
        List<CommentSyncDto> updatedComments = recruitSyncService.getUpdatedComments(lastSyncTime);
        return ResponseEntity.ok(updatedComments);
    }
}
