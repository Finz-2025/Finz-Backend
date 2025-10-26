package com.finz.domain.expense;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpenseCategory {
    FOOD("음식"),
    CAFE("카페"),
    SHOPPING("쇼핑"),
    TRANSPORTATION("교통"),
    LIVING("주거"),
    CULTURE("문화생활"),
    ETC("기타");

    private final String description;
}