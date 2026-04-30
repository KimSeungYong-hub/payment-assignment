package com.practice.paymentassignment.dto;

import com.practice.paymentassignment.domain.payment.PaymentRequestEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

public class PaymentDto {

    // 1. 결제 준비 API 전용
    public static class Prepare {
        @Getter
        @RequiredArgsConstructor
        public static class Request {
            private final Long userId;
            private final Long merchantId;
        }

        @Getter
        @RequiredArgsConstructor
        public static class Response {
            private final String merchantName;
            private final BigDecimal amount;
            private final Long paymentId;

            public static Response from(PaymentRequestEntity paymentRequest) {
                return new Response(
                        paymentRequest.getMerchant().getMerchantName(),
                        paymentRequest.getTotalAmount(),
                        paymentRequest.getId()
                );
            }
        }
    }

    // 2. 결제 승인 API 전용
    public static class Approve {
        @Getter
        @RequiredArgsConstructor
        public static class Request {
            private final Long paymentId;
            private final Long merchantId;
            private final BigDecimal amount;
        }

        @Getter
        @RequiredArgsConstructor
        public static class Response {
            private final boolean success;
            private final String message;

            public static Response of(boolean success, String message) {
                return new Response(success, message);
            }
        }
    }

    // 3. 결제 정보 조회 API 전용
    public static class Info {
        @Getter
        @RequiredArgsConstructor
        public static class Response {
            private final String merchantName;
            private final BigDecimal amount;

            public static Response from(PaymentRequestEntity paymentRequest) {
                return new Response(
                        paymentRequest.getMerchant().getMerchantName(),
                        paymentRequest.getTotalAmount()
                );
            }
        }
    }
}
