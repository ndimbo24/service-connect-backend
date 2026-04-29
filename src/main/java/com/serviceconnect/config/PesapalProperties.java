package com.serviceconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "pesapal")
public class PesapalProperties {
    private String consumerKey;
    private String consumerSecret;
    private String baseUrl;
    private String ipnUrl;
    private String callbackUrl;
    private Double registrationFeeAmount;
    private Double monthlySubscriptionAmount;
}
