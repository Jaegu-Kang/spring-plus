package org.example.expert.domain.log.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Column
    private LocalDateTime createdAt;

    // 로그 메시지를 인자로 받아 생성 시점의 현재 시스템 시간을 자동으로 매핑해 주는 생성자 메서드입니다.
    public Log(String message) {
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

}
