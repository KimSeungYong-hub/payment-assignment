package com.practice.paymentassignment.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 해당 abstract class가 사용되는 곳은 domain layer에 한정되기에, domain layer에 위치하는 것이 맞을 것 같습니다.
@MappedSuperclass
@EntityListeners(value = {AuditingEntityListener.class})
@Getter
public abstract class BaseEntity extends BaseTimeEntity{

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;       //등록자

    @LastModifiedBy
    private String modifiedBy;      //수정자

}
