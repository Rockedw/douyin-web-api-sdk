package com.dy_web_api.sdk.message.utils;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dy_web_api.sdk.message.protobuf.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * Protobuf消息处理工具类
 */
@Slf4j
public class ProtobufUtils {

    /**
     * 解析protobuf消息
     * 注意：这里需要根据实际的protobuf定义来实现
     */
    public static JSONObject parseMessage(ByteBuffer bytes) {
        try {
            // 这里应该根据实际的protobuf定义来解析
            // 由于原代码中使用了自定义的protobuf类，这里提供一个模拟实现


            PushFrame frame = PushFrame.parseFrom(bytes);
            byte[] payload = frame.getPayload().toByteArray();
            Response response = Response.parseFrom(payload);
            ResponseBody responseBody = response.getBody();
            if(! responseBody.hasHasNewMessageNotify()) {
                log.info("收到消息: {}", responseBody);
                return null;
            }

            NewMessageNotify notify = responseBody.getHasNewMessageNotify();
            MessageBody message = notify.getMessage();

            int messageType = message.getMessageType();
            if(messageType == 50001) return null;
            JSONObject content = JSON.parseObject(message.getContent());

            JSONObject result = new JSONObject();
            String conversationId = message.getConversationId();
            Long conversationShortId = message.getConversationShortId();
            result.put("messageId", message.getServerMessageId());
            result.put("conversationId", conversationId);
            result.put("conversationShortId", conversationShortId);
            result.put("senderSecUid", message.getSecSender());
            long sender = message.getSender();
            result.put("senderId", sender);
            result.put("content", content);
            result.put("rawContent", message.getContent());
            result.put("messageType", messageType);
            result.put("timestamp", System.currentTimeMillis());
            result.put("msgType","text");

            return result;

        } catch (Exception e) {
            log.error("解析protobuf消息异常", e);
            return null;
        }
    }

    /**
     * 构建protobuf消息
     */
    public static byte[] buildMessage(String messageId, String conversationId, String content) {
        try {
            // 这里应该根据实际的protobuf定义来构建消息
            // 返回构建好的protobuf字节数组
            return new byte[0]; // placeholder

        } catch (Exception e) {
            log.error("构建protobuf消息异常", e);
            return new byte[0];
        }
    }
}