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
    
    // 목표 설정 대화 첫 메시지 생성
    public String generateInitialGoalMessage(String systemPrompt) {
        
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
                .text("위 정보를 바탕으로 목표 설정 대화를 시작하는 첫 메시지를 작성해주세요.")
                .build()))
            .build());
        
        return callGeminiApi(contents);
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
    
    // Gemini API 호출
    private String callGeminiApi(List<GeminiRequest.Content> contents) {
        
        String url = apiUrl + "?key=" + apiKey;
        
        GeminiRequest request = GeminiRequest.builder()
            .contents(contents)
            .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            log.debug("Gemini API 호출 - URL: {}", url);
            
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
                
                log.debug("Gemini API 응답: {}", text);
                return text;
            }
            
            throw new RuntimeException("Gemini API 응답이 비어있습니다.");
            
        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new RuntimeException("AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
