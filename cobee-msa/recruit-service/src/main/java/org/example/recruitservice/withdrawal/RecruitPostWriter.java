package org.example.recruitservice.withdrawal;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.recruitservice.domain.*;
import org.example.recruitservice.dto.recruit.*;
import org.example.recruitservice.dto.converter.RecruitConverter;
import org.example.recruitservice.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RecruitPostWriter {
    private final MemberWriteGuard guard;
    private final RecruitRepository posts;
    private final ImagesRepository images;
    public record PreparedImage(String url, String name, int order) {
    }

    @Transactional(timeout = 10)
    public RecruitResponse save(RecruitPost post, List<PreparedImage> prepared) {
        guard.requireWritable(post.getOwnerId());
        posts.save(post);
        attach(post, prepared);
        return RecruitConverter.from(post);
    }

    @Transactional(timeout = 10)
    public RecruitResponse update(long postId, long memberId, RecruitRequest request, Map<String, Object> geocode) {
        RecruitPost post = posts.findById(postId).orElseThrow();
        guard.requireWritable(memberId, post.getOwnerId());
        requireOwner(post, memberId);
        post.updatePost(request, geocode);
        posts.save(post);
        return RecruitConverter.from(post);
    }

    @Transactional(timeout = 10)
    public String delete(long postId, long memberId) {
        RecruitPost post = posts.findById(postId).orElseThrow();
        guard.requireWritable(memberId, post.getOwnerId());
        requireOwner(post, memberId);
        images.deleteAllByRecruitPost_Id(postId);
        posts.delete(post);
        return "구인글 삭제 완료";
    }

    @Transactional(timeout = 10)
    public List<String> addImages(long postId, long memberId, List<PreparedImage> prepared) {
        RecruitPost post = posts.findById(postId).orElseThrow();
        guard.requireWritable(memberId, post.getOwnerId());
        requireOwner(post, memberId);
        attach(post, prepared);
        return prepared.stream().map(PreparedImage::url).toList();
    }
    private void attach(RecruitPost post, List<PreparedImage> prepared) {
        for (var image : prepared)
            images.save(Images.builder().recruitPost(post).imageUrl(image.url()).originalName(image.name())
                    .displayOrder(image.order()).build());
    }
    private void requireOwner(RecruitPost post, long memberId) {
        if (!post.getOwnerId().equals(memberId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "POST_OWNER_REQUIRED");
    }
}
