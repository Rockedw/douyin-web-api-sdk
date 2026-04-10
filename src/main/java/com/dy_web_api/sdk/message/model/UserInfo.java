package com.dy_web_api.sdk.message.model;

import lombok.Data;
import lombok.Builder;

/**
 * 用户信息模型
 */
@Data
@Builder
public class UserInfo {
    
    /**
     * 用户ID
     */
    private String uid;
    
    /**
     * 安全用户ID
     */
    private String secUid;
    
    /**
     * 用户昵称
     */
    private String nickname;

    private String accountCertInfo;
    
    /**
     * 用户签名
     */
    private String signature;
    
    /**
     * 唯一ID
     */
    private String uniqueId;
    
    /**
     * 头像信息
     */
    private AvatarInfo avatarInfo;
    
    /**
     * 关注数
     */
    private Long followingCount;
    
    /**
     * 粉丝数
     */
    private Long followerCount;
    
    /**
     * 获赞数
     */
    private Long totalFavorited;
    
    /**
     * 作品数
     */
    private Long awemeCount;
    
    /**
     * 是否已关注
     */
    private Integer followStatus;
    
    /**
     * 是否被关注
     */
    private Integer followerStatus;
    
    /**
     * 认证信息
     */
    private String customVerify;
    
    /**
     * 企业认证原因
     */
    private String enterpriseVerifyReason;
    
    /**
     * 是否为政务媒体VIP
     */
    private Boolean isGovMediaVip;
    
    /**
     * 是否为明星
     */
    private Boolean isStar;
    
    /**
     * 直播状态 (0: 未直播, 1: 直播中)
     */
    private Integer liveStatus;
    
    /**
     * 房间ID
     */
    private String roomIdStr;
    
    /**
     * 分享信息
     */
    private ShareInfo shareInfo;
    
    /**
     * 头像信息
     */
    @Data
    @Builder
    public static class AvatarInfo {
        /**
         * 头像URI
         */
        private String uri;
        
        /**
         * 头像URL列表
         */
        private java.util.List<String> urlList;
        
        /**
         * 头像宽度
         */
        private Integer width;
        
        /**
         * 头像高度
         */
        private Integer height;
    }
    
    /**
     * 分享信息
     */
    @Data
    @Builder
    public static class ShareInfo {
        /**
         * 分享标题
         */
        private String shareTitle;
        
        /**
         * 分享描述
         */
        private String shareDesc;
        
        /**
         * 分享URL
         */
        private String shareUrl;
        
        /**
         * 分享二维码URL
         */
        private String shareQrcodeUrl;
    }
}