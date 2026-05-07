package com.practice.paymentassignment.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    // Builder를 사용한다면 생성자는 private 하게 만들 것 같습니다.
    @Builder
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
