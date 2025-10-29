package com.finz.repository;

import com.finz.domain.expense.Expense;
import com.finz.domain.expense.ExpensePattern;
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
}
