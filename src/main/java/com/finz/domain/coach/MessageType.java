package com.finz.domain.coach;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {
    GOAL_SETTING("목표 설정"),
    EXPENSE_ADVICE("지출 조언"),
    FREE_CHAT("자유 대화");
    
    private final String description;
}
