package com.finz.service;

import com.finz.domain.expense.Expense;
import com.finz.domain.expense.ExpenseCategory;
import com.finz.domain.user.User;
import com.finz.domain.user.UserRepository;
import com.finz.dto.home.*;
import com.finz.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

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

    public HomeDetailsResponseDto getHomeDetails(Long userId, LocalDate date) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 지출 내역 조회
        List<Expense> expenses = expenseRepository.findByUserAndExpenseDate(user, date);

        // DTO 리스트로 변환
        List<DailyExpenseDto> expenseDtos = expenses.stream()
                .map(DailyExpenseDto::fromEntity)
                .collect(Collectors.toList());

        // TODO: 수입 입력 구현 후 수정 요망... 혹은 그냥 수입은 빼고 가기....
        // Incomes는 빈 리스트로 반환
        List<Object> incomeDtos = Collections.emptyList();

        log.info("[User: {}] {} 날짜 세부 내역 조회 (지출: {}건)", userId, date, expenseDtos.size());

        return new HomeDetailsResponseDto(incomeDtos, expenseDtos);
    }

    /**
     * 홈 화면 하이라이트 조회
     */
    public HomeHighlightResponseDto getHomeHighlight(Long userId) {

        LocalDate today = LocalDate.now();
        LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfThisWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = endOfThisWeek.minusWeeks(1);

        log.info("[User: {}] 하이라이트 계산 - 이번 주: {} ~ {}", userId, startOfThisWeek, endOfThisWeek);
        log.info("[User: {}] 하이라이트 계산 - 지난주: {} ~ {}", userId, startOfLastWeek, endOfLastWeek);

        // 2. 항목별 계산 (MVP 하드코딩)
        // (수정) "식비" -> "음식"
        HighlightItemDto categoryItem = calculateCategoryHighlight(userId, "음식", startOfThisWeek, endOfThisWeek, startOfLastWeek, endOfLastWeek);
        HighlightItemDto tagItem = calculateTagHighlight(userId, "충동구매", startOfThisWeek, endOfThisWeek, startOfLastWeek, endOfLastWeek);
        HighlightItemDto recommendItem = calculateRecommendedSpend(userId, today);

        return new HomeHighlightResponseDto(categoryItem, tagItem, recommendItem);
    }

    /**
     * (수정) 헬퍼 1: 오늘 권장 지출 계산 (페이스 기반 로직)
     */
    private HighlightItemDto calculateRecommendedSpend(Long userId, LocalDate today) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Integer monthlyBudget = user.getMonthlyBudget();
        if (monthlyBudget == null || monthlyBudget == 0) {
            return HighlightItemDto.builder()
                    .title("오늘 권장 지출")
                    .valueText("예산 미설정")
                    .build();
        }

        int daysInMonth = today.lengthOfMonth();
        Integer dailyPace = monthlyBudget / daysInMonth; // 일일 페이스

        Integer spentToday = expenseRepository.findTotalAmountByUserIdAndDate(userId, today);

        Integer remainingTodaySpend = dailyPace - spentToday;

        if (remainingTodaySpend < 0) {
            return HighlightItemDto.builder()
                    .title("오늘 권장 지출")
                    .valueText(String.format("%,d원 초과", Math.abs(remainingTodaySpend)))
                    .build();
        }

        return HighlightItemDto.builder()
                .title("오늘 권장 지출")
                .valueText(String.format("%,d원", remainingTodaySpend))
                .build();
    }

    /**
     * 헬퍼 2: 카테고리 하이라이트 계산
     */
    private HighlightItemDto calculateCategoryHighlight(Long userId, String categoryName,
                                                        LocalDate thisWeekStart, LocalDate thisWeekEnd,
                                                        LocalDate lastWeekStart, LocalDate lastWeekEnd) {

        // (수정) "음식" 문자열을 -> ExpenseCategory.FOOD_SERVICE (예시) Enum으로 변환
        ExpenseCategory category = ExpenseCategory.fromDescription(categoryName);

        Integer thisWeekAmount = expenseRepository.findTotalAmountByCategoryAndDateRange(
                userId, category, thisWeekStart, thisWeekEnd
        );
        Integer lastWeekAmount = expenseRepository.findTotalAmountByCategoryAndDateRange(
                userId, category, lastWeekStart, lastWeekEnd
        );

        String valueText;

        if (lastWeekAmount == 0 && thisWeekAmount > 0) {
            valueText = "지출 발생";
        } else if (lastWeekAmount == 0 && thisWeekAmount == 0) {
            valueText = "지출 없음";
        } else if (lastWeekAmount > 0) { // 0으로 나누기 방지
            double rate = ((double) thisWeekAmount / lastWeekAmount) - 1.0;
            int percentage = (int) (rate * 100);
            valueText = String.format("%+d%%", percentage); // "+10%", "-18%"
        } else {
            valueText = "N/A";
        }

        return HighlightItemDto.builder()
                .title(categoryName) // "음식"
                .valueText(valueText)
                .build();
    }

    /**
     * 헬퍼 3: 태그 하이라이트 계산
     */
    private HighlightItemDto calculateTagHighlight(Long userId, String tagName,
                                                   LocalDate thisWeekStart, LocalDate thisWeekEnd,
                                                   LocalDate lastWeekStart, LocalDate lastWeekEnd) {

        Integer thisWeekCount = expenseRepository.findCountByTagAndDateRange(
                userId, tagName, thisWeekStart, thisWeekEnd
        );
        Integer lastWeekCount = expenseRepository.findCountByTagAndDateRange(
                userId, tagName, lastWeekStart, lastWeekEnd
        );

        int diff = thisWeekCount - lastWeekCount;
        String valueText;

        if (diff > 0) {
            valueText = String.format("%d회 증가", diff);
        } else if (diff < 0) {
            valueText = String.format("%d회 감소", Math.abs(diff));
        } else {
            valueText = "변동 없음";
        }

        return HighlightItemDto.builder()
                .title(tagName)
                .valueText(valueText)
                .build();
    }

    /**
     * (신규 추가) 달력 데이터 조회 (월별)
     */
    public CalendarResponseDto getCalendarData(Long userId, int year, int month) {

        log.info("[User: {}] 달력 데이터 조회 요청 - Year: {}, Month: {}", userId, year, month);

        // 1. 사용자 및 월 예산 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Integer monthlyBudget = user.getMonthlyBudget();

        // 2. "일일 권장 지출액 (페이스)" 계산
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth(); // 해당 월의 총 일수

        Integer dailyPace = 0; // 예산 미설정 시 '일일 예산'은 0
        if (monthlyBudget != null && monthlyBudget > 0) {
            dailyPace = monthlyBudget / daysInMonth;
        }

        // 3. DB에서 "이번 달 지출 합계" 목록을 1번의 쿼리로 가져옴
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DailyExpenseTotalDto> dailyTotals = expenseRepository.findDailyTotalsByMonth(userId, startDate, endDate);

        // 4. (성능 최적화) 빠른 조회를 위해 List -> Map으로 변환
        Map<LocalDate, Integer> expenseMap = dailyTotals.stream()
                .collect(Collectors.toMap(DailyExpenseTotalDto::getDate, DailyExpenseTotalDto::getTotalAmount));

        // 5. 1일부터 말일까지 루프 돌면서 상태(status) 계산
        List<CalendarDayDto> calendarDays = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = yearMonth.atDay(day);

            int totalAmountToday = expenseMap.getOrDefault(currentDate, 0);

            String status;

            if (totalAmountToday == 0) {
                status = "none"; // 지출 없음
            } else if (dailyPace == 0) {
                status = "normal"; // 예산 미설정시 (0원 초과)는 무조건 normal
            } else if (totalAmountToday > dailyPace) {
                status = "over"; // 일일 예산 초과
            } else {
                status = "normal"; // 일일 예산 범위 내
            }

            calendarDays.add(new CalendarDayDto(currentDate.toString(), status));
        }

        // 6. DTO로 감싸서 반환
        return new CalendarResponseDto(calendarDays);
    }
}