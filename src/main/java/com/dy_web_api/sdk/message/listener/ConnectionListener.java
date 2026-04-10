package com.dy_web_api.sdk.message.listener;


import com.dy_web_api.sdk.message.model.ConnectionStatus;

/**
 * 连接状态监听器接口
 */
public interface ConnectionListener {

    /**
     * 连接状态变化时的回调
     * @param status 新的连接状态
     */
    void onStatusChanged(ConnectionStatus status);

    /**
     * 连接出错时的回调
     * @param e 异常信息
     */
    default void onError(Exception e) {
        // 默认空实现
    }
}