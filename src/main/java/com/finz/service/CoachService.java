package com.finz.service;

import com.finz.domain.coach.*;
import com.finz.domain.expense.ExpensePattern;
import com.finz.domain.expense.TagExpenseSummary;
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
import com.finz.domain.expense.Expense;

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
    public GlobalResponseDto<CoachResponseDto> requestGoalConsult(Long userId) {
        log.info("목표 상담 요청 - userId: {}", userId);
        
        CoachResponseDto data = startGoalSettingConversation(userId);

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
        log.info("지출 상담 요청 - userId: {}", userId);
        
        CoachResponseDto data = startExpenseConsultConversation(userId);

        return GlobalResponseDto.<CoachResponseDto>builder()
                .status(200)
                .success(true)
                .message("지출 상담이 시작되었습니다.")
                .data(data)
                .build();
    }

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
        String initialMessage = geminiClient.generateInitialMessage(systemPrompt);

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
    
    // 지출 상담 대화 시작
    @Transactional
    public CoachResponseDto startExpenseConsultConversation(Long userId) {
        
        log.info("지출 상담 대화 시작 - userId: {}", userId);
        
        // 1. 사용자 정보 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 2. 최근 1개월 지출 패턴 분석
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        List<ExpensePattern> expenses = expenseRepository
            .findRecentPatternsByUserId(userId, oneMonthAgo);
        
        // 3. 현재 목표 조회 (선택)
        List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE);
        
        // 4. 개인화된 지출 상담 프롬프트 생성
        String systemPrompt = buildExpenseConsultPrompt(user, expenses, activeGoals);
        
        log.debug("Expense Consult Prompt: {}", systemPrompt);
        
        // 5. Gemini API 호출
        String initialMessage = geminiClient.generateInitialMessage(systemPrompt);
        
        // 6. AI 첫 메시지 저장
        CoachMessage aiMessage = CoachMessage.builder()
            .userId(userId)
            .sender(MessageSender.AI)
            .messageType(MessageType.EXPENSE_CONSULT)
            .content(initialMessage)
            .build();
        
        messageRepository.save(aiMessage);
        
        log.info("지출 상담 대화 시작 완료 - messageId: {}", aiMessage.getMessageId());
        
        return CoachResponseDto.builder()
            .message(initialMessage)
            .messageType(MessageType.EXPENSE_CONSULT)
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
        String systemPrompt;
        if (request.getMessageType() == MessageType.GOAL_SETTING) {
            systemPrompt = buildGoalSettingPrompt(user, goals, expenses);
        } else if (request.getMessageType() == MessageType.EXPENSE_CONSULT) {
            systemPrompt = buildExpenseConsultPrompt(user, expenses, goals);
        } else {
            systemPrompt = buildGeneralChatPrompt(user, goals, expenses);
        }

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
            .message(aiResponse)
            .messageType(request.getMessageType())
            .build();
    }

    // 개인화된 목표 설정 시스템 프롬프트 생성
    private String buildGoalSettingPrompt(User user, List<Goal> goals, List<ExpensePattern> expenses) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 Finz의 친근한 AI 재무 코치입니다.\n");
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
    
    // 지출 상담 전용 시스템 프롬프트 생성
    private String buildExpenseConsultPrompt(User user, List<ExpensePattern> expenses, List<Goal> goals) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 Finz의 친근한 AI 재무 코치입니다.\n");
        prompt.append("사용자가 '지출 상담' 버튼을 눌러서 대화를 시작했습니다.\n\n");
        
        // 사용자 개인 정보
        prompt.append("## 사용자 정보\n");
        prompt.append(String.format("- 이름: %s\n", user.getNickname()));
        prompt.append(String.format("- 연령대: %s\n", user.getAgeGroup().getDescription()));
        prompt.append(String.format("- 직업: %s\n", user.getJob().getDescription()));
        prompt.append(String.format("- 월 목표 예산: %,d원\n\n", user.getMonthlyBudget()));
        
        // 지출 패턴 분석 (핵심!)
        if (!expenses.isEmpty()) {
            prompt.append("## 최근 1개월 지출 패턴 (중요!)\n");
            
            // 총 지출액 계산
            long totalExpense = expenses.stream()
                .mapToLong(ExpensePattern::getTotalAmount)
                .sum();
            
            prompt.append(String.format("- 총 지출액: %,d원\n", totalExpense));
            prompt.append(String.format("- 예산 대비: %d%%\n\n", (totalExpense * 100 / user.getMonthlyBudget())));
            
            // 카테고리별 상위 5개
            prompt.append("### 카테고리별 지출 (상위 5개)\n");
            int limit = Math.min(5, expenses.size());
            for (int i = 0; i < limit; i++) {
                ExpensePattern expense = expenses.get(i);
                long percentage = (expense.getTotalAmount() * 100) / totalExpense;
                prompt.append(String.format("%d. %s: %,d원 (%d%%, %d회)\n",
                    i + 1,
                    expense.getCategory().getDescription(),
                    expense.getTotalAmount(),
                    percentage,
                    expense.getCount()));
            }
            prompt.append("\n");
        } else {
            prompt.append("## 최근 1개월 지출 패턴\n");
            prompt.append("- 아직 지출 내역이 없습니다.\n\n");
        }
        
        // 활성 목표 (있다면)
        if (!goals.isEmpty()) {
            prompt.append("## 현재 진행 중인 목표\n");
            for (Goal goal : goals) {
                int progress = (int) ((goal.getCurrentAmount() * 100.0) / goal.getTargetAmount());
                prompt.append(String.format("- %s: %,d원 목표 (현재 %d%% 달성)\n",
                    goal.getGoalType(), goal.getTargetAmount(), progress));
            }
            prompt.append("\n");
        }
        
        // AI의 역할
        prompt.append("## 당신의 역할과 말투\n");
        prompt.append("1. 친근하고 격려하는 존댓말 사용\n");
        prompt.append("2. 이모지를 적절히 활용 (💰, 📊, 💡, 🎯, 👍 등)\n");
        prompt.append("3. 지출 패턴을 분석해 구체적인 절약 방법 제안\n");
        prompt.append("4. 비난하지 말고, 개선점을 긍정적으로 제시\n");
        prompt.append("5. 실천 가능한 작은 변화 제안\n\n");
        
        // 대화 진행 가이드
        prompt.append("## 대화 진행 방법\n");
        prompt.append("1. 친근하게 인사하며 지출 패턴 언급\n");
        prompt.append("2. 가장 많이 지출한 카테고리 지적\n");
        prompt.append("3. 예산 대비 사용률 피드백\n");
        prompt.append("4. 구체적인 절약 방법 제안\n");
        prompt.append("5. 사용자의 의견 물어보기\n\n");
        
        // 첫 메시지 작성 가이드
        prompt.append("## 첫 메시지 작성 가이드\n");
        prompt.append(String.format("- %s님의 이름을 부르며 시작하세요\n", user.getNickname()));
        
        if (!expenses.isEmpty()) {
            ExpensePattern topExpense = expenses.get(0);
            prompt.append(String.format("- 최근 '%s'에 가장 많이 지출했다는 점을 자연스럽게 언급하세요\n",
                topExpense.getCategory().getDescription()));
            
            // 예산 초과 여부
            long totalExpense = expenses.stream()
                .mapToLong(ExpensePattern::getTotalAmount)
                .sum();
            if (totalExpense > user.getMonthlyBudget()) {
                prompt.append("- 예산을 초과했다는 점을 부드럽게 지적하고 절약 방법을 제안하세요\n");
            } else {
                prompt.append("- 예산 안에서 잘 관리하고 있다고 칭찬하되, 더 절약할 수 있는 팁을 제공하세요\n");
            }
        }
        
        return prompt.toString();
    }

    // 일반 대화용 시스템 프롬프트
    private String buildGeneralChatPrompt(User user, List<Goal> goals, List<ExpensePattern> expenses) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 Finz의 친근한 AI 재무 코치입니다.\n\n");
        
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

    @Transactional
    public void processNewExpenseRecord(Long userId, Expense expense) {

        log.info("[User: {}] 신규 지출 기록 처리 시작 - ExpenseId: {}", userId, expense.getId());

        // 1. 지출 내역을 "USER" 메시지로 변환하여 DB 저장
        String userContent = String.format(
                "[지출 기록 📝] %s | %s | %,d원 (태그: #%s)",
                expense.getCategory().getDescription(),
                expense.getExpenseName(),
                expense.getAmount(),
                expense.getExpenseTag() != null ? expense.getExpenseTag() : "없음"
        );

        CoachMessage userMsg = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.USER)
                .messageType(MessageType.EXPENSE_RECORD)
                .content(userContent)
                .build();
        messageRepository.save(userMsg);

        // 2. AI 피드백 생성을 위한 컨텍스트(사용자) 수집
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // --- (컨텍스트 수집 고도화) ---
        // 3. "이번 달"의 시작일 계산
        LocalDate startOfMonth = expense.getExpenseDate().withDayOfMonth(1);

        // 4. "이번 달"의 총 지출액 및 남은 예산 계산
        Integer totalSpentThisMonth = expenseRepository.findTotalAmountByUserIdAndDateAfter(userId, startOfMonth);
        Integer remainingBudget = user.getMonthlyBudget() - totalSpentThisMonth;

        // 5. (핵심) 태그 기반 심층 분석
        String currentTag = expense.getExpenseTag();
        TagExpenseSummary tagSummary = null; // 기본값 null

        if (currentTag != null && !currentTag.isEmpty()) {
            // 이번 달에 이 태그를 몇 번 썼는지, 총 얼마 썼는지 조회
            tagSummary = expenseRepository.findTagSummaryByUserIdAndTagAfter(
                    userId,
                    currentTag,
                    startOfMonth
            );
            log.info("[User: {}] 태그 '#{}' 분석: {}회 / {}원", userId, currentTag, tagSummary.getCount(), tagSummary.getTotalAmount());
        }
        // --- (고도화 끝) ---

        // 6. 지출 피드백 전용 시스템 프롬프트 생성 (모든 정보 전달)
        String systemPrompt = buildExpenseFeedbackPrompt(
                user,
                expense,
                totalSpentThisMonth,
                remainingBudget,
                tagSummary // 5번에서 조회한 태그 정보 (null일 수 있음)
        );

        // 7. Gemini API 호출
        String aiResponse = geminiClient.chat(
                systemPrompt,
                Collections.emptyList(),
                userContent // (chat 메서드 형식을 맞추기 위해 전달)
        );

        // 8. AI 응답 DB 저장
        CoachMessage aiMsg = CoachMessage.builder()
                .userId(userId)
                .sender(MessageSender.AI)
                .messageType(MessageType.EXPENSE_RECORD)
                .content(aiResponse)
                .build();
        messageRepository.save(aiMsg);

        log.info("[User: {}] 지출 기록 피드백 생성 완료 - MessageId: {}", userId, aiMsg.getMessageId());
    }


    private String buildExpenseFeedbackPrompt(
            User user,
            Expense expense,
            Integer totalSpentThisMonth,
            Integer remainingBudget,
            TagExpenseSummary tagSummary // <-- 파라미터 추가
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 FiNZ의 긍정적이고 격려하는 AI 재무 코치입니다.\n");
        prompt.append("사용자가 방금 앱에 지출 내역을 기록했으며, 당신은 이 지출에 대해 **즉각적이고 짧은 피드백**을 제공해야 합니다.\n\n");

        prompt.append("## 1. 사용자 정보\n");
        prompt.append(String.format("- 이름: %s\n", user.getNickname()));
        prompt.append(String.format("- 월 목표 예산: %,d원\n\n", user.getMonthlyBudget()));

        prompt.append("## 2. 방금 기록된 지출 (분석 대상)\n");
        prompt.append(String.format("- 카테고리: %s\n", expense.getCategory().getDescription()));
        prompt.append(String.format("- 금액: %,d원\n", expense.getAmount()));
        prompt.append(String.format("- 내용: %s\n", expense.getExpenseName()));
        if (expense.getExpenseTag() != null && !expense.getExpenseTag().isEmpty()) {
            prompt.append(String.format("- 태그: #%s\n", expense.getExpenseTag()));
        }
        prompt.append("\n");

        prompt.append("## 3. 현재 재무 상태 (중요 맥락)\n");
        prompt.append(String.format("- 이번 달 총 지출액: %,d원\n", totalSpentThisMonth));
        prompt.append(String.format("- 남은 예산: %,d원\n\n", remainingBudget));

        // --- (핵심 수정) ---
        prompt.append("## 4. 태그 심층 분석 (Contextual Insight)\n");
        if (tagSummary != null) {
            prompt.append(String.format("- 사용자는 '#%s' 태그를 이번 달에 %d회 사용했습니다.\n",
                    expense.getExpenseTag(), tagSummary.getCount()));
            prompt.append(String.format("- 이 태그로만 총 %,d원을 지출했습니다.\n\n",
                    tagSummary.getTotalAmount()));
        } else {
            prompt.append("- 이 지출에는 태그가 없습니다.\n\n");
        }
        // --- (수정 끝) ---

        prompt.append("## 5. 당신의 임무 (매우 중요)\n");
        prompt.append("당신은 **두 부분**으로 구성된 **매우 짧은** 피드백을 생성해야 합니다.\n");
        prompt.append("1. **(코멘트)**: '방금 기록된 지출(2번)'에 대해 1~2문장으로 긍정적/중립적 코멘트를 하세요.\n");
        prompt.append("2. **(브리핑)**: '현재 재무 상태(3번)'와 **특히 '태그 분석(4번)'**을 결합하여 **남은 예산**과 **태그 사용 현황**을 간결하게 브리핑하세요.\n\n");

        prompt.append("## 6. 말투 및 제약사항\n");
        prompt.append("- **절대 비난 금지.** (나쁜 예: '또 돈을 쓰셨네요.')\n");
        prompt.append("- 긍정적/격려하는 톤, 친근한 존댓말, 이모지 1~2개 사용.\n");
        prompt.append("- **반드시 한두 문장으로 매우 짧게** 요약하세요.\n");
        prompt.append(String.format("- UI 예시 (태그 O): '기분 전환 간식이군요! 🧁 이번 달 '#스트레스' 태그로 %s번째 지출이네요. 남은 예산은 %,d원입니다! 🔥'\n",
                (tagSummary != null ? tagSummary.getCount() : 1), remainingBudget)); // 예시도 동적으로
        prompt.append(String.format("- UI 예시 (태그 X): '기록 완료! 꼼꼼하시네요 👍. 남은 예산은 %,d원입니다!'\n\n", remainingBudget));

        prompt.append("위 모든 정보를 바탕으로, 사용자의 방금 지출(2번)에 대한 '코멘트'와 '브리핑'을 포함한 피드백을 작성하세요:");

        return prompt.toString();
    }
}
