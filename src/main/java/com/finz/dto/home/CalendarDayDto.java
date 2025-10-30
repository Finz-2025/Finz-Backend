package com.finz.dto.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalendarDayDto {

    private String date; // "2025-10-01" (ISO 8601)

    @JsonProperty("expense_status")
    private String expenseStatus; // "over", "normal", "none"
}
