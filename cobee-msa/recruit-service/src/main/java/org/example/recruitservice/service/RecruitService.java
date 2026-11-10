package org.example.recruitservice.service;

import lombok.RequiredArgsConstructor;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.nhn.NhnStorageService;
import org.example.common.util.ImageValidationUtil;
import org.example.recruitservice.client.MemberClient;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.map.RecruitMapFilterResponse;
import org.example.recruitservice.dto.recruit.RecruitCoreResponse;
import org.example.recruitservice.repository.RecruitRepository;
import org.example.recruitservice.domain.Enum.RecruitStatus;
import org.example.recruitservice.domain.RecruitPost;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.example.recruitservice.dto.recruit.RecruitResponse;
import org.example.recruitservice.dto.converter.RecruitConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.example.recruitservice.withdrawal.RecruitPostWriter;
import org.example.recruitservice.withdrawal.RecruitMemberLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitService {
    private final RecruitRepository recruitRepository;
    private final MemberClient memberClient;
    private final GoogleMapService googleMapService;
    private final NhnStorageService nhnStorageService;
    private final RecruitPostWriter writer;
    private final RecruitMemberLifecycle lifecycle;

    public RecruitResponse createRecruitPost(RecruitRequest recruitPost, Long memberId) {
        return writer.save(preparePost(recruitPost, memberId), List.of());
    }

    private RecruitPost preparePost(RecruitRequest recruitPost, Long memberId) {
        ApiResponse<MemberCoreResponse> memberInfoResponse = memberClient.getMyInfo(memberId);
        Map<String, Object> geocodeData = googleMapService.getGeocode(recruitPost.getAddress());
        double latitude = (double) geocodeData.get("latitude");
        double longitude = (double) geocodeData.get("longitude");
        String formattedAddress = (String) geocodeData.get("formattedAddress");

        if (memberInfoResponse == null || !memberInfoResponse.isSuccess() || memberInfoResponse.getData() == null)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "MEMBER_LOOKUP_FAILED");
        MemberCoreResponse memberInfo = memberInfoResponse.getData();
        if (!memberId.equals(memberInfo.getId()))
            throw new IllegalStateException("Member identity mismatch");
        Long ownerId = memberInfo.getId();

        RecruitPost recruit = RecruitPost.builder().ownerId(ownerId)
                .ownerName(Objects.requireNonNull(memberInfoResponse).getData().getName())
                .ownerEmail(memberInfoResponse.getData().getEmail()).title(recruitPost.getTitle())
                .recruitCount(recruitPost.getRecruitCount()).rentCostMin(recruitPost.getRentCostMin())
                .rentCostMax(recruitPost.getRentCostMax()).monthlyCostMin(recruitPost.getMonthlyCostMin())
                .monthlyCostMax(recruitPost.getMonthlyCostMax()).minAge(recruitPost.getMinAge())
                .maxAge(recruitPost.getMaxAge()).preferredGender(recruitPost.getGender())
                .lifeStyle(recruitPost.getLifestyle()).personality(recruitPost.getPersonality())
                .isSmoking(recruitPost.getIsSmoking()).isSnoring(recruitPost.getIsSnoring())
                .hasPet(recruitPost.getHasPet()).hasRoom(recruitPost.getHasRoom()).address(formattedAddress)
                .regionLatitude(latitude).regionLongitude(longitude)
                .detailDescription(recruitPost.getDetailDescription())
                .additionalDescription(recruitPost.getAdditionalDescription()).status(RecruitStatus.RECRUITING) // 구인글
                                                                                                                // 등록하자마자
                                                                                                                // 구인중으로
                                                                                                                // 변경
                .build();

        // 위도 경도 저장 - location api에서 가져와서 저장하기
        return recruit;

    }

    public RecruitResponse updateRecruit(Long postId, RecruitRequest recruitRequest, Long memberId) {
        RecruitPost post = recruitRepository.findById(postId).orElseThrow(); // 에러 처리도 해야하는데...
        Map<String, Object> geocodeData = null;
        if (recruitRequest.getAddress() != null && !recruitRequest.getAddress().equals(post.getAddress())) {
            geocodeData = googleMapService.getGeocode(recruitRequest.getAddress());
        }
        return writer.update(postId, memberId, recruitRequest, geocodeData);
    }

    public RecruitResponse getOneRecruitInfo(Long postId) {
        RecruitPost post = recruitRepository.findById(postId).orElseThrow();
        return RecruitConverter.from(post);
    }

    public List<RecruitCoreResponse> getAllRecruitInfo() {
        List<RecruitPost> recruits = recruitRepository.findAll();
        return recruits.stream().map(RecruitConverter::fromRecruit).toList();
    }

    public List<RecruitCoreResponse> getMyAllRecruitInfo(Long userId) {
        List<RecruitPost> recruits = recruitRepository.findAllByOwnerId(userId);
        return recruits.stream().map(RecruitConverter::fromRecruit).toList();
    }

    public String deleteRecruitPost(Long postId, Long memberId) {
        return writer.delete(postId, memberId);
    }

    public void deleteAllRecruitData(Long memberId) {
        var operation = java.util.UUID
                .nameUUIDFromBytes(("withdrawal:" + memberId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        lifecycle.block(memberId, operation);
        lifecycle.cleanup(memberId, operation);
    }

    public List<RecruitMapFilterResponse> getfilterRecruitPosts(Double latitude, Double longitude, Double radius,
            Integer recruitCount, Integer rentCostMin, Integer rentCostMax, Integer monthlyCostMin,
            Integer monthlyCostMax) {
        List<RecruitPost> posts;

        if (latitude != null && longitude != null && radius != null) {
            posts = recruitRepository.findFilteredRecruitPosts(latitude, longitude, radius, recruitCount, rentCostMin,
                    rentCostMax, monthlyCostMin, monthlyCostMax);
        } else {
            posts = recruitRepository.findFilteredRecruitPosts(null, null, null, recruitCount, rentCostMin, rentCostMax,
                    monthlyCostMin, monthlyCostMax);
        }
        return posts.stream().map(RecruitMapFilterResponse::new).collect(Collectors.toList());
    }

    public RecruitResponse createRecruitPostWithImages(RecruitRequest recruitRequest, Long memberId,
            MultipartFile[] images) {
        return writer.save(preparePost(recruitRequest, memberId), prepareImages(images));
    }

    public List<String> addImages(Long postId, Long memberId, MultipartFile[] files) {
        RecruitPost post = recruitRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("구인글을 찾을 수 없습니다."));
        if (!post.getOwnerId().equals(memberId)) {
            throw new RuntimeException("본인의 구인글에만 이미지를 추가할 수 있습니다.");
        }

        return writer.addImages(postId, memberId, prepareImages(files));
    }

    private List<RecruitPostWriter.PreparedImage> prepareImages(MultipartFile[] files) {
        if (files == null || files.length == 0 || files[0].isEmpty())
            return List.of();
        ImageValidationUtil.validateMultipleImageFiles(files);
        List<RecruitPostWriter.PreparedImage> prepared = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String imageUrl = nhnStorageService.uploadFile(file).block();
            if (imageUrl != null) {
                prepared.add(new RecruitPostWriter.PreparedImage(imageUrl, file.getOriginalFilename(), i + 1));
            }
        }
        return prepared;
    }
}
