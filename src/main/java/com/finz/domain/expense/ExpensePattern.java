package com.finz.domain.expense;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePattern {
    private ExpenseCategory category;  // 카테고리 (Enum)
    private Long totalAmount;          // 총 지출액
    private Long count;                // 지출 횟수
}
