package org.example.recruitservice.service;

import lombok.RequiredArgsConstructor;
import org.example.recruitservice.dto.sync.ApplyRecordSyncDto;
import org.example.recruitservice.dto.sync.BookmarkSyncDto;
import org.example.recruitservice.dto.sync.CommentSyncDto;
import org.example.recruitservice.dto.sync.RecruitPostSyncDto;
import org.example.recruitservice.repository.ApplyRepository;
import org.example.recruitservice.repository.BookmarkRepository;
import org.example.recruitservice.repository.CommentRepository;
import org.example.recruitservice.repository.RecruitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitSyncService {

    private final RecruitRepository recruitRepository;
    private final ApplyRepository applyRepository;
    private final BookmarkRepository bookmarkRepository;
    private final CommentRepository commentRepository;

    public List<RecruitPostSyncDto> getUpdatedRecruitPosts(LocalDateTime lastSyncTime) {
        if (lastSyncTime == null) {
            return recruitRepository.findAllForSync().stream()
                    .map(RecruitPostSyncDto::from)
                    .collect(Collectors.toList());
        }
        return recruitRepository.findAllByUpdatedAtAfter(lastSyncTime).stream()
                .map(RecruitPostSyncDto::from)
                .collect(Collectors.toList());
    }

    public List<ApplyRecordSyncDto> getUpdatedApplyRecords(LocalDateTime lastSyncTime) {
        if (lastSyncTime == null) {
            return applyRepository.findAllForSync().stream()
                    .map(ApplyRecordSyncDto::from)
                    .collect(Collectors.toList());
        }
        return applyRepository.findAllByUpdatedAtAfter(lastSyncTime).stream()
                .map(ApplyRecordSyncDto::from)
                .collect(Collectors.toList());
    }

    public List<BookmarkSyncDto> getUpdatedBookmarks(LocalDateTime lastSyncTime) {
        if (lastSyncTime == null) {
            return bookmarkRepository.findAllForSync().stream()
                    .map(BookmarkSyncDto::from)
                    .collect(Collectors.toList());
        }
        return bookmarkRepository.findAllByUpdatedAtAfter(lastSyncTime).stream()
                .map(BookmarkSyncDto::from)
                .collect(Collectors.toList());
    }

    public List<CommentSyncDto> getUpdatedComments(LocalDateTime lastSyncTime) {
        if (lastSyncTime == null) {
            return commentRepository.findAllForSync().stream()
                    .map(CommentSyncDto::from)
                    .collect(Collectors.toList());
        }
        return commentRepository.findAllByUpdatedAtAfter(lastSyncTime).stream()
                .map(CommentSyncDto::from)
                .collect(Collectors.toList());
    }
}
