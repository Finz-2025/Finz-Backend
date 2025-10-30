package com.finz.dto.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
public class HomeDetailsResponseDto {

    // (참고) DailyIncomeDto.java가 없으므로 Object로 처리하고 빈 리스트를 반환합니다.
    private final List<Object> incomes;

    private final List<DailyExpenseDto> expenses;

    public HomeDetailsResponseDto(List<Object> incomes, List<DailyExpenseDto> expenses) {
        this.incomes = incomes;
        this.expenses = expenses;
    }
}
