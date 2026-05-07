package com.practice.paymentassignment.dto;

import com.practice.paymentassignment.domain.payment.PaymentRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

// DTO 컨벤션이 일관적이지 않습니다. 스타일을 하나로 통일해주세요.
// nested class를 사용하신 이유 또한 명확해야할 것 같습니다.
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

            public static Response from(PaymentRequest paymentRequest) {
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

            public static Response from(PaymentRequest paymentRequest) {
                return new Response(
                        paymentRequest.getMerchant().getMerchantName(),
                        paymentRequest.getTotalAmount()
                );
            }
        }
    }
}
