package com.example.mealdangapi.global.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답.
 *
 * Spring Data의 Page 객체를 그대로 응답에 실으면 안 되는 이유:
 *   Page 직렬화 결과에는 pageable, sort, first, numberOfElements 같은
 *   내부 필드가 20개 가까이 딸려 나오고, Spring 버전이 올라가면 그 모양이 바뀐다.
 *   실제로 Spring Boot 3.x에서 경고를 띄운다. 필요한 것만 골라 내려주는 게 안전하다.
 *
 * 미식 연구소(게시판) 목록 화면에서 쓴다.
 * 와이어프레임 하단 페이지 번호(1 2 3 4)가 totalPages에 대응된다.
 */
public class PageResponse<T> {

    private final List<T> content;
    private final int page;          // 현재 페이지 (0부터 시작)
    private final int size;          // 페이지당 개수
    private final long totalElements;// 전체 건수
    private final int totalPages;    // 전체 페이지 수
    private final boolean last;      // 마지막 페이지 여부

    private PageResponse(List<T> content, int page, int size,
                         long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    /** Page<T>를 그대로 감쌀 때 */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    /**
     * 엔티티 Page를 DTO로 변환하면서 감쌀 때.
     * 사용: PageResponse.from(postPage, BoardPostListResponse::from)
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }
}
