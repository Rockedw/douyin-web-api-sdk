package com.dy_web_api.sdk.message.handler;

import com.dy_web_api.sdk.message.DouyinMessageSDK;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.config.TokenConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.utils.AWS4SignatureGenerator;
import com.dy_web_api.sdk.message.utils.AWS4SignatureUtils;
import com.dy_web_api.sdk.message.utils.ImageUploadAuthUtil;
import com.dy_web_api.sdk.message.utils.MsTokenUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dy_web_api.sdk.message.utils.HttpUtils.buildUrlWithParams;

@Slf4j
public class ResourceUploader {

    private final DouyinConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResourceUploader(DouyinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    public UploadAuthResponse uploadAuth() {
        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("device_platform", "webapp");
        params.put("aid", "6383");
        params.put("channel", "channel_pc_web");
        params.put("update_version_code", "170400");
        params.put("pc_client_type", "1");
        params.put("pc_libra_divert", "Mac");
        params.put("support_h265", "1");
        params.put("support_dash", "1");
        params.put("cpu_core_num", "8");
        params.put("version_code", "170400");
        params.put("version_name", "17.4.0");
        params.put("cookie_enabled", "true");
        params.put("screen_width", "1440");
        params.put("screen_height", "900");
        params.put("browser_language", "zh-CN");
        params.put("browser_platform", "MacIntel");
        params.put("browser_name", "Chrome");
        params.put("browser_version", "139.0.0.0");
        params.put("browser_online", "true");
        params.put("engine_name", "Blink");
        params.put("engine_version", "139.0.0.0");
        params.put("os_name", "Mac OS");
        params.put("os_version", "10.15.7");
        params.put("device_memory", "8");
        params.put("platform", "PC");
        params.put("downlink", "10");
        params.put("effective_type", "4g");
        params.put("round_trip_time", "150");
        params.put("webid", config.getWebId());
        params.put("uifid", config.getUifid());
        params.put("msToken", config.getMsToken()); // 使用配置中的msToken作为可变参数
        params.put("verifyFp", config.getFp());
        params.put("fp", config.getFp());

        // 构建URL
        String baseUrl = "https://www.douyin.com/aweme/v1/web/im/upload/config/v2";
        String url = buildUrlWithParams(baseUrl, params);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader("accept", "application/json, text/plain, */*")
                .setHeader("accept-language", "zh-CN,zh;q=0.9")
                .setHeader("cache-control", "no-cache")
                .setHeader("Cookie", "sessionid=" + config.getSessionId() + "; sessionid_ss=" + config.getSessionId())
                .setHeader("pragma", "no-cache")
                .setHeader("priority", "u=1, i")
                .setHeader("referer", "https://www.douyin.com/jingxuan")
                .setHeader("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .setHeader("sec-ch-ua-mobile", "?0")
                .setHeader("sec-ch-ua-platform", "\"macOS\"")
                .setHeader("sec-fetch-dest", "empty")
                .setHeader("sec-fetch-mode", "cors")
                .setHeader("sec-fetch-site", "same-origin")
                .setHeader("uifid", config.getUifid())
                .setHeader("user-agent", config.getUserAgent())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("上传配置响应状态码: {}", response.statusCode());

            // 处理HTTP错误
            if(response.statusCode() != 200) {
                handleUploadAuthHttpError(response.statusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());

            // 检查响应状态
            if(jsonNode.has("status_code") && jsonNode.get("status_code").asInt() != 0) {
                String errorMsg = jsonNode.has("status_msg") ? jsonNode.get("status_msg").asText() : "未知错误";
                throw new DouyinMessageException(ErrorCode.UPLOAD_AUTH_FAILED, "获取上传授权失败: " + errorMsg);
            }

            // 解析响应数据 - 根据实际JSON结构解析
            UploadAuthResponse authResponse = new UploadAuthResponse();

            // 这个接口返回的是多种配置，这里使用 public_image_config 作为主要配置
            if(jsonNode.has("public_image_config")) {
                JsonNode publicImageConfig = jsonNode.get("public_image_config");
                authResponse.setAccessKeyId(publicImageConfig.path("access_key_id").asText());
                authResponse.setSecretAccessKey(publicImageConfig.path("secret_access_key").asText());
                authResponse.setSessionToken(publicImageConfig.path("session_token").asText());
                authResponse.setSpaceName(publicImageConfig.path("space_name").asText());
                authResponse.setExpiredTime(String.valueOf(publicImageConfig.path("expire_at").asLong()));
            }

            // 如果需要其他配置，也可以解析
            if(jsonNode.has("inner_image_config")) {
                JsonNode innerImageConfig = jsonNode.get("inner_image_config");
                // 可以添加内部图片配置相关字段
            }

            authResponse.setCurrentTime(String.valueOf(System.currentTimeMillis()));

            log.info("上传授权获取成功: spaceName={}", authResponse.getSpaceName());
            return authResponse;

        } catch(DouyinMessageException e) {
            throw e;
        } catch(java.net.http.HttpTimeoutException e) {
            String errorMsg = "获取上传授权超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch(java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch(IOException e) {
            String errorMsg = "获取上传授权网络异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "获取上传授权被中断";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "获取上传授权失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_AUTH_FAILED, errorMsg, e);
        }
    }


    public CommentImageUploadAuthResponse uploadCommentImgAuth() {
        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("device_platform", "webapp");
        params.put("aid", "6383");
        params.put("channel", "channel_pc_web");
        params.put("update_version_code", "170400");
        params.put("pc_client_type", "1");
        params.put("pc_libra_divert", "Mac");
        params.put("support_h265", "1");
        params.put("support_dash", "1");
        params.put("cpu_core_num", "8");
        params.put("version_code", "170400");
        params.put("version_name", "17.4.0");
        params.put("cookie_enabled", "true");
        params.put("screen_width", "1440");
        params.put("screen_height", "900");
        params.put("browser_language", "zh-CN");
        params.put("browser_platform", "MacIntel");
        params.put("browser_name", "Chrome");
        params.put("browser_version", "139.0.0.0");
        params.put("browser_online", "true");
        params.put("engine_name", "Blink");
        params.put("engine_version", "139.0.0.0");
        params.put("os_name", "Mac OS");
        params.put("os_version", "10.15.7");
        params.put("device_memory", "8");
        params.put("platform", "PC");
        params.put("downlink", "10");
        params.put("effective_type", "4g");
        params.put("round_trip_time", "150");
        params.put("webid", config.getWebId());
        params.put("uifid", config.getUifid());
        params.put("msToken", config.getMsToken());
        params.put("verifyFp", config.getFp());
        params.put("fp", config.getFp());
        params.put("content_type", "0");

        // 构建URL
        String baseUrl = "https://www.douyin.com/aweme/v1/web/comment/upload/auth/";
        String url = buildUrlWithParams(baseUrl, params);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .setHeader("accept", "application/json, text/plain, */*")
                .setHeader("accept-language", "zh-CN,zh;q=0.9")
                .setHeader("cache-control", "no-cache")
                .setHeader("Cookie", "sessionid=" + config.getSessionId() + "; sessionid_ss=" + config.getSessionId())
                .setHeader("pragma", "no-cache")
                .setHeader("priority", "u=1, i")
                .setHeader("referer", "https://www.douyin.com/jingxuan")
                .setHeader("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .setHeader("sec-ch-ua-mobile", "?0")
                .setHeader("sec-ch-ua-platform", "\"macOS\"")
                .setHeader("sec-fetch-dest", "empty")
                .setHeader("sec-fetch-mode", "cors")
                .setHeader("sec-fetch-site", "same-origin")
                .setHeader("uifid", config.getUifid())
                .setHeader("user-agent", config.getUserAgent())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.info("评论图片上传授权响应状态码: {}", response.statusCode());

            // 处理HTTP错误
            if(response.statusCode() != 200) {
                handleUploadAuthHttpError(response.statusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());

            // 检查响应状态
            if(jsonNode.has("status_code") && jsonNode.get("status_code").asInt() != 0) {
                String errorMsg = jsonNode.has("status_msg") ? jsonNode.get("status_msg").asText() : "未知错误";
                throw new DouyinMessageException(ErrorCode.UPLOAD_AUTH_FAILED, "获取评论图片上传授权失败: " + errorMsg);
            }

            // 解析新的响应数据结构
            CommentImageUploadAuthResponse authResponse = new CommentImageUploadAuthResponse();
            authResponse.setExpiredTime(jsonNode.path("expired_time").asLong());
            authResponse.setServiceId(jsonNode.path("service_id").asText());
            authResponse.setStatusCode(jsonNode.path("status_code").asInt());
            authResponse.setUploadDomain(jsonNode.path("upload_domain").asText());
            authResponse.setAccessKey(jsonNode.path("access_key").asText());
            authResponse.setSecretKey(jsonNode.path("secret_key").asText());
            authResponse.setSessionToken(jsonNode.path("session_token").asText());
            authResponse.setCurrentTime(jsonNode.path("current_time").asLong());

            // 解析extra字段
            if(jsonNode.has("extra")) {
                JsonNode extraNode = jsonNode.get("extra");
                CommentImageUploadAuthResponse.Extra extra = new CommentImageUploadAuthResponse.Extra();
                extra.setNow(extraNode.path("now").asLong());
                extra.setLogid(extraNode.path("logid").asText());

                // 解析fatal_item_ids数组
                if(extraNode.has("fatal_item_ids") && extraNode.get("fatal_item_ids").isArray()) {
                    JsonNode fatalItemIds = extraNode.get("fatal_item_ids");
                    String[] items = new String[fatalItemIds.size()];
                    for(int i = 0; i < fatalItemIds.size(); i++) {
                        items[i] = fatalItemIds.get(i).asText();
                    }
                    extra.setFatalItemIds(items);
                }
                authResponse.setExtra(extra);
            }

            // 解析log_pb字段
            if(jsonNode.has("log_pb")) {
                JsonNode logPbNode = jsonNode.get("log_pb");
                CommentImageUploadAuthResponse.LogPb logPb = new CommentImageUploadAuthResponse.LogPb();
                logPb.setImprId(logPbNode.path("impr_id").asText());
                authResponse.setLogPb(logPb);
            }

            log.info("评论图片上传授权获取成功: serviceId={}, accessKey={}",
                    authResponse.getServiceId(), authResponse.getAccessKey());
            return authResponse;

        } catch(DouyinMessageException e) {
            throw e;
        } catch(java.net.http.HttpTimeoutException e) {
            String errorMsg = "获取评论图片上传授权超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch(java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch(IOException e) {
            String errorMsg = "获取评论图片上传授权网络异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "获取评论图片上传授权被中断";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "获取评论图片上传授权失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_AUTH_FAILED, errorMsg, e);
        }
    }



    public UploadAddressResponse getResourceUploadAddress(String authorization, String token, String s, String fileSize, String userId, String amzDate) {

        // 构建请求URL
        Map<String, String> params = new HashMap<>();
        params.put("Action", "ApplyUploadInner");
        params.put("FileSize", fileSize);
        params.put("FileType", "image");
        params.put("IsInner", "1");
        params.put("s", s);
        params.put("SpaceName", "zhenzhen");
        params.put("Version", "2020-11-19");
        params.put("NeedFallback", "true");

        String url = "https://vod.bytedanceapi.com/?";
        url += AWS4SignatureGenerator.buildStrParams(params);

        log.info("请求URL: {}", url);

        // 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "*/*")
                .header("Authorization", authorization)
                .header("User-Agent", config.getUserAgent())
                .header("X-Amz-Date", amzDate)
                .header("x-amz-security-token", token)
                .GET()
                .build();

        try {
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("获取图片上传地址响应状态码: {}", response.statusCode());
            log.debug("获取图片上传地址响应: {}", response.body());

            // 处理HTTP错误
            if(response.statusCode() != 200) {
                handleUploadAddressHttpError(response.statusCode());
            }

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response.body());

            // 检查错误
            if(jsonNode.has("ResponseMetadata") && jsonNode.get("ResponseMetadata").has("Error")) {
                JsonNode error = jsonNode.get("ResponseMetadata").get("Error");
                String errorCode = error.path("Code").asText();
                String errorMessage = error.path("Message").asText();
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "获取上传地址失败: " + errorCode + " - " + errorMessage);
            }

            // 解析成功响应
            UploadAddressResponse uploadResponse = new UploadAddressResponse();

            if(jsonNode.has("ResponseMetadata")) {
                JsonNode metadata = jsonNode.get("ResponseMetadata");
                uploadResponse.setRequestId(metadata.path("RequestId").asText());
                uploadResponse.setAction(metadata.path("Action").asText());
                uploadResponse.setVersion(metadata.path("Version").asText());
                uploadResponse.setService(metadata.path("Service").asText());
                uploadResponse.setRegion(metadata.path("Region").asText());
            }

            if(jsonNode.has("Result")) {
                JsonNode result = jsonNode.get("Result");

                // 解析 UploadAddress
                if(result.has("UploadAddress")) {
                    JsonNode uploadAddress = result.get("UploadAddress");
                    UploadAddressInfo uploadAddressInfo = new UploadAddressInfo();

                    // 解析 StoreInfos
                    if(uploadAddress.has("StoreInfos") && uploadAddress.get("StoreInfos").isArray()) {
                        JsonNode storeInfos = uploadAddress.get("StoreInfos").get(0); // 取第一个
                        StoreInfo storeInfo = new StoreInfo();
                        storeInfo.setStoreUri(storeInfos.path("StoreUri").asText());
                        storeInfo.setAuth(storeInfos.path("Auth").asText());
                        storeInfo.setUploadID(storeInfos.path("UploadID").asText());

                        if(storeInfos.has("StorageHeader")) {
                            JsonNode storageHeader = storeInfos.get("StorageHeader");
                            storeInfo.setUserId(storageHeader.path("USER_ID").asText());
                        }

                        uploadAddressInfo.setStoreInfo(storeInfo);
                    }

                    // 解析 UploadHosts
                    if(uploadAddress.has("UploadHosts") && uploadAddress.get("UploadHosts").isArray()) {
                        String uploadHost = uploadAddress.get("UploadHosts").get(0).asText();
                        uploadAddressInfo.setUploadHost(uploadHost);
                    }

                    uploadAddressInfo.setSessionKey(uploadAddress.path("SessionKey").asText());
                    uploadResponse.setUploadAddress(uploadAddressInfo);
                }

                // 解析 InnerUploadAddress
                if(result.has("InnerUploadAddress")) {
                    JsonNode innerUploadAddress = result.get("InnerUploadAddress");
                    if(innerUploadAddress.has("UploadNodes") && innerUploadAddress.get("UploadNodes").isArray()) {
                        JsonNode uploadNode = innerUploadAddress.get("UploadNodes").get(0);
                        InnerUploadAddressInfo innerUploadAddressInfo = new InnerUploadAddressInfo();

                        if(uploadNode.has("StoreInfos") && uploadNode.get("StoreInfos").isArray()) {
                            JsonNode storeInfos = uploadNode.get("StoreInfos").get(0);
                            StoreInfo storeInfo = new StoreInfo();
                            storeInfo.setStoreUri(storeInfos.path("StoreUri").asText());
                            storeInfo.setAuth(storeInfos.path("Auth").asText());
                            storeInfo.setUploadID(storeInfos.path("UploadID").asText());

                            if(storeInfos.has("StorageHeader")) {
                                JsonNode storageHeader = storeInfos.get("StorageHeader");
                                storeInfo.setUserId(storageHeader.path("USER_ID").asText());
                            }

                            innerUploadAddressInfo.setStoreInfo(storeInfo);
                        }

                        innerUploadAddressInfo.setUploadHost(uploadNode.path("UploadHost").asText());
                        innerUploadAddressInfo.setSessionKey(uploadNode.path("SessionKey").asText());
                        uploadResponse.setInnerUploadAddress(innerUploadAddressInfo);
                    }
                }
            }

            log.info("获取上传地址成功: requestId={}, storeUri={}",
                    uploadResponse.getRequestId(),
                    uploadResponse.getUploadAddress() != null ? uploadResponse.getUploadAddress().getStoreInfo().getStoreUri() : "null");

            return uploadResponse;

        } catch(DouyinMessageException e) {
            throw e;
        } catch(java.net.http.HttpTimeoutException e) {
            String errorMsg = "获取上传地址超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch(java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch(IOException e) {
            String errorMsg = "获取上传地址网络异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "获取上传地址被中断";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "获取上传地址失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }


    public UploadAddressResponse getCommentImgResourceUploadAddress(String authorization, String token, String s, String fileSize, String userId, String amzDate) {

        // 构建请求URL
        Map<String, String> params = new HashMap<>();
        params.put("Action", "ApplyImageUpload");
        params.put("Version", "2018-08-01");
        params.put("ServiceId", "p14lwwcsbr");
        params.put("s", s);

        String url = "https://imagex.bytedanceapi.com/?";
        url += AWS4SignatureGenerator.buildStrParams(params);

        log.info("请求URL: {}", url);

        // 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "*/*")
                .header("Authorization", authorization)
                .header("User-Agent", config.getUserAgent())
                .header("X-Amz-Date", amzDate)
                .header("x-amz-security-token", token)
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "cross-site")
                .header("sec-ch-ua","\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("origin", "https://www.douyin.com")
                .header("priority", "u=1, i")
                .header("referer", "https://www.douyin.com/")
                .GET()
                .build();

        try {
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("获取图片上传地址响应状态码: {}", response.statusCode());
            log.debug("获取图片上传地址响应: {}", response.body());

            // 处理HTTP错误
            if(response.statusCode() != 200) {
                handleUploadAddressHttpError(response.statusCode());
            }

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response.body());

            // 检查错误
            if(jsonNode.has("ResponseMetadata") && jsonNode.get("ResponseMetadata").has("Error")) {
                JsonNode error = jsonNode.get("ResponseMetadata").get("Error");
                String errorCode = error.path("Code").asText();
                String errorMessage = error.path("Message").asText();
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "获取上传地址失败: " + errorCode + " - " + errorMessage);
            }

            // 解析成功响应
            UploadAddressResponse uploadResponse = new UploadAddressResponse();

            if(jsonNode.has("ResponseMetadata")) {
                JsonNode metadata = jsonNode.get("ResponseMetadata");
                uploadResponse.setRequestId(metadata.path("RequestId").asText());
                uploadResponse.setAction(metadata.path("Action").asText());
                uploadResponse.setVersion(metadata.path("Version").asText());
                uploadResponse.setService(metadata.path("Service").asText());
                uploadResponse.setRegion(metadata.path("Region").asText());
            }

            if(jsonNode.has("Result")) {
                JsonNode result = jsonNode.get("Result");

                // 解析 UploadAddress
                if(result.has("UploadAddress")) {
                    JsonNode uploadAddress = result.get("UploadAddress");
                    UploadAddressInfo uploadAddressInfo = new UploadAddressInfo();

                    // 解析 StoreInfos
                    if(uploadAddress.has("StoreInfos") && uploadAddress.get("StoreInfos").isArray()) {
                        JsonNode storeInfos = uploadAddress.get("StoreInfos").get(0); // 取第一个
                        StoreInfo storeInfo = new StoreInfo();
                        storeInfo.setStoreUri(storeInfos.path("StoreUri").asText());
                        storeInfo.setAuth(storeInfos.path("Auth").asText());
                        storeInfo.setUploadID(storeInfos.path("UploadID").asText());

                        if(storeInfos.has("StorageHeader")) {
                            JsonNode storageHeader = storeInfos.get("StorageHeader");
                            storeInfo.setUserId(storageHeader.path("USER_ID").asText());
                        }

                        uploadAddressInfo.setStoreInfo(storeInfo);
                    }

                    // 解析 UploadHosts
                    if(uploadAddress.has("UploadHosts") && uploadAddress.get("UploadHosts").isArray()) {
                        String uploadHost = uploadAddress.get("UploadHosts").get(0).asText();
                        uploadAddressInfo.setUploadHost(uploadHost);
                    }

                    uploadAddressInfo.setSessionKey(uploadAddress.path("SessionKey").asText());
                    uploadResponse.setUploadAddress(uploadAddressInfo);
                }

                // 解析 InnerUploadAddress
                if(result.has("InnerUploadAddress")) {
                    JsonNode innerUploadAddress = result.get("InnerUploadAddress");
                    if(innerUploadAddress.has("UploadNodes") && innerUploadAddress.get("UploadNodes").isArray()) {
                        JsonNode uploadNode = innerUploadAddress.get("UploadNodes").get(0);
                        InnerUploadAddressInfo innerUploadAddressInfo = new InnerUploadAddressInfo();

                        if(uploadNode.has("StoreInfos") && uploadNode.get("StoreInfos").isArray()) {
                            JsonNode storeInfos = uploadNode.get("StoreInfos").get(0);
                            StoreInfo storeInfo = new StoreInfo();
                            storeInfo.setStoreUri(storeInfos.path("StoreUri").asText());
                            storeInfo.setAuth(storeInfos.path("Auth").asText());
                            storeInfo.setUploadID(storeInfos.path("UploadID").asText());

                            if(storeInfos.has("StorageHeader")) {
                                JsonNode storageHeader = storeInfos.get("StorageHeader");
                                storeInfo.setUserId(storageHeader.path("USER_ID").asText());
                            }

                            innerUploadAddressInfo.setStoreInfo(storeInfo);
                        }

                        innerUploadAddressInfo.setUploadHost(uploadNode.path("UploadHost").asText());
                        innerUploadAddressInfo.setSessionKey(uploadNode.path("SessionKey").asText());
                        uploadResponse.setInnerUploadAddress(innerUploadAddressInfo);
                    }
                }
            }

            log.info("获取上传地址成功: requestId={}, storeUri={}",
                    uploadResponse.getRequestId(),
                    uploadResponse.getUploadAddress() != null ? uploadResponse.getUploadAddress().getStoreInfo().getStoreUri() : "null");

            return uploadResponse;

        } catch(DouyinMessageException e) {
            throw e;
        } catch(java.net.http.HttpTimeoutException e) {
            String errorMsg = "获取上传地址超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch(java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch(IOException e) {
            String errorMsg = "获取上传地址网络异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "获取上传地址被中断";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "获取上传地址失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }


    /**
     * 上传文件到TOS
     */
    public UploadFileResponse uploadFileToTos(UploadAddressResponse uploadAddressResponse, byte[] fileData) {

        StoreInfo storeInfo = uploadAddressResponse.getInnerUploadAddress().getStoreInfo();
        String uploadHost = uploadAddressResponse.getInnerUploadAddress().getUploadHost();

        // 构建上传URL
        String uploadUrl = "https://" + uploadHost + "/upload/v1/" + storeInfo.getStoreUri();

        // 计算CRC32
        String crc32 = calculateCRC32(fileData);

        log.info("开始上传文件, URL: {}, 文件大小: {} bytes, CRC32: {}", uploadUrl, fileData.length, crc32);

        // 构建HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("accept", "*/*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("authorization", storeInfo.getAuth())
                .header("cache-control", "no-cache")
                .header("content-crc32", crc32)
                .header("content-disposition", "attachment; filename=\"image\"")
                .header("content-type", "application/octet-stream")
                .header("origin", "https://www.douyin.com")
                .header("pragma", "no-cache")
                .header("priority", "u=1, i")
                .header("referer", "https://www.douyin.com/")
                .header("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-site")
                .header("user-agent", config.getUserAgent())
                .header("x-storage-u", config.getUserId())
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileData))
                .build();

        try {
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("上传响应状态码: {}", response.statusCode());
            log.debug("上传响应内容: {}", response.body());

            // 处理HTTP错误
            if(response.statusCode() != 200) {
                handleFileUploadHttpError(response.statusCode());
            }

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response.body());

            UploadFileResponse uploadFileResponse = new UploadFileResponse();
            uploadFileResponse.setCode(jsonNode.path("code").asInt());
            uploadFileResponse.setApiVersion(jsonNode.path("apiversion").asText());
            uploadFileResponse.setMessage(jsonNode.path("message").asText());

            if(jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                uploadFileResponse.setCrc32(data.path("crc32").asText());
            }

            // 检查上传结果
            if(uploadFileResponse.getCode() != 2000) {
                throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, "文件上传失败: " + uploadFileResponse.getMessage());
            }

            log.info("文件上传成功: CRC32={}", uploadFileResponse.getCrc32());
            return uploadFileResponse;

        } catch(DouyinMessageException e) {
            throw e;
        } catch(java.net.http.HttpTimeoutException e) {
            String errorMsg = "文件上传超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch(java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch(IOException e) {
            String errorMsg = "文件上传网络异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "文件上传被中断";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "文件上传失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }

    /**
     * 计算CRC32校验和
     */
    private String calculateCRC32(byte[] data) {
        java.util.zip.CRC32 crc32 = new java.util.zip.CRC32();
        crc32.update(data);
        return String.format("%08x", crc32.getValue());
    }

    /**
     * 从文件路径上传
     */
    public UploadFileResponse uploadFile(UploadAddressResponse uploadAddressResponse, String filePath) {
        try {
            // 文件存在性检查
            File file = new File(filePath);
            if(! file.exists()) {
                throw new DouyinMessageException(ErrorCode.FILE_NOT_FOUND, "文件不存在: " + filePath);
            }
            if(! file.isFile()) {
                throw new DouyinMessageException(ErrorCode.FILE_FORMAT_UNSUPPORTED, "路径不是文件: " + filePath);
            }
            if(file.length() > 50 * 1024 * 1024) { // 50MB限制
                throw new DouyinMessageException(ErrorCode.FILE_TOO_LARGE, "文件过大，超过50MB限制");
            }

            byte[] fileData = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            return uploadFileToTos(uploadAddressResponse, fileData);
        } catch(DouyinMessageException e) {
            throw e;
        } catch(IOException e) {
            String errorMsg = "读取文件失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.FILE_NOT_FOUND, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "上传文件失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }

    /**
     * 从InputStream上传
     */
    public UploadFileResponse uploadFile(UploadAddressResponse uploadAddressResponse, InputStream inputStream) {
        try {
            if(inputStream == null) {
                throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "输入流不能为空");
            }

            byte[] fileData = inputStream.readAllBytes();
            if(fileData.length == 0) {
                throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "文件内容为空");
            }
            if(fileData.length > 50 * 1024 * 1024) { // 50MB限制
                throw new DouyinMessageException(ErrorCode.FILE_TOO_LARGE, "文件过大，超过50MB限制");
            }

            return uploadFileToTos(uploadAddressResponse, fileData);
        } catch(DouyinMessageException e) {
            throw e;
        } catch(IOException e) {
            String errorMsg = "读取流数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch(Exception e) {
            String errorMsg = "上传流数据失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.UPLOAD_FAILED, errorMsg, e);
        }
    }

    public CommitUploadResponse commitUpload(String sessionKey, String accessKeyId, String secretAccessKey,
                                             String sessionToken, String[] times) throws Exception
    {

        // 构建请求体
        CommitUploadRequest requestBody = buildCommitUploadRequest(sessionKey);

        // 转换为JSON字符串
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        log.info("请求体JSON: {}", jsonBody);

        // 计算 x-amz-content-sha256
        String contentSha256 = AWS4SignatureUtils.generateContentSha256(jsonBody);
        log.info("计算的 x-amz-content-sha256: {}", contentSha256);

        // 构建查询参数
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Action", "CommitUploadInner");
        queryParams.put("Version", "2020-11-19");
        queryParams.put("SpaceName", "zhenzhen");

        // 生成时间戳
        String amzDate = times[0];
        String dateStamp = times[1];

        log.info("使用时间戳: amzDate={}, dateStamp={}", amzDate, dateStamp);

        // 生成Authorization签名
        String authorization = AWS4SignatureUtils.generateCommitUploadAuthorization(
                secretAccessKey, "cn-north-1", "vod", queryParams,
                amzDate, sessionToken, dateStamp, accessKeyId, jsonBody
        );

        log.info("生成的Authorization: {}", authorization);

        // 构建完整URL
        String url = "https://vod.bytedanceapi.com/?" + AWS4SignatureUtils.buildStrParams(queryParams);
        log.info("请求URL: {}", url);

        // 发送HTTP请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "*/*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("authorization", authorization)
                .header("cache-control", "no-cache")
                .header("content-type", "text/plain;charset=UTF-8")
                .header("origin", "https://www.douyin.com")
                .header("pragma", "no-cache")
                .header("priority", "u=1, i")
                .header("referer", "https://www.douyin.com/")
                .header("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "cross-site")
                .header("user-agent", config.getUserAgent())
                .header("x-amz-content-sha256", contentSha256)
                .header("x-amz-date", amzDate)
                .header("x-amz-security-token", sessionToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        // 发送请求
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("提交上传响应状态码: {}", response.statusCode());
        log.info("提交上传响应内容: {}", response.body());

        // 解析响应
        return parseCommitUploadResponse(response.body());
    }


   public CommitUploadResponse commitCommentImgUpload(String sessionKey, String accessKeyId, String secretAccessKey,
                                             String sessionToken, String[] times) throws Exception
    {
        // 构建查询参数
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Action", "CommitImageUpload");
        queryParams.put("Version", "2018-08-01");
        queryParams.put("ServiceId", "p14lwwcsbr");
        queryParams.put("SessionKey", sessionKey);

        // 生成时间戳
        String amzDate = times[0];
        String dateStamp = times[1];

        // **关键修改：空请求体**
        String jsonBody = "";  // 或者 null

        // 计算空body的SHA256
        String contentSha256 = AWS4SignatureUtils.generateContentSha256(jsonBody);

        // 生成Authorization签名（基于空body）
        String authorization = AWS4SignatureUtils.generateCommentImgCommitUploadAuthorization(
                secretAccessKey, "cn-north-1", "imagex", queryParams,
                amzDate, sessionToken, dateStamp, accessKeyId, jsonBody
        );

        // 构建完整URL
        String url = "https://imagex.bytedanceapi.com/?" + AWS4SignatureUtils.buildStrParams(queryParams);

        // **修改HTTP请求 - 空body**
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "*/*")  // 修改为与cURL一致
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("authorization", authorization)
                .header("content-type", "application/json")
                .header("origin", "https://www.douyin.com")
                .header("priority", "u=1, i")
                .header("referer", "https://www.douyin.com/")
                .header("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "cross-site")
                .header("user-agent", config.getUserAgent())
                .header("x-amz-date", amzDate)
                .header("x-amz-security-token", sessionToken)
                .POST(HttpRequest.BodyPublishers.ofString(""))  // 空body
                .build();

        // 发送请求
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        log.info("提交上传响应状态码: {}", response.statusCode());
        log.info("提交上传响应内容: {}", response.body());

        // 解析响应
        return parseCommentImgCommitUploadResponse(response.body());
    }


    /**
     * 构建提交上传请求体
     */
    private CommitUploadRequest buildCommitUploadRequest(String sessionKey) {
        CommitUploadRequest request = new CommitUploadRequest();
        request.setSessionKey(sessionKey);

        // 添加加密函数配置
        CommitUploadRequest.Function function = new CommitUploadRequest.Function();
        function.setName("Encryption");

        CommitUploadRequest.FunctionInput input = new CommitUploadRequest.FunctionInput();

        // 配置加密选项
        CommitUploadRequest.Config encryptionConfig = new CommitUploadRequest.Config();
        encryptionConfig.setCopies("cipher_v2");
        input.setConfig(encryptionConfig);

        // 配置策略参数
        CommitUploadRequest.PolicyParams policyParams = new CommitUploadRequest.PolicyParams();
        policyParams.setPolicySet("check,thumb,medium,large");
        input.setPolicyParams(policyParams);

        function.setInput(input);
        request.setFunctions(List.of(function));

        return request;
    }

    /**
     * 解析提交上传响应
     */
    private CommitUploadResponse parseCommitUploadResponse(String responseBody) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // 检查是否有错误
        if(jsonNode.has("ResponseMetadata") && jsonNode.get("ResponseMetadata").has("Error")) {
            JsonNode error = jsonNode.get("ResponseMetadata").get("Error");
            String errorCode = error.path("Code").asText();
            String errorMessage = error.path("Message").asText();
            throw new DouyinMessageException("提交上传失败: " + errorCode + " - " + errorMessage);
        }

        CommitUploadResponse response = new CommitUploadResponse();

        // 解析Results数组
        if(jsonNode.has("Result") && jsonNode.get("Result").has("Results")) {
            JsonNode resultsArray = jsonNode.get("Result").get("Results");
            List<CommitUploadResponse.UploadResult> results = new java.util.ArrayList<>();

            for(JsonNode resultNode : resultsArray) {
                CommitUploadResponse.UploadResult uploadResult = new CommitUploadResponse.UploadResult();
                uploadResult.setUri(resultNode.path("Uri").asText());
                uploadResult.setUriStatus(resultNode.path("UriStatus").asInt());

                // 解析Encryption信息
                if(resultNode.has("Encryption")) {
                    JsonNode encryptionNode = resultNode.get("Encryption");
                    CommitUploadResponse.Encryption encryption = new CommitUploadResponse.Encryption();
                    encryption.setUri(encryptionNode.path("Uri").asText());
                    encryption.setSecretKey(encryptionNode.path("SecretKey").asText());
                    encryption.setAlgorithm(encryptionNode.path("Algorithm").asText());
                    encryption.setVersion(encryptionNode.path("Version").asText());
                    encryption.setSourceMd5(encryptionNode.path("SourceMd5").asText());

                    uploadResult.setEncryption(encryption);
                }

                results.add(uploadResult);
            }

            response.setResults(results);
        }

        log.info("提交上传解析成功: 结果数量={}, 第一个URI={}",
                response.getResults() != null ? response.getResults().size() : 0,
                response.getFirstEncryptionUri());

        return response;
    }



    private CommitUploadResponse parseCommentImgCommitUploadResponse(String responseBody) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // 检查是否有错误
        if(jsonNode.has("ResponseMetadata") && jsonNode.get("ResponseMetadata").has("Error")) {
            JsonNode error = jsonNode.get("ResponseMetadata").get("Error");
            String errorCode = error.path("Code").asText();
            String errorMessage = error.path("Message").asText();
            throw new DouyinMessageException("提交上传失败: " + errorCode + " - " + errorMessage);
        }

        CommitUploadResponse response = new CommitUploadResponse();

        // 解析Result
        if(jsonNode.has("Result")) {
            JsonNode result = jsonNode.get("Result");

            // 解析Results数组
            List<CommitUploadResponse.UploadResult> results = new ArrayList<>();
            if(result.has("Results") && result.get("Results").isArray()) {
                JsonNode resultsArray = result.get("Results");
                for(JsonNode resultNode : resultsArray) {
                    CommitUploadResponse.UploadResult uploadResult = new CommitUploadResponse.UploadResult();
                    uploadResult.setUri(resultNode.path("Uri").asText());
                    uploadResult.setUriStatus(resultNode.path("UriStatus").asInt());
                    results.add(uploadResult);
                }
            }

            // 解析PluginResult数组
            if(result.has("PluginResult") && result.get("PluginResult").isArray()) {
                JsonNode pluginResultArray = result.get("PluginResult");
                for(int i = 0; i < pluginResultArray.size() && i < results.size(); i++) {
                    JsonNode pluginNode = pluginResultArray.get(i);
                    CommitUploadResponse.UploadResult uploadResult = results.get(i);

                    // 创建并设置PluginResult信息
                    CommitUploadResponse.PluginResult pluginResult = new CommitUploadResponse.PluginResult();
                    pluginResult.setFileName(pluginNode.path("FileName").asText());
                    pluginResult.setSourceUri(pluginNode.path("SourceUri").asText());
                    pluginResult.setImageUri(pluginNode.path("ImageUri").asText());
                    pluginResult.setImageWidth(pluginNode.path("ImageWidth").asInt());
                    pluginResult.setImageHeight(pluginNode.path("ImageHeight").asInt());
                    pluginResult.setImageMd5(pluginNode.path("ImageMd5").asText());
                    pluginResult.setImageFormat(pluginNode.path("ImageFormat").asText());
                    pluginResult.setImageSize(pluginNode.path("ImageSize").asInt());
                    pluginResult.setFrameCnt(pluginNode.path("FrameCnt").asInt());

                    uploadResult.setPluginResult(pluginResult);
                }
            }

            response.setResults(results);
        }

        log.info("提交上传解析成功: 结果数量={}, 第一个URI={}",
                response.getResults() != null ? response.getResults().size() : 0,
                response.getFirstEncryptionUri());

        return response;
    }


    /**
     * 处理上传授权HTTP错误
     */
    private void handleUploadAuthHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;

        switch(statusCode) {
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "上传授权认证失败，请检查sessionId";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "上传授权访问被拒绝";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "上传授权请求频率超限";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "上传授权服务不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "上传授权服务超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "上传授权HTTP请求失败，状态码: " + statusCode;
        }

        throw new DouyinMessageException(errorCode, errorMsg);
    }

    /**
     * 处理获取上传地址HTTP错误
     */
    private void handleUploadAddressHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;

        switch(statusCode) {
            case 400:
                errorCode = ErrorCode.INVALID_PARAMETER;
                errorMsg = "获取上传地址参数错误";
                break;
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "获取上传地址认证失败";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "获取上传地址访问被拒绝";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "获取上传地址请求频率超限";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "上传地址服务不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "获取上传地址超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "获取上传地址HTTP请求失败，状态码: " + statusCode;
        }

        throw new DouyinMessageException(errorCode, errorMsg);
    }

    /**
     * 处理文件上传HTTP错误
     */
    private void handleFileUploadHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;

        switch(statusCode) {
            case 400:
                errorCode = ErrorCode.INVALID_PARAMETER;
                errorMsg = "文件上传参数错误";
                break;
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "文件上传认证失败";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "文件上传访问被拒绝";
                break;
            case 413:
                errorCode = ErrorCode.FILE_TOO_LARGE;
                errorMsg = "上传文件过大";
                break;
            case 415:
                errorCode = ErrorCode.FILE_FORMAT_UNSUPPORTED;
                errorMsg = "不支持的文件格式";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "文件上传请求频率超限";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "文件上传服务不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "文件上传超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "文件上传HTTP请求失败，状态码: " + statusCode;
        }

        throw new DouyinMessageException(errorCode, errorMsg);
    }


    public static void main(String[] args) throws Exception {
        TokenConfig tokenConfig = TokenConfig.createFromYourConfig();
        String msToken = MsTokenUtil.genRealMsToken(tokenConfig);
        DouyinConfig config = DouyinConfig.builder()
                .sessionId("f9321232b62b215b2d31c9ec0a88786e")
                .cookie("__ac_signature=_02B4Z6wo00f01JIwgaAAAIDCoaV34ISXepCSEIUAAExk64; enter_pc_once=1; UIFID_TEMP=f718f562fcd874811d9c30568517194c189689a7c74491d0ed9c7c2e831358f1c4998440983fedec5f1826ae1862c87d03a9da8647f04224479aadd114e2afd2b9fe9e4585ecedb06f20bd82b5415d5d; x-web-secsdk-uid=bbd5aceb-67c5-4fcf-a5a8-bd56d77087f2; s_v_web_id=verify_mfxp8wx5_cXZv6rsf_EneE_4H3Z_BGyV_wVmOUpHVRlNU; douyin.com; device_web_cpu_core=8; device_web_memory_size=8; hevc_supported=true; dy_swidth=3440; dy_sheight=1440; fpk1=U2FsdGVkX19SKl5gwDWlhKWKAI19HkxDD3vAroZP9VTTgKGgFpnsqPQ5hXW9nLgE3anV1NFBmCApGXn0rmRVNA==; fpk2=df4bf04f9bf7b6af09e3e94179733770; __security_mc_1_s_sdk_crypt_sdk=c9157a1c-4aab-96c5; bd_ticket_guard_client_web_domain=2; passport_csrf_token=5b385f7d8d6c5ba72d558369d3041cd4; passport_csrf_token_default=5b385f7d8d6c5ba72d558369d3041cd4; n_mh=pjNH3dHUb0WTMX5kvUoozRKRt8BR84cyEweox7lDPrk; is_staff_user=false; __security_mc_1_s_sdk_cert_key=b2627c33-4bdf-82f6; __security_server_data_status=1; UIFID=f718f562fcd874811d9c30568517194c189689a7c74491d0ed9c7c2e831358f1d1812dc1b701b679156cb1a5438080a259a88ee53317dbc5e3dae356758512a5bd4dcb31d638dd951eed6273bccc1320e53b028b6976970f86a72abf439129a630c55c74425b1f1fa5eeb96950d68133d2eb9a12c825147a0e5d1ce365df685032b5d5a4f59c627316dd3dbfc2cfda5dde54e6a95d9f10dc1b37f2f6c394b137; SelfTabRedDotControl=%5B%5D; is_dash_user=1; publish_badge_show_info=%220%2C0%2C0%2C1758701183695%22; my_rd=2; stream_player_status_params=%22%7B%5C%22is_auto_play%5C%22%3A0%2C%5C%22is_full_screen%5C%22%3A0%2C%5C%22is_full_webscreen%5C%22%3A0%2C%5C%22is_mute%5C%22%3A1%2C%5C%22is_speed%5C%22%3A1%2C%5C%22is_visible%5C%22%3A0%7D%22; volume_info=%7B%22isUserMute%22%3Afalse%2C%22isMute%22%3Atrue%2C%22volume%22%3A0.5%7D; DiscoverFeedExposedAd=%7B%7D; strategyABtestKey=%221758853161.45%22; download_guide=%223%2F20250925%2F1%22; WallpaperGuide=%7B%22showTime%22%3A1758782455328%2C%22closeTime%22%3A0%2C%22showCount%22%3A1%2C%22cursor1%22%3A32%2C%22cursor2%22%3A10%2C%22hoverTime%22%3A1758873584914%7D; gulu_source_res=eyJwX2luIjoiYmY5ZDgyN2ZlMmQ3ZWYyOGU3ZjJmZGI4NTdhYTAxZGNlMjNlMTViMmRkZDM5ODM0MzJiMzE3NjA2OGU3OTEyNiJ9; FOLLOW_LIVE_POINT_INFO=%22MS4wLjABAAAAfirqDDJqwzCZ8YKzXTKpFMiPA844zPijmJw7FZkxO6Q%2F1759075200000%2F0%2F0%2F1759044613981%22; FOLLOW_NUMBER_YELLOW_POINT_INFO=%22MS4wLjABAAAAfirqDDJqwzCZ8YKzXTKpFMiPA844zPijmJw7FZkxO6Q%2F1759075200000%2F0%2F0%2F1759045213981%22; stream_recommend_feed_params=%22%7B%5C%22cookie_enabled%5C%22%3Atrue%2C%5C%22screen_width%5C%22%3A3440%2C%5C%22screen_height%5C%22%3A1440%2C%5C%22browser_online%5C%22%3Atrue%2C%5C%22cpu_core_num%5C%22%3A8%2C%5C%22device_memory%5C%22%3A8%2C%5C%22downlink%5C%22%3A10%2C%5C%22effective_type%5C%22%3A%5C%224g%5C%22%2C%5C%22round_trip_time%5C%22%3A150%7D%22; sdk_source_info=7e276470716a68645a606960273f276364697660272927676c715a6d6069756077273f276364697660272927666d776a68605a607d71606b766c6a6b5a7666776c7571273f275e58272927666a6b766a69605a696c6061273f27636469766027292762696a6764695a7364776c6467696076273f275e582729277672715a646971273f2763646976602729277f6b5a666475273f2763646976602729276d6a6e5a6b6a716c273f2763646976602729276c6b6f5a7f6367273f27636469766027292771273f273436343c36343131353c303234272927676c715a75776a716a666a69273f2763646976602778; bit_env=W9lmKGKwINnkyrQVYtJksJkvpqx6Nu4XZjMHLfXYjiNc8OLB9h7P15IkCBdL-QtyU5qpTZZqUsBlat4yClFR1qkreH1BEkxRkSozkzt3UVqadLn4G4ge2wIGew5356eRoZTKl6YvjMU-ojFM-97j7uC6G-Kn2hj6PiFTxaEcDNczZAz_RrZHAiXnLJyTrSWIwtr7-iZ0N956FzNTYeJnuCcNOrfQYhD2td_Djs2qib0CV9cusVdFjQKnq_5AbIqkRdsxmo5mwDczflWEWrx9Qybz-RCrqcV9ufi5JfTYVa7WbueJ44JwFAHehVPHkwvIjT5whkvhDpbUl-58f0uRsSGoAuEf63iwaVIEEvgmJZjBbwkZSpu21Ftrx6ILX0xBxZoILdCmDUluz6-PqiJBSr50FH0ogYouN6Hs_C_B4ObUFNEot6E034ecKxYBjqZDuo9vuUD2nBxl2BeMwhVb6QVIHn-qvFytWgIaE3WHS6noiXtKlPJwuY4ZMO39GV0my2OzuTFybygzUmskBLvAflGOe4LQDNx3MxjBnBWyjgk%3D; passport_auth_mix_state=ncjwuhed1n623a1aetcv9jf4uuos7g73; passport_assist_user=Cjz3Imdo7LeIkQtGeMkF2sktPbBfDg4tooVPVtKLfZuP9f3gxaHaNdWF5O8OcCSjBLXRIpmLy6EeCB4AUS4aSgo8AAAAAAAAAAAAAE-Hth5yEkW57pvbPSs5PzM3Fvf4IhY76uBtR6n9h0TvAcgUKujQA_jlJLjyME0PYiyCEOGx_Q0Yia_WVCABIgEDWQVsLQ%3D%3D; sid_guard=f9321232b62b215b2d31c9ec0a88786e%7C1759044173%7C5184000%7CThu%2C+27-Nov-2025+07%3A22%3A53+GMT; uid_tt=efa6301c9136957c98337f84c3fd4316; uid_tt_ss=efa6301c9136957c98337f84c3fd4316; sid_tt=f9321232b62b215b2d31c9ec0a88786e; sessionid=f9321232b62b215b2d31c9ec0a88786e; sessionid_ss=f9321232b62b215b2d31c9ec0a88786e; session_tlb_tag=sttt%7C11%7C-TISMrYrIVstMcnsCoh4bv_________ryo10WdsYVXecHpQPEQL8w1TXYVvYDKZ_L-N6m2B8a90%3D; sid_ucp_v1=1.0.0-KDU3YTBhMDIyNGZmMWE4ODkxM2UxMDU4ODI5ZjU0MWJmZjhkYTcyZGEKHwjA__bPyAIQzcTjxgYY7zEgDDCb363TBTgFQPsHSAQaAmhsIiBmOTMyMTIzMmI2MmIyMTViMmQzMWM5ZWMwYTg4Nzg2ZQ; ssid_ucp_v1=1.0.0-KDU3YTBhMDIyNGZmMWE4ODkxM2UxMDU4ODI5ZjU0MWJmZjhkYTcyZGEKHwjA__bPyAIQzcTjxgYY7zEgDDCb363TBTgFQPsHSAQaAmhsIiBmOTMyMTIzMmI2MmIyMTViMmQzMWM5ZWMwYTg4Nzg2ZQ; login_time=1759044173094; __security_mc_1_s_sdk_sign_data_key_web_protect=f35cec24-47f3-9be0; _bd_ticket_crypt_cookie=8db126765d2828d433db0ae43b18dfe5; IsDouyinActive=true; bd_ticket_guard_client_data=eyJiZC10aWNrZXQtZ3VhcmQtdmVyc2lvbiI6MiwiYmQtdGlja2V0LWd1YXJkLWl0ZXJhdGlvbi12ZXJzaW9uIjoxLCJiZC10aWNrZXQtZ3VhcmQtcmVlLXB1YmxpYy1rZXkiOiJCQUpDKzl1L0pILzhwZkY0OFZ1SWs2MC9Pdk9RVjU3KzZtamRUcWdrK2ZmcFN4RFk4SURNc0pxbGFndTZlNFI1MFRLNHJEYUg3VG9DalVlbHY3MjBCQ2s9IiwiYmQtdGlja2V0LWd1YXJkLXdlYi12ZXJzaW9uIjoyfQ%3D%3D; home_can_add_dy_2_desktop=%221%22; ttwid=1%7CFSK6B0vNqaD9DchgUF_WyFtdac7Jj6FKi0PzDwVn9J4%7C1759044181%7Ca8c1f118004fda1c31bdf709c922646b6f436f190d0a6098db4a43828556b203; biz_trace_id=3215d360; playRecommendGuideTagCount=13; totalRecommendGuideTagCount=13; odin_tt=74498b01f6a8e69bb1c9dead0b9161f5802e9064be6a30ab2b2764705e20754078abc399beabeaea6df40699b9da5ba31b088b25774f045210351562025269cfa9e81f6f4ec0c5e3a98d5ddadcca4855")
                .userId("88214454208")
//                .webId("7553563806136698431")
//                .uifid("f718f562fcd874811d9c30568517194c189689a7c74491d0ed9c7c2e831358f1d1812dc1b701b679156cb1a5438080a259a88ee53317dbc5e3dae356758512a5bd4dcb31d638dd951eed6273bccc1320e53b028b6976970f86a72abf439129a630c55c74425b1f1fa5eeb96950d68133d2eb9a12c825147a0e5d1ce365df685032b5d5a4f59c627316dd3dbfc2cfda5dde54e6a95d9f10dc1b37f2f6c394b137")
//                .fp("verify_mfxp8wx5_cXZv6rsf_EneE_4H3Z_BGyV_wVmOUpHVRlNU")
//                .verifyFp("verify_mfxp8wx5_cXZv6rsf_EneE_4H3Z_BGyV_wVmOUpHVRlNU")
                .build();

        DouyinMessageSDK sdk = new DouyinMessageSDK(config);
        ResourceUploader resourceUploader = new ResourceUploader(config);
        CommentImageUploadAuthResponse uploadAuthResponse = resourceUploader.uploadCommentImgAuth();

        String accessKeyId = uploadAuthResponse.getAccessKey();
        String secretAccessKey = uploadAuthResponse.getSecretKey();
        String sessionToken = uploadAuthResponse.getSessionToken();
        String s = AWS4SignatureUtils.randomS();
        String[] times = AWS4SignatureGenerator.getCurrentUTCTime();
        String filePath = "/Users/a58/Desktop/封面.png";
        File file = new java.io.File(filePath);
        String fileSize = String.valueOf(file.length());
        int width = 0;
        int height = 0;
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
            if(image != null) {
                width = image.getWidth();
                height = image.getHeight();
                log.info("图片尺寸: {}x{}", width, height);
            }
        } catch(IOException e) {
            log.warn("无法读取图片尺寸: {}", e.getMessage());
        }

        String authorization = ImageUploadAuthUtil.generateCommentImageUploadAuthorization(
                accessKeyId,
                secretAccessKey,
                sessionToken,
                "",
                s,
                fileSize,
                times
        );
        log.info("authorization: {}", authorization);
        UploadAddressResponse resourceUploadAddress = resourceUploader.getCommentImgResourceUploadAddress(authorization, sessionToken, s, fileSize, "", times[0]);
        UploadFileResponse uploadFileResponse = resourceUploader.uploadFile(resourceUploadAddress, filePath);
        String sessionKey = resourceUploadAddress.getInnerUploadAddress().getSessionKey();
        CommitUploadResponse result = resourceUploader.commitCommentImgUpload(sessionKey, accessKeyId, secretAccessKey, sessionToken, times);
        String uri = result.getResults().get(0).getEncryption().getUri();
        String md5 = result.getResults().get(0).getEncryption().getSourceMd5();
        String skey = result.getResults().get(0).getEncryption().getSecretKey();
        String conversationId = "0:1:88214454208:4213783596110564";
        Long shortId = 7473797002657038875L;
//        String conversationId = "0:1:88214454208:3296813943621225";
//        Long shortId = 7472697347256959498L;
//        sdk.sendImageMessage(conversationId,shortId,md5, skey, uri,Integer.parseInt(fileSize),height,width).get();
        log.info("结束");


    }

    /**
     * 上传认证响应实体
     */
    @Data
    public class UploadAuthResponse {
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        private String ak;
        private String expiredTime;
        private String currentTime;
        private String spaceName;
    }

    @Data
    public static class CommentImageUploadAuthResponse {
        private long expiredTime;
        private String serviceId;
        private int statusCode;
        private String uploadDomain;
        private String accessKey;
        private String secretKey;
        private String sessionToken;
        private long currentTime;
        private Extra extra;
        private LogPb logPb;

        @Data
        public static class Extra {
            private long now;
            private String[] fatalItemIds;
            private String logid;
        }

        @Data
        public static class LogPb {
            private String imprId;
        }
    }



    @Data
    public static class UploadImageResponse {
        private String requestId;
        private String uploadAddress;
        private String storeUri;
        private String sessionKey;
    }

    /**
     * 上传地址响应
     */
    @Data
    public static class UploadAddressResponse {
        private String requestId;
        private String action;
        private String version;
        private String service;
        private String region;
        private UploadAddressInfo uploadAddress;
        private InnerUploadAddressInfo innerUploadAddress;
    }

    /**
     * 上传地址信息
     */
    @Data
    public static class UploadAddressInfo {
        private StoreInfo storeInfo;
        private String uploadHost;
        private String sessionKey;
    }

    /**
     * 内部上传地址信息
     */
    @Data
    public static class InnerUploadAddressInfo {
        private StoreInfo storeInfo;
        private String uploadHost;
        private String sessionKey;
    }

    /**
     * 存储信息
     */
    @Data
    public static class StoreInfo {
        private String storeUri;
        private String auth;
        private String uploadID;
        private String userId;
    }

    /**
     * 文件上传响应
     */
    @Data
    public static class UploadFileResponse {
        private int code;
        private String apiVersion;
        private String message;
        private String crc32;

        /**
         * 判断上传是否成功
         */
        public boolean isSuccess() {
            return code == 2000;
        }
    }

    @Data
    class CommitUploadRequest {
        @JsonProperty("SessionKey")
        private String sessionKey;

        @JsonProperty("Functions")
        private List<Function> functions;

        @Data
        public static class Function {
            @JsonProperty("name")
            private String name;

            @JsonProperty("input")
            private FunctionInput input;
        }

        @Data
        public static class FunctionInput {
            @JsonProperty("Config")
            private Config config;

            @JsonProperty("PolicyParams")
            private PolicyParams policyParams;
        }

        @Data
        public static class Config {
            @JsonProperty("copies")
            private String copies;
        }

        @Data
        public static class PolicyParams {
            @JsonProperty("policy-set")
            private String policySet;
        }
    }

    @Data
    public class CommitUploadResponse {
        private List<UploadResult> results;

        @Data
        public static class UploadResult {
            private String uri;
            private Integer uriStatus;
            private Encryption encryption;
            private PluginResult pluginResult; // 新增字段
        }

        @Data
        public static class Encryption {
            private String uri;
            private String secretKey;
            private String algorithm;
            private String version;
            private String sourceMd5;
        }

        @Data
        public static class PluginResult {
            private String fileName;
            private String sourceUri;
            private String imageUri;
            private int imageWidth;
            private int imageHeight;
            private String imageMd5;
            private String imageFormat;
            private int imageSize;
            private int frameCnt;
        }

        /**
         * 判断是否成功
         */
        public boolean isSuccess() {
            return results != null &&
                    ! results.isEmpty() &&
                    results.stream().allMatch(r -> r.getUriStatus() == 2000);
        }

        /**
         * 获取第一个上传结果的URI
         */
        public String getFirstUri() {
            if(isSuccess()) {
                return results.get(0).getUri();
            }
            return null;
        }

        /**
         * 获取第一个上传结果的加密URI
         */
        public String getFirstEncryptionUri() {
            if(isSuccess() && results.get(0).getEncryption() != null) {
                return results.get(0).getEncryption().getUri();
            }
            return null;
        }

        /**
         * 获取第一个结果的加密密钥
         */
        public String getFirstSecretKey() {
            if(isSuccess() && results.get(0).getEncryption() != null) {
                return results.get(0).getEncryption().getSecretKey();
            }
            return null;
        }
    }

}

