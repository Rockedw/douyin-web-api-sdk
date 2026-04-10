package com.dy_web_api.sdk.message.handler;

import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.model.StrangerMessageInfo;
import com.dy_web_api.sdk.message.protobuf.DyStrangerMsgRequest;
import com.dy_web_api.sdk.message.protobuf.DyStrangerMsgResponseOuterClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 陌生人消息获取器
 */
@Slf4j
public class StrangerMessageFetcher {

    private final DouyinConfig config;
    private final HttpClient httpClient;

    public StrangerMessageFetcher(DouyinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }

    /**
     * 获取陌生人会话消息列表
     * @param count 拉取数量（默认20）
     * @return List<StrangerMessageInfo> 消息列表
     * @throws IOException 网络异常
     * @throws InterruptedException 线程异常
     */
    public List<StrangerMessageInfo> fetchStrangerMessages(int count) {
        try {
            // 参数校验
            if (count < 0 || count > 100) {
                throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "拉取数量必须在0-100之间");
            }
            
            // 构建请求体
            String base64Content = "COkHEN1OGgUxLjEuMyIxaGFzaC5KSU9zMGhTRDh4dGNIeW1PTVpNMlB3TmJtTDM2bU5GYWU2OUdBT21nenV3PSgDMAE6OjhhYTJkY2I6RGV0YWNoZWQ6IDhhYTJkY2I4OGI0MTUzODg4NTE2OGU0YWZiYmQyYjZiYWM4YWVmYjJCCcI+BggAEAEYAUoBMFoJZG91eWluX3BjehMKC3Nlc3Npb25fYWlkEgQ2MzgzehAKC3Nlc3Npb25fZGlkEgEwehUKCGFwcF9uYW1lEglkb3V5aW5fcGN6FQoPcHJpb3JpdHlfcmVnaW9uEgJjbnqDAQoKdXNlcl9hZ2VudBJ1TW96aWxsYS81LjAgKE1hY2ludG9zaDsgSW50ZWwgTWFjIE9TIFggMTBfMTVfNykgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzEzOC4wLjAuMCBTYWZhcmkvNTM3LjM2ehYKDmNvb2tpZV9lbmFibGVkEgR0cnVlehkKEGJyb3dzZXJfbGFuZ3VhZ2USBXpoLUNOehwKEGJyb3dzZXJfcGxhdGZvcm0SCE1hY0ludGVsehcKDGJyb3dzZXJfbmFtZRIHTW96aWxsYXqAAQoPYnJvd3Nlcl92ZXJzaW9uEm01LjAgKE1hY2ludG9zaDsgSW50ZWwgTWFjIE9TIFggMTBfMTVfNykgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzEzOC4wLjAuMCBTYWZhcmkvNTM3LjM2ehYKDmJyb3dzZXJfb25saW5lEgR0cnVlehQKDHNjcmVlbl93aWR0aBIEMzQ0MHoVCg1zY3JlZW5faGVpZ2h0EgQxNDQwej4KB3JlZmVyZXISM2h0dHBzOi8vd3d3LmRvdXlpbi5jb20vdXNlci9zZWxmP2Zyb21fdGFiX25hbWU9bWFpbnoeCg10aW1lem9uZV9uYW1lEg1Bc2lhL1NoYW5naGFpeg0KCGRldmljZUlkEgEwehwKBXdlYmlkEhM3MzYwOTcxNzMxMTMxNTY1NTgzejoKAmZwEjR2ZXJpZnlfbWQ0Y2wzbTZfWDlXb2lDT1ZfbW5vNl80SzFsX0J5NmRfdXhwV1ppOWVGWGdheg0KCGlzLXJldHJ5EgEwkAEEqgEKZG91eWluX3dlYrIBB3dlYl9zZGu6AYUBdHMuMi5kYmEyNDE3ZGI0MzFkZDgxZTQxM2M5ODU3MmYyMTRiMmM1YzFhNmE5YTJiODNkZTI0MTk5NTdlYjExNjljYjU0YzRmYmU4N2QyMzE5Y2YwNTMxODYyNGNlZGExNDkxMWNhNDA2ZGVkYmViZWRkYjJlMzBmY2U4ZDRmYTAyNTc1ZMIBfGNIVmlMa0pGYjBsMFEwWXJUVkpoV0hFcmFIVmlMMk5WYTFaSlpHY3dka3ByU21KdmVrMWFkbVZrVGpKUGFtZG5ka1pMY1N0UFRrSlVTa3RPZG1OeWJrWlpRMjlwY2toSFQwRnJkQzlqUjBWSFpubExLMEZDUVZSamR6MD0=";
            byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
            DyStrangerMsgRequest.DyStrangerConversationRequest originReq = DyStrangerMsgRequest.DyStrangerConversationRequest.parseFrom(decodedBytes);

            DyStrangerMsgRequest.ConversationQuery.Builder queryBuilder = originReq.getBody().getQuery1000().toBuilder();
            queryBuilder.setCount(count <= 0 ? 20 : count);

            DyStrangerMsgRequest.DyStrangerConversationRequest.Builder reqBuilder = originReq.toBuilder();
            DyStrangerMsgRequest.DyStrangerConversationRequest req =
                    reqBuilder.setBody(originReq.getBody().toBuilder().setQuery1000(queryBuilder.build()).build()).build();
            byte[] reqBytes = req.toByteArray();

            // 构建HTTP请求
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiBaseUrl() + "/v1/stranger/get_conversation_list"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(reqBytes))
                    .setHeader("Cookie", "sessionid=" + config.getSessionId() + ";sessionid_ss=" + config.getSessionId())
                    .setHeader("accept", "application/x-protobuf")
                    .setHeader("content-type", "application/x-protobuf")
                    .setHeader("user-agent", config.getUserAgent())
                    .setHeader("origin", "https://www.douyin.com")
                    .setHeader("referer", "https://www.douyin.com/")
                    .setHeader("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                    .setHeader("sec-ch-ua-mobile", "?0")
                    .setHeader("sec-ch-ua-platform", "\"macOS\"")
                    .setHeader("accept-language", "zh-CN,zh;q=0.9")
                    .setHeader("cache-control", "no-cache")
                    .setHeader("pragma", "no-cache")
                    .setHeader("priority", "u=1, i")
                    .setHeader("sec-fetch-dest", "empty")
                    .setHeader("sec-fetch-mode", "cors")
                    .setHeader("sec-fetch-site", "same-site")
                    .build();

            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            
            // 处理HTTP错误
            if (response.statusCode() != 200) {
                handleFetchMessageHttpError(response.statusCode());
            }

            // 解析protobuf响应
            DyStrangerMsgResponseOuterClass.DyStrangerMsgResponse pbResponse =
                    DyStrangerMsgResponseOuterClass.DyStrangerMsgResponse.parseFrom(response.body());

            // 检查响应状态
            if (!"OK".equals(pbResponse.getStatus())) {
                String errorMsg = "获取陌生人消息失败: " + pbResponse.getStatus();
                log.error(errorMsg);
                throw new DouyinMessageException(ErrorCode.MESSAGE_SEND_FAILED, errorMsg);
            }

            // 转为结构化结果
            List<StrangerMessageInfo> result = new ArrayList<>();
            if (pbResponse.hasData() && pbResponse.getData().hasMessageInfo()) {
                List<DyStrangerMsgResponseOuterClass.MessageContent> contentList =
                        pbResponse.getData().getMessageInfo().getContentList();
                for (DyStrangerMsgResponseOuterClass.MessageContent messageContent : contentList) {
                    StrangerMessageInfo info = new StrangerMessageInfo();
                    info.setConversationShortId(messageContent.getConversationShortId());
                    info.setConversationId(messageContent.getConversationId());
                    info.setContentJson(messageContent.getDetail().getTextContent());
                    info.setSenderId(messageContent.getUsersCount() > 1 ? messageContent.getUsers(1).getUserId() : null);
                    info.setReceiverId(messageContent.getUsersCount() > 0 ? messageContent.getUsers(0).getUserId() : null);
                    info.setSenderSecUid(messageContent.getUsersCount() > 1 ? messageContent.getUsers(1).getSecUid() : null);
                    info.setReceiverSecUid(messageContent.getUsersCount() > 0 ? messageContent.getUsers(0).getSecUid() : null);
                    result.add(info);
                }
            }
            
            log.info("获取陌生人消息成功，数量: {}", result.size());
            return result;
            
        } catch (DouyinMessageException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            String errorMsg = "获取陌生人消息超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch (java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch (IOException e) {
            String errorMsg = "获取陌生人消息网络异常: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "获取陌生人消息被中断";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "获取陌生人消息失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }
    
    /**
     * 处理获取消息HTTP错误
     */
    private void handleFetchMessageHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;
        
        switch (statusCode) {
            case 400:
                errorCode = ErrorCode.INVALID_PARAMETER;
                errorMsg = "获取消息参数错误";
                break;
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "获取消息认证失败，请检查sessionId";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "获取消息访问被拒绝";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "获取消息请求频率超限";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "消息服务不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "获取消息服务超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "获取消息HTTP请求失败，状态码: " + statusCode;
        }
        
        throw new DouyinMessageException(errorCode, errorMsg);
    }
}