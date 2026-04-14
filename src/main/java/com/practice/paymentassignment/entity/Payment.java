package com.practice.paymentassignment.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchantName;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private OffsetDateTime dateTime;


}
