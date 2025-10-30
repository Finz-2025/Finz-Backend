package com.finz.service;

import com.finz.domain.expense.Expense;
import com.finz.domain.expense.ExpenseCategory;
import com.finz.dto.coach.CoachResponseDto;
import com.finz.repository.ExpenseRepository;
import com.finz.domain.expense.PaymentMethod;
import com.finz.dto.expense.CreateExpenseResponseDto;
import com.finz.dto.expense.ExpenseDetailResponseDto;
import com.finz.dto.expense.ExpenseRequestDto;
import com.finz.domain.user.User;
import com.finz.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CoachService coachService;

    @Transactional
    public CreateExpenseResponseDto createExpense(ExpenseRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUser_id())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. id=" + requestDto.getUser_id()));

        // DTO의 문자열을 Enum 상수로 변환
        ExpenseCategory category = ExpenseCategory.fromDescription(requestDto.getCategory());
        PaymentMethod paymentMethod = PaymentMethod.fromDescription(requestDto.getPayment_method());

        Expense expense = Expense.builder()
                .user(user)
                .expenseName(requestDto.getExpense_name()) // (참고: title -> expenseName으로 사용 중이시네요)
                .amount(requestDto.getAmount())
                .category(category)
                .expenseTag(requestDto.getExpense_tag())
                .memo(requestDto.getMemo())
                .paymentMethod(paymentMethod)
                .expenseDate(requestDto.getExpense_date())
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        coachService.processNewExpenseRecord(
                user.getId(),
                savedExpense
        );

        return new CreateExpenseResponseDto(savedExpense.getId());
    }

    // 지출 내역 아이디로 지출 내역 조회하기
    @Transactional(readOnly = true)
    public ExpenseDetailResponseDto getExpenseDetail(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지출 내역을 찾을 수 없습니다. id=" + expenseId));

        return ExpenseDetailResponseDto.from(expense);
    }

    @Transactional
    public CreateExpenseResponseDto updateExpense(Long expenseId, ExpenseRequestDto requestDto) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지출 내역을 찾을 수 없습니다. id=" + expenseId));

        ExpenseCategory category = ExpenseCategory.fromDescription(requestDto.getCategory());
        PaymentMethod paymentMethod = PaymentMethod.fromDescription(requestDto.getPayment_method());

        expense.update(
                requestDto.getExpense_name(),
                requestDto.getAmount(),
                category,
                requestDto.getExpense_tag(),
                requestDto.getMemo(),
                paymentMethod,
                requestDto.getExpense_date()
        );

        // 이 부분은 이미 aiFeedback 없이 expenseId만 반환하고 있었네요.
        return new CreateExpenseResponseDto(expense.getId());
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지출 내역을 찾을 수 없습니다. id=" + expenseId));

        expenseRepository.delete(expense);
    }
}
