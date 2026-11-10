package org.example.memberservice.dto.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrValueDto {
    private String idType;
    private List<KeyValue> keyValues;

    // keyValues 리스트에서 원하는 key의 value를 쉽게 찾기 위한 헬퍼 메서드
    public String findValueByKey(String key) {
        return keyValues.stream()
                .filter(kv -> kv.getKey().equals(key))
                .map(KeyValue::getValue)
                .findFirst()
                .orElse(null);
    }
    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyValue {
        private String key;
        private String value;
    }
}
