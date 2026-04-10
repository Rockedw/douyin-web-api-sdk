package com.dy_web_api.sdk.message.client;


import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.handler.MessageHandler;
import com.dy_web_api.sdk.message.handler.MessageSender;
import com.dy_web_api.sdk.message.listener.ConnectionListener;
import com.dy_web_api.sdk.message.listener.MessageListener;
import com.dy_web_api.sdk.message.model.ConnectionStatus;
import com.dy_web_api.sdk.message.model.DouyinMessage;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 抖音WebSocket客户端
 */
@Slf4j
public class DouyinWebChatClient extends WebSocketClient {

    private final DouyinConfig config;
    private final MessageListener messageListener;
    private final ConnectionListener connectionListener;
    private final MessageHandler messageHandler;
    private final MessageSender messageSender;

    private volatile long lastMessageTime = 0;
    private volatile AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledExecutorService timeoutCheckExecutor;
    /**
     * -- SETTER --
     *  设置连接状态回调
     */
    @Setter
    private Consumer<ConnectionStatus> connectionStatusCallback;

    public DouyinWebChatClient(URI serverUri, DouyinConfig config,
                               MessageListener messageListener,
                               ConnectionListener connectionListener) {
        super(serverUri);
        this.config = config;
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
        this.messageHandler = new MessageHandler(config);
        this.messageSender = new MessageSender(config);

        // 设置连接超时
        this.setConnectionLostTimeout(config.getConnectTimeout() / 1000);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("WebSocket连接已建立");

        lastMessageTime = System.currentTimeMillis();
        reconnectAttempts.set(0);

        // 启动心跳
        startHeartbeat();

        // 启动超时检查
        startTimeoutCheck();

        // 通知连接成功
        notifyConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    public void onMessage(String message) {
        if (!"hi".equals(message)) {
            log.debug("收到文本消息: {}", message);
        }
        lastMessageTime = System.currentTimeMillis();
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        lastMessageTime = System.currentTimeMillis();

        try {
            DouyinMessage message = messageHandler.parseMessage(bytes);
            if (message != null && messageListener != null) {
                messageListener.onMessageReceived(message);
            }
        } catch (Exception e) {
            log.error("处理消息异常", e);
            if (messageListener != null) {
                messageListener.onError(e);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket连接已关闭: code={}, reason={}, remote={}", code, reason, remote);

        stopExecutors();
        notifyConnectionStatus(ConnectionStatus.DISCONNECTED);

        // 如果不是主动关闭且启用自动重连，则尝试重连
        if (remote && config.isAutoReconnect()) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception e) {
        log.error("WebSocket连接异常", e);

        if (connectionListener != null) {
            connectionListener.onError(e);
        }

        if (config.isAutoReconnect()) {
            scheduleReconnect();
        }
    }

    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "douyin-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                if (isOpen()) {
                    sendPing();
                    send("hi");
                }
            } catch (Exception e) {
                log.error("心跳发送异常", e);
            }
        }, 0, config.getHeartbeatInterval(), TimeUnit.SECONDS);
    }

    private void startTimeoutCheck() {
        timeoutCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "douyin-timeout-check");
            t.setDaemon(true);
            return t;
        });

        timeoutCheckExecutor.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMessageTime > config.getMessageTimeout()) {
                log.warn("消息接收超时，可能sessionId已过期");
                close();
            }
        }, 0, 3, TimeUnit.HOURS);
    }

    private void scheduleReconnect() {
        int attempts = reconnectAttempts.get();
        if (attempts >= config.getMaxReconnectAttempts()) {
            log.error("达到最大重连次数，停止重连");
            return;
        }

        // 指数退避算法
        long delay = Math.min(1000L * (long) Math.pow(2, attempts), 30000L);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                log.info("尝试第{}次重连", attempts + 1);
                reconnectAttempts.incrementAndGet();
                notifyConnectionStatus(ConnectionStatus.RECONNECTING);
                reconnect();
            } catch (Exception e) {
                log.error("重连失败", e);
                scheduleReconnect();
            } finally {
                executor.shutdown();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void stopExecutors() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
        }
        if (timeoutCheckExecutor != null && !timeoutCheckExecutor.isShutdown()) {
            timeoutCheckExecutor.shutdownNow();
        }
    }

    private void notifyConnectionStatus(ConnectionStatus status) {
        if (connectionStatusCallback != null) {
            connectionStatusCallback.accept(status);
        }

        if (connectionListener != null) {
            connectionListener.onStatusChanged(status);
        }
    }
}