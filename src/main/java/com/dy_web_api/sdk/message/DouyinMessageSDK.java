package com.dy_web_api.sdk.message;

import com.dy_web_api.sdk.message.client.DouyinWebChatClient;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.handler.MessageSender;
import com.dy_web_api.sdk.message.handler.ResourceUploader;
import com.dy_web_api.sdk.message.handler.StrangerMessageFetcher;
import com.dy_web_api.sdk.message.handler.UserInfoFetcher;
import com.dy_web_api.sdk.message.handler.UserPostFetcher;
import com.dy_web_api.sdk.message.handler.VideoCommentHandler;
import com.dy_web_api.sdk.message.handler.VideoCommentListHandler;
import com.dy_web_api.sdk.message.listener.ConnectionListener;
import com.dy_web_api.sdk.message.listener.MessageListener;
import com.dy_web_api.sdk.message.model.ConnectionStatus;
import com.dy_web_api.sdk.message.model.ImageInfo;
import com.dy_web_api.sdk.message.model.StrangerMessageInfo;
import com.dy_web_api.sdk.message.model.UserInfo;
import com.dy_web_api.sdk.message.model.UserPostResponse;
import com.dy_web_api.sdk.message.model.VideoCommentListResponse;
import com.dy_web_api.sdk.message.service.ImageUploadService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 抖音私信SDK主入口类
 */
@Slf4j
public class DouyinMessageSDK implements AutoCloseable {

    private final DouyinConfig config;
    private final AtomicReference<DouyinWebChatClient> clientRef = new AtomicReference<>();
    private final MessageSender messageSender;
    private final ResourceUploader resourceUploader;
    private final StrangerMessageFetcher strangerMessageFetcher;
    private final UserInfoFetcher userInfoFetcher;
    private final UserPostFetcher userPostFetcher;
    private final ImageUploadService imageUploadService;
    private final VideoCommentHandler videoCommentHandler;
    private final VideoCommentListHandler videoCommentListHandler;
    private volatile MessageListener messageListener;
    private volatile ConnectionListener connectionListener;
    private final AtomicReference<ConnectionStatus> status = new AtomicReference<>(ConnectionStatus.DISCONNECTED);

    public DouyinMessageSDK(DouyinConfig config) {
        this.config = config;
        this.messageSender = new MessageSender(config);
        this.resourceUploader = new ResourceUploader(config);
        this.strangerMessageFetcher = new StrangerMessageFetcher(config);
        this.userInfoFetcher = new UserInfoFetcher(config);
        this.userPostFetcher = new UserPostFetcher(config);
        this.imageUploadService = new ImageUploadService(config, resourceUploader);
        this.videoCommentHandler = new VideoCommentHandler(config);
        this.videoCommentListHandler = new VideoCommentListHandler(config);
        validateConfig(config);
    }

    /**
     * 连接到抖音服务器（仅用于接收消息）
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                ConnectionStatus currentStatus = status.get();
                if (currentStatus == ConnectionStatus.CONNECTED || currentStatus == ConnectionStatus.CONNECTING) {
                    log.warn("SDK已连接或正在连接中");
                    return;
                }

                if (!status.compareAndSet(currentStatus, ConnectionStatus.CONNECTING)) {
                    log.warn("连接状态已被其他线程修改");
                    return;
                }
                notifyConnectionStatusChanged(ConnectionStatus.CONNECTING);


                URI serverUri = URI.create(config.getWebSocketUrl()+"?access_key=" + config.getAccessKey() + "&fpid=9&aid=6383&device_id=" + config.getDeviceId());
                DouyinWebChatClient client = new DouyinWebChatClient(
                        serverUri, config, messageListener, connectionListener
                );
                client.addHeader("cookie", "sessionid=" + config.getSessionId());
                client.addHeader("user-agent", config.getUserAgent());
                client.addHeader("origin", "https://www.douyin.com");
                client.addHeader("host", "frontier-im.douyin.com");


                client.setConnectionStatusCallback(this::handleConnectionStatusChange);
                client.connect();

                clientRef.set(client);

            } catch (DouyinMessageException e) {
                status.set(ConnectionStatus.DISCONNECTED);
                notifyConnectionError(e);
                throw e;
            } catch (Exception e) {
                status.set(ConnectionStatus.DISCONNECTED);
                String errorMsg = "连接失败: " + e.getMessage();
                log.error(errorMsg, e);
                DouyinMessageException exception = new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
                notifyConnectionError(exception);
                throw exception;
            }
        });
    }

    /**
     * 断开连接
     */
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            DouyinWebChatClient client = clientRef.get();
            if (client != null) {
                client.close();
                clientRef.set(null);
            }
            status.set(ConnectionStatus.DISCONNECTED);
            notifyConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        });
    }

    /**
     * 发送文本消息（不依赖WebSocket连接）
     */
    public CompletableFuture<Boolean> sendMessage(String conversationId, Long conversationShortId, String content, boolean isGroup) {
        return messageSender.sendMessage(conversationId, conversationShortId, content, isGroup);
    }

    /**
     * 发送图片消息（不依赖WebSocket连接）
     */
    public CompletableFuture<Boolean> sendImageMessage(String conversationId, Long conversationShortId, String md5, String skey, String oid, int fileSize, int height, int width, boolean isGroup) {
        return messageSender.sendImageMessage(conversationId, conversationShortId, md5, skey, oid, fileSize , height, width, isGroup);
    }

    /**
     * 发送动态表情消息（按表情名选择模板）
     */
    public CompletableFuture<Boolean> sendDynamicEmojiMessage(String conversationId, Long conversationShortId, String emojiName, boolean isGroup) {
        return messageSender.sendDynamicEmojiMessage(conversationId, conversationShortId, emojiName, isGroup);
    }

    public CompletableFuture<Boolean> sendVideoCardMessage(String conversationId, Long conversationShortId,
                                                           String itemId, boolean isGroup) {
        return messageSender.sendVideoCardMessage(conversationId, conversationShortId, itemId, isGroup);
    }

    public Map<String,Object> createConversation(Long receiver) throws IOException, InterruptedException {
        return messageSender.createConversation(Long.valueOf(config.getUserId()), receiver);
    }

    public Map<String,Object> sendMsg2Stranger(String secUid, String msg) throws IOException, InterruptedException {
        UserInfo userInfo = getUserInfo(secUid);
        String uid = userInfo.getUid();
        Long receiver = Long.valueOf(uid);
        Map<String,Object> map = createConversation(receiver);
        String conversationId = (String) map.get("conversationId");
        Long shortId = (Long) map.get("conversationShortId");
        sendMessage(conversationId, shortId, msg, false);
        return map;
    }

    /**
     * 上传并发送图片消息 - 完整流程（从文件路径）
     * @param conversationId 会话ID
     * @param conversationShortId 会话短ID
     * @param imagePath 图片文件路径
     * @return 发送结果
     */
    public CompletableFuture<Boolean> uploadAndSendImageMessage(String conversationId, Long conversationShortId, String imagePath, boolean isGroup) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始上传并发送图片消息: conversationId={}, imagePath={}", conversationId, imagePath);

                // 1. 上传图片
                ImageInfo imageInfo = imageUploadService.uploadImage(imagePath);
                log.info("图片上传成功: {}", imageInfo);

                // 2. 发送图片消息
                CompletableFuture<Boolean> sendResult = sendImageMessage(conversationId, conversationShortId,
                        imageInfo.getMd5(), imageInfo.getSkey(), imageInfo.getUri(), 
                        imageInfo.getFileSize(), imageInfo.getHeight(), imageInfo.getWidth(), isGroup);

                boolean success = sendResult.get();
                log.info("图片消息发送完成: success={}", success);
                return success;

            } catch (DouyinMessageException e) {
                throw e;
            } catch (Exception e) {
                String errorMsg = "上传并发送图片消息失败: " + e.getMessage();
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
            }
        });
    }


    /**
     * 上传并发送图片消息 - 完整流程（从字节数组）
     * @param conversationId 会话ID
     * @param conversationShortId 会话短ID
     * @param imageData 图片字节数据
     * @param fileName 文件名（可选，用于日志）
     * @return 发送结果
     */
    public CompletableFuture<Boolean> uploadAndSendImageMessage(String conversationId, Long conversationShortId,
                                                                byte[] imageData, String fileName, boolean isGroup) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始上传并发送图片消息: conversationId={}, fileName={}, size={} bytes",
                        conversationId, fileName, imageData.length);

                // 1. 上传图片
                ImageInfo imageInfo = imageUploadService.uploadCommentImage(imageData, fileName);
                log.info("图片上传成功: {}", imageInfo);

                // 2. 发送图片消息
                CompletableFuture<Boolean> sendResult = sendImageMessage(conversationId, conversationShortId,
                        imageInfo.getMd5(), imageInfo.getSkey(), imageInfo.getUri(), 
                        imageInfo.getFileSize(), imageInfo.getHeight(), imageInfo.getWidth(), isGroup);

                boolean success = sendResult.get();
                log.info("图片消息发送完成: success={}", success);
                return success;

            } catch (DouyinMessageException e) {
                throw e;
            } catch (Exception e) {
                String errorMsg = "上传并发送图片消息失败: " + e.getMessage();
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
            }
        });
    }

    public List<StrangerMessageInfo> fetchStrangerMessages(int count) {
        try {
            return strangerMessageFetcher.fetchStrangerMessages(count);
        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "获取陌生人消息失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 获取用户信息
     * @param secUserId 用户的安全ID
     * @return 用户信息
     */
    public UserInfo getUserInfo(String secUserId) {
        try {
            return userInfoFetcher.getUserInfo(secUserId);
        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "获取用户信息失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 获取用户作品列表
     * @param secUserId 用户的安全ID
     * @param maxCursor 分页游标，首次调用传null或"0"
     * @param count 每页数量，建议1-50
     * @return 用户作品列表响应
     */
    public UserPostResponse getUserPosts(String secUserId, String maxCursor, Integer count) {
        try {
            log.info("开始获取用户作品列表: secUserId={}, maxCursor={}, count={}", secUserId, maxCursor, count);

            UserPostResponse response = userPostFetcher.getUserPosts(secUserId, maxCursor, count);

            log.info("获取用户作品列表完成: total={}, hasMore={}, 作品数量={}",
                    response.getTotal(), response.getHasMore(),
                    response.getAwemeList() != null ? response.getAwemeList().size() : 0);

            return response;

        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "获取用户作品列表失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 获取用户作品列表（使用默认参数）
     * @param secUserId 用户的安全ID
     * @return 用户作品列表响应
     */
    public UserPostResponse getUserPosts(String secUserId) {
        return getUserPosts(secUserId, null, 18);
    }

    /**
     * 获取用户作品列表（自动分页获取指定数量）
     * @param secUserId 用户的安全ID
     * @param targetCount 目标获取数量，自动处理分页直到获取到指定数量的作品或获取完所有作品
     * @return 包含所有获取到的作品的列表响应
     */
    public UserPostResponse getUserPostsWithCount(String secUserId, Integer targetCount) {
        if (targetCount == null || targetCount <= 0) {
            targetCount = 18; // 默认获取18个
        }

        if (targetCount > 1000) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "单次获取数量不能超过1000");
        }

        try {
            log.info("开始自动分页获取用户作品列表: secUserId={}, targetCount={}", secUserId, targetCount);

            // 直接调用 UserPostFetcher 中已实现的自动分页方法
            UserPostResponse response = userPostFetcher.getUserPostsWithCount(secUserId, targetCount);

            log.info("自动分页获取完成: secUserId={}, 目标数量={}, 实际获取={}, 是否还有更多={}",
                    secUserId, targetCount,
                    response.getAwemeList() != null ? response.getAwemeList().size() : 0,
                    response.getHasMore());

            return response;

        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "自动分页获取用户作品列表失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 获取用户作品列表（指定页数）
     * @param secUserId 用户的安全ID
     * @param page 页码，从1开始
     * @param pageSize 每页数量，建议1-50
     * @return 用户作品列表响应
     */
    public UserPostResponse getUserPostsByPage(String secUserId, Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }

        // 根据页码计算游标
        String maxCursor = "0";
        if (page > 1) {
            // 这里需要通过逐页查询来获取对应页的游标
            // 简化实现：假设每页固定数量，计算大致游标
            // 实际使用中建议通过maxCursor进行分页
            maxCursor = String.valueOf((page - 1) * pageSize);
        }

        return getUserPosts(secUserId, maxCursor, pageSize);
    }

    /**
     * 评论视频
     * @param awemeId 视频ID
     * @param text 评论内容
     * @return 评论ID
     */
    public String commentVideo(String awemeId, String text) {
        try {
            return videoCommentHandler.commentVideo(awemeId, text);
        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "评论视频失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 评论视频（带图片）
     * @param awemeId 视频ID
     * @param text 评论内容
     * @param imageUri 图片URI
     * @param imageWidth 图片宽度
     * @param imageHeight 图片高度
     * @return 评论ID
     */
    public String commentVideoWithImage(String awemeId, String text, String imageUri, Integer imageWidth, Integer imageHeight) {
        try {
            return videoCommentHandler.commentVideo(awemeId, text, imageUri, imageWidth, imageHeight);
        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "评论视频失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 上传图片并评论视频 - 完整流程（从文件路径）
     * @param awemeId 视频ID
     * @param text 评论内容
     * @param imagePath 图片文件路径
     * @return 评论ID
     */
    public String uploadImageAndCommentVideo(String awemeId, String text, String imagePath) {
        try {
            log.info("开始上传图片并评论视频: awemeId={}, imagePath={}", awemeId, imagePath);

            // 1. 上传图片
            ImageInfo imageInfo = imageUploadService.uploadCommentImage(imagePath);
            log.info("图片上传成功: {}", imageInfo);

            // 2. 发送图片评论
            String commentId = commentVideoWithImage(awemeId, text, imageInfo.getUri(), imageInfo.getWidth(), imageInfo.getHeight());
            log.info("图片评论发送完成: commentId={}", commentId);
            return commentId;

        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "上传图片并评论视频失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }


    /**
     * 上传图片并评论视频 - 完整流程（从字节数组）
     * @param awemeId 视频ID
     * @param text 评论内容
     * @param imageData 图片字节数据
     * @param fileName 文件名（可选，用于日志）
     * @return 评论ID
     */
    public String uploadImageAndCommentVideo(String awemeId, String text, byte[] imageData, String fileName) {
        try {
            log.info("开始上传图片并评论视频: awemeId={}, fileName={}, size={} bytes",
                    awemeId, fileName, imageData.length);

            // 1. 上传图片
            ImageInfo imageInfo = imageUploadService.uploadCommentImage(imageData, fileName);
            log.info("图片上传成功: {}", imageInfo);

            // 2. 发送图片评论
            String commentId = commentVideoWithImage(awemeId, text, imageInfo.getUri(), imageInfo.getWidth(), imageInfo.getHeight());
            log.info("图片评论发送完成: commentId={}", commentId);
            return commentId;

        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "上传图片并评论视频失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }

    /**
     * 获取视频评论列表
     * @param awemeId 视频ID
     * @param cursor 游标，第一页传0
     * @param count 每页数量，建议1-100
     * @return 评论列表响应
     */
    public VideoCommentListResponse getVideoCommentList(String awemeId, Long cursor, Integer count) {
        try {
            log.info("开始获取视频评论列表: awemeId={}, cursor={}, count={}", awemeId, cursor, count);

            VideoCommentListResponse response = videoCommentListHandler.getCommentList(awemeId, cursor, count);

            log.info("获取视频评论列表完成: total={}, hasMore={}, 评论数量={}",
                    response.getTotal(), response.isHasMore(),
                    response.getComments() != null ? response.getComments().size() : 0);

            return response;

        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "获取视频评论列表失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    /**
     * 获取视频评论列表（使用默认参数）
     * @param awemeId 视频ID
     * @return 评论列表响应
     */
    public VideoCommentListResponse getVideoCommentList(String awemeId) {
        return getVideoCommentList(awemeId, 0L, 10);
    }

    /**
     * 设置消息监听器
     */
    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    /**
     * 设置连接监听器
     */
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    /**
     * 获取当前连接状态
     */
    public ConnectionStatus getConnectionStatus() {
        return status.get();
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return status.get() == ConnectionStatus.CONNECTED;
    }

    private void handleConnectionStatusChange(ConnectionStatus newStatus) {
        this.status.set(newStatus);
        notifyConnectionStatusChanged(newStatus);
    }

    private void notifyConnectionStatusChanged(ConnectionStatus status) {
        if (connectionListener != null) {
            try {
                connectionListener.onStatusChanged(status);
            } catch (Exception e) {
                log.error("连接状态回调异常", e);
            }
        }
    }

    private void notifyConnectionError(Exception e) {
        if (connectionListener != null) {
            try {
                connectionListener.onError(e);
            } catch (Exception ex) {
                log.error("连接错误回调异常", ex);
            }
        }
    }

    /**
     * 关闭SDK并清理资源
     */
    @Override
    public void close() {
        try {
            disconnect().get();
        } catch (Exception e) {
            log.warn("关闭连接时发生异常", e);
        }
        
        // 清理监听器引用
        messageListener = null;
        connectionListener = null;
        
        log.info("SDK资源已清理");
    }

    private void validateConfig(DouyinConfig config) {
        if (config == null) {
            throw new DouyinMessageException(ErrorCode.CONFIG_MISSING, "配置不能为空");
        }
        if (config.getSessionId() == null || config.getSessionId().trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.CONFIG_INVALID, "SessionId不能为空");
        }
        if (config.getUserId() == null || config.getUserId().trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.CONFIG_INVALID, "UserId不能为空");
        }
        if (config.getApiBaseUrl() == null || config.getApiBaseUrl().trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.CONFIG_INVALID, "API基础URL不能为空");
        }
        if (config.getWebSocketUrl() == null || config.getWebSocketUrl().trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.CONFIG_INVALID, "WebSocket URL不能为空");
        }
    }
}

