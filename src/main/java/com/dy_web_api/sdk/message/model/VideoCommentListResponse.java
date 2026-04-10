package com.dy_web_api.sdk.message.model;

import lombok.Data;
import java.util.List;

/**
 * 视频评论列表响应
 */
@Data
public class VideoCommentListResponse {
    /**
     * 状态码，0表示成功
     */
    private Integer statusCode;

    /**
     * 评论列表
     */
    private List<Comment> comments;

    /**
     * 下一页游标
     */
    private Long cursor;

    /**
     * 是否有更多数据
     */
    private boolean hasMore;

    /**
     * 评论总数
     */
    private Integer total;

    /**
     * 当前用户是否已评论
     */
    private boolean userCommented;
}