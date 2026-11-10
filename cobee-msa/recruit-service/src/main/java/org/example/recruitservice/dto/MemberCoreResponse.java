package org.example.recruitservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class MemberCoreResponse {
    private Long id;
    private String name;
    private String email;
    private String profileUrl;
}
