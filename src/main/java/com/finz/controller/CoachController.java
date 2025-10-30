package com.finz.controller;

import com.finz.dto.coach.CoachResponseDto;
import com.finz.dto.coach.MessageRequest;
import com.finz.service.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.finz.dto.coach.CoachMessageDto;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@Tag(name = "Coach", description = "AI 코치 대화 API")
public class CoachController {
    
    private final CoachService coachService;
    
    // 1인 사용자용 고정 ID
    private static final Long DEFAULT_USER_ID = 1L;
    
    // 빠른 제안: 목표 설정 대화 시작
    @PostMapping("/start-goal-setting")
    @Operation(summary = "목표 설정 대화 시작", description = "AI 코치가 사용자의 정보를 바탕으로 목표 설정 대화를 시작합니다.")
    public ResponseEntity<CoachResponseDto> startGoalSetting() {
        log.info("목표 설정 시작 요청");
        
        CoachResponseDto response = coachService.startGoalSettingConversation(DEFAULT_USER_ID);
        return ResponseEntity.ok(response);
    }
    
    // 메시지 전송 (대화 진행)
    @PostMapping("/message")
    @Operation(summary = "메시지 전송", description = "사용자 메시지를 전송하고 AI 코치의 응답을 받습니다.")
    public ResponseEntity<CoachResponseDto> sendMessage(@RequestBody MessageRequest request) {
        log.info("메시지 전송 - type: {}", request.getMessageType());
        
        CoachResponseDto response = coachService.generateResponse(DEFAULT_USER_ID, request);
        return ResponseEntity.ok(response);
    }

    // 그간의 대화 내역 조회
    @GetMapping("/history")
    @Operation(summary = "대화 내역 조회", description = "AI 코치와의 전체 대화 내역을 조회합니다.")
    public ResponseEntity<List<CoachMessageDto>> getChatHistory() {
        log.info("대화 내역 조회 요청 - userId: {}", DEFAULT_USER_ID);

        List<CoachMessageDto> history = coachService.getChatHistory(DEFAULT_USER_ID);
        return ResponseEntity.ok(history);
    }
}
