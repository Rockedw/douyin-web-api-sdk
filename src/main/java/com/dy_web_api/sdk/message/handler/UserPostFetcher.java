package com.dy_web_api.sdk.message.handler;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.model.UserPostResponse;
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
 * 用户作品列表获取器
 * 用于获取指定用户的视频作品列表，支持分页查询
 *
 * @author kol_dy_msg SDK
 * @since 2025-10-21
 */
@Slf4j
public class UserPostFetcher {

    private static final String USER_POST_API_URL = "https://www.douyin.com/aweme/v1/web/aweme/post/";
    private static final int HTTP_TIMEOUT_SECONDS = 15;
    private static final int DEFAULT_PAGE_SIZE = 18;
    private static final int MAX_PAGE_SIZE = 50;

    private final HttpClient httpClient;
    private final DouyinConfig douyinConfig;

    /**
     * 构造函数
     *
     * @param config 抖音配置
     */
    public UserPostFetcher(DouyinConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("DouyinConfig 不能为 null");
        }
        this.douyinConfig = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 获取用户作品列表（使用默认参数）
     *
     * @param secUserId 安全用户ID
     * @return 用户作品列表响应
     * @throws DouyinMessageException 获取失败时抛出异常
     */
    public UserPostResponse getUserPosts(String secUserId) throws DouyinMessageException {
        return getUserPosts(secUserId, null, null);
    }

    /**
     * 获取用户指定数量的作品列表（自动处理分页）
     *
     * @param secUserId 安全用户ID
     * @param totalCount 需要获取的作品总数量
     * @return 包含所有作品的响应对象
     * @throws DouyinMessageException 获取失败时抛出异常
     */
    public UserPostResponse getUserPostsWithCount(String secUserId, int totalCount) throws DouyinMessageException {
        if (secUserId == null || secUserId.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "用户ID不能为空");
        }

        if (totalCount <= 0) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "获取数量必须大于0");
        }

        log.info("开始获取用户作品列表（自动分页），用户ID: {}, 需要获取数量: {}", secUserId, totalCount);

        List<UserPostResponse.AwemeInfo> allAwemeList = new ArrayList<>();
        String currentCursor = "0";
        boolean hasMore = true;
        int fetchedCount = 0;
        int pageNum = 0;

        try {
            while (hasMore && fetchedCount < totalCount) {
                pageNum++;
                // 计算本次需要获取的数量
                int remainingCount = totalCount - fetchedCount;
                int pageSize = Math.min(remainingCount, MAX_PAGE_SIZE);

                log.debug("分页请求第{}页，游标: {}, 本次获取数量: {}", pageNum, currentCursor, pageSize);

                // 调用原有的分页方法
                UserPostResponse pageResponse = getUserPosts(secUserId, currentCursor, pageSize);

                if (pageResponse == null || pageResponse.getAwemeList() == null) {
                    log.warn("第{}页响应为空，停止分页", pageNum);
                    break;
                }

                // 收集本页数据
                List<UserPostResponse.AwemeInfo> pageAwemeList = pageResponse.getAwemeList();
                int currentPageCount = pageAwemeList.size();

                // 只添加需要的数量
                int needCount = totalCount - fetchedCount;
                if (currentPageCount > needCount) {
                    allAwemeList.addAll(pageAwemeList.subList(0, needCount));
                    fetchedCount += needCount;
                } else {
                    allAwemeList.addAll(pageAwemeList);
                    fetchedCount += currentPageCount;
                }

                log.debug("第{}页获取了{}个作品，累计获取: {}/{}",
                    pageNum, currentPageCount, fetchedCount, totalCount);

                // 检查是否还有更多数据
                hasMore = pageResponse.getHasMore() != null && pageResponse.getHasMore();
                currentCursor = pageResponse.getMaxCursor();

                // 如果已经获取够了，退出循环
                if (fetchedCount >= totalCount) {
                    log.info("已获取足够数量的作品，停止分页");
                    break;
                }

                // 如果没有更多数据了，退出循环
                if (!hasMore) {
                    log.info("用户没有更多作品，实际获取: {}/{}", fetchedCount, totalCount);
                    break;
                }

                // 避免请求过快，添加短暂延迟
                if (hasMore && fetchedCount < totalCount) {
                    try {
                        Thread.sleep(200); // 延迟200毫秒
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("分页延迟被中断");
                    }
                }
            }

            log.info("成功获取用户作品列表（自动分页），用户ID: {}, 请求数量: {}, 实际获取: {}, 总页数: {}",
                secUserId, totalCount, fetchedCount, pageNum);

            // 构建最终响应
            return UserPostResponse.builder()
                    .awemeList(allAwemeList)
                    .hasMore(hasMore)
                    .maxCursor(currentCursor)
                    .minCursor("0")
                    .total(fetchedCount)
                    .statusCode(0)
                    .statusMsg("success")
                    .build();

        } catch (DouyinMessageException e) {
            log.error("自动分页获取用户作品失败: secUserId={}, 已获取={}/{}, 页数={}",
                secUserId, fetchedCount, totalCount, pageNum, e);
            throw e;
        } catch (Exception e) {
            log.error("自动分页获取用户作品时发生未知异常: secUserId={}, 已获取={}/{}",
                secUserId, fetchedCount, totalCount, e);
            throw new DouyinMessageException(ErrorCode.UNKNOWN_ERROR,
                    "自动分页获取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户作品列表
     *
     * @param secUserId 安全用户ID
     * @param maxCursor 下一页游标，首次调用传null或"0"
     * @param count     每页数量，建议18-30，null则使用默认值18
     * @return 用户作品列表响应
     * @throws DouyinMessageException 获取失败时抛出异常
     */
    public UserPostResponse getUserPosts(String secUserId, String maxCursor, Integer count)
            throws DouyinMessageException {

        // 参数校验与默认值设置
        validateAndSetDefaults(secUserId, maxCursor, count);

        if (count == null || count <= 0 || count > MAX_PAGE_SIZE) {
            count = DEFAULT_PAGE_SIZE;
        }

        if (maxCursor == null || maxCursor.trim().isEmpty()) {
            maxCursor = "0";
        }

        try {
            log.info("开始获取用户作品列表，用户ID: {}, 游标: {}, 数量: {}", secUserId, maxCursor, count);

            // 构建请求头
            Map<String, String> headers = buildHeaders();

            // 构建请求参数
            Map<String, String> params = buildRequestParams(secUserId, maxCursor, count);

            // 生成ABogus签名
            String paramsStr = params2Str(params);
            String aBogus = ABogusUtil.generateABogus(paramsStr, headers.get("user-agent"));
            params.put("a_bogus", aBogus);

            // 构建完整URL
            String url = buildUrlWithParams(USER_POST_API_URL, params);
            log.debug("请求URL: {}", url);

            // 发送HTTP请求
            HttpRequest request = buildHttpRequest(url, headers);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 处理响应
            return handleResponse(response, secUserId);

        } catch (DouyinMessageException e) {
            throw e;
        } catch (IOException e) {
            log.error("获取用户作品列表时发生IO异常: secUserId={}", secUserId, e);
            throw new DouyinMessageException(ErrorCode.HTTP_ERROR,
                    "HTTP请求IO异常: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取用户作品列表时被中断: secUserId={}", secUserId, e);
            throw new DouyinMessageException(ErrorCode.HTTP_ERROR,
                    "HTTP请求被中断: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("获取用户作品列表时发生未知异常: secUserId={}", secUserId, e);
            throw new DouyinMessageException(ErrorCode.UNKNOWN_ERROR,
                    "获取用户作品列表时发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 参数校验与默认值设置
     */
    private void validateAndSetDefaults(String secUserId, String maxCursor, Integer count) {
        if (secUserId == null || secUserId.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "用户ID不能为空");
        }

        if (count != null && (count <= 0 || count > MAX_PAGE_SIZE)) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER,
                    "每页数量必须在1-" + MAX_PAGE_SIZE + "之间");
        }

        if (maxCursor != null && !maxCursor.trim().isEmpty()) {
            try {
                Long.parseLong(maxCursor);
            } catch (NumberFormatException e) {
                throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER,
                        "游标格式不正确，必须为数字字符串");
            }
        }
    }

    /**
     * 构建请求头
     *
     * @return 请求头Map
     */
    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("accept", "application/json, text/plain, */*");
        headers.put("accept-language", "zh-CN,zh;q=0.9");
        headers.put("origin", "https://www.douyin.com");
        headers.put("referer", "https://www.douyin.com/");
        headers.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");
        headers.put("sec-ch-ua", "\"Google Chrome\";v=\"135\", \"Not-A.Brand\";v=\"8\", \"Chromium\";v=\"135\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"macOS\"");
        headers.put("sec-fetch-dest", "empty");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-site", "same-origin");
        headers.put("priority", "u=1, i");

        // 构建 Cookie
        StringBuilder cookie = new StringBuilder();
        cookie.append("sessionid=").append(douyinConfig.getSessionId());

        if (douyinConfig.getWebId() != null && !douyinConfig.getWebId().isEmpty()) {
            cookie.append("; webid=").append(douyinConfig.getWebId());
        }

        headers.put("cookie", cookie.toString());

        return headers;
    }

    /**
     * 构建请求参数
     *
     * @param secUserId 安全用户ID
     * @param maxCursor 分页游标
     * @param count     每页数量
     * @return 请求参数Map
     */
    private Map<String, String> buildRequestParams(String secUserId, String maxCursor, Integer count) {
        Map<String, String> params = new LinkedHashMap<>();

        // 基础参数
        params.put("device_platform", "webapp");
        params.put("aid", "6383");
        params.put("channel", "channel_pc_web");
        params.put("sec_user_id", secUserId);
        params.put("max_cursor", maxCursor);
        params.put("locate_query", "false");
        params.put("show_live_replay_strategy", "1");
        params.put("need_time_list", "1");
        params.put("time_list_query", "0");
        params.put("whale_cut_token", "");
        params.put("cut_version", "1");
        params.put("count", String.valueOf(count));
        params.put("publish_video_strategy_type", "2");
        params.put("from_user_page", "1");
        params.put("update_version_code", "170400");
        params.put("pc_client_type", "1");
        params.put("pc_libra_divert", "Mac");

        // 设备能力参数
        params.put("support_h265", "1");
        params.put("support_dash", "1");
        params.put("cpu_core_num", "8");

        // 版本信息
        params.put("version_code", "290100");
        params.put("version_name", "29.1.0");

        // 浏览器信息
        params.put("cookie_enabled", "true");
        params.put("screen_width", "3440");
        params.put("screen_height", "1440");
        params.put("browser_language", "zh-CN");
        params.put("browser_platform", "MacIntel");
        params.put("browser_name", "Chrome");
        params.put("browser_version", "135.0.0.0");
        params.put("browser_online", "true");
        params.put("engine_name", "Blink");
        params.put("engine_version", "135.0.0.0");
        params.put("os_name", "Mac OS");
        params.put("os_version", "10.15.7");

        // 网络信息
        params.put("device_memory", "8");
        params.put("platform", "PC");
        params.put("downlink", "1.25");
        params.put("effective_type", "3g");
        params.put("round_trip_time", "250");

        // 配置中的参数
        if (douyinConfig.getWebId() != null && !douyinConfig.getWebId().isEmpty()) {
            params.put("webid", douyinConfig.getWebId());
        }
        if (douyinConfig.getUifid() != null && !douyinConfig.getUifid().isEmpty()) {
            params.put("uifid", douyinConfig.getUifid());
        }
        if (douyinConfig.getVerifyFp() != null && !douyinConfig.getVerifyFp().isEmpty()) {
            params.put("verifyFp", douyinConfig.getVerifyFp());
            params.put("fp", douyinConfig.getVerifyFp());
        }
        if (douyinConfig.getMsToken() != null && !douyinConfig.getMsToken().isEmpty()) {
            params.put("msToken", douyinConfig.getMsToken());
        }

        return params;
    }

    /**
     * 构建HTTP请求
     */
    private HttpRequest buildHttpRequest(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .GET();

        // 添加请求头
        headers.forEach(builder::header);

        return builder.build();
    }

    /**
     * 构建带参数的URL
     */
    private String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        String query = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        return baseUrl + "?" + query;
    }

    /**
     * URL编码
     */
    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * 处理HTTP响应
     */
    private UserPostResponse handleResponse(HttpResponse<String> response, String secUserId) {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        log.debug("响应状态码: {}", statusCode);

        // 检查HTTP状态码
        if (statusCode != 200) {
            log.error("抖音获取用户作品列表接口HTTP状态码异常. 状态码: {}, secUserId: {}",
                    statusCode, secUserId);
            handleHttpError(statusCode);
        }

        // 解析响应JSON
        JSONObject jsonResponse;
        try {
            jsonResponse = JSONObject.parseObject(responseBody);
        } catch (Exception e) {
            log.error("解析响应JSON失败: secUserId={}, error={}", secUserId, e.getMessage(), e);
            throw new DouyinMessageException(ErrorCode.PARSE_ERROR,
                    "解析响应JSON失败: " + e.getMessage(), e);
        }

        if (jsonResponse == null) {
            log.error("抖音获取用户作品列表接口响应异常. 返回数据为空, secUserId: {}", secUserId);
            throw new DouyinMessageException(ErrorCode.PARSE_ERROR,
                    "抖音获取用户作品列表接口响应异常. 返回数据为空");
        }

        // 检查API状态码
        Integer apiStatusCode = jsonResponse.getInteger("status_code");
        if (apiStatusCode == null || apiStatusCode != 0) {
            String statusMsg = jsonResponse.getString("status_msg");
            log.error("抖音获取用户作品列表接口错误码异常. 错误码: {}, 错误信息: {}, secUserId: {}",
                    apiStatusCode, statusMsg, secUserId);
            throw new DouyinMessageException(ErrorCode.API_ERROR,
                    "抖音API返回错误: " + (statusMsg != null ? statusMsg : "未知错误"));
        }

        // 解析并返回结果
        return parseResponse(jsonResponse, secUserId);
    }

    /**
     * 处理HTTP错误
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
                errorMsg = "用户不存在或作品不可见";
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

    /**
     * 解析API响应
     *
     * @param responseBody API响应JSON
     * @param secUserId    用户ID（用于日志）
     * @return 用户作品列表响应实体
     */
    private UserPostResponse parseResponse(JSONObject responseBody, String secUserId) {
        try {
            JSONArray awemeList = responseBody.getJSONArray("aweme_list");
            List<UserPostResponse.AwemeInfo> awemeInfoList = new ArrayList<>();

            if (awemeList != null && !awemeList.isEmpty()) {
                for (int i = 0; i < awemeList.size(); i++) {
                    JSONObject awemeDetail = awemeList.getJSONObject(i);
                    if (awemeDetail != null) {
                        try {
                            UserPostResponse.AwemeInfo awemeInfo = parseAwemeInfo(awemeDetail);
                            awemeInfoList.add(awemeInfo);
                        } catch (Exception e) {
                            log.warn("解析单个作品信息失败，跳过: awemeId={}, error={}",
                                    awemeDetail.getString("aweme_id"), e.getMessage());
                        }
                    }
                }
            }

            UserPostResponse response = UserPostResponse.builder()
                    .awemeList(awemeInfoList)
                    .hasMore(responseBody.getIntValue("has_more") == 1)
                    .maxCursor(responseBody.getString("max_cursor"))
                    .minCursor(responseBody.getString("min_cursor"))
                    .total(responseBody.getIntValue("total"))
                    .statusCode(responseBody.getIntValue("status_code"))
                    .statusMsg(responseBody.getString("status_msg"))
                    .build();

            log.info("成功获取用户作品列表: secUserId={}, 作品数={}, hasMore={}, maxCursor={}",
                    secUserId, awemeInfoList.size(), response.getHasMore(), response.getMaxCursor());

            return response;

        } catch (Exception e) {
            log.error("解析用户作品列表响应失败: secUserId={}, error={}", secUserId, e.getMessage(), e);
            throw new DouyinMessageException(ErrorCode.PARSE_ERROR,
                    "解析用户作品列表响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析单个作品信息
     *
     * @param awemeDetail 作品详情JSON
     * @return 作品信息实体
     */
    private UserPostResponse.AwemeInfo parseAwemeInfo(JSONObject awemeDetail) {
        UserPostResponse.AwemeInfo.AwemeInfoBuilder builder = UserPostResponse.AwemeInfo.builder();

        // 基本信息
        builder.awemeId(awemeDetail.getString("aweme_id"))
                .itemTitle(awemeDetail.getString("item_title"))
                .caption(awemeDetail.getString("desc")) // 注意：描述字段通常是"desc"
                .createTime(awemeDetail.getLong("create_time"))
                .duration(awemeDetail.getLong("duration"))
                .shareUrl(awemeDetail.getString("share_url"))
                .isTop(awemeDetail.getInteger("is_top") != null && awemeDetail.getInteger("is_top") == 1)
                .type(awemeDetail.getInteger("aweme_type")); // 注意：类型字段通常是"aweme_type"

        // 视频信息
        JSONObject video = awemeDetail.getJSONObject("video");
        if (video != null) {
            // 封面图
            JSONObject cover = video.getJSONObject("cover");
            if (cover != null) {
                JSONArray urlList = cover.getJSONArray("url_list");
                if (urlList != null && !urlList.isEmpty()) {
                    builder.videoCover(urlList.getString(0));
                }
            }

            // 播放地址
            JSONObject playAddr = video.getJSONObject("play_addr");
            if (playAddr != null) {
                JSONArray urlList = playAddr.getJSONArray("url_list");
                if (urlList != null && !urlList.isEmpty()) {
                    builder.videoUrl(urlList.getString(0));
                }
            }
        }

        // 作者信息
        JSONObject author = awemeDetail.getJSONObject("author");
        if (author != null) {
            builder.author(parseAuthorInfo(author));
        }

        // 统计信息
        JSONObject statistics = awemeDetail.getJSONObject("statistics");
        if (statistics != null) {
            builder.statistics(parseStatisticsInfo(statistics));
        }

        return builder.build();
    }

    /**
     * 解析作者信息
     *
     * @param author 作者信息JSON
     * @return 作者信息实体
     */
    private UserPostResponse.AuthorInfo parseAuthorInfo(JSONObject author) {
        UserPostResponse.AuthorInfo.AuthorInfoBuilder builder = UserPostResponse.AuthorInfo.builder();

        builder.secUid(author.getString("sec_uid"))
                .uid(author.getString("uid"))
                .uniqueId(author.getString("unique_id"))
                .shortId(author.getString("short_id"))
                .nickname(author.getString("nickname"))
                .signature(author.getString("signature"))
                .verificationType(author.getString("verification_type"))
                .followerCount(author.getLong("follower_count"))
                .followingCount(author.getLong("following_count"))
                .totalFavorited(author.getLong("total_favorited"));

        // 头像信息
        JSONObject avatarThumb = author.getJSONObject("avatar_thumb");
        if (avatarThumb != null) {
            JSONArray urlList = avatarThumb.getJSONArray("url_list");
            if (urlList != null && !urlList.isEmpty()) {
                builder.avatarUrl(urlList.getString(0));
            }
        }

        return builder.build();
    }

    /**
     * 解析统计信息
     *
     * @param statistics 统计信息JSON
     * @return 统计信息实体
     */
    private UserPostResponse.StatisticsInfo parseStatisticsInfo(JSONObject statistics) {
        return UserPostResponse.StatisticsInfo.builder()
                .diggCount(statistics.getLong("digg_count"))
                .commentCount(statistics.getLong("comment_count"))
                .collectCount(statistics.getLong("collect_count"))
                .shareCount(statistics.getLong("share_count"))
                .playCount(statistics.getLong("play_count"))
                .forwardCount(statistics.getLong("forward_count"))
                .build();
    }
}

