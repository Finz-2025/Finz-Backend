package com.finz.controller;

import com.finz.dto.GlobalResponseDto;
import com.finz.dto.coach.CoachResponseDto;
import com.finz.dto.coach.ExpenseConsultRequest;
import com.finz.dto.coach.GoalConsultRequest;
import com.finz.dto.coach.MessageRequest;
import com.finz.service.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@Tag(name = "Coach", description = "AI 코치 상담 API")
public class CoachController {
    
    private final CoachService coachService;
    
    // 목표 상담 요청
    @PostMapping("/goal-consult")
    @Operation(summary = "목표 상담 요청", description = "특정 목표에 대한 AI 코치 상담을 시작합니다.")
    public ResponseEntity<GlobalResponseDto<CoachResponseDto>> startGoalConsult(
            @RequestBody GoalConsultRequest request) {
        log.info("목표 상담 요청 - userId: {}, goalId: {}", request.getUserId(), request.getGoalId());
        
        return ResponseEntity.ok(
            coachService.requestGoalConsult(request.getUserId(), request.getGoalId())
        );
    }
    
    // 지출 상담 요청
    @PostMapping("/expense-consult")
    @Operation(summary = "지출 상담 요청", description = "사용자의 지출 패턴에 대한 AI 코치 상담을 시작합니다.")
    public ResponseEntity<GlobalResponseDto<CoachResponseDto>> startExpenseConsult(
            @RequestBody ExpenseConsultRequest request) {
        log.info("지출 상담 요청 - userId: {}", request.getUserId());
        
        return ResponseEntity.ok(
            coachService.requestExpenseConsult(request.getUserId())
        );
    }
    
    // 메시지 전송 (대화 진행)
    @PostMapping("/message")
    @Operation(summary = "메시지 전송", description = "사용자 메시지를 전송하고 AI 코치의 응답을 받습니다.")
    public ResponseEntity<GlobalResponseDto<CoachResponseDto>> sendMessage(
            @RequestBody MessageRequest request) {
        log.info("메시지 전송 - userId: {}, type: {}", request.getUserId(), request.getMessageType());
        
        return ResponseEntity.ok(
            coachService.sendMessage(request.getUserId(), request)
        );
    }
}
