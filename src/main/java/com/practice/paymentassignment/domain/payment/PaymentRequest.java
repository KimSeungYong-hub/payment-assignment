package com.practice.paymentassignment.domain.payment;

import com.practice.paymentassignment.domain.merchant.Merchant;
import com.practice.paymentassignment.global.entity.BaseEntity;
import com.practice.paymentassignment.global.exception.AlreadyProcessedException;
import com.practice.paymentassignment.global.exception.PaymentExpiredException;
import com.practice.paymentassignment.global.exception.PaymentForgeryException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Table(name = "payment_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자는 Protected로 막아둠
public class PaymentRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Merchant merchant;

    // 컬럼명이 혼란만 야기할 것 같습니다. 
    // orderId라면 주문(order)이란 테이블의 id이라고 해석하는 게 일반적이지 않을까요
    // 또한 이후에 order라는 테이블이 생겼을 때 어떻게 대처하기 힘들 것 같습니다.
    @Column(nullable = false, unique = true)
    private String orderId;


// 사용 안하는 코드는 주석이 아니라, 제거해주세요. 혼란만 야기합니다.

//    @JdbcTypeCode(SqlTypes.BINARY) // 핵심! DB에 BINARY(16)으로 저장하라는 지시어
//    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
//    private UUID orderId;

    // BigDecimal과 BigDecimal의 precision과 scale을 설정할 땐 합당한 이유가 필요합니다.
    @Column( precision = 19, scale = 0, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRequestStatus status;

    private Instant expiredAt;

    @Builder
    public PaymentRequest(Merchant merchant, String orderId, BigDecimal totalAmount) {
        this.merchant = merchant;
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.status = PaymentRequestStatus.READY; // 최초 생성 시 무조건 대기 상태
        this.expiredAt = Instant.now().plusSeconds(60 * 10); // 10분 후 만료
    }

    public static PaymentRequest createSuccess(Merchant merchant, BigDecimal totalAmount, String idempotencyKey) {
        return PaymentRequest.builder()
                .merchant(merchant)
                .orderId(idempotencyKey)
                .totalAmount(totalAmount)
                .build();
    }

    public void markAsDone() {
        this.status = PaymentRequestStatus.SUCCESS;
    }

    public boolean isExpired() {
        if (this.status == PaymentRequestStatus.EXPIRED) {
            return true;
        }

        if (this.expiredAt != null && Instant.now().isAfter(this.expiredAt)) {
            return true;
        }

        return false;
    }

    public void markAsExpired() {
        this.status = PaymentRequestStatus.EXPIRED;
    }
    public void markAsFail() {
        this.status = PaymentRequestStatus.FAILURE;
    }

    public void verifyCanBeApproved(Long merchantId, BigDecimal amount) {
        if (!this.getStatus().equals(PaymentRequestStatus.READY)) {
            throw new AlreadyProcessedException("이미 처리 중이거나 완료된 주문입니다.");
        }

        if (!this.merchant.getId().equals(merchantId)) {
            throw new PaymentForgeryException("가맹점 정보가 일치하지 않습니다. (위변조 의심)");
        }
        if (this.totalAmount.compareTo(amount) != 0) {
            throw new PaymentForgeryException("결제 요청 금액이 실제 주문 금액과 일치하지 않습니다. (위변조 결제 방어)");
        }
        // 이렇게 되면 verifyCanBeApproved 호출하기 전까지 expired 상태가 업데이트 안되는 거 아닌가요?
        // 그렇게 되면 사용자는 만료된 요청이 만료가 안된 것처럼 보일 것 같은데요.
        if(this.isExpired()){
            this.markAsExpired();
            throw new PaymentExpiredException("결제 시간이 만료되었습니다. 처음부터 다시 시도해주세요.");
        }
    }


}