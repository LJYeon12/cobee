package org.example.alarmservice.client;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NhnPushClient {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${nhn.push.base-url}")
    private String baseUrl;

    @Value("${nhn.push.app-key}")
    private String appKey;

    @Value("${nhn.push.secret-key}")
    private String secretKey;

    /**
     * NHN에 토큰을 사전 등록(바인딩)하는 API.
     * 엔드포인트/스키마는 NHN v2.4 문서를 따름.
     */
    @Retry(name = "nhnPush") @CircuitBreaker(name = "nhnPush")
    public Mono<Void> registerToken(String userId, String token) {
        // TODO: 토큰 등록에 필요한 추가 필드(isNotificationAgreement 등) 처리 필요
        Map<String, Object> body = Map.of(
                "uid", userId,
                "token", token,
                "pushType", "FCM"     // iOS라면 APNS
        );

        String path = String.format("%s/push/v2.4/appkeys/%s/tokens", baseUrl, appKey);

        return webClient.post()
                .uri(path)
                .header("X-Secret-Key", secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("NHN token registered userId={}, status={}", userId, r.getStatusCode()))
                .then();
    }

    /**
     * userId 대상으로 발송 (사전 바인딩이 된 경우)
     */
    @Retry(name = "nhnPush") @CircuitBreaker(name = "nhnPush")
    public Mono<Void> sendByUserIds(List<String> userIds, String title, String body, Map<String, String> data) {
        Map<String, Object> payload = Map.of(
                "target", Map.of(
                        "type", "UID",
                        "to", userIds
                ),
                "content", Map.of(
                        "default", Map.of("title", title, "body", body)
                        // TODO: android, ios specific content if needed. For custom data, use customKey field inside default.
                ),
                "messageType", "NOTIFICATION"
        );

        String path = String.format("%s/push/v2.4/appkeys/%s/messages", baseUrl, appKey);

        return webClient.post()
                .uri(path)
                .header("X-Secret-Key", secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("NHN push sent to userIds ({}), status={}", userIds, r.getStatusCode()))
                .then();
    }
}

