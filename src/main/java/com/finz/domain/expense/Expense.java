package com.finz.domain.expense;

import com.finz.domain.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor()
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "expense")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // user_id 컬럼으로 User와 조인
    private User user;

    @Column(name = "expense_name", length = 20, nullable = false)
    private String expenseName;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @Column(name = "expense_tag", length = 20,nullable = false)
    private String expenseTag;

    @Column(name = "memo", length = 50)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Expense(User user, String expenseName, Integer amount, ExpenseCategory category, String expenseTag,
                   String memo, PaymentMethod paymentMethod, LocalDate expenseDate) {
        this.user = user;
        this.expenseName = expenseName;
        this.amount = amount;
        this.category = category;
        this.expenseTag = expenseTag;
        this.memo = memo;
        this.paymentMethod = paymentMethod;
        this.expenseDate = expenseDate;
    }
}
