package org.example.recruitservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@ComponentScan(basePackages = {"org.example.recruitservice", "org.example.common"})
public class RecruitServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecruitServiceApplication.class, args);
    }

}
