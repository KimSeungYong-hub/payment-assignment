package com.practice.paymentassignment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int money;

    public void pay(int amount) {
        if (money < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.money -= amount;
    }

    @Builder
    public User(Long id, String name, int money) {
        this.id = id;
        this.name = name;
        this.money = money;
    }
}
