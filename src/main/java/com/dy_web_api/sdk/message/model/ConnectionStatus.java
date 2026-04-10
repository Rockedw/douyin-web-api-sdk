package com.dy_web_api.sdk.message.model;

public enum ConnectionStatus {
    /**
     * 已断开连接
     */
    DISCONNECTED,

    /**
     * 连接中
     */
    CONNECTING,

    /**
     * 已连接
     */
    CONNECTED,

    /**
     * 重连中
     */
    RECONNECTING,

    /**
     * 连接失败
     */
    FAILED
}