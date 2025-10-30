package com.finz.controller;

import com.finz.dto.GlobalResponseDto; // (GlobalResponseDto 경로)
import com.finz.dto.home.HomeSummaryResponseDto;
import com.finz.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
