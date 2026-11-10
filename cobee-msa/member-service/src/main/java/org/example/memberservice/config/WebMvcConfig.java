package org.example.memberservice.config;

import lombok.RequiredArgsConstructor;
import org.example.common.interceptor.SyncKeyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SyncKeyInterceptor syncKeyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(syncKeyInterceptor)
                .addPathPatterns("/sync/**");
    }
}

