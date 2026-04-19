package com.practice.paymentassignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PaymentAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentAssignmentApplication.class, args);
    }

}
