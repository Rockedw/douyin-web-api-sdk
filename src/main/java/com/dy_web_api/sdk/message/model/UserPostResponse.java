package com.dy_web_api.sdk.message.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 用户作品列表响应实体
 *
 * @author kol_dy_msg SDK
 * @since 2025-10-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPostResponse {

    /**
     * 作品列表
     */
    private List<AwemeInfo> awemeList;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;

    /**
     * 下一页游标
     */
    private String maxCursor;

    /**
     * 上一页游标
     */
    private String minCursor;

    /**
     * 总数量
     */
    private Integer total;

    /**
     * 请求状态码
     */
    private Integer statusCode;

    /**
     * 错误信息
     */
    private String statusMsg;

    /**
     * 作品信息实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AwemeInfo {

        /**
         * 作品ID
         */
        private String awemeId;

        /**
         * 作品标题
         */
        private String itemTitle;

        /**
         * 作品描述
         */
        private String caption;

        /**
         * 创建时间（时间戳）
         */
        private Long createTime;

        /**
         * 作者信息
         */
        private AuthorInfo author;

        /**
         * 统计信息
         */
        private StatisticsInfo statistics;

        /**
         * 视频时长（毫秒）
         */
        private Long duration;

        /**
         * 视频封面URL
         */
        private String videoCover;

        /**
         * 视频播放地址
         */
        private String videoUrl;

        /**
         * 分享链接
         */
        private String shareUrl;

        /**
         * 是否置顶
         */
        private Boolean isTop;

        /**
         * 作品类型
         */
        private Integer type;
    }

    /**
     * 作者信息实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {

        /**
         * 安全用户ID
         */
        private String secUid;

        /**
         * 用户ID
         */
        private String uid;

        /**
         * 唯一ID
         */
        private String uniqueId;

        /**
         * 短ID
         */
        private String shortId;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 签名
         */
        private String signature;

        /**
         * 头像URL
         */
        private String avatarUrl;

        /**
         * 认证信息
         */
        private String verificationType;

        /**
         * 粉丝数
         */
        private Long followerCount;

        /**
         * 关注数
         */
        private Long followingCount;

        /**
         * 获赞数
         */
        private Long totalFavorited;
    }

    /**
     * 统计信息实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticsInfo {

        /**
         * 点赞数
         */
        private Long diggCount;

        /**
         * 评论数
         */
        private Long commentCount;

        /**
         * 收藏数
         */
        private Long collectCount;

        /**
         * 分享数
         */
        private Long shareCount;

        /**
         * 播放数
         */
        private Long playCount;

        /**
         * 转发数
         */
        private Long forwardCount;
    }
}