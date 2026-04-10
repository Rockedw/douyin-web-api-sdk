package com.dy_web_api.sdk.message.model;

import lombok.Data;

/**
 * 评论信息
 */
@Data
public class Comment {
    /**
     * 评论ID
     */
    private String cid;

    /**
     * 评论内容
     */
    private String text;

    /**
     * 视频ID
     */
    private String awemeId;

    /**
     * 创建时间（时间戳）
     */
    private Long createTime;

    /**
     * 点赞数
     */
    private Integer diggCount;

    /**
     * 回复数量
     */
    private Integer replyCommentTotal;

    /**
     * 当前用户是否点赞
     */
    private boolean userDigged;

    /**
     * 评论层级
     */
    private Integer level;

    /**
     * 评论用户
     */
    private CommentUser user;

    /**
     * 回复的评论ID
     */
    private String replyId;

    /**
     * 回复的回复ID
     */
    private String replyToReplyId;

    /**
     * 置顶位置
     */
    private Integer stickPosition;

    /**
     * 是否热门评论
     */
    private Boolean isHot;

    /**
     * 是否被折叠
     */
    private Boolean isFolded;

}