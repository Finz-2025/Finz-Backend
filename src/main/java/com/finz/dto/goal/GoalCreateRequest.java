package com.finz.dto.goal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoalCreateRequest {
    private String goalType;
    private Integer targetAmount;
    private Integer durationMonths;
    private String method;
}
