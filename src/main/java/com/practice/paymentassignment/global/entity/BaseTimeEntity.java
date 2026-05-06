package com.practice.paymentassignment.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class BaseTimeEntity{

    @CreatedDate
    @Column(updatable = false)
    private Instant regTime;      //등록일

    @LastModifiedDate
    private Instant updateTime;   //수정일


}
