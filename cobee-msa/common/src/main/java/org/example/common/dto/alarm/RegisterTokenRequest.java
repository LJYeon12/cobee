package org.example.common.dto.alarm;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterTokenRequest {

    @NotBlank
    private String token;
}