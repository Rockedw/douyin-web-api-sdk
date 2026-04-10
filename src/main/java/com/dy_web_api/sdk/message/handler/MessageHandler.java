package com.dy_web_api.sdk.message.handler;


import com.alibaba.fastjson2.JSONObject;

import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.model.DouyinMessage;

import com.dy_web_api.sdk.message.utils.ProtobufUtils;
import lombok.extern.slf4j.Slf4j;


import java.nio.ByteBuffer;

/**
 * 消息处理器
 */
@Slf4j
public class MessageHandler {

    private final DouyinConfig config;

    public MessageHandler(DouyinConfig config) {
        this.config = config;
    }

    /**
     * 解析WebSocket接收到的消息
     */
    public DouyinMessage parseMessage(ByteBuffer bytes) {
        try {
            // 这里需要根据实际的protobuf定义来解析
            // 由于原代码中使用了自定义的protobuf类，这里提供一个通用的解析框架

            // 解析PushFrame
//            byte[] data = new byte[bytes.remaining()];
//            bytes.get(data);

            // 使用ProtobufUtils解析消息
            JSONObject messageData = ProtobufUtils.parseMessage(bytes);

            if (messageData == null) {
                return null;
            }

            // 构建DouyinMessage对象
            return DouyinMessage.builder()
                    .messageId(messageData.getString("messageId"))
                    .conversationId(messageData.getString("conversationId"))
                    .conversationShortId(messageData.getLong("conversationShortId"))
                    .senderId(messageData.getLong("senderId"))
                    .content(messageData.getJSONObject("content"))
                    .rawContent(messageData.getString("rawContent"))
                    .messageType(messageData.getInteger("messageType"))
                    .timestamp(System.currentTimeMillis())
                    .senderSecUid(messageData.getString("senderSecUid"))
                    .msgType(messageData.getString("msgType"))
                    .build();

        } catch (Exception e) {
            log.error("解析消息异常", e);
            return null;
        }
    }

}