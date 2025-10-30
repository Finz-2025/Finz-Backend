package com.finz.domain.coach;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {
    GOAL_CONSULT("목표 상담"),
    EXPENSE_CONSULT("지출 상담"),
    FREE_CHAT("자유 대화");
    
    private final String description;
}
