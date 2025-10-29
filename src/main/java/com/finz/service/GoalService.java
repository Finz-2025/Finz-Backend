package com.finz.service;

import com.finz.domain.coach.CoachMessage;
import com.finz.domain.coach.CoachMessageRepository;
import com.finz.domain.coach.MessageSender;
import com.finz.domain.coach.MessageType;
import com.finz.domain.goal.Goal;
import com.finz.domain.goal.GoalRepository;
import com.finz.domain.goal.GoalStatus;
import com.finz.dto.goal.GoalCreateRequest;
import com.finz.dto.goal.GoalResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {
    
    private final GoalRepository goalRepository;
    private final CoachMessageRepository messageRepository;
    
    // 목표 생성
    @Transactional
    public GoalResponseDto createGoal(Long userId, GoalCreateRequest request) {
        
        log.info("목표 생성 - userId: {}, goalType: {}", userId, request.getGoalType());
        
        // 1. Goal 테이블에 저장
        Goal goal = Goal.builder()
            .userId(userId)
            .goalType(request.getGoalType())
            .targetAmount(request.getTargetAmount())
            .currentAmount(0)
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusMonths(request.getDurationMonths()))
            .method(request.getMethod())
            .status(GoalStatus.ACTIVE)
            .build();
        
        Goal savedGoal = goalRepository.save(goal);
        
        // 2. 확인 메시지 저장
        String confirmMsg = String.format(
            "🎉 '%s' 목표가 설정되었어요!\n목표 금액: %,d원\n함께 달성해봐요! 💪",
            goal.getGoalType(),
            goal.getTargetAmount()
        );
        
        CoachMessage aiMsg = CoachMessage.builder()
            .userId(userId)
            .sender(MessageSender.AI)
            .messageType(MessageType.GOAL_SETTING)
            .content(confirmMsg)
            .build();
        
        messageRepository.save(aiMsg);
        
        log.info("목표 생성 완료 - goalId: {}", savedGoal.getGoalId());
        
        return GoalResponseDto.builder()
            .goalId(savedGoal.getGoalId())
            .message(confirmMsg)
            .build();
    }
}
