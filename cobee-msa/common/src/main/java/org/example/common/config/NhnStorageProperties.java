package org.example.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nhn.storage")
@Getter
@Setter
public class NhnStorageProperties {
    private String identityEndpoint;
    private String storageEndpoint;
    private String tenantId;
    private String userId;
    private String password;
    private String containerName;
    private String account;
}