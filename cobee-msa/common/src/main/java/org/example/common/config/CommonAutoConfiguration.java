package org.example.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.example.common")
public class CommonAutoConfiguration {
    // 이 Configuration이 로드되면
    // org.example.common 패키지를 스캔해서 빈들을 등록해요
}