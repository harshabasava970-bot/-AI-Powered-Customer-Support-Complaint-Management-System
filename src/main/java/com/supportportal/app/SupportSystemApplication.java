package com.supportportal.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.supportportal")
@EntityScan("com.supportportal.entity")
@EnableJpaRepositories("com.supportportal.repository")
public class SupportSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportSystemApplication.class, args);
    }
}
