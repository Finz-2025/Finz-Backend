package com.finz.dto.expense;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ExpenseRequestDto {
    private Long user_id;
    private Integer amount;
    private String category;
    private String expense_tag;
    private String memo;
    private String payment_method;
    private LocalDate expense_date;
}