package com.finz.dto.coach;

import com.finz.domain.coach.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private String message;
    private MessageType messageType;
}
