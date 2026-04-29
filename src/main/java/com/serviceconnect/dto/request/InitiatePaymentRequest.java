package com.serviceconnect.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    @NotNull
    private Long userId;

    private Double amount;

    @NotNull
    private PaymentType type;

    public enum PaymentType {
        REGISTRATION, SUBSCRIPTION
    }
}

