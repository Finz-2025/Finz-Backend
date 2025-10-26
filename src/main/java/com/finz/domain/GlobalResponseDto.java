package com.finz.domain;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // data 값 null일 경우 숨기기
public class GlobalResponseDto<T> {
    private final int status;
    private final boolean success;
    private final String message;
    private final T data; // data 부분은 Generic Type으로 처리
}