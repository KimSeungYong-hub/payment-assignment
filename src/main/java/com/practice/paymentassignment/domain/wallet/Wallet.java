package com.practice.paymentassignment.domain.wallet;

import com.practice.paymentassignment.domain.user.User;
import com.practice.paymentassignment.global.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "wallets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn( unique = true, nullable = false)
    private User user;

    @Column(precision = 19, scale = 0, nullable = false)
    private BigDecimal balance;

    @Builder
    public Wallet(User user, BigDecimal balance) {
        this.user = user;
        this.balance = balance;
    }

    public void pay(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0원보다 커야 합니다.");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("잔액이 부족합니다.");
        }
        this.balance = this.balance.subtract(amount);
    }
}
