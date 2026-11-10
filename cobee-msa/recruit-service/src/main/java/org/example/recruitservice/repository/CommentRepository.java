package org.example.recruitservice.repository;

import org.example.recruitservice.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findCommentsByPost_Id(Long postId);

    List<Comment> findAllByUpdatedAtAfter(LocalDateTime updatedAt);

    @Query("SELECT c FROM Comment c")
    List<Comment> findAllForSync();
}
