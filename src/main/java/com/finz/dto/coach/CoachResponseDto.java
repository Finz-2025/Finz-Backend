package com.finz.dto.coach;

import com.finz.domain.coach.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoachResponseDto {
    private String message;
    private MessageType messageType;
    private GoalSuggestion suggestedGoal;  // nullable
}
