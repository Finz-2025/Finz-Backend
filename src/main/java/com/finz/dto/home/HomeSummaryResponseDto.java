package com.finz.dto.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class HomeSummaryResponseDto {

    @JsonProperty("total_expense")
    private final Integer totalExpense;

    @JsonProperty("remaining_budget")
    private final Integer remainingBudget;

    @JsonProperty("progress_rate")
    private final Double progressRate; // 0.36 같은 소수점

    public HomeSummaryResponseDto(Integer totalExpense, Integer remainingBudget, Double progressRate) {
        this.totalExpense = totalExpense;
        this.remainingBudget = remainingBudget;
        this.progressRate = progressRate;
    }
}
