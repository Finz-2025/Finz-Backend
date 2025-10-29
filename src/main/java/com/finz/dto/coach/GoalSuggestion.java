package com.finz.dto.coach;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalSuggestion {
    private String goalType;
    private Integer targetAmount;
    private Integer durationMonths;
    private String method;
}
