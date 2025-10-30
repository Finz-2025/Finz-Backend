package com.finz.dto.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.finz.dto.coach.CoachResponseDto; // Coach DTO 임포트
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateExpenseResponseDto {
    @JsonProperty("expense_id")
    private final Long expenseId;

    private final CoachResponseDto aiFeedback;

    public CreateExpenseResponseDto(Long expenseId, CoachResponseDto aiFeedback) {
        this.expenseId = expenseId;
        this.aiFeedback = aiFeedback;
    }

    public CreateExpenseResponseDto(Long expenseId) {
        this(expenseId, null); // 메인 생성자 호출
    }
}