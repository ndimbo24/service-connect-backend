package com.serviceconnect.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private String paymentMethod;  // IPA, CARD, CASH, BANK_TRANSFER, etc.
    private String transactionId;  // Optional: reference ID from payment gateway
}
