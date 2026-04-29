package com.serviceconnect.service;

import com.serviceconnect.config.PesapalProperties;
import com.serviceconnect.dto.PesapalApiDtos;
import com.serviceconnect.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PesapalService {

    private final RestTemplate restTemplate;
    private final PesapalProperties properties;

    private static final String SUBMIT_ORDER_PATH = "/api/SubmitOrderRequest";
    private static final String QUERY_PAYMENT_STATUS_PATH = "/api/querypaymentstatus";

    private HttpHeaders buildHeaders(String method, String url, Map<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", generateOAuthHeader(method, url, params));
        return headers;
    }

    private String generateOAuthHeader(String method, String url, Map<String, String> params) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString();

        TreeMap<String, String> oauthParams = new TreeMap<>();
        oauthParams.put("oauth_consumer_key", properties.getConsumerKey());
        oauthParams.put("oauth_nonce", nonce);
        oauthParams.put("oauth_signature_method", "HMAC-SHA1");
        oauthParams.put("oauth_timestamp", timestamp);
        oauthParams.put("oauth_version", "1.0");

        // Add request params
        oauthParams.putAll(params);

        String signature = generateSignature(method, url, oauthParams);

        return String.format("OAuth oauth_consumer_key=\"%s\", oauth_nonce=\"%s\", oauth_signature=\"%s\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"%s\", oauth_version=\"1.0\"",
                properties.getConsumerKey(), nonce, signature, timestamp);
    }

    private String generateSignature(String method, String url, TreeMap<String, String> params) {
        try {
            String paramString = params.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                             URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .reduce((a, b) -> a + "&" + b).orElse("");

            String baseString = method.toUpperCase() + "&" +
                               URLEncoder.encode(url, StandardCharsets.UTF_8) + "&" +
                               URLEncoder.encode(paramString, StandardCharsets.UTF_8);

            String signingKey = URLEncoder.encode(properties.getConsumerSecret(), StandardCharsets.UTF_8) + "&";

            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));

            return URLEncoder.encode(Base64.getEncoder().encodeToString(signatureBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OAuth signature", e);
        }
    }

    public PesapalApiDtos.SubmitOrderResponse submitOrder(PesapalApiDtos.SubmitOrderRequest request) {
        try {
            String url = properties.getBaseUrl() + SUBMIT_ORDER_PATH;
            Map<String, String> params = new TreeMap<>();
            params.put("id", request.getId());
            params.put("currency", request.getCurrency());
            params.put("amount", String.valueOf(request.getAmount()));
            params.put("description", request.getDescription());
            params.put("callback_url", request.getCallbackUrl());
            params.put("notification_id", request.getNotificationId());

            HttpEntity<PesapalApiDtos.SubmitOrderRequest> entity = new HttpEntity<>(request, buildHeaders("POST", url, params));
            ResponseEntity<PesapalApiDtos.SubmitOrderResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PesapalApiDtos.SubmitOrderResponse.class
            );

            PesapalApiDtos.SubmitOrderResponse body = response.getBody();
            if (body == null || !"200".equals(body.getStatus())) {
                throw new BadRequestException("Failed to submit order: " + (body != null ? body.getError() : "empty response"));
            }
            return body;
        } catch (Exception e) {
            log.error("Pesapal submit order failed", e);
            throw new BadRequestException("Order submission failed: " + e.getMessage());
        }
    }

    public PesapalApiDtos.QueryPaymentStatusResponse queryPaymentStatus(String orderTrackingId) {
        try {
            String url = properties.getBaseUrl() + QUERY_PAYMENT_STATUS_PATH;
            Map<String, String> params = new TreeMap<>();
            params.put("orderTrackingId", orderTrackingId);

            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders("GET", url, params));
            ResponseEntity<PesapalApiDtos.QueryPaymentStatusResponse> response = restTemplate.exchange(
                    url + "?orderTrackingId=" + URLEncoder.encode(orderTrackingId, StandardCharsets.UTF_8),
                    HttpMethod.GET,
                    entity,
                    PesapalApiDtos.QueryPaymentStatusResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Pesapal query payment status failed for orderTrackingId={}", orderTrackingId, e);
            throw new BadRequestException("Payment status query failed: " + e.getMessage());
        }
    }
}
