package com.finz.infrastructure.gemini;

import com.finz.infrastructure.gemini.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    
    // 초기 메시지 생성 (범용)
    public String generateInitialMessage(String systemPrompt) {
        
        List<GeminiRequest.Content> contents = new ArrayList<>();
        
        // 시스템 프롬프트를 user 메시지로 추가
        contents.add(GeminiRequest.Content.builder()
            .role("user")
            .parts(List.of(GeminiRequest.Part.builder().text(systemPrompt).build()))
            .build());
        
        // AI에게 첫 메시지 생성 요청
        contents.add(GeminiRequest.Content.builder()
            .role("user")
            .parts(List.of(GeminiRequest.Part.builder()
                .text("위 정보를 바탕으로 상담 대화를 시작하는 첫 메시지를 작성해주세요.")
                .build()))
            .build());
        
        return callGeminiApi(contents);
    }
    
    // 목표 설정 대화 첫 메시지 생성 (하위 호환성을 위해 유지)
    @Deprecated
    public String generateInitialGoalMessage(String systemPrompt) {
        return generateInitialMessage(systemPrompt);
    }
    
    // 대화 진행 (히스토리 포함)
    public String chat(String systemPrompt, List<GeminiMessage> history, String userMessage) {
        
        List<GeminiRequest.Content> contents = new ArrayList<>();
        
        // 1. 시스템 프롬프트
        contents.add(GeminiRequest.Content.builder()
            .role("user")
            .parts(List.of(GeminiRequest.Part.builder().text(systemPrompt).build()))
            .build());
        
        // 2. 대화 히스토리
        for (GeminiMessage msg : history) {
            contents.add(GeminiRequest.Content.builder()
                .role(msg.getRole())
                .parts(List.of(GeminiRequest.Part.builder().text(msg.getContent()).build()))
                .build());
        }
        
        // 3. 현재 사용자 메시지
        contents.add(GeminiRequest.Content.builder()
            .role("user")
            .parts(List.of(GeminiRequest.Part.builder().text(userMessage).build()))
            .build());
        
        return callGeminiApi(contents);
    }
    
    // Gemini API 호출 (재시도 로직 포함)
    private String callGeminiApi(List<GeminiRequest.Content> contents) {
        
        String url = apiUrl + "?key=" + apiKey;
        
        GeminiRequest request = GeminiRequest.builder()
            .contents(contents)
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
        
        // 최대 3번 재시도
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                log.debug("Gemini API 호출 시도 {}/{} - URL: {}", retryCount + 1, maxRetries, url);
                
                GeminiResponse response = restTemplate.postForObject(
                    url,
                    entity,
                    GeminiResponse.class
                );
                
                if (response != null && !response.getCandidates().isEmpty()) {
                    String text = response.getCandidates().get(0)
                        .getContent()
                        .getParts()
                        .get(0)
                        .getText();
                    
                    log.debug("Gemini API 응답 성공");
                    return text;
                }
                
                throw new RuntimeException("Gemini API 응답이 비어있습니다.");
                
            } catch (Exception e) {
                retryCount++;
                
                // 503 에러인 경우
                if (e.getMessage() != null && e.getMessage().contains("503")) {
                    if (retryCount < maxRetries) {
                        log.warn("Gemini API 과부하 (503) - {}초 후 재시도 {}/{}", 
                                retryCount * 2, retryCount, maxRetries);
                        try {
                            Thread.sleep(retryCount * 2000L); // 2초, 4초, 6초
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    } else {
                        log.error("Gemini API 재시도 초과 - 최대 {}번 시도 완료", maxRetries);
                        throw new RuntimeException("AI 서비스가 현재 사용량이 많습니다. 잠시 후 다시 시도해주세요.");
                    }
                }
                
                // 다른 에러는 즉시 throw
                log.error("Gemini API 호출 실패", e);
                throw new RuntimeException("AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
        
        throw new RuntimeException("AI 서비스가 현재 사용량이 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
