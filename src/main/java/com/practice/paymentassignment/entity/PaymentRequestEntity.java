package com.practice.paymentassignment.entity;

import com.practice.paymentassignment.entity.Merchant;
import com.practice.paymentassignment.entity.PaymentRequestStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자는 Protected로 막아둠
public class PaymentRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 가맹점 정보 (다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // 🌟 핵심: 가맹점의 주문번호를 유니크 키로 설정하여 DB 레벨의 멱등성 보장
    @Column(name = "merchant_order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRequestStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime expiredAt;

    @Builder
    public PaymentRequestEntity(Merchant merchant, String orderId, BigDecimal totalAmount) {
        this.merchant = merchant;
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.status = PaymentRequestStatus.READY; // 최초 생성 시 무조건 대기 상태
        this.createdAt = LocalDateTime.now();
    }

    public void markAsDone() {
//        if (this.status == PaymentRequestStatus.SUCCESS) {
//            throw new IllegalStateException("이미 처리 완료된 결제 요청입니다.");
//        }
        this.status = PaymentRequestStatus.SUCCESS;
    }

    public boolean isExpired() {
        if (this.status == PaymentRequestStatus.EXPIRED) {
            return true;
        }

        if (this.expiredAt != null && LocalDateTime.now().isAfter(this.expiredAt)) {
            return true;
        }

        return false;
    }

    public void markAsExpired() {
        this.status = PaymentRequestStatus.EXPIRED;
    }
}