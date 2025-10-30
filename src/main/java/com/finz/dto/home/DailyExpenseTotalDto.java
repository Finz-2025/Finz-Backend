package com.finz.dto.home;

import java.time.LocalDate;

public interface DailyExpenseTotalDto {
    LocalDate getDate();
    Integer getTotalAmount();
}