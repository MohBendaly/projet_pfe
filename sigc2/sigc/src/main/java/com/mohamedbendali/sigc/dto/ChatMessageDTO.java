package com.mohamedbendali.sigc.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private Long id;
    private String content;
    private boolean isFromBot;
    private Long interviewId;
    private LocalDateTime timestamp;
}