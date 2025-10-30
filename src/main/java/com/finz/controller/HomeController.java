package com.finz.controller;

import com.finz.dto.GlobalResponseDto; // (GlobalResponseDto 경로)
import com.finz.dto.home.CalendarResponseDto;
import com.finz.dto.home.HomeDetailsResponseDto;
import com.finz.dto.home.HomeHighlightResponseDto;
import com.finz.dto.home.HomeSummaryResponseDto;
import com.finz.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "홈 화면 요약 API")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/summary")
    @Operation(summary = "홈 화면 요약 정보 조회", description = "이번 달 총 지출, 남은 예산, 진행률을 조회합니다.")
    public ResponseEntity<GlobalResponseDto<HomeSummaryResponseDto>> getHomeSummary(
            @RequestParam("user_id") Long userId
    ) {

        // TODO: (보안) 추후 Spring Security로 사용자 인증 구현

        log.info("홈 요약 정보 조회 요청 - userId: {}", userId);

        HomeSummaryResponseDto data = homeService.getHomeSummary(userId);

        GlobalResponseDto<HomeSummaryResponseDto> response = GlobalResponseDto.<HomeSummaryResponseDto>builder()
                .status(200)
                .success(true)
                .message("요약 정보 조회 성공")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/details")
    @Operation(summary = "홈 화면 일일 세부 내역 조회", description = "특정 날짜의 지출/수입 내역을 조회합니다.")
    public ResponseEntity<GlobalResponseDto<HomeDetailsResponseDto>> getHomeDetails(
            @RequestParam("user_id") Long userId,
            // "2025-10-15" 형식의 문자열을 LocalDate로 자동 변환
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {

        // TODO: (보안)

        log.info("홈 세부 내역 조회 요청 - userId: {}, date: {}", userId, date);

        HomeDetailsResponseDto data = homeService.getHomeDetails(userId, date);

        GlobalResponseDto<HomeDetailsResponseDto> response = GlobalResponseDto.<HomeDetailsResponseDto>builder()
                .status(200)
                .success(true)
                .message("세부 내역 조회 성공")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/highlight")
    @Operation(summary = "홈 화면 하이라이트 조회", description = "이번 주 하이라이트(지난주 대비)를 조회합니다.")
    public ResponseEntity<GlobalResponseDto<HomeHighlightResponseDto>> getHomeHighlight(
            @RequestParam("user_id") Long userId
    ) {

        // TODO: (보안) MVP 이후, Spring Security에서 인증된 ID를 가져오도록 수정.
        log.info("홈 하이라이트 조회 요청 - userId: {}", userId);

        HomeHighlightResponseDto data = homeService.getHomeHighlight(userId);

        GlobalResponseDto<HomeHighlightResponseDto> response = GlobalResponseDto.<HomeHighlightResponseDto>builder()
                .status(200)
                .success(true)
                .message("이번 주 하이라이트 조회 성공")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 홈 화면 달력 데이터 조회 (월별)
     */
    @GetMapping("/calendar")
    @Operation(summary = "홈 화면 달력 데이터 조회", description = "월별 지출 상태(over/normal/none)를 조회합니다.")
    public ResponseEntity<GlobalResponseDto<CalendarResponseDto>> getCalendarData(
            @RequestParam("user_id") Long userId,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        // TODO: (보안) MVP 이후, Spring Security에서 인증된 ID를 가져오도록 수정.
        log.info("달력 데이터 조회 요청 - userId: {}, year: {}, month: {}", userId, year, month);

        CalendarResponseDto data = homeService.getCalendarData(userId, year, month);
        GlobalResponseDto<CalendarResponseDto> response = GlobalResponseDto.<CalendarResponseDto>builder()
                .status(200)
                .success(true)
                .message("달력 데이터 조회 성공")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}
