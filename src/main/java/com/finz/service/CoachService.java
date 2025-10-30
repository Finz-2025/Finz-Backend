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
import com.finz.infrastructure.gemini.GeminiApiClient;
import com.finz.infrastructure.gemini.dto.GeminiMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finz.domain.expense.Expense;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Transactional(readOnly = true) // 데이터 변경이 없는 조회 작업
    public List<CoachMessageDto> getChatHistory(Long userId) {
        log.info("대화 내역 조회 - userId: {}", userId);

        // 1. Repository를 통해 엔티티 조회 (시간 오름차순)
        List<CoachMessage> messages = messageRepository.findByUserIdOrderByCreatedAtAsc(userId);

        // 2. 엔티티 리스트를 DTO 리스트로 변환
        return messages.stream()
                .map(CoachMessageDto::fromEntity) // DTO의 팩토리 메서드 사용
                .collect(Collectors.toList());
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
            .messageType(MessageType.GOAL_SETTING)
            .content(initialMessage)
            .build();

        messageRepository.save(aiMessage);

        log.info("목표 설정 대화 시작 완료 - messageId: {}", aiMessage.getMessageId());

        return CoachResponseDto.builder()
            .message(initialMessage)
            .messageType(MessageType.GOAL_SETTING)
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
        String systemPrompt = request.getMessageType() == MessageType.GOAL_SETTING
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

        // 7. 목표 제안 추출
        GoalSuggestion suggestion = null;
        if (request.getMessageType() == MessageType.GOAL_SETTING) {
            suggestion = extractGoalFromResponse(aiResponse);
        }

        return CoachResponseDto.builder()
            .message(aiResponse)
            .messageType(request.getMessageType())
            .suggestedGoal(suggestion)
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

    // AI 응답에서 목표 정보 추출
    private GoalSuggestion extractGoalFromResponse(String response) {
        Pattern amountPattern = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)\\s*(?:만\\s*)?원");
        Matcher matcher = amountPattern.matcher(response);

        if (matcher.find()) {
            String amountStr = matcher.group(1).replace(",", "");
            int amount = Integer.parseInt(amountStr);

            if (response.contains("만원") || response.contains("만 원")) {
                amount *= 10000;
            }

            String goalType = "저축";
            if (response.contains("줄이") || response.contains("절약")) {
                goalType = "지출 줄이기";
            } else if (response.contains("투자")) {
                goalType = "투자";
            }

            return GoalSuggestion.builder()
                .goalType(goalType)
                .targetAmount(amount)
                .durationMonths(1)
                .build();
        }

        return null;
    }

    @Transactional
    public void processNewExpenseRecord(Long userId, Expense expense) {
        log.info("[User: {}] 신규 지출 기록 처리 시작 - ExpenseId: {}", userId, expense.getId());

        // 지출 내역을 "USER" 메시지로 변환하여 DB 저장
        String userContent = String.format(
                "[지출 기록 📝] %s | %s | %,d원",
                expense.getCategory().getDescription(),
                expense.getExpenseName(),
                expense.getAmount()
        );

        CoachMessage userMsg = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.USER)
                .messageType(MessageType.EXPENSE_RECORD) // (MessageType에 EXPENSE_RECORD가 있어야 함)
                .content(userContent)
                .build();
        messageRepository.save(userMsg);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String systemPrompt = buildExpenseFeedbackPrompt(user, expense);

        String aiResponse = geminiClient.chat(
                systemPrompt,
                Collections.emptyList(),
                userContent
        );

        CoachMessage aiMsg = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.AI)
                .messageType(MessageType.EXPENSE_RECORD)
                .content(aiResponse)
                .build();
        messageRepository.save(aiMsg);

        log.info("[User: {}] 지출 기록 피드백 생성 완료 - MessageId: {}", userId, aiMsg.getMessageId());
    }


    private String buildExpenseFeedbackPrompt(User user, Expense expense) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 FiNZ의 긍정적이고 격려하는 AI 재무 코치입니다.\n");
        prompt.append("사용자가 방금 앱에 지출 내역을 기록했으며, 당신은 이 지출에 대해 **즉각적이고 짧은 피드백**을 제공해야 합니다.\n\n");

        prompt.append("## 사용자 정보\n");
        prompt.append(String.format("- 이름: %s\n", user.getNickname()));
        prompt.append(String.format("- 월 목표 예산: %,d원\n\n", user.getMonthlyBudget()));

        prompt.append("## 방금 기록된 지출\n");
        prompt.append(String.format("- 카테고리: %s\n", expense.getCategory().getDescription()));
        prompt.append(String.format("- 금액: %,d원\n", expense.getAmount()));
        prompt.append(String.format("- 내용: %s\n\n", expense.getExpenseName()));

        // (추후 고도화: 이 카테고리의 예산 대비 사용 현황을 여기에 추가하면 좋습니다)
        // 예: prompt.append("- 현재 '식비' 예산의 70%를 사용했습니다.\n\n");

        prompt.append("## 당신의 역할과 말투 (매우 중요)\n");
        prompt.append("1. **긍정적이고 격려하는 톤**을 사용하세요. (예: '기록 완료! 꼼꼼하시네요 👍')\n");
        prompt.append("2. **절대 비난하거나 지적하지 마세요.** (나쁜 예: '또 돈을 쓰셨네요.', '지출이 너무 많아요.')\n");
        prompt.append("3. 한두 문장으로 **짧고 간결하게** 피드백하세요.\n");
        prompt.append("4. 예산에 큰 영향을 주는 지출이라면 가볍게 주의를 환기시킬 수 있습니다. (예: '큰 지출이 있었네요! 월말까지 예산 관리 잘해봐요! 🔥')\n");
        prompt.append("5. 이모지를 1~2개 사용하여 친근감을 표현하세요.\n");

        return prompt.toString();
    }
}
