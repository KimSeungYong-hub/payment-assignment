package com.practice.paymentassignment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
public abstract class BaseTimeEntity{

    @CreatedDate
    @Column(updatable = false)
    private OffsetDateTime regTime;      //등록일

    @LastModifiedDate
    private OffsetDateTime updateTime;   //수정일


}
