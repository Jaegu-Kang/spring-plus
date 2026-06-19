package org.example.expert.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping("/todos")
    public ResponseEntity<TodoSaveResponse> saveTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TodoSaveRequest todoSaveRequest
    ) {
        return ResponseEntity.ok(todoService.saveTodo(authUser, todoSaveRequest));
    }

    // 할 일 목록을 조건별(날씨, 수정일 기준 시작/종료일)로 필터링하여 조회할 수 있는 API 엔드포인트입니다.
    @GetMapping("/todos")
    public ResponseEntity<Page<TodoResponse>> getTodos(
            // 페이징 처리를 위해 클라이언트로부터 페이지 번호와 한 페이지당 사이즈를 전달받으며, 값이 없을 경우 기본값(1페이지, 10개씩)으로 세팅합니다.
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            // 필터링 조건인 날씨와 날짜 범위(start, end)를 선택적 파라미터(필수 아님)로 바인딩합니다.
            @RequestParam(required = false) String weather,
            // 문자열 형태로 들어오는 날짜 데이터를 자바의 LocalDateTime 객체로 안전하게 파싱하기 위해 ISO 표준 날짜/시간 포맷 형식을 지정합니다.
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
            ) {
        return ResponseEntity.ok(todoService.getTodos(page, size, weather, start, end));
    }

    @GetMapping("/todos/{todoId}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable long todoId) {
        return ResponseEntity.ok(todoService.getTodo(todoId));
    }

    // 다중 검색 조건(키워드, 날짜 범위, 담당자 닉네임)과 페이징 설정을 받아 할 일 목록을 조회하는 API 엔드포인트입니다.
    @GetMapping("/todos/search")
    public ResponseEntity<Page<TodoSearchResponse>> searchTodos(
            // 쿼리 스트링(Query Parameter) 형태로 넘어오는 여러 검색 조건들을 TodoSearchRequest DTO 객체에 바인딩합니다.
            @ModelAttribute TodoSearchRequest request,
            // 클라이언트가 페이징 파라미터(page, size)를 생략하더라도 스프링의 기본 페이징을 적용하여 Pageable 객체로 바인딩합니다.
            @PageableDefault Pageable pageable
            ) {
        return ResponseEntity.ok(todoService.searchTodos(request, pageable));
    }
}
