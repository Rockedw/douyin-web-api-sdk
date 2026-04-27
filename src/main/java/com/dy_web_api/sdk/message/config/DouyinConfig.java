package com.dy_web_api.sdk.message.config;


import com.dy_web_api.sdk.message.utils.MsTokenUtil;
import lombok.Builder;
import lombok.Data;

/**
 * 抖音SDK配置类
 *
 * <p>注释中的“用途”基于当前代码实现的真实引用关系整理：
 * <ul>
 *     <li>私信发送：sendMessage / sendImageMessage / sendDynamicEmojiMessage / sendVideoCardMessage / createConversation / sendMsg2Stranger</li>
 *     <li>消息接收：connect（WebSocket）</li>
 *     <li>陌生人消息：fetchStrangerMessages</li>
 *     <li>用户信息：getUserInfo</li>
 *     <li>用户作品：getUserPosts*</li>
 *     <li>图片上传：uploadAndSendImageMessage / uploadImageAndCommentVideo 的上传阶段</li>
 *     <li>视频评论：commentVideo / commentVideoWithImage / uploadImageAndCommentVideo 的评论阶段</li>
 *     <li>评论列表：getVideoCommentList</li>
 * </ul>
 */
@Data
@Builder
public class DouyinConfig {

    static TokenConfig tokenConfig = TokenConfig.createFromYourConfig();



    /**
     * 会话ID
     *
     * <p>用途：
     * <ul>
     *     <li>SDK初始化时必填，构造函数会校验</li>
     *     <li>消息接收：WebSocket 握手 Cookie</li>
     *     <li>私信发送：发送消息、创建会话</li>
     *     <li>陌生人消息：拉取陌生人会话</li>
     *     <li>用户信息：请求 Cookie</li>
     *     <li>用户作品：请求 Cookie</li>
     *     <li>图片上传：普通图片上传、评论图片上传</li>
     *     <li>评论列表：评论列表接口 Cookie</li>
     * </ul>
     */
    private String sessionId;

    /**
     * 用户ID
     *
     * <p>用途：
     * <ul>
     *     <li>SDK初始化时必填，构造函数会校验</li>
     *     <li>私信发送：createConversation 以当前用户身份建会话</li>
     *     <li>sendMsg2Stranger：建陌生人会话时作为发送方 ID</li>
     *     <li>图片上传：部分上传请求头会带 x-storage-u</li>
     * </ul>
     */
    private String userId;

    /**
     * WebSocket access_key。
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：connect 时拼接到 WebSocket URL</li>
     * </ul>
     */
    @Builder.Default
    private String accessKey = "9f01759faef7536279ba97fb9ca6cde5";

    /**
     * User-Agent
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：WebSocket 握手头</li>
     *     <li>陌生人消息：HTTP 请求头</li>
     *     <li>用户信息：请求头和 URL 构造</li>
     *     <li>图片上传：上传授权、上传阶段请求头</li>
     *     <li>视频评论：请求头</li>
     *     <li>私信发送：参与 a_bogus 计算</li>
     * </ul>
     */
    @Builder.Default
    private String userAgent = tokenConfig.getUserAgent();

    /**
     * WebSocket服务器地址
     *
     * <p>用途：
     * <ul>
     *     <li>SDK初始化时必填，构造函数会校验</li>
     *     <li>消息接收：connect 时作为 WebSocket 基地址</li>
     * </ul>
     */
    @Builder.Default
    private String webSocketUrl = "wss://frontier-im.douyin.com/ws/v2";

    /**
     * API基础URL
     *
     * <p>用途：
     * <ul>
     *     <li>SDK初始化时必填，构造函数会校验</li>
     *     <li>私信发送：发送消息接口基地址</li>
     *     <li>陌生人消息：拉取陌生人会话接口基地址</li>
     * </ul>
     */
    @Builder.Default
    private String apiBaseUrl = "https://imapi.douyin.com";

    /**
     * 心跳间隔（秒）
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：WebSocket 心跳频率</li>
     * </ul>
     */
    @Builder.Default
    private int heartbeatInterval = 10;

    /**
     * 连接超时时间（毫秒）
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：WebSocket connection lost timeout</li>
     * </ul>
     */
    @Builder.Default
    private int connectTimeout = 30000;

    /**
     * 消息接收超时时间（毫秒）
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：长时间无消息时主动断开连接</li>
     * </ul>
     */
    @Builder.Default
    private long messageTimeout = 14 * 60 * 60 * 1000; // 14小时

    /**
     * 最大重连次数
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：WebSocket 自动重连上限</li>
     * </ul>
     */
    @Builder.Default
    private int maxReconnectAttempts = 5;

    /**
     * 是否启用自动重连
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：WebSocket 断线后是否重连</li>
     * </ul>
     */
    @Builder.Default
    private boolean autoReconnect = true;

    /**
     * msToken参数
     *
     * <p>用途：
     * <ul>
     *     <li>私信发送：发送消息接口查询参数</li>
     *     <li>用户信息：用户主页信息接口查询参数</li>
     *     <li>用户作品：作品列表接口查询参数</li>
     *     <li>图片上传：上传授权接口查询参数</li>
     *     <li>视频评论：评论接口查询参数</li>
     * </ul>
     */
    @Builder.Default
    private String msToken = MsTokenUtil.genRealMsToken(tokenConfig);

    /**
     * verifyFp参数
     *
     * <p>用途：
     * <ul>
     *     <li>私信发送：发送消息接口查询参数</li>
     *     <li>用户信息：用户主页信息接口查询参数</li>
     *     <li>用户作品：作品列表接口查询参数</li>
     *     <li>视频评论：评论接口查询参数</li>
     * </ul>
     */
    @Builder.Default
    private String verifyFp = "verify_md4cl3m6_X9WoiCOV_mno6_4K1l_By6d_uxpWZi9eFXga";

    /**
     * fp参数
     *
     * <p>用途：
     * <ul>
     *     <li>私信发送：发送消息接口查询参数</li>
     *     <li>图片上传：上传授权接口查询参数</li>
     *     <li>视频评论：评论接口查询参数</li>
     * </ul>
     */
    @Builder.Default
    private String fp = "verify_md4cl3m6_X9WoiCOV_mno6_4K1l_By6d_uxpWZi9eFXga";

    /**
     * 完整 Cookie。
     *
     * <p>用途：
     * <ul>
     *     <li>视频评论：commentVideo / commentVideoWithImage 必须使用</li>
     *     <li>上传图片评论：最终评论阶段复用该字段</li>
     * </ul>
     */
    private String cookie;
    
    /**
     * webid参数
     *
     * <p>用途：
     * <ul>
     *     <li>用户信息：接口查询参数</li>
     *     <li>用户作品：Cookie 和查询参数</li>
     *     <li>图片上传：上传授权接口查询参数</li>
     *     <li>视频评论：评论接口查询参数</li>
     * </ul>
     */
    @Builder.Default
    private String webId = "7360971731131565583";
    
    /**
     * uifid参数
     *
     * <p>用途：
     * <ul>
     *     <li>用户信息：接口查询参数</li>
     *     <li>用户作品：接口查询参数</li>
     *     <li>图片上传：上传授权接口查询参数和请求头</li>
     *     <li>视频评论：评论接口查询参数和请求头</li>
     * </ul>
     */
    @Builder.Default
    private String uifid = "ecb38e5e86f1f8799c3256ca6c3446710d152cd7b76a2f92ca888d379e861b9e592f1cfebae9f8bc36d43ba909c32bccd00011275cfc0707f3b54dadb26e6edcbc999bf3da6fce8c04b4815fcd9ec91629ec5a439f2cf8b6db3c47890f38a0f35ae54ba5c027fa57d8cf4162ba62c4dabd78c5a3f8ad01b2624453efcaf2a0bfaf4c34049f6bb2ff46156ce9428b9309e12660d0ee4bd9e47d2d0f8aa5322c55";
    
    /**
     * a_bogus参数。
     *
     * <p>用途：
     * <ul>
     *     <li>当前版本未直接读取该字段</li>
     *     <li>私信发送、用户作品、评论列表等场景均在运行时动态计算 a_bogus</li>
     *     <li>可视为保留字段</li>
     * </ul>
     */
    private String aBogus;

    /**
     * bd-ticket-guard 的 ts_sign。
     *
     * <p>用途：
     * <ul>
     *     <li>视频评论：commentVideo / commentVideoWithImage 必需</li>
     *     <li>上传图片评论：最终评论阶段必需</li>
     * </ul>
     */
    private String tsSign;

    /**
     * bd-ticket-guard ticket。
     *
     * <p>用途：
     * <ul>
     *     <li>视频评论：commentVideo / commentVideoWithImage 必需</li>
     *     <li>上传图片评论：最终评论阶段必需</li>
     * </ul>
     */
    private String ticket;

    /**
     * 评论签名所需证书。
     *
     * <p>用途：
     * <ul>
     *     <li>视频评论：commentVideo / commentVideoWithImage 必需</li>
     *     <li>上传图片评论：最终评论阶段必需</li>
     * </ul>
     */
    private String certificatePem;

    /**
     * 评论签名所需公钥。
     *
     * <p>用途：
     * <ul>
     *     <li>视频评论：commentVideo / commentVideoWithImage 必需</li>
     *     <li>上传图片评论：最终评论阶段必需</li>
     * </ul>
     */
    private String publicKeyPem;

    /**
     * 评论签名所需私钥。
     *
     * <p>用途：
     * <ul>
     *     <li>视频评论：commentVideo / commentVideoWithImage 必需</li>
     *     <li>上传图片评论：最终评论阶段必需</li>
     * </ul>
     */
    private String privateKeyPem;

    /**
     * 设备ID。
     *
     * <p>用途：
     * <ul>
     *     <li>消息接收：connect 时拼接到 WebSocket URL</li>
     * </ul>
     */
    @Builder.Default
    private String deviceId = "88214454208";
}
