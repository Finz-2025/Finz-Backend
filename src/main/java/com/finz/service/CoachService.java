package com.finz.service;

import com.finz.domain.coach.*;
import com.finz.domain.expense.ExpensePattern;
import com.finz.repository.ExpenseRepository;
import com.finz.domain.goal.Goal;
import com.finz.domain.goal.GoalRepository;
import com.finz.domain.goal.GoalStatus;
import com.finz.domain.user.User;
import com.finz.domain.user.UserRepository;
import com.finz.dto.coach.*;
import com.finz.dto.GlobalResponseDto;
import com.finz.infrastructure.gemini.GeminiApiClient;
import com.finz.infrastructure.gemini.dto.GeminiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final ExpenseRepository expenseRepository;
    private final GeminiApiClient geminiClient;

    // 목표 상담 요청
    @Transactional
    public GlobalResponseDto<CoachResponseDto> requestGoalConsult(Long userId, Long goalId) {
        CoachResponseDto data = startGoalConsult(userId, goalId);

        return GlobalResponseDto.<CoachResponseDto>builder()
                .status(200)
                .success(true)
                .message("목표 상담이 시작되었습니다.")
                .data(data)
                .build();
    }

    // 지출 상담 요청
    @Transactional
    public GlobalResponseDto<CoachResponseDto> requestExpenseConsult(Long userId) {
        CoachResponseDto data = startExpenseConsult(userId);

        return GlobalResponseDto.<CoachResponseDto>builder()
                .status(200)
                .success(true)
                .message("지출 상담이 시작되었습니다.")
                .data(data)
                .build();
    }

    // 메시지 전송
    @Transactional
    public GlobalResponseDto<CoachResponseDto> sendMessage(Long userId, MessageRequest request) {
        CoachResponseDto data = generateResponse(userId, request);

        return GlobalResponseDto.<CoachResponseDto>builder()
                .status(200)
                .success(true)
                .message("메시지가 전송되었습니다.")
                .data(data)
                .build();
    }

    // 내부 로직 메서드
    // 목표 상담 시작
    @Transactional
    public CoachResponseDto startGoalConsult(Long userId, Long goalId) {
        log.info("목표 상담 시작 - userId: {}, goalId: {}", userId, goalId);
        // goalId는 받지만, 기존 목표 설정 대화 로직을 그대로 사용
        return startGoalSettingConversation(userId);
    }

    // 지출 상담 시작
    @Transactional
    public CoachResponseDto startExpenseConsult(Long userId) {
        log.info("지출 상담 시작 - userId: {}", userId);

        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 최근 1개월 지출 패턴 분석
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        List<ExpensePattern> expenses = expenseRepository
                .findRecentPatternsByUserId(userId, oneMonthAgo);

        // 3. 개인화된 시스템 프롬프트 생성
        String systemPrompt = buildExpenseConsultPrompt(user, expenses);

        log.debug("System Prompt: {}", systemPrompt);

        // 4. Gemini API 호출
        String initialMessage = geminiClient.generateInitialGoalMessage(systemPrompt);

        // 5. 오늘의 미션 생성
        String todayMission = generateTodayMission(expenses);

        // 6. AI 첫 메시지 저장
        CoachMessage aiMessage = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.AI)
                .messageType(MessageType.EXPENSE_CONSULT)
                .content(initialMessage)
                .build();

        messageRepository.save(aiMessage);

        log.info("지출 상담 시작 완료 - messageId: {}", aiMessage.getMessageId());

        return CoachResponseDto.builder()
                .messageId(aiMessage.getMessageId())
                .aiResponse(initialMessage)
                .todayMission(todayMission)
                .build();
    }

    // 빠른 제안: 목표 설정 대화 시작
    @Transactional
    public CoachResponseDto startGoalSettingConversation(Long userId) {

        log.info("목표 설정 대화 시작 - userId: {}", userId);

        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 기존 목표 조회
        List<Goal> existingGoals = goalRepository.findByUserIdAndStatus(
                userId, GoalStatus.ACTIVE
        );

        // 3. 최근 1개월 지출 패턴 분석
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        List<ExpensePattern> expenses = expenseRepository
                .findRecentPatternsByUserId(userId, oneMonthAgo);

        // 4. 개인화된 시스템 프롬프트 생성
        String systemPrompt = buildGoalSettingPrompt(user, existingGoals, expenses);

        log.debug("System Prompt: {}", systemPrompt);

        // 5. Gemini API 호출
        String initialMessage = geminiClient.generateInitialGoalMessage(systemPrompt);

        // 6. AI 첫 메시지 저장
        CoachMessage aiMessage = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.AI)
                .messageType(MessageType.GOAL_CONSULT)
                .content(initialMessage)
                .build();

        messageRepository.save(aiMessage);

        log.info("목표 설정 대화 시작 완료 - messageId: {}", aiMessage.getMessageId());

        return CoachResponseDto.builder()
                .messageId(aiMessage.getMessageId())
                .aiResponse(initialMessage)
                .build();
    }

    // 일반 메시지 응답 생성
    @Transactional
    public CoachResponseDto generateResponse(Long userId, MessageRequest request) {

        log.info("메시지 응답 생성 - userId: {}, type: {}", userId, request.getMessageType());

        // 1. 사용자 메시지 저장
        CoachMessage userMsg = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.USER)
                .messageType(request.getMessageType())
                .content(request.getMessage())
                .build();

        messageRepository.save(userMsg);

        // 2. 컨텍스트 수집
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Goal> goals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);

        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        List<ExpensePattern> expenses = expenseRepository
                .findRecentPatternsByUserId(userId, oneMonthAgo);

        // 3. 대화 히스토리 조회
        List<CoachMessage> history = messageRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(userId);
        Collections.reverse(history);

        // 4. 시스템 프롬프트 생성
        String systemPrompt = request.getMessageType() == MessageType.GOAL_CONSULT
                ? buildGoalSettingPrompt(user, goals, expenses)
                : buildGeneralChatPrompt(user, goals, expenses);

        // 5. Gemini API 호출
        List<GeminiMessage> geminiHistory = convertToGeminiFormat(history);
        String aiResponse = geminiClient.chat(systemPrompt, geminiHistory, request.getMessage());

        // 6. AI 응답 저장
        CoachMessage aiMsg = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.AI)
                .messageType(request.getMessageType())
                .content(aiResponse)
                .build();

        messageRepository.save(aiMsg);

        return CoachResponseDto.builder()
                .messageId(aiMsg.getMessageId())
                .aiResponse(aiResponse)
                .build();
    }

    // 개인화된 목표 설정 시스템 프롬프트 생성
    private String buildGoalSettingPrompt(User user, List<Goal> goals, List<ExpensePattern> expenses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 FiNZ의 친근한 AI 재무 코치입니다.\n");
        prompt.append("사용자가 '목표 설정' 버튼을 눌러서 대화를 시작했습니다.\n\n");

        // 사용자 개인 정보 포함
        prompt.append("## 사용자 정보\n");
        prompt.append(String.format("- 이름: %s\n", user.getNickname()));
        prompt.append(String.format("- 연령대: %s\n", user.getAgeGroup().getDescription()));
        prompt.append(String.format("- 직업: %s\n", user.getJob().getDescription()));
        prompt.append(String.format("- 월 목표 예산: %,d원\n\n", user.getMonthlyBudget()));

        // 기존 목표 정보
        if (!goals.isEmpty()) {
            prompt.append("## 현재 진행 중인 목표\n");
            for (Goal goal : goals) {
                int progress = (int) ((goal.getCurrentAmount() * 100.0) / goal.getTargetAmount());
                prompt.append(String.format("- %s: %,d원 목표 (현재 %d%% 달성)\n",
                        goal.getGoalType(), goal.getTargetAmount(), progress));
            }
            prompt.append("\n");
        } else {
            prompt.append("## 현재 진행 중인 목표\n");
            prompt.append("- 아직 설정된 목표가 없습니다.\n\n");
        }

        // 지출 패턴 분석
        if (!expenses.isEmpty()) {
            prompt.append("## 최근 1개월 지출 패턴 (상위 5개)\n");
            int limit = Math.min(5, expenses.size());
            for (int i = 0; i < limit; i++) {
                ExpensePattern expense = expenses.get(i);
                prompt.append(String.format("- %s: %,d원 (%d회 사용)\n",
                        expense.getCategory().getDescription(),
                        expense.getTotalAmount(),
                        expense.getCount()));
            }
            prompt.append("\n");
        }

        // AI의 역할 및 톤
        prompt.append("## 당신의 역할과 말투\n");
        prompt.append("1. 친근하고 격려하는 존댓말 사용\n");
        prompt.append("2. 이모지를 적절히 활용 (🎯, 💰, 😊, 🔥, 💪 등)\n");
        prompt.append("3. 사용자의 연령대와 직업을 고려한 맞춤형 조언\n");
        prompt.append("4. 지출 패턴을 분석해 구체적인 목표 제안\n");
        prompt.append("5. 목표는 현실적이고 달성 가능한 수준으로\n\n");

        // 대화 진행 가이드
        prompt.append("## 대화 진행 방법\n");
        prompt.append("1. 먼저 친근하게 인사하며 목표 설정 시작\n");
        prompt.append("2. 사용자의 지출 패턴을 언급하며 목표 후보 제시\n");
        prompt.append("3. 사용자가 원하는 목표 유형 파악\n");
        prompt.append("4. 구체적인 금액과 기간 질문\n");
        prompt.append("5. 실행 가능한 방법 함께 고민\n");
        prompt.append("6. 최종적으로 명확한 목표 제안\n\n");

        // 개인화된 첫 메시지 예시
        prompt.append("## 첫 메시지 작성 가이드\n");
        prompt.append(String.format("- %s님의 이름을 부르며 친근하게 시작하세요\n", user.getNickname()));

        if (!expenses.isEmpty()) {
            ExpensePattern topExpense = expenses.get(0);
            prompt.append(String.format("- 최근 '%s' 지출이 %,d원으로 가장 많다는 점을 자연스럽게 언급하세요\n",
                    topExpense.getCategory().getDescription(), topExpense.getTotalAmount()));
        }

        return prompt.toString();
    }

    // 지출 상담 프롬프트 생성
    private String buildExpenseConsultPrompt(User user, List<ExpensePattern> expenses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 FiNZ의 친근한 AI 재무 코치입니다.\n");
        prompt.append("사용자가 지출 패턴 상담을 요청했습니다.\n\n");

        // 사용자 정보
        prompt.append("## 사용자 정보\n");
        prompt.append(String.format("- 이름: %s\n", user.getNickname()));
        prompt.append(String.format("- 연령대: %s\n", user.getAgeGroup().getDescription()));
        prompt.append(String.format("- 직업: %s\n", user.getJob().getDescription()));
        prompt.append(String.format("- 월 목표 예산: %,d원\n\n", user.getMonthlyBudget()));

        // 지출 패턴 분석
        if (!expenses.isEmpty()) {
            prompt.append("## 최근 1개월 지출 패턴\n");
            int totalExpense = 0;
            for (ExpensePattern expense : expenses) {
                totalExpense += expense.getTotalAmount();
                prompt.append(String.format("- %s: %,d원 (%d회)\n",
                        expense.getCategory().getDescription(),
                        expense.getTotalAmount(),
                        expense.getCount()));
            }
            prompt.append(String.format("\n총 지출: %,d원\n", totalExpense));

            // 예산 대비 분석
            double budgetRatio = (totalExpense * 100.0) / user.getMonthlyBudget();
            if (budgetRatio > 100) {
                prompt.append(String.format("⚠️ 예산 초과: %.1f%%\n\n", budgetRatio - 100));
            } else {
                prompt.append(String.format("✅ 예산 준수: %.1f%% 사용\n\n", budgetRatio));
            }
        }

        // AI 역할
        prompt.append("## 당신의 역할\n");
        prompt.append("1. 지출 패턴을 친근하게 분석\n");
        prompt.append("2. 가장 많이 지출한 카테고리 언급\n");
        prompt.append("3. 절약 가능한 구체적 방법 제안\n");
        prompt.append("4. 격려하는 톤으로 실천 가능한 조언 제공\n");
        prompt.append("5. 이모지를 적절히 활용\n");

        return prompt.toString();
    }

    // 오늘의 미션 생성
    private String generateTodayMission(List<ExpensePattern> expenses) {
        if (expenses.isEmpty()) {
            return "오늘은 지출 없이 보내기! 💪";
        }

        // 가장 많이 지출한 카테고리 찾기
        ExpensePattern topExpense = expenses.get(0);
        String category = topExpense.getCategory().getDescription();

        // 카테고리별 미션 생성
        if (category.contains("카페") || category.contains("커피")) {
            return "오늘은 집에서 커피 마시기 ☕";
        } else if (category.contains("식사") || category.contains("음식")) {
            return "오늘은 집밥 먹기 🍚";
        } else if (category.contains("배달")) {
            return "오늘은 배달 대신 직접 요리하기 👨‍🍳";
        } else if (category.contains("쇼핑")) {
            return "오늘은 장바구니만 담고 구매는 내일 생각하기 🛒";
        } else if (category.contains("택시")) {
            return "오늘은 대중교통 이용하기 🚇";
        } else {
            return String.format("오늘은 %s 지출 안 하기! 💰", category);
        }
    }

    // 일반 대화용 시스템 프롬프트
    private String buildGeneralChatPrompt(User user, List<Goal> goals, List<ExpensePattern> expenses) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 FiNZ의 친근한 AI 재무 코치입니다.\n\n");

        prompt.append("## 사용자 정보\n");
        prompt.append(String.format("- 이름: %s\n", user.getNickname()));
        prompt.append(String.format("- 연령대: %s\n", user.getAgeGroup().getDescription()));
        prompt.append(String.format("- 직업: %s\n\n", user.getJob().getDescription()));

        prompt.append("## 역할\n");
        prompt.append("친근하고 격려하는 톤으로 사용자의 재무 관련 질문에 답변하세요.\n");
        prompt.append("이모지를 적절히 사용하고, 구체적이고 실행 가능한 조언을 제공하세요.\n");

        return prompt.toString();
    }

    // DB 메시지를 Gemini API 형식으로 변환
    private List<GeminiMessage> convertToGeminiFormat(List<CoachMessage> messages) {
        return messages.stream()
                .map(msg -> GeminiMessage.builder()
                        .role(msg.getSender() == MessageSender.USER ? "user" : "model")
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());
    }
}
