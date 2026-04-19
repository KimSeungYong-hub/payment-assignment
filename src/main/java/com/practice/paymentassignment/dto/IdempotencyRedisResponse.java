package com.practice.paymentassignment.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRedisResponse implements Serializable {
    private int status;
    private byte[] body;
}
