package com.dy_web_api.sdk.message.listener;


import com.dy_web_api.sdk.message.model.DouyinMessage;

/**
 * 消息监听器接口
 */
public interface MessageListener {

    /**
     * 收到消息时的回调
     * @param message 收到的消息
     */
    void onMessageReceived(DouyinMessage message);

    /**
     * 消息处理出错时的回调
     * @param e 异常信息
     */
    default void onError(Exception e) {
        // 默认空实现
    }
}