package com.practice.paymentassignment.dto;


import java.io.Serializable;

public record IdempotencyRedisResponse (
         int status,
         byte[] body) implements Serializable {}
