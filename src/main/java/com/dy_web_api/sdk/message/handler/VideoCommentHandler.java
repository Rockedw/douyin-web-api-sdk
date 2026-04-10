package com.dy_web_api.sdk.message.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.utils.BdTicketGuard;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dy_web_api.sdk.message.utils.TTSessionDtraitGenerator.getSessionDtrait;


/**
 * 视频评论处理器
 */
@Slf4j
public class VideoCommentHandler {

    private static final String COMMENT_API_URL = "https://www.douyin.com/aweme/v1/web/comment/publish";










    private final DouyinConfig config;
    private final HttpClient httpClient;

    public VideoCommentHandler(DouyinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * 评论视频
     * @param awemeId 视频ID
     * @param text 评论内容
     * @return 评论ID
     */
    public String commentVideo(String awemeId, String text) {
        return commentVideo(awemeId, text, null, null, null);
    }

    /**
     * 评论视频（带图片）
     * @param awemeId 视频ID
     * @param text 评论内容
     * @param imageUri 图片URI（可选）
     * @param imageWidth 图片宽度（可选）
     * @param imageHeight 图片高度（可选）
     * @return 评论ID
     */
    public String commentVideo(String awemeId, String text, String imageUri, Integer imageWidth, Integer imageHeight) {
        try {
            validateCommentParams(awemeId, text);
            
            Long currentTimeSeconds = System.currentTimeMillis() / 1000L;
            String clientData = BdTicketGuard.getHeadersBdTicketGuardClientData(
                    config.getPrivateKeyPem(), config.getCertificatePem(), config.getTsSign(),
                    config.getTicket(), "/aweme/v1/web/comment/publish", currentTimeSeconds);
            String reePublicKey = BdTicketGuard.getBdTicketGuardReePublicKey(config.getPublicKeyPem());
            
            // 构造查询参数
            Map<String, String> query = buildQueryParams();
            String fullUri = buildUriWithQuery(COMMENT_API_URL, query);

            // 构造 form-urlencoded body
            Map<String, String> form = buildCommentFormData(awemeId, text, imageUri, imageWidth, imageHeight);
            String formBody = buildForm(form);


            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUri))
                    .timeout(Duration.ofSeconds(30))
                    .header("accept", "application/json, text/plain, */*")
                    .header("accept-language", "zh-CN,zh;q=0.9")
                    .header("bd-ticket-guard-client-data", clientData)
                    .header("bd-ticket-guard-iteration-version", "1")
                    .header("bd-ticket-guard-ree-public-key", reePublicKey)
                    .header("bd-ticket-guard-version", "2")
                    .header("bd-ticket-guard-web-sign-type", "1")
                    .header("bd-ticket-guard-web-version", "2")
                    .header("origin", "https://www.douyin.com")
                    .header("priority", "u=1, i")
                    .header("referer", "https://www.douyin.com/user/self?from_tab_name=main&modal_id=" + awemeId)
                    .header("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"macOS\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-origin")
                    .header("uifid", query.get("uifid"))
                    .header("user-agent", config.getUserAgent())
                    .header("x-secsdk-csrf-token", "DOWNGRADE")
                    .header("x-tt-session-dtrait",  getSessionDtrait("/aweme/v1/web/comment/publish"))
                    .header("Cookie", config.getCookie())
                    .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            log.info("评论视频响应状态码: {}", response.statusCode());
            log.debug("评论视频响应内容: {}", response.body());
            
            if (response.statusCode() != 200) {
                handleHttpError(response.statusCode());
            }
            
            return parseCommentResponse(response.body());
            
        } catch (DouyinMessageException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            String errorMsg = "评论视频异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "评论视频异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }

    private void validateCommentParams(String awemeId, String text) {
        if (awemeId == null || awemeId.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "视频ID不能为空");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "评论内容不能为空");
        }
        if (text.length() > 1000) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "评论内容过长，最多1000个字符");
        }
    }

    private Map<String, String> buildQueryParams() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("app_name", "aweme");
        query.put("enter_from", "friend");
        query.put("previous_page", "friend");
        query.put("device_platform", "webapp");
        query.put("aid", "6383");
        query.put("channel", "channel_pc_web");
        query.put("pc_client_type", "1");
        query.put("pc_libra_divert", "Mac");
        query.put("update_version_code", "170400");
        query.put("support_h265", "1");
        query.put("support_dash", "1");
        query.put("version_code", "170400");
        query.put("version_name", "17.4.0");
        query.put("cookie_enabled", "true");
        query.put("screen_width", "3440");
        query.put("screen_height", "1440");
        query.put("browser_language", "zh-CN");
        query.put("browser_platform", "MacIntel");
        query.put("browser_name", "Chrome");
        query.put("browser_version", "139.0.0.0");
        query.put("browser_online", "true");
        query.put("engine_name", "Blink");
        query.put("engine_version", "139.0.0.0");
        query.put("os_name", "Mac+OS");
        query.put("os_version", "10.15.7");
        query.put("cpu_core_num", "8");
        query.put("device_memory", "8");
        query.put("platform", "PC");
        query.put("downlink", "10");
        query.put("effective_type", "4g");
        query.put("round_trip_time", "150");
        query.put("webid", config.getWebId());
        query.put("uifid", config.getUifid());
        query.put("msToken", config.getMsToken());
        query.put("verifyFp", config.getVerifyFp());
        query.put("fp", config.getFp());
        return query;
    }

    private Map<String, String> buildCommentFormData(String awemeId, String text, String imageUri, Integer imageWidth, Integer imageHeight) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("aweme_id", awemeId);
        form.put("comment_send_celltime", String.valueOf(System.currentTimeMillis() % 100000));
        form.put("comment_video_celltime", String.valueOf(System.currentTimeMillis() % 100000));
        form.put("one_level_comment_rank", "-1");
        form.put("paste_edit_method", "non_paste");
        form.put("text", text);
        form.put("text_extra", "[]");
        
        // 添加图片相关参数
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            form.put("image_formats", "jpeg");
            form.put("image_heights", imageHeight != null ? imageHeight.toString() : "3024");
            form.put("image_uri_list", imageUri);
            form.put("image_widths", imageWidth != null ? imageWidth.toString() : "4032");
        }
        
        return form;
    }

    private String buildUriWithQuery(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) return base;
        String q = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return base + "?" + q;
    }

    private String buildForm(Map<String, String> formParams) {
        if (formParams == null || formParams.isEmpty()) return "";
        return formParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }


    private String parseCommentResponse(String responseBody) {
        try {
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            if (jsonResponse.containsKey("status_code")) {
                int statusCode = jsonResponse.getIntValue("status_code");
                if (statusCode == 0) {
                    // 从响应中提取评论ID
                    JSONObject comment = jsonResponse.getJSONObject("comment");
                    if (comment != null && comment.containsKey("cid")) {
                        String commentId = comment.getString("cid");
                        log.info("评论发布成功，评论ID: {}", commentId);
                        return commentId;
                    } else {
                        log.warn("响应中未找到评论ID: {}", responseBody);
                        throw new DouyinMessageException(ErrorCode.RESPONSE_FORMAT_ERROR, "响应中未找到评论ID");
                    }
                } else {
                    String statusMsg = jsonResponse.getString("status_msg");
                    String errorMsg = String.format("评论发布失败: 状态码=%d, 错误信息=%s", statusCode, statusMsg);
                    log.error(errorMsg);
                    throw new DouyinMessageException(ErrorCode.COMMENT_FAILED, errorMsg);
                }
            } else {
                log.warn("响应格式异常，无法解析状态码: {}", responseBody);
                throw new DouyinMessageException(ErrorCode.RESPONSE_FORMAT_ERROR, "响应格式异常");
            }
            
        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = "解析评论响应失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.RESPONSE_PARSE_ERROR, errorMsg, e);
        }
    }

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
                errorMsg = "访问被拒绝，可能被限制评论";
                break;
            case 404:
                errorCode = ErrorCode.VIDEO_NOT_FOUND;
                errorMsg = "视频不存在";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "评论频率超限，请稍后重试";
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