package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
// QueryDSL을 사용한 맞춤형 메서드를 정의하기 위해 선언한 명세용 인터페이스입니다.
public interface TodoRepositoryCustom {

    // 특정 할 일(Id)을 조회할 때 작성자(User)를 페치 조인하여 효율적으로 가져오기 위한 추상 메서드 명세입니다.
    Optional<Todo> findByIdWithUser(Long todoId);

    // 다중 검색 조건 및 페이징 객체를 인자로 받아 DTO 구조로 포매팅된 슬라이싱 페이징 결과를 반환하도록 선언한 추상 메서드 명세입니다.
    Page<TodoSearchResponse> searchTodos(TodoSearchRequest request, Pageable pageable);
}
