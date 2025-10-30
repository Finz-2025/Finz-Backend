package com.finz.dto.coach;

import com.finz.domain.coach.CoachMessage;
import com.finz.domain.coach.MessageSender;
import com.finz.domain.coach.MessageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 과거 채팅 내역 조회용 DTO
@Getter
@Builder
public class CoachMessageDto {
    private Long messageId;
    private MessageSender sender;
    private MessageType messageType;
    private String content;
    private LocalDateTime createdAt;

    public static CoachMessageDto fromEntity(CoachMessage entity) {
        return CoachMessageDto.builder()
                .messageId(entity.getMessageId())
                .sender(entity.getSender())
                .messageType(entity.getMessageType())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
