package com.lims.common.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessage {
    /** NOTIFICATION/TODO/REFRESH */
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private LocalDateTime time = LocalDateTime.now();

    public static WsMessage notification(String title, String content, String bizType, Long bizId) {
        return new WsMessage("NOTIFICATION", title, content, bizType, bizId, LocalDateTime.now());
    }

    public static WsMessage todo(String title, String content, String bizType, Long bizId) {
        return new WsMessage("TODO", title, content, bizType, bizId, LocalDateTime.now());
    }
}
