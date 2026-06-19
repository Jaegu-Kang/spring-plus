package org.example.expert.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {

    // EntityManager를 스프링 컨테이너로부터 주입받아 영속성 컨텍스트를 참조합니다.
    @PersistenceContext
    private EntityManager em;

    // 프로젝트 전역에서 QueryDSL 동적 쿼리를 타입 안정성 있게 작성할 수 있도록 JPAQueryFactory를 스프링 빈으로 등록합니다.
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
