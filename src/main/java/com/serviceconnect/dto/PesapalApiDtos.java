package com.serviceconnect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class PesapalApiDtos {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitOrderRequest {
        private String id;
        private String currency = "TZS";
        private Double amount;
        private String description;
        @JsonProperty("callback_url")
        private String callbackUrl;
        @JsonProperty("notification_id")
        private String notificationId;
        @JsonProperty("billing_address")
        private BillingAddress billingAddress;

        @Data
        public static class BillingAddress {
            @JsonProperty("email_address")
            private String emailAddress;
            @JsonProperty("phone_number")
            private String phoneNumber;
            @JsonProperty("country_code")
            private String countryCode = "TZ";
            @JsonProperty("first_name")
            private String firstName;
            @JsonProperty("middle_name")
            private String middleName;
            @JsonProperty("last_name")
            private String lastName;
            @JsonProperty("line_1")
            private String line1;
            @JsonProperty("line_2")
            private String line2;
            private String city;
            private String state;
            @JsonProperty("postal_code")
            private String postalCode;
            @JsonProperty("zip_code")
            private String zipCode;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmitOrderResponse {
        private String status;
        private Error error;
        @JsonProperty("order_tracking_id")
        private String orderTrackingId;
        @JsonProperty("merchant_reference")
        private String merchantReference;
        @JsonProperty("redirect_url")
        private String redirectUrl;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Error {
            private String code;
            private String message;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryPaymentStatusResponse {
        private String status;
        private Error error;
        @JsonProperty("order_tracking_id")
        private String orderTrackingId;
        @JsonProperty("merchant_reference")
        private String merchantReference;
        @JsonProperty("payment_status_description")
        private String paymentStatusDescription;
        @JsonProperty("payment_account")
        private String paymentAccount;
        @JsonProperty("call_back_url")
        private String callBackUrl;
        private Double amount;
        @JsonProperty("payment_method")
        private String paymentMethod;
        @JsonProperty("created_date")
        private String createdDate;
        @JsonProperty("confirmation_code")
        private String confirmationCode;
        @JsonProperty("payment_status_code")
        private String paymentStatusCode;
        private String currency;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Error {
            private String code;
            private String message;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IpnNotification {
        @JsonProperty("OrderTrackingId")
        private String orderTrackingId;
        @JsonProperty("OrderMerchantReference")
        private String orderMerchantReference;
        @JsonProperty("OrderNotificationType")
        private String orderNotificationType;
    }
}
