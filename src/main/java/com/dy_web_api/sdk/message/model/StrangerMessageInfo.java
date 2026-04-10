package com.dy_web_api.sdk.message.model;

import lombok.Data;

/**
 * 陌生人消息结构
 */
@Data
public class StrangerMessageInfo {
    private Long conversationShortId;
    private String conversationId;
    private String contentJson;
    private Long senderId;
    private Long receiverId;
    private String senderSecUid;
    private String receiverSecUid;
}