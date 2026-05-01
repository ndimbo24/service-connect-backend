package com.serviceconnect.service;

import com.serviceconnect.config.PesapalProperties;
import com.serviceconnect.dto.PesapalApiDtos;
import com.serviceconnect.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PesapalService {

    private final RestTemplate restTemplate;
    private final PesapalProperties properties;

    // ── 1. Get Bearer Token ──────────────────────────────────────────
    public String getToken() {
        String url = properties.getBaseUrl() + "/api/Auth/RequestToken";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of(
            "consumer_key", properties.getConsumerKey(),
            "consumer_secret", properties.getConsumerSecret()
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
            url, new HttpEntity<>(body, headers), Map.class
        );

        if (response.getBody() == null || response.getBody().get("token") == null) {
            throw new BadRequestException("Failed to get PesaPal token");
        }

        log.info("PesaPal token obtained successfully");
        return (String) response.getBody().get("token");
    }

    // ── 2. Register IPN and get IPN ID ───────────────────────────────
    public String registerIpn() {
        String token = getToken();
        String url = properties.getBaseUrl() + "/api/URLSetup/RegisterIPN";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, String> body = Map.of(
            "url", properties.getIpnUrl(),
            "ipn_notification_type", "POST"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
            url, new HttpEntity<>(body, headers), Map.class
        );

        if (response.getBody() == null) {
            throw new BadRequestException("Failed to register IPN");
        }

        String ipnId = (String) response.getBody().get("ipn_id");
        log.info("PesaPal IPN registered successfully. IPN ID: {}", ipnId);
        return ipnId;
    }

    // ── 3. Submit Order ──────────────────────────────────────────────
    public PesapalApiDtos.SubmitOrderResponse submitOrder(PesapalApiDtos.SubmitOrderRequest request) {
        try {
            String token = getToken();
            String url = properties.getBaseUrl() + "/api/Transactions/SubmitOrderRequest";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            ResponseEntity<PesapalApiDtos.SubmitOrderResponse> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(request, headers),
                PesapalApiDtos.SubmitOrderResponse.class
            );

            PesapalApiDtos.SubmitOrderResponse body = response.getBody();
            if (body == null || !"200".equals(body.getStatus())) {
                throw new BadRequestException("Failed to submit order: " +
                    (body != null ? body.getError() : "empty response"));
            }
            return body;
        } catch (Exception e) {
            log.error("PesaPal submit order failed", e);
            throw new BadRequestException("Order submission failed: " + e.getMessage());
        }
    }

    // ── 4. Query Payment Status ──────────────────────────────────────
    public PesapalApiDtos.QueryPaymentStatusResponse queryPaymentStatus(String orderTrackingId) {
        try {
            String token = getToken();
            String url = properties.getBaseUrl() +
                "/api/Transactions/GetTransactionStatus?orderTrackingId=" + orderTrackingId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            ResponseEntity<PesapalApiDtos.QueryPaymentStatusResponse> response = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(headers),
                PesapalApiDtos.QueryPaymentStatusResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("PesaPal query payment status failed for orderTrackingId={}", orderTrackingId, e);
            throw new BadRequestException("Payment status query failed: " + e.getMessage());
        }
    }
}