package com.finz.dto.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finz.domain.expense.Expense;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DailyExpenseDto {

    @JsonProperty("expense_id")
    private final Long expenseId;
    @JsonProperty("expense_name")
    private final String expenseName;
    private final Integer amount;
    private final String category; // "음식" (description)
    @JsonProperty("expense_tag")
    private final String expenseTag;
    private final String memo;

    @Builder
    private DailyExpenseDto(Long expenseId, String expenseName, Integer amount, String category, String expenseTag, String memo) {
        this.expenseId = expenseId;
        this.expenseName = expenseName;
        this.amount = amount;
        this.category = category;
        this.expenseTag = expenseTag;
        this.memo = memo;
    }

    // Expense 엔티티를 이 DTO로 변환하는 팩토리 메서드
    public static DailyExpenseDto fromEntity(Expense expense) {
        return DailyExpenseDto.builder()
                .expenseId(expense.getId())
                .expenseName(expense.getExpenseName())
                .amount(expense.getAmount())
                .category(expense.getCategory().getDescription())
                .expenseTag(expense.getExpenseTag())
                .memo(expense.getMemo())
                .build();
    }
}
