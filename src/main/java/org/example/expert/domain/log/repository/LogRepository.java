package org.example.expert.domain.log.repository;

import org.example.expert.domain.log.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

// Log 엔티티를 데이터베이스에 영속화하고 관리하기 위한 Spring Data JPA 레포지토리 인터페이스입니다
public interface LogRepository extends JpaRepository<Log, Long> {
}
