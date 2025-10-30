package com.finz.controller;

import com.finz.dto.GlobalResponseDto;
import com.finz.dto.coach.CoachMessageDto;
import com.finz.dto.coach.CoachResponseDto;
import com.finz.dto.coach.MessageRequest;
import com.finz.service.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@Tag(name = "Coach", description = "AI 코치 상담 API")
public class CoachController {
    
    private final CoachService coachService;
    
    // 목표 상담 요청
    @PostMapping("/goal-consult/{userId}")
    @Operation(summary = "목표 상담 요청", description = "AI 코치와 목표 설정 대화를 시작합니다.")
    public ResponseEntity<GlobalResponseDto<CoachResponseDto>> startGoalConsult(
            @PathVariable Long userId) {
        log.info("목표 상담 요청 - userId: {}", userId);
        
        return ResponseEntity.ok(
            coachService.requestGoalConsult(userId)
        );
    }
    
    // 지출 상담 요청
    @PostMapping("/expense-consult/{userId}")
    @Operation(summary = "지출 상담 요청", description = "AI 코치와 지출 패턴에 대한 대화를 시작합니다.")
    public ResponseEntity<GlobalResponseDto<CoachResponseDto>> startExpenseConsult(
            @PathVariable Long userId) {
        log.info("지출 상담 요청 - userId: {}", userId);
        
        return ResponseEntity.ok(
            coachService.requestExpenseConsult(userId)
        );
    }
    
    // 메시지 전송 (대화 진행)
    @PostMapping("/message/{userId}")
    @Operation(summary = "메시지 전송", description = "사용자 메시지를 전송하고 AI 코치의 응답을 받습니다.")
    public ResponseEntity<CoachResponseDto> sendMessage(
            @PathVariable Long userId,
            @RequestBody MessageRequest request) {
        log.info("메시지 전송 - userId: {}, type: {}", userId, request.getMessageType());
        
        CoachResponseDto response = coachService.generateResponse(userId, request);
        return ResponseEntity.ok(response);
    }

    // 그간의 대화 내역 조회
    @GetMapping("/history/{userId}")
    @Operation(summary = "대화 내역 조회", description = "AI 코치와의 전체 대화 내역을 조회합니다.")
    public ResponseEntity<List<CoachMessageDto>> getChatHistory(@PathVariable Long userId) {
        log.info("대화 내역 조회 요청 - userId: {}", userId);

        List<CoachMessageDto> history = coachService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }
}
