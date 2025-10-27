package com.finz.domain.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor()
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", length = 16, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "job", nullable = false)
    private Job job;

    @Column(name = "monthly_budget", nullable = false)
    private Integer monthlyBudget;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String nickname, AgeGroup ageGroup, Job job, Integer monthlyBudget) {
        this.nickname = nickname;
        this.ageGroup = ageGroup;
        this.job = job;
        this.monthlyBudget = monthlyBudget;
    }
}
