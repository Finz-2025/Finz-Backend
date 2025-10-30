package com.finz.service;

import com.finz.domain.user.User;
import com.finz.domain.user.UserRepository;
import com.finz.repository.ExpenseRepository;
import com.finz.dto.home.HomeSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // (조회만 하므로 readOnly=true)
public class HomeService {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public HomeSummaryResponseDto getHomeSummary(Long userId) {

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Integer monthlyBudget = user.getMonthlyBudget();

        // 이번 달의 총 지출액 조회
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        Integer totalExpense = expenseRepository.findTotalAmountByUserIdAndDateAfter(
                userId,
                startOfMonth
        );

        // 남은 예산 계산
        Integer remainingBudget = monthlyBudget - totalExpense;

        // 진행률(퍼센트) 계산
        Double progressRate = 0.0;
        if (monthlyBudget != null && monthlyBudget > 0) {
            progressRate = (double) totalExpense / monthlyBudget;
        }

        log.info("[User: {}] 홈 요약 조회: 예산(%), 총지출(%), 남은금액(%), 진행률({})",
                userId, monthlyBudget, totalExpense, remainingBudget, progressRate);

        return new HomeSummaryResponseDto(totalExpense, remainingBudget, progressRate);
    }
}