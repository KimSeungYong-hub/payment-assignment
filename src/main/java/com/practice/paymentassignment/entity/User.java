package com.practice.paymentassignment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
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
}
