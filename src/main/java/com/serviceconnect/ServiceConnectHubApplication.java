package com.serviceconnect;

import com.serviceconnect.config.PesapalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(PesapalProperties.class)
public class ServiceConnectHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceConnectHubApplication.class, args);
    }
}
