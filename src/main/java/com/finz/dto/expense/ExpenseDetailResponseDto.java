package com.finz.dto.expense;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.finz.domain.expense.Expense;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ExpenseDetailResponseDto {
    @JsonProperty("expense_id")
    private final Long expenseId;

    @JsonProperty("expense_name")
    private final String expenseName;

    private final Integer amount;
    private final String category;

    @JsonProperty("expense_tag")
    private final String expenseTag;

    private final String memo;

    @JsonProperty("payment_method")
    private final String paymentMethod;

    @JsonProperty("expense_date")
    private final LocalDate expenseDate;


    @Builder
    private ExpenseDetailResponseDto(Long expenseId, String expenseName, Integer amount, String category, String expenseTag,
                                     String memo, String paymentMethod, LocalDate expenseDate) {
        this.expenseId = expenseId;
        this.expenseName = expenseName;
        this.amount = amount;
        this.category = category;
        this.expenseTag = expenseTag;
        this.memo = memo;
        this.paymentMethod = paymentMethod;
        this.expenseDate = expenseDate;
    }

    public static ExpenseDetailResponseDto from(Expense expense) {
        return ExpenseDetailResponseDto.builder()
                .expenseId(expense.getId())
                .expenseName(expense.getExpenseName())
                .amount(expense.getAmount())
                .category(expense.getCategory().getDescription())
                .expenseTag(expense.getExpenseTag())
                .memo(expense.getMemo())
                .paymentMethod(expense.getPaymentMethod().getDescription())
                .expenseDate(expense.getExpenseDate())
                .build();
    }
}