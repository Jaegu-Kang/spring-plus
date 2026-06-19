package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// 기본 Spring Data JPA 기능과 더불어 사용자가 직접 정의한 QueryDSL 확장 기능을 다중 상속받아 하나의 빈으로 결합합니다.
public interface TodoRepository extends JpaRepository<Todo, Long>, TodoRepositoryCustom {

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

    // 날짜(수정일) 범위 및 날씨 조건에 따라 데이터를 동적으로 필터링하기 위해 JPQL 내부에 NULL 체크 분기 조건을 적용한 파라미터 기반 다중 조건 검색 메서드입니다.
    @Query("""
                SELECT t FROM Todo t LEFT JOIN FETCH t.user u
                WHERE (:weather IS NULL OR t.weather = :weather)
                AND (:start IS NULL OR t.modifiedAt >= :start)
                AND (:end IS NULL OR t.modifiedAt <= :end)
                ORDER BY t.modifiedAt DESC
                """)
    Page<Todo> findAllByParams(
            @Param("weather") String weather,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}
