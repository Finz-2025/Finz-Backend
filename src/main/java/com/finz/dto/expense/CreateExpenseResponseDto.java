package com.finz.dto.expense;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CreateExpenseResponseDto {

    @JsonProperty("expense_id")
    private final Long expenseId;

    public CreateExpenseResponseDto(Long expenseId) {
        this.expenseId = expenseId;
    }
}
