package com.finz.controller;

import com.finz.exception.ApiResponse;
import com.finz.exception.BaseException;
import com.finz.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    // 1. 기본 응답 테스트
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("message", "Finz API is running successfully!");

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 2. 성공 응답 - 커스텀 메시지
    @GetMapping("/custom")
    public ResponseEntity<ApiResponse<String>> customSuccess() {
        return ResponseEntity.ok(
                ApiResponse.success("커스텀 메시지 테스트", "초기 설정이 정상적으로 완료되었습니다.")
        );
    }

    // 3. 성공 응답 - 데이터 없음
    @GetMapping("/no-content")
    public ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.ok(
                ApiResponse.successWithNoContent("데이터 없는 성공 응답 테스트")
        );
    }

    // 4. 에러 응답 테스트
    @GetMapping("/error")
    public ResponseEntity<ApiResponse<Void>> errorTest() {
        throw new BaseException(ErrorCode.NOT_FOUND);
    }

    // 5. 예상치 못한 에러 테스트
    @GetMapping("/exception")
    public ResponseEntity<ApiResponse<Void>> exceptionTest() {
        throw new RuntimeException("예상치 못한 에러 발생!");
    }
}