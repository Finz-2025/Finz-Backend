package com.finz.controller;

import com.finz.dto.goal.GoalCreateRequest;
import com.finz.dto.goal.GoalResponseDto;
import com.finz.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/goal")
@RequiredArgsConstructor
@Tag(name = "Goal", description = "목표 관리 API")
public class GoalController {
    
    private final GoalService goalService;
    
    // 1인 사용자용 고정 ID
    private static final Long DEFAULT_USER_ID = 1L;
    
    // 목표 생성
    @PostMapping("/create")
    @Operation(summary = "목표 생성", description = "AI 코치와의 대화를 통해 설정한 목표를 생성합니다.")
    public ResponseEntity<GoalResponseDto> createGoal(@RequestBody GoalCreateRequest request) {
        log.info("목표 생성 요청 - goalType: {}", request.getGoalType());
        
        GoalResponseDto response = goalService.createGoal(DEFAULT_USER_ID, request);
        return ResponseEntity.ok(response);
    }
}
