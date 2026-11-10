package org.example.common.nhn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.config.NhnStorageProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NhnStorageService {

    private final NhnStorageProperties properties;
    private final WebClient webClient = WebClient.create();

    // 토큰 캐시용 내부 클래스
    private static class NhnAuthToken {
        String tokenId;
        LocalDateTime expires;

        NhnAuthToken(String tokenId, String expires) {
            this.tokenId = tokenId;
            this.expires = ZonedDateTime.parse(expires).toLocalDateTime();
        }

        boolean isExpired() {
            // 만료 5분 전에 갱신
            return LocalDateTime.now().isAfter(expires.minusMinutes(5));
        }
    }

    private NhnAuthToken cachedToken;

    /**
     * 인증 토큰을 발급받거나 캐시된 토큰을 반환합니다.
     */
    public Mono<String> getAuthToken() {
        if (cachedToken != null && !cachedToken.isExpired()) {
            return Mono.just(cachedToken.tokenId);
        }

        // NHN API 가이드에 따른 인증 요청 본문
        Map<String, Object> authRequest = Map.of(
                "auth", Map.of(
                        "tenantId", properties.getTenantId(),
                        "passwordCredentials", Map.of(
                                "username", properties.getUserId(),
                                "password", properties.getPassword()
                        )
                )
        );

        String authUrl = properties.getIdentityEndpoint() + "/tokens";

        log.info("Requesting new NHN Auth Token...");

        return webClient.post()
                .uri(authUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .retrieve()
                .bodyToMono(Map.class) // 응답을 Map으로 받음
                .map(response -> {
                    // 사용자 cURL 응답을 기반으로 경로 파싱
                    Map<String, Object> access = (Map<String, Object>) response.get("access");
                    Map<String, Object> token = (Map<String, Object>) access.get("token");

                    String tokenId = (String) token.get("id");
                    String expires = (String) token.get("expires");

                    this.cachedToken = new NhnAuthToken(tokenId, expires);
                    log.info("Successfully fetched and cached new NHN Auth Token.");
                    return tokenId;
                })
                .doOnError(e -> log.error("Failed to get NHN Auth Token", e));
    }
    /**
     * NHN Object Storage에 파일을 업로드합니다.
     *
     * @param file 업로드할 MultipartFile
     * @return 업로드된 파일의 전체 URL
     */
    public Mono<String> uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Mono.error(new IllegalArgumentException("파일이 비어있습니다."));
        }

        // ... (1, 2번 단계는 동일) ...
        String objectName = UUID.randomUUID().toString();
        String contentType = file.getContentType();
        String uploadUrl = String.format("%s/%s/%s/%s",
                properties.getStorageEndpoint(),
                properties.getAccount(),
                properties.getContainerName(),
                objectName);

        log.info("NHN Storage Upload: {}", uploadUrl);

        // 3. 인증 토큰 가져오기 (비동기)
        return getAuthToken().flatMap(token -> {
            try {
                // 4. WebClient를 사용하여 PUT 요청
                return webClient.put()
                        .uri(uploadUrl)
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.valueOf(contentType)) // 파일의 Content-Type 설정

                        // [수정] fromBytes 대신 fromValue 를 사용합니다.
                        .body(BodyInserters.fromValue(file.getBytes())) // <--- 여기를 수정!

                        .retrieve() // 응답 받기
                        .toBodilessEntity() // 응답 본문은 필요 없음
                        .map(response -> {
                            if (response.getStatusCode().is2xxSuccessful()) {
                                log.info("File upload successful: {}", objectName);
                                return uploadUrl; // 성공 시 전체 URL 반환
                            } else {
                                log.error("File upload failed with status: {}", response.getStatusCode());
                                throw new RuntimeException("NHN Storage 파일 업로드 실패: " + response.getStatusCode());
                            }
                        });
            } catch (IOException e) {
                log.error("File reading error", e);
                return Mono.error(new RuntimeException("파일을 읽는 중 오류가 발생했습니다.", e));
            }
        });
    }
}