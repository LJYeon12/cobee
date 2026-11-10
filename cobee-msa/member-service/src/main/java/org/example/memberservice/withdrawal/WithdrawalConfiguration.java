package org.example.memberservice.withdrawal;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration
@EnableScheduling
public class WithdrawalConfiguration {
    @Bean
    feign.Request.Options withdrawalRequestOptions() {
        return new feign.Request.Options(2000, java.util.concurrent.TimeUnit.MILLISECONDS, 8000,
                java.util.concurrent.TimeUnit.MILLISECONDS, false);
    }
}
