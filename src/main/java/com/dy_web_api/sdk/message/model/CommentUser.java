package com.dy_web_api.sdk.message.model;

import lombok.Data;

/**
 * 评论用户信息
 */
@Data
public class CommentUser {
    /**
     * 用户ID
     */
    private String uid;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 唯一ID
     */
    private String uniqueId;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 粉丝数
     */
    private Integer followerCount;

    /**
     * 关注数
     */
    private Integer followingCount;

    /**
     * 作品数
     */
    private Integer awemeCount;

    /**
     * 是否认证
     */
    private Boolean isVerified;

    /**
     * 认证类型
     */
    private Integer verificationType;

    /**
     * 头像缩略图URL
     */
    private String avatarThumb;

    /**
     * 头像中图URL
     */
    private String avatarMedium;

    /**
     * 头像大图URL
     */
    private String avatarLarger;

    private String secUid;
}