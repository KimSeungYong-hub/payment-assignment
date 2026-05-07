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

    // 필드명을 줄여서 작성하지 말아주세요. 명확히 registrationTime 과 같이 정의해주시길 바랍니다.
    // 혼동될 사항을 만들지 말아주세요.
    // Instant를 사용하신 이유가 있나요? 이유가 명확해야할 것 같습니다.
    @CreatedDate
    @Column(updatable = false)
    private Instant regTime;      //등록일

    @LastModifiedDate
    private Instant updateTime;   //수정일


}
