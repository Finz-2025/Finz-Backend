package com.finz.infrastructure.gemini.dto;

import lombok.Data;

import java.util.List;

@Data
public class GeminiResponse {
    private List<Candidate> candidates;
    
    @Data
    public static class Candidate {
        private GeminiRequest.Content content;
    }
}
