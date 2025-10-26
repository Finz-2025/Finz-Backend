package com.finz.domain.user;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Job {
    STUDENT("학생"),
    OFFICE_WORKER("직장인"),
    FREELANCER("프리랜서"),
    ETC("기타");

    private final String description;
}