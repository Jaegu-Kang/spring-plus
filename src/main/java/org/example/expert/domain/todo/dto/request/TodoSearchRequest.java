package org.example.expert.domain.todo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TodoSearchRequest {

    // 할 일 제목 등을 검색하기 위해 전달받는 문자열 키워드 필드입니다.
    private String keyword;

    // 검색 시작일과 종료일을 문자열 파라미터에서 자바 객체로 안전하게 파싱하도록 ISO 표준 데이터 포맷을 강제하는 설정입니다.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    // 특정 담당자의 별칭으로 할 일을 필터링하기 위한 닉네임 검색 조건 필드입니다.
    private String nickname;
}
