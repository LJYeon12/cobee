package org.example.memberservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.memberservice.domain.Member;
import org.example.memberservice.dto.member.MemberInfoDto;
import org.example.memberservice.repository.MemberRepository;
import org.example.memberservice.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {
    private final MemberRepository memberRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${nhn.ocr.url}")
    private String apiUrl;

    @Value("${OCR_APP_KEY}")
    private String appkey;

    @Value("${nhn.ocr.secret-key}")
    private String secretKey;

    // 공개 키 발급 메서드
    private JsonNode getPublicKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", secretKey); // 헤더 이름 수정 (SecretKey 직접 사용)

        String url = apiUrl + "/v2.0/appkeys/" + appkey + "/public-keys/id-card";
        log.debug("Requesting Public Key from URL: {}", url); // 로그 추가

        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, java.net.URI.create(url));

        try {
            ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
            log.debug("Public Key Response Status: {}", response.getStatusCode()); // 로그 추가
            log.debug("Public Key Response Body: {}", response.getBody()); // 로그 추가
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching public key: {}", e.getMessage(), e); // 에러 로그 상세화
            throw new RuntimeException("공개 키를 발급받는 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public MemberInfoDto verify(Long memberId, MultipartFile image) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        try {
            // 1. 공개 키 발급
            JsonNode publicKeyResponse = getPublicKey();
            if (!publicKeyResponse.path("header").path("isSuccessful").asBoolean()) {
                throw new RuntimeException("공개 키 발급 실패: " + publicKeyResponse.path("header").path("resultMessage").asText());
            }
            String base64PublicKey = publicKeyResponse.path("result").path("key").asText();
            String publicKeyVersion = publicKeyResponse.path("result").path("version").asText();
            log.info("Public Key Version: {}", publicKeyVersion); // 로그 추가

            // 2. 대칭 키 생성 및 암호화
            SecretKey symmetricKey = CryptoUtil.generateSymmetricKey();
            String encryptedSymmetricKey = CryptoUtil.encryptSymmetricKey(symmetricKey, base64PublicKey);
            log.debug("Generated and Encrypted Symmetric Key"); // 로그 추가

            // 3. 이미지 암호화
            byte[] imageBytes = image.getBytes();
            byte[] encryptedImageBytes = CryptoUtil.encryptData(imageBytes, symmetricKey);
            log.debug("Encrypted Image Data"); // 로그 추가

            // 4. 신분증 분석 API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", secretKey); // 헤더 이름 수정
            headers.set("X-Key-Version", publicKeyVersion);
            headers.set("Symmetric-Key", encryptedSymmetricKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // 암호화된 이미지 바이트를 ByteArrayResource로 감싸서 추가
            ByteArrayResource encryptedImageResource = new ByteArrayResource(encryptedImageBytes) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename(); // 원본 파일 이름 유지
                }
            };
            body.add("image", encryptedImageResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String idCardApiUrl = apiUrl + "/v2.0/appkeys/" + appkey + "/id-card"; // API URL 수정
            log.info("Calling ID Card Analysis API: {}", idCardApiUrl); // 로그 추가

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(idCardApiUrl, requestEntity, String.class); // postForEntity 사용
            String response = responseEntity.getBody();

            log.debug("ID Card Analysis Response Status: {}", responseEntity.getStatusCode()); // 로그 추가
            log.debug("ID Card Analysis Response Body: {}", response); // 로그 추가

            // 5. 응답 처리 및 복호화
            JsonNode root = objectMapper.readTree(response);
            if (!root.path("header").path("isSuccessful").asBoolean()) {
                throw new RuntimeException("신분증 분석 실패: " + root.path("header").path("resultMessage").asText());
            }

            JsonNode keyValues = root.path("result").path("keyValues"); // 응답 구조 변경 반영

            String ocrName = "";
            String ocrResidentNumberEncrypted = ""; // 암호화된 주민번호

            for (JsonNode keyValue : keyValues) {
                String key = keyValue.path("key").asText();
                String value = keyValue.path("value").asText(); // 암호화된 값
                if ("name".equals(key)) {
                    // 이름은 암호화되지 않았다고 가정 (문서 확인 필요)
                    // 만약 이름도 암호화되어 있다면 복호화 필요
                    ocrName = CryptoUtil.decryptData(value, symmetricKey); // 복호화 추가
                } else if ("residentNumber".equals(key)) {
                    ocrResidentNumberEncrypted = value;
                }
            }

            // 주민등록번호 복호화
            String ocrResidentNumber = CryptoUtil.decryptData(ocrResidentNumberEncrypted, symmetricKey);
            log.info("Decrypted Resident Number"); // 로그 추가

            // 6. 정보 비교 및 업데이트 로직
            if (!member.getName().equals(ocrName)) {
                log.warn("Name mismatch: DB={}, OCR={}", member.getName(), ocrName); // 로그 추가
                throw new RuntimeException("이름이 일치하지 않습니다.");
            }
// 주민등록번호 파싱 및 성별/생년월일 추출
            String residentNumber = ocrResidentNumber.replace("-", "");
            if (residentNumber.length() != 13) {
                log.error("Invalid resident number length after decryption: {}", ocrResidentNumber);
                throw new RuntimeException("주민등록번호 형식이 올바르지 않습니다.");
            }
            String birthPart = residentNumber.substring(0, 6);
            String genderNum = residentNumber.substring(6, 7);
            String ocrGender = ("1".equals(genderNum) || "3".equals(genderNum)) ? "MALE" : (("2".equals(genderNum) || "4".equals(genderNum)) ? "FEMALE" : null);
            if (ocrGender == null) {
                log.error("Invalid gender digit in resident number: {}", genderNum);
                throw new RuntimeException("주민등록번호 성별 코드가 올바르지 않습니다.");
            }
            String birthYearPrefix = ("1".equals(genderNum) || "2".equals(genderNum)) ? "19" : "20";
            String ocrFullBirthDate = birthYearPrefix + birthPart.substring(0, 2) + "-" + birthPart.substring(2, 4) + "-" + birthPart.substring(4, 6); // YYYY-MM-DD 형식
            log.debug("Calculated Full Birth Date: {}, Gender: {}", ocrFullBirthDate, ocrGender);

            // 성별 비교 및 업데이트
            if (member.getGender() == null) {
                member.updateGender(ocrGender); // 주석 해제 및 호출
                log.info("Updating member gender to: {}", ocrGender);
            } else if (!member.getGender().equals(ocrGender)) {
                log.warn("Gender mismatch: DB={}, OCR={}", member.getGender(), ocrGender);
                throw new RuntimeException("성별이 일치하지 않습니다.");
            }

            // 생년월일 비교 및 업데이트
            boolean updateBirthDate = false; // 생년월일 업데이트 여부 플래그
            if (member.getBirthDate() == null) {
                updateBirthDate = true; // DB에 없으면 업데이트
                log.info("Member birth date is null. Updating to OCR birth date: {}", ocrFullBirthDate);
            } else {
                String userBirthDate = member.getBirthDate().replace("-", ""); // DB 생년월일 (YYYYMMDD, YYYY, MMDD 등)
                String ocrBirthYear = ocrFullBirthDate.substring(0, 4); // YYYY
                String ocrBirthMonthDay = ocrFullBirthDate.substring(5, 7) + ocrFullBirthDate.substring(8, 10); // MMdd
                String ocrFullBirthDateDigits = ocrBirthYear + ocrBirthMonthDay; // YYYYMMDD

                // DB값이 YYYY 형식일 때
                if (userBirthDate.length() == 4 && userBirthDate.matches("\\d{4}") && !userBirthDate.equals(ocrBirthYear)) {
                    log.warn("Birth year mismatch: DB={}, OCR={}", userBirthDate, ocrBirthYear);
                    throw new RuntimeException("생년이 일치하지 않습니다.");
                } else if (userBirthDate.length() == 4 && userBirthDate.matches("\\d{4}") && userBirthDate.equals(ocrBirthYear)) {
                    updateBirthDate = true; // 생년만 있고 일치하면 전체로 업데이트
                    log.info("Birth year matches. Updating to full birth date: {}", ocrFullBirthDate);
                }
                // DB값이 MMDD 형식일 때 (YYYY와 겹치지 않도록 정규식 강화 또는 다른 구분자 확인 필요 - 여기서는 MMDD 형식만 있다고 가정)
                // 예시: 생일이 "MM-DD" 형식으로 저장되어 있다고 가정
                else if (member.getBirthDate().matches("\\d{2}-\\d{2}")) {
                    String userMonthDay = userBirthDate; // MMdd 가정
                    if(!userMonthDay.equals(ocrBirthMonthDay)){
                        log.warn("Birthday (MMDD) mismatch: DB={}, OCR={}", userMonthDay, ocrBirthMonthDay);
                        throw new RuntimeException("생일(월일)이 일치하지 않습니다.");
                    } else {
                        updateBirthDate = true; // 월일만 있고 일치하면 전체로 업데이트
                        log.info("Birthday (MMDD) matches. Updating to full birth date: {}", ocrFullBirthDate);
                    }
                }
                // DB값이 YYYYMMDD 또는 YYYY-MM-DD 형식일 때
                else if ((userBirthDate.length() == 8 && userBirthDate.matches("\\d{8}")) || member.getBirthDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
                    String userFullBirthDateDigits = userBirthDate.replace("-",""); // YYYYMMDD
                    if (!userFullBirthDateDigits.equals(ocrFullBirthDateDigits)) {
                        log.warn("Full birth date mismatch: DB={}, OCR={}", userFullBirthDateDigits, ocrFullBirthDateDigits);
                        throw new RuntimeException("생년월일이 일치하지 않습니다.");
                    }
                }
            }

            // 조건에 맞으면 생년월일 업데이트
            if (updateBirthDate) {
                member.updateBirthDate(ocrFullBirthDate); // 주석 해제 및 호출
            }

            // ocrValidation 상태 업데이트
            member.updateOcrValidation(true); // 주석 해제 및 호출
            log.info("OCR Validation successful for memberId: {}", memberId);

            memberRepository.save(member); // 모든 변경 사항 저장

            return MemberInfoDto.from(member);

        } catch (RuntimeException re) { // 명시적 RuntimeException 처리
            log.error("OCR verification failed for memberId {}: {}", memberId, re.getMessage());
            throw re; // 예외 다시 던지기 (GlobalExceptionHandler 에서 처리하도록)
        } catch (Exception e) { // 그 외 예외 처리
            log.error("OCR verification failed for memberId {} due to unexpected error: {}", memberId, e.getMessage(), e);
            throw new RuntimeException("OCR 처리 중 오류가 발생했습니다.", e);
        }
    }
}
