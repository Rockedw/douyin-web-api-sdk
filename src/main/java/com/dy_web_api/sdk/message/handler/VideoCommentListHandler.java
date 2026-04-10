package com.dy_web_api.sdk.message.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.model.Comment;
import com.dy_web_api.sdk.message.model.CommentUser;
import com.dy_web_api.sdk.message.model.VideoCommentListResponse;
import com.dy_web_api.sdk.message.utils.ABogusUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.dy_web_api.sdk.message.utils.HttpUtils.params2Str;

/**
 * 视频评论列表处理器
 */
@Slf4j
public class VideoCommentListHandler {

    private static final String COMMENT_LIST_API_URL = "https://www-hj.douyin.com/aweme/v1/web/comment/list/";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int HTTP_TIMEOUT_SECONDS = 15;

    private final DouyinConfig config;
    private final HttpClient httpClient;

    public VideoCommentListHandler(DouyinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 获取视频评论列表
     * @param awemeId 视频ID
     * @param cursor 游标，第一页传0
     * @param count 每页数量，默认10
     * @return 评论列表响应
     */
    public VideoCommentListResponse getCommentList(String awemeId, Long cursor, Integer count) {
        // 参数校验
        validateCommentListParams(awemeId, cursor, count);

        // 构建请求头
        Map<String, String> header = buildHeaders();

        // 构建请求参数
        Map<String, String> params = buildParams(awemeId, cursor, count);

        // 添加 a_bogus 参数
        String paramsStr = params2Str(params);
        String aBogus = ABogusUtil.generateABogus(paramsStr, header.get("User-Agent"));
        params.put("a_bogus", aBogus);

        // 构建完整 URI
        String uri = buildUriWithQuery(COMMENT_LIST_API_URL, params);
        log.debug("请求评论列表URL: {}", uri);

        // 构建并发送请求
        HttpRequest request = buildHttpRequest(uri, header);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            log.debug("响应状态码: {}, 响应体: {}", statusCode, responseBody);

            // 处理 HTTP 错误
            if (statusCode != 200) {
                handleHttpError(statusCode);
            }

            // 解析响应
            return parseCommentListResponse(responseBody, awemeId);

        } catch (IOException e) {
            log.error("HTTP请求IO异常: awemeId={}, error={}", awemeId, e.getMessage(), e);
            throw new DouyinMessageException(ErrorCode.HTTP_ERROR, "HTTP请求IO异常: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("HTTP请求被中断: awemeId={}, error={}", awemeId, e.getMessage(), e);
            throw new DouyinMessageException(ErrorCode.HTTP_ERROR, "HTTP请求被中断: " + e.getMessage(), e);
        }
    }

    /**
     * 获取视频评论列表（使用默认参数）
     * @param awemeId 视频ID
     * @return 评论列表响应
     */
    public VideoCommentListResponse getCommentList(String awemeId) {
        return getCommentList(awemeId, 0L, DEFAULT_PAGE_SIZE);
    }

    /**
     * 构建请求头
     */
    private Map<String, String> buildHeaders() {
        return Map.of(
                "User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0",
                "Origin", "https://www.douyin.com",
                "Referer", "https://www.douyin.com/",
                "Cookie", "sessionid=" + config.getSessionId()
        );
    }

    /**
     * 构建请求参数
     */
    private Map<String, String> buildParams(String awemeId, Long cursor, Integer count) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("aweme_id", awemeId);
        params.put("cursor", cursor.toString());
        params.put("count", count.toString());
        return params;
    }

    /**
     * 构建 HTTP 请求
     */
    private HttpRequest buildHttpRequest(String uri, Map<String, String> header) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .headers(
                        "User-Agent", header.get("User-Agent"),
                        "Origin", header.get("Origin"),
                        "Referer", header.get("Referer"),
                        "Cookie", header.get("Cookie")
                )
                .GET()
                .build();
    }

    /**
     * 解析评论列表响应
     */
    private VideoCommentListResponse parseCommentListResponse(String responseBody, String awemeId) {
        try {
            JSONObject jsonResponse = JSON.parseObject(responseBody);

            // 检查 status_code
            Integer statusCode = jsonResponse.getInteger("status_code");
            if (statusCode == null || statusCode != 0) {
                String statusMsg = jsonResponse.getString("status_msg");
                log.error("抖音API返回错误: awemeId={}, statusCode={}, statusMsg={}",
                        awemeId, statusCode, statusMsg);
                throw new DouyinMessageException(ErrorCode.RESPONSE_FORMAT_ERROR,
                        "抖音API返回错误: " + (statusMsg != null ? statusMsg : "未知错误"));
            }

            // 解析评论列表
            JSONArray commentsArray = jsonResponse.getJSONArray("comments");
            List<Comment> comments = new ArrayList<>();

            if (commentsArray != null) {
                for (int i = 0; i < commentsArray.size(); i++) {
                    JSONObject commentJson = commentsArray.getJSONObject(i);
                    Comment comment = parseComment(commentJson);
                    if (comment != null) {
                        comments.add(comment);
                    }
                }
            }

            // 构建响应对象
            VideoCommentListResponse response = new VideoCommentListResponse();
            response.setStatusCode(statusCode);
            response.setComments(comments);
            response.setCursor(jsonResponse.getLong("cursor"));
            response.setHasMore(jsonResponse.getInteger("has_more") == 1);
            response.setTotal(jsonResponse.getInteger("total"));
//            response.setUserCommented(jsonResponse.getInteger("user_commented") == 1);

            log.info("成功获取评论列表: awemeId={}, 评论数={}, hasMore={}, cursor={}",
                    awemeId, comments.size(), response.isHasMore(), response.getCursor());

            return response;

        } catch (Exception e) {
            log.error("解析评论列表响应失败: awemeId={}, error={}", awemeId, e.getMessage(), e);
            throw new DouyinMessageException(ErrorCode.RESPONSE_PARSE_ERROR,
                    "解析评论列表响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析单条评论
     */
    private Comment parseComment(JSONObject commentJson) {
        try {
            Comment comment = new Comment();

            // 基本信息
            comment.setCid(commentJson.getString("cid"));
            comment.setText(commentJson.getString("text"));
            comment.setAwemeId(commentJson.getString("aweme_id"));
            comment.setCreateTime(commentJson.getLong("create_time"));
            comment.setDiggCount(commentJson.getInteger("digg_count"));
            comment.setReplyCommentTotal(commentJson.getInteger("reply_comment_total"));
            comment.setUserDigged(commentJson.getInteger("user_digged") == 1);
            comment.setLevel(commentJson.getInteger("level"));

            // 用户信息
            JSONObject userJson = commentJson.getJSONObject("user");
            if (userJson != null) {
                CommentUser user = parseCommentUser(userJson);
                comment.setUser(user);
            }

            // 回复信息
            comment.setReplyId(commentJson.getString("reply_id"));
            comment.setReplyToReplyId(commentJson.getString("reply_to_reply_id"));

            // 其他信息
            comment.setStickPosition(commentJson.getInteger("stick_position"));
            comment.setIsHot(commentJson.getBoolean("is_hot"));
            comment.setIsFolded(commentJson.getBoolean("is_folded"));

            return comment;

        } catch (Exception e) {
            log.warn("解析评论失败: cid={}, error={}",
                    commentJson.getString("cid"), e.getMessage());
            return null;
        }
    }

    /**
     * 解析评论用户信息
     */
    private CommentUser parseCommentUser(JSONObject userJson) {
        CommentUser user = new CommentUser();

        user.setUid(userJson.getString("uid"));
        user.setNickname(userJson.getString("nickname"));
        user.setUniqueId(userJson.getString("unique_id"));
        user.setSignature(userJson.getString("signature"));
        user.setFollowerCount(userJson.getInteger("follower_count"));
        user.setFollowingCount(userJson.getInteger("following_count"));
        user.setAwemeCount(userJson.getInteger("aweme_count"));
        user.setIsVerified(userJson.getBoolean("is_verified"));
        user.setVerificationType(userJson.getInteger("verification_type"));
        user.setSecUid(userJson.getString("sec_uid"));

        // 头像信息
        JSONObject avatarThumb = userJson.getJSONObject("avatar_thumb");
        if (avatarThumb != null && avatarThumb.containsKey("url_list")) {
            JSONArray urlList = avatarThumb.getJSONArray("url_list");
            if (urlList != null && !urlList.isEmpty()) {
                user.setAvatarThumb(urlList.getString(0));
            }
        }

        JSONObject avatarMedium = userJson.getJSONObject("avatar_medium");
        if (avatarMedium != null && avatarMedium.containsKey("url_list")) {
            JSONArray urlList = avatarMedium.getJSONArray("url_list");
            if (urlList != null && !urlList.isEmpty()) {
                user.setAvatarMedium(urlList.getString(0));
            }
        }

        JSONObject avatarLarger = userJson.getJSONObject("avatar_larger");
        if (avatarLarger != null && avatarLarger.containsKey("url_list")) {
            JSONArray urlList = avatarLarger.getJSONArray("url_list");
            if (urlList != null && !urlList.isEmpty()) {
                user.setAvatarLarger(urlList.getString(0));
            }
        }

        return user;
    }

    /**
     * 参数校验
     */
    private void validateCommentListParams(String awemeId, Long cursor, Integer count) {
        if (awemeId == null || awemeId.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "视频ID不能为空");
        }
        if (cursor == null || cursor < 0) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "游标不能为空且不能小于0");
        }
        if (count == null || count <= 0 || count > MAX_PAGE_SIZE) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER,
                    "每页数量必须在1-" + MAX_PAGE_SIZE + "之间");
        }
    }

    /**
     * 构建带查询参数的 URI
     */
    private String buildUriWithQuery(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return base;
        }
        String query = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return base + "?" + query;
    }

    /**
     * URL 编码
     */
    private String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * 处理 HTTP 错误
     */
    private void handleHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;

        switch (statusCode) {
            case 400:
                errorCode = ErrorCode.INVALID_PARAMETER;
                errorMsg = "请求参数错误";
                break;
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "认证失败，请检查登录状态";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "访问被拒绝，可能被限制访问";
                break;
            case 404:
                errorCode = ErrorCode.VIDEO_NOT_FOUND;
                errorMsg = "视频不存在或评论不可见";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "访问频率超限，请稍后重试";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "服务暂时不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "服务器响应超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "HTTP请求失败，状态码: " + statusCode;
        }

        throw new DouyinMessageException(errorCode, errorMsg);
    }
}