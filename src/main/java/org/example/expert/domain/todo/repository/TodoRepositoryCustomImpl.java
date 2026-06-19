package org.example.expert.domain.todo.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
// Spring Data JPA 규칙(인터페이스명 + Impl)에 맞게 클래스명을 명명하여 레포지토리가 이 커스텀 구현을 자동으로 감지하도록 매핑합니다.
public class TodoRepositoryCustomImpl implements TodoRepositoryCustom {

    // 설정 클래스(QuerydslConfig)에서 빈으로 등록한 JPAQueryFactory를 가져옵니다.
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        // Q클래스 기반으로 컴파일 시점에 문법 오타가 검증되는 안정적인 구조의 쿼리를 선언합니다.
        Todo result = queryFactory
                .selectFrom(todo)
                // leftJoin 뒤에 .fetchJoin()을 결합하여, 연관된 User 엔티티까지 메모리에 한 번에 올리는 QueryDSL 방식의 페치 조인을 수행합니다.
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                // 단건 조회를 위해 fetchOne()을 호출하며, 데이터가 부재하여 발생할 수 있는 NullPointerException을 방지하기 위해 Optional 구조로 감싸서 반환합니다.
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(TodoSearchRequest request, Pageable pageable) {
        // 메인 쿼리의 데이터 집계와 충돌 및 왜곡이 발생하지 않도록 서브쿼리(SubQuery) 전용 Q클래스 인스턴스를 별도로 정의합니다.
        QManager subManager = new QManager("subManager");
        QComment subComment = new QComment("subCount");
        QManager manager = QManager.manager;

        // 검색 조건에 부합하는 데이터 본문 목록(content)을 규격에 맞춰 페이징 조회합니다.
        List<TodoSearchResponse> content = queryFactory
                // Projections.constructor를 활용해 엔티티가 아닌 DTO 형태로 필요한 필드만 select 절에서 최적화하여 추출합니다.
                .select(Projections.constructor(TodoSearchResponse.class,
                        todo.title,
                        // 각 일정별로 매핑된 총 매니저 수와 총 댓글 수를 데이터베이스 레벨에서 독립 서브쿼리로 카운트 연산하여 가져옵니다.
                        JPAExpressions.select(subManager.count())
                                .from(subManager)
                                .where(subManager.todo.eq(todo)),
                        JPAExpressions.select(subComment.count())
                                .from(subComment)
                                .where(subComment.todo.eq(todo))
                ))
                .from(todo)
                // 담당자 닉네임 필터링 조건을 수행하기 위해 Todo -> Manager -> User 순으로 조인 관계를 구성합니다.
                .leftJoin(todo.managers, manager)
                .leftJoin(manager.user, user)
                .where(
                        // 분기용 BooleanExpression 메서드들을 AND 조건으로 엮어 where 절에 바인딩합니다.
                        titleContains(request.getKeyword()),
                        createdAtBetween(request.getStartDate(), request.getEndDate()),
                        nicknameContains(request.getNickname())
                )
                .groupBy(todo.id)
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                // 데이터를 끊어 가져오기 위한 오프셋 및 페이지 사이즈 제한(limit) 쿼리를 주입합니다.
                .limit(pageable.getPageSize())
                .fetch();

        // 조인 관계로 인해 발생하는 전체 데이터 row 카운팅 왜곡을 막기 위해 countDistinct()를 사용하여 조건에 맞는 순수 Todo의 총 개수를 집계합니다.
        Long total = queryFactory
                .select(todo.countDistinct())
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(manager.user, user)
                .where(
                        titleContains(request.getKeyword()),
                        createdAtBetween(request.getStartDate(), request.getEndDate()),
                        nicknameContains(request.getNickname())
                )
                .fetchOne();
        // 데이터 본문 목록과 페이징 규격 정보, 그리고 총 개수를 스프링 표준 구현체인 PageImpl 객체로 조립하여 반환합니다.
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 검색 키워드가 존재할 경우에만 문법의 조건식을 동적으로 생성하고, 공백일 경우 null을 반환하여 쿼리에서 자동 제외되도록 처리합니다.
    private BooleanExpression titleContains(String keyword) {
        return StringUtils.hasText(keyword) ? todo.title.contains(keyword) : null;
    }

    // 날짜 조건 파라미터의 입력 유무(시작일만 입력, 종료일만 입력, 혹은 둘 다 입력)에 따라 크거나 같다(goe), 작거나 같다(loe), 범위(between) 조건식을 분기 생성합니다.
    private BooleanExpression createdAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return todo.createdAt.between(startDate, endDate);
        } else if (startDate != null) {
            return todo.createdAt.goe(startDate);
        } else if (endDate != null) {
            return todo.createdAt.loe(endDate);
        }
        return null;
    }

    // 닉네임 검색 파라미터가 유효하게 넘어왔을 경우에만 유저 테이블의 닉네임에 필터를 적용하는 메서드입니다.
    private BooleanExpression nicknameContains(String nickname) {
        return StringUtils.hasText(nickname) ? user.nickname.contains(nickname) : null;
    }
}
