package com.finz.controller;
import com.finz.dto.GlobalResponseDto;
import com.finz.dto.expense.CreateExpenseResponseDto;
import com.finz.dto.expense.ExpenseDetailResponseDto;
import com.finz.dto.expense.ExpenseRequestDto;
import com.finz.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expense")
public class ExpenseController {

    private final ExpenseService expenseService;
    // private final AuthService authService; // 실제로는 토큰 검증 로직 필요

    @PostMapping
    public ResponseEntity<GlobalResponseDto<CreateExpenseResponseDto>> createExpense(
            @RequestHeader("accessToken") String accessToken, // 헤더의 토큰, required가 아니라서 일단 검증 로직 구현은 안 했습니다.
            @RequestBody ExpenseRequestDto requestDto       // 본문의 DTO
    ) {

        CreateExpenseResponseDto data = expenseService.createExpense(requestDto);

        // GlobalResponseDto로 래핑
        GlobalResponseDto<CreateExpenseResponseDto> response = GlobalResponseDto.<CreateExpenseResponseDto>builder()
                .status(200)
                .success(true)
                .message("지출이 입력되었습니다.")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    // 건당 상세내역 조회
    @GetMapping("/{expenseId}")
    public ResponseEntity<GlobalResponseDto<ExpenseDetailResponseDto>> getExpenseDetail(
            @PathVariable Long expenseId
    ) {
        ExpenseDetailResponseDto data = expenseService.getExpenseDetail(expenseId);

        // GlobalResponseDto로 래핑
        GlobalResponseDto<ExpenseDetailResponseDto> response = GlobalResponseDto.<ExpenseDetailResponseDto>builder()
                .status(200)
                .success(true)
                .message("지출 내역 상세 조회 성공")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}