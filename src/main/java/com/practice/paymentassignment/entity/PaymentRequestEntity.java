package com.practice.paymentassignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Table(name = "payment_requests")
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "total_amount", precision = 19, scale = 0, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRequestStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Builder
    public PaymentRequestEntity(Merchant merchant, String orderId, BigDecimal totalAmount) {
        this.merchant = merchant;
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.status = PaymentRequestStatus.READY; // 최초 생성 시 무조건 대기 상태
        this.expiredAt = Instant.now().plusSeconds(60 * 10); // 10분 후 만료
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
}