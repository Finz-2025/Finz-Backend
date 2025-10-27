package com.finz.domain.expense;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

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

    // Request의 카테고리 문자열을 Enum 상수로 변경
    public static ExpenseCategory fromDescription(String description) {
        return Arrays.stream(ExpenseCategory.values())
                .filter(category -> category.getDescription().equals(description))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리입니다: " + description));
    }
}