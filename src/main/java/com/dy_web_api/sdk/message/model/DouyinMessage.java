package com.dy_web_api.sdk.message.model;


import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抖音消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DouyinMessage {
    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 会话ID
     */
    private String conversationId;

    private Long conversationShortId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 消息内容
     */
    private JSONObject content;

    private String rawContent;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 发送者昵称
     */
    private String senderNickname;

    /**
     * 额外数据
     */
    private Object extraData;

    private String mediaUrl;

    private String senderSecUid;

    private String msgType;
}