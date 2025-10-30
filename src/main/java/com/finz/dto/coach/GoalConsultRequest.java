package com.finz.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "목표 상담 요청")
public class GoalConsultRequest {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
}
