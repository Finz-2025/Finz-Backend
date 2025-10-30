package com.finz.dto.coach;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoalConsultRequest {
    private Long userId;
    private Long goalId;
}
