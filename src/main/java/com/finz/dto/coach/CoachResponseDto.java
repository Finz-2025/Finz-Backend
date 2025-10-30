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
    private Long messageId;
    private String aiResponse;
    private String todayMission;  // 지출 상담용 (optional)
}
