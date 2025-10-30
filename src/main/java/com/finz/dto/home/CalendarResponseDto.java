package com.finz.dto.home;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class CalendarResponseDto {
    private List<CalendarDayDto> dates;
}