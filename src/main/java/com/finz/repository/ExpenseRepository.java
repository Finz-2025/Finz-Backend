package com.finz.repository;

import com.finz.domain.expense.Expense;
import com.finz.domain.expense.ExpensePattern;
import com.finz.domain.expense.TagExpenseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    // 최근 지출 패턴 분석 (카테고리별 합계)
    @Query("SELECT new com.finz.domain.expense.ExpensePattern(" +
           "e.category, SUM(e.amount), COUNT(e)) " +
           "FROM Expense e " +
           "WHERE e.user.id = :userId " +
           "AND e.expenseDate >= :startDate " +
           "GROUP BY e.category " +
           "ORDER BY SUM(e.amount) DESC")
    List<ExpensePattern> findRecentPatternsByUserId(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate
    );

    // 특정 사용자의 특정 날짜 이후 총 지출액 합산
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.expenseDate >= :startDate")
    Integer findTotalAmountByUserIdAndDateAfter(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    @Query("SELECT COUNT(e.id) as count, COALESCE(SUM(e.amount), 0) as totalAmount " +
            "FROM Expense e " +
            "WHERE e.user.id = :userId AND e.expenseTag = :tag AND e.expenseDate >= :startDate")
    TagExpenseSummary findTagSummaryByUserIdAndTagAfter(
            @Param("userId") Long userId,
            @Param("tag") String tag,
            @Param("startDate") LocalDate startDate
    );
}
