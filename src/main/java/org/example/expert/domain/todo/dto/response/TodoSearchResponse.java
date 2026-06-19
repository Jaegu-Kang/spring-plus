package org.example.expert.domain.todo.dto.response;

import lombok.Getter;

@Getter
public class TodoSearchResponse {

    // 클라이언트 화면에 보여줄 요약 정보(제목, 총 매니저 수, 총 댓글 수)만을 담도록 설계된 DTO입니다.
    private final String title;

    private final Long managerCount;

    private final Long commentCount;

    // QueryDSL의 Projections.constructor 구조를 통해 쿼리 select 절에서 데이터를 직접 바인딩하여 인스턴스를 매핑해 주는 생성자입니다.
    public TodoSearchResponse(String title, Long managerCount, Long commentCount) {
        this.title = title;
        this.managerCount = managerCount;
        this.commentCount = commentCount;
    }
}
